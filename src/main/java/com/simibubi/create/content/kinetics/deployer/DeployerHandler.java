package com.simibubi.create.content.kinetics.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.BucketItemAccessor;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Multimap;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.Mode;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;

import io.github.fabricators_of_create.porting_lib.item.UseFirstBehaviorItem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class DeployerHandler {

	private static final class ItemUseWorld extends WrappedWorld {
		private final Direction face;
		private final BlockPos pos;
		boolean rayMode = false;

		private ItemUseWorld(World world, Direction face, BlockPos pos) {
			super(world);
			this.face = face;
			this.pos = pos;
		}

		@Override
		public BlockHitResult raycast(RaycastContext context) {
			rayMode = true;
			BlockHitResult rayTraceBlocks = super.raycast(context);
			rayMode = false;
			return rayTraceBlocks;
		}

		@Override
		public BlockState getBlockState(BlockPos position) {
			if (rayMode && (pos.offset(face.getOpposite(), 3)
				.equals(position)
				|| pos.offset(face.getOpposite(), 1)
					.equals(position)))
				return Blocks.BEDROCK.getDefaultState();
			return world.getBlockState(position);
		}
	}

	static boolean shouldActivate(ItemStack held, World world, BlockPos targetPos, @Nullable Direction facing) {
		if (held.getItem() instanceof BlockItem)
			if (world.getBlockState(targetPos)
				.getBlock() == ((BlockItem) held.getItem()).getBlock())
				return false;

		if (held.getItem() instanceof BucketItem) {
			BucketItem bucketItem = (BucketItem) held.getItem();
			Fluid fluid = ((BucketItemAccessor) bucketItem).port_lib$getContent();
			if (fluid != Fluids.EMPTY && world.getFluidState(targetPos)
				.getFluid() == fluid)
				return false;
		}

		if (!held.isEmpty() && facing == Direction.DOWN
			&& BlockEntityBehaviour.get(world, targetPos, TransportedItemStackHandlerBehaviour.TYPE) != null)
			return false;

		return true;
	}

	static void activate(DeployerFakePlayer player, Vec3d vec, BlockPos clickedPos, Vec3d extensionVector, Mode mode) {
		Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers = player.getMainHandStack()
			.getAttributeModifiers(EquipmentSlot.MAINHAND);
		player.getAttributes()
			.addTemporaryModifiers(attributeModifiers);
		activateInner(player, vec, clickedPos, extensionVector, mode);
		player.getAttributes()
			.addTemporaryModifiers(attributeModifiers);
	}

	private static void activateInner(DeployerFakePlayer player, Vec3d vec, BlockPos clickedPos, Vec3d extensionVector,
		Mode mode) {

		Vec3d rayOrigin = vec.add(extensionVector.multiply(3 / 2f + 1 / 64f));
		Vec3d rayTarget = vec.add(extensionVector.multiply(5 / 2f - 1 / 64f));
		player.setPosition(rayOrigin.x, rayOrigin.y, rayOrigin.z);
		BlockPos pos = BlockPos.ofFloored(vec);
		ItemStack stack = player.getMainHandStack();
		Item item = stack.getItem();

		// Check for entities
		final World world = player.getWorld();
		List<Entity> entities = world.getNonSpectatingEntities(Entity.class, new Box(clickedPos))
			.stream()
			.filter(e -> !(e instanceof AbstractContraptionEntity))
			.collect(Collectors.toList());
		Hand hand = Hand.MAIN_HAND;
		if (!entities.isEmpty()) {
			Entity entity = entities.get(world.random.nextInt(entities.size()));
			List<ItemEntity> capturedDrops = new ArrayList<>();
			boolean success = false;
			entity.captureDrops(capturedDrops);

			// Use on entity
			if (mode == Mode.USE) {
				ActionResult cancelResult = UseEntityCallback.EVENT.invoker().interact(player, world, hand, entity, new EntityHitResult(entity));
				if (cancelResult == ActionResult.FAIL) {
					entity.captureDrops(null);
					return;
				}
				if (cancelResult == null || cancelResult == ActionResult.PASS) {
					if (entity.interact(player, hand)
						.isAccepted()) {
						if (entity instanceof MerchantEntity) {
							MerchantEntity villager = ((MerchantEntity) entity);
							if (villager.getCustomer() instanceof DeployerFakePlayer)
								villager.setCustomer(null);
						}
						success = true;
					} else if (entity instanceof LivingEntity
						&& stack.useOnEntity(player, (LivingEntity) entity, hand)
							.isAccepted())
						success = true;
				}
				if (!success && entity instanceof PlayerEntity playerEntity) {
					if (stack.isFood()) {
						FoodComponent foodProperties = item.getFoodComponent();
						if (playerEntity.canConsume(foodProperties.isAlwaysEdible())) {
							playerEntity.eatFood(world, stack);
							player.spawnedItemEffects = stack.copy();
							success = true;
						}
					}
					if (AllItemTags.DEPLOYABLE_DRINK.matches(stack)) {
						player.spawnedItemEffects = stack.copy();
						player.setStackInHand(hand, stack.finishUsing(world, playerEntity));
						success = true;
					}
				}
			}

			// Punch entity
			if (mode == Mode.PUNCH) {
				player.resetLastAttackedTicks();
				player.attack(entity);
				success = true;
			}

			entity.captureDrops(null);
			capturedDrops.forEach(e -> player.getInventory()
					.offerOrDrop(e.getStack()));
			if (success)
				return;
		}

		// Shoot ray
		RaycastContext rayTraceContext =
			new RaycastContext(rayOrigin, rayTarget, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player);
		BlockHitResult result = world.raycast(rayTraceContext);
		if (result.getBlockPos() != clickedPos)
			result = new BlockHitResult(result.getPos(), result.getSide(), clickedPos, result.isInsideBlock());
		BlockState clickedState = world.getBlockState(clickedPos);
		Direction face = result.getSide();
		if (face == null)
			face = Direction.getFacing(extensionVector.x, extensionVector.y, extensionVector.z)
				.getOpposite();

		// Left click
		if (mode == Mode.PUNCH) {
			if (!world.canPlayerModifyAt(player, clickedPos))
				return;
			if (clickedState.getOutlineShape(world, clickedPos)
				.isEmpty()) {
				player.blockBreakingProgress = null;
				return;
			}
			ActionResult actionResult = UseBlockCallback.EVENT.invoker().interact(player, player.getWorld(), player.getActiveHand(), result);
			if (actionResult == ActionResult.FAIL)
				return;
			if (BlockHelper.extinguishFire(world, player, clickedPos, face))
				return;
//			if (actionResult != InteractionResult.FAIL) // fabric: checked above
			clickedState.onBlockBreakStart(world, clickedPos, player);
			if (stack.isEmpty())
				return;

			float progress = clickedState.calcBlockBreakingDelta(player, world, clickedPos) * 16;
			float before = 0;
			Pair<BlockPos, Float> blockBreakingProgress = player.blockBreakingProgress;
			if (blockBreakingProgress != null)
				before = blockBreakingProgress.getValue();
			progress += before;
			world.playSound(null, clickedPos, clickedState.getSoundGroup()
				.getHitSound(), SoundCategory.NEUTRAL, .25f, 1);

			if (progress >= 1) {
				tryHarvestBlock(player, player.interactionManager, clickedPos);
				world.setBlockBreakingInfo(player.getId(), clickedPos, -1);
				player.blockBreakingProgress = null;
				return;
			}
			if (progress <= 0) {
				player.blockBreakingProgress = null;
				return;
			}

			if ((int) (before * 10) != (int) (progress * 10))
				world.setBlockBreakingInfo(player.getId(), clickedPos, (int) (progress * 10));
			player.blockBreakingProgress = Pair.of(clickedPos, progress);
			return;
		}

		// Right click
		ItemUsageContext itemusecontext = new ItemUsageContext(player, hand, result);
		ActionResult useBlock = ActionResult.PASS;
		ActionResult useItem = ActionResult.PASS;
		if (!clickedState.getOutlineShape(world, clickedPos)
			.isEmpty()) {
			useBlock = UseBlockCallback.EVENT.invoker().interact(player, player.getWorld(), hand, result);
			useItem = useBlock;
		}

		// Item has custom active use
		if (useItem != ActionResult.FAIL && stack.getItem() instanceof UseFirstBehaviorItem first) {
			ActionResult actionresult = first.onItemUseFirst(stack, itemusecontext);
			if (actionresult != ActionResult.PASS)
				return;
		}

		boolean holdingSomething = !player.getMainHandStack()
			.isEmpty();
		boolean flag1 =
			!(player.isSneaking() && holdingSomething)/* || (stack.doesSneakBypassUse(world, clickedPos, player))*/;

		// Use on block
		if (useBlock != null && flag1
			&& safeOnUse(clickedState, world, clickedPos, player, hand, result).isAccepted())
			return;
		if (stack.isEmpty())
			return;
		if (useItem == null)
			return;

		// Reposition fire placement for convenience
		if (item == Items.FLINT_AND_STEEL) {
			Direction newFace = result.getSide();
			BlockPos newPos = result.getBlockPos();
			if (!AbstractFireBlock.canPlaceAt(world, clickedPos, newFace))
				newFace = Direction.UP;
			if (clickedState.isAir())
				newPos = newPos.offset(face.getOpposite());
			result = new BlockHitResult(result.getPos(), newFace, newPos, result.isInsideBlock());
			itemusecontext = new ItemUsageContext(player, hand, result);
		}

		// 'Inert' item use behaviour & block placement
		ActionResult onItemUse = stack.useOnBlock(itemusecontext);
		if (onItemUse.isAccepted()) {
			if (stack.getItem() instanceof BlockItem bi
				&& (bi.getBlock() instanceof AbstractRailBlock || bi.getBlock() instanceof ITrackBlock))
				player.placedTracks = true;
			return;
		}

		if (item instanceof BlockItem && !(item instanceof CartAssemblerBlockItem)
				&& !clickedState.canReplace(new ItemPlacementContext(itemusecontext)))
			return;
		if (item == Items.ENDER_PEARL)
			return;
		if (AllItemTags.DEPLOYABLE_DRINK.matches(item))
			return;

		// buckets create their own ray, We use a fake wall to contain the active area
		World itemUseWorld = world;
		if (item instanceof BucketItem || item instanceof SandPaperItem)
			itemUseWorld = new ItemUseWorld(world, face, pos);

		TypedActionResult<ItemStack> onItemRightClick = item.use(itemUseWorld, player, hand);
		ItemStack resultStack = onItemRightClick.getValue();
		if (resultStack != stack || resultStack.getCount() != stack.getCount() || resultStack.getMaxUseTime() > 0
			|| resultStack.getDamage() != stack.getDamage()) {
			player.setStackInHand(hand, onItemRightClick.getValue());
		}

		NbtCompound tag = stack.getNbt();
		if (tag != null && stack.getItem() instanceof SandPaperItem && tag.contains("Polishing")) {
			player.spawnedItemEffects = ItemStack.fromNbt(tag.getCompound("Polishing"));
			AllSoundEvents.SANDING_SHORT.playOnServer(world, pos, .25f, 1f);
		}

		if (!player.getActiveItem()
			.isEmpty())
			player.setStackInHand(hand, stack.finishUsing(world, player));

		player.clearActiveItem();
	}

	public static boolean tryHarvestBlock(ServerPlayerEntity player, ServerPlayerInteractionManager interactionManager, BlockPos pos) {
		// <> PlayerInteractionManager#tryHarvestBlock

		ServerWorld world = player.getServerWorld();
		BlockState blockstate = world.getBlockState(pos);
		GameMode gameType = interactionManager.getGameMode();

		if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), world.getBlockEntity(pos)))
			return false;

		BlockEntity blockEntity = world.getBlockEntity(pos);
//		if (player.getMainHandItem()
//			.onBlockStartBreak(pos, player))
//			return false;
		if (player.isBlockBreakingRestricted(world, pos, gameType))
			return false;

		ItemStack prevHeldItem = player.getMainHandStack();
		ItemStack heldItem = prevHeldItem.copy();

		boolean canHarvest = player.canHarvest(blockstate) && player.canModifyBlocks();
		prevHeldItem.postMine(world, blockstate, pos, player);
//		if (prevHeldItem.isEmpty() && !heldItem.isEmpty())
//			net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, heldItem, InteractionHand.MAIN_HAND);

		BlockPos posUp = pos.up();
		BlockState stateUp = world.getBlockState(posUp);
		if (blockstate.getBlock() instanceof TallPlantBlock
			&& blockstate.get(TallPlantBlock.HALF) == DoubleBlockHalf.LOWER
			&& stateUp.getBlock() == blockstate.getBlock()
			&& stateUp.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER) {
			// hack to prevent DoublePlantBlock from dropping a duplicate item
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), 35);
			world.setBlockState(posUp, Blocks.AIR.getDefaultState(), 35);
		} else {
			blockstate.getBlock().onBreak(world, pos, blockstate, player);
			if (!world.setBlockState(pos, world.getFluidState(pos).getFluid().getDefaultState().getBlockState(), world.isClient ? 11 : 3))
				return true;
		}

		blockstate.getBlock()
			.onBroken(world, pos, blockstate);
		if (!canHarvest)
			return true;

		Block.getDroppedStacks(blockstate, world, pos, blockEntity, player, prevHeldItem)
			.forEach(item -> player.getInventory().offerOrDrop(item));
		blockstate.onStacksDropped(world, pos, prevHeldItem, true);
		return true;
	}

	public static ActionResult safeOnUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
		Hand hand, BlockHitResult ray) {
		if (state.getBlock() instanceof BeehiveBlock)
			return safeOnBeehiveUse(state, world, pos, player, hand);
		return state.onUse(world, player, hand, ray);
	}

	protected static ActionResult safeOnBeehiveUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
		Hand hand) {
		// <> BeehiveBlock#onUse

		BeehiveBlock block = (BeehiveBlock) state.getBlock();
		ItemStack prevHeldItem = player.getStackInHand(hand);
		int honeyLevel = state.get(BeehiveBlock.HONEY_LEVEL);
		boolean success = false;
		if (honeyLevel < 5)
			return ActionResult.PASS;

		if (prevHeldItem.getItem() == Items.SHEARS) {
			world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEEHIVE_SHEAR,
				SoundCategory.NEUTRAL, 1.0F, 1.0F);
			// <> BeehiveBlock#dropHoneycomb
			player.getInventory().offerOrDrop(new ItemStack(Items.HONEYCOMB, 3));
			prevHeldItem.damage(1, player, s -> s.sendToolBreakStatus(hand));
			success = true;
		}

		if (prevHeldItem.getItem() == Items.GLASS_BOTTLE) {
			prevHeldItem.decrement(1);
			world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOTTLE_FILL,
				SoundCategory.NEUTRAL, 1.0F, 1.0F);
			ItemStack honeyBottle = new ItemStack(Items.HONEY_BOTTLE);
			if (prevHeldItem.isEmpty())
				player.setStackInHand(hand, honeyBottle);
			else
				player.getInventory().offerOrDrop(honeyBottle);
			success = true;
		}

		if (!success)
			return ActionResult.PASS;

		block.takeHoney(world, state, pos);
		return ActionResult.SUCCESS;
	}

}
