package com.simibubi.create.content.contraptions.mounted;

import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity.Type;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import com.simibubi.create.foundation.utility.AdventureUtil;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionData;
import com.simibubi.create.content.contraptions.ContraptionMovementSetting;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;

public class MinecartContraptionItem extends Item {

	private final AbstractMinecartEntity.Type minecartType;

	public static MinecartContraptionItem rideable(Settings builder) {
		return new MinecartContraptionItem(Type.RIDEABLE, builder);
	}

	public static MinecartContraptionItem furnace(Settings builder) {
		return new MinecartContraptionItem(Type.FURNACE, builder);
	}

	public static MinecartContraptionItem chest(Settings builder) {
		return new MinecartContraptionItem(Type.CHEST, builder);
	}

	@Override
	public boolean canBeNested() {
		return AllConfigs.server().kinetics.minecartContraptionInContainers.get();
	}

	private MinecartContraptionItem(Type minecartTypeIn, Settings builder) {
		super(builder);
		this.minecartType = minecartTypeIn;
		DispenserBlock.registerBehavior(this, DISPENSER_BEHAVIOR);
	}

	// Taken and adjusted from MinecartItem
	private static final DispenserBehavior DISPENSER_BEHAVIOR = new ItemDispenserBehavior() {
		private final ItemDispenserBehavior behaviourDefaultDispenseItem = new ItemDispenserBehavior();

		@Override
		public ItemStack dispenseSilently(BlockPointer source, ItemStack stack) {
			if (!canPlace())
				return behaviourDefaultDispenseItem.dispense(source, stack);

			Direction direction = source.getBlockState()
				.get(DispenserBlock.FACING);
			World world = source.getWorld();
			double d0 = source.getX() + (double) direction.getOffsetX() * 1.125D;
			double d1 = Math.floor(source.getY()) + (double) direction.getOffsetY();
			double d2 = source.getZ() + (double) direction.getOffsetZ() * 1.125D;
			BlockPos blockpos = source.getPos()
				.offset(direction);
			BlockState blockstate = world.getBlockState(blockpos);
			RailShape railshape = blockstate.getBlock() instanceof AbstractRailBlock
				? MinecartAndRailUtil.getDirectionOfRail(blockstate, world, blockpos, null)
				: RailShape.NORTH_SOUTH;
			double d3;
			if (blockstate.isIn(BlockTags.RAILS)) {
				if (railshape.isAscending()) {
					d3 = 0.6D;
				} else {
					d3 = 0.1D;
				}
			} else {
				if (!blockstate.isAir() || !world.getBlockState(blockpos.down())
					.isIn(BlockTags.RAILS)) {
					return this.behaviourDefaultDispenseItem.dispense(source, stack);
				}

				BlockState blockstate1 = world.getBlockState(blockpos.down());
				RailShape railshape1 = blockstate1.getBlock() instanceof AbstractRailBlock
					? MinecartAndRailUtil.getDirectionOfRail(blockstate1, world, blockpos.down(),
						null)
					: RailShape.NORTH_SOUTH;
				if (direction != Direction.DOWN && railshape1.isAscending()) {
					d3 = -0.4D;
				} else {
					d3 = -0.9D;
				}
			}

			AbstractMinecartEntity abstractminecartentity = AbstractMinecartEntity.create(world, d0, d1 + d3, d2,
				((MinecartContraptionItem) stack.getItem()).minecartType);
			if (stack.hasCustomName())
				abstractminecartentity.setCustomName(stack.getName());
			world.spawnEntity(abstractminecartentity);
			addContraptionToMinecart(world, stack, abstractminecartentity, direction);

			stack.decrement(1);
			return stack;
		}

		@Override
		protected void playSound(BlockPointer source) {
			source.getWorld()
				.syncWorldEvent(1000, source.getPos(), 0);
		}
	};

	// Taken and adjusted from MinecartItem
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos blockpos = context.getBlockPos();
		BlockState blockstate = world.getBlockState(blockpos);
		if (!blockstate.isIn(BlockTags.RAILS)) {
			return ActionResult.FAIL;
		} else if (!canPlace()) {
			PlayerEntity player = context.getPlayer();
			if (player != null) {
				Text message = Lang.translateDirect("contraption.minecart_contraption_illegal_placement").formatted(Formatting.RED);
				player.sendMessage(message, true);
			}
			return ActionResult.FAIL;
		} else {
			ItemStack itemstack = context.getStack();
			if (!world.isClient) {
				RailShape railshape = blockstate.getBlock() instanceof AbstractRailBlock
					? MinecartAndRailUtil.getDirectionOfRail(blockstate, world, blockpos, null)
					: RailShape.NORTH_SOUTH;
				double d0 = 0.0D;
				if (railshape.isAscending()) {
					d0 = 0.5D;
				}

				AbstractMinecartEntity abstractminecartentity =
					AbstractMinecartEntity.create(world, (double) blockpos.getX() + 0.5D,
						(double) blockpos.getY() + 0.0625D + d0, (double) blockpos.getZ() + 0.5D, this.minecartType);
				if (itemstack.hasCustomName())
					abstractminecartentity.setCustomName(itemstack.getName());
				PlayerEntity player = context.getPlayer();
				world.spawnEntity(abstractminecartentity);
				addContraptionToMinecart(world, itemstack, abstractminecartentity,
					player == null ? null : player.getHorizontalFacing());
			}

			itemstack.decrement(1);
			return ActionResult.SUCCESS;
		}
	}

	// fabric: temp fix for command smuggling for Blanketcon
	private static boolean canPlace() {
		return AllConfigs.server().kinetics.contraptionPlacing.get();
	}

	public static void addContraptionToMinecart(World world, ItemStack itemstack, AbstractMinecartEntity cart,
		@Nullable Direction newFacing) {
		NbtCompound tag = itemstack.getOrCreateNbt();
		if (tag.contains("Contraption")) {
			NbtCompound contraptionTag = tag.getCompound("Contraption");

			Direction intialOrientation = NBTHelper.readEnum(contraptionTag, "InitialOrientation", Direction.class);

			Contraption mountedContraption = Contraption.fromNBT(world, contraptionTag, false);
			OrientedContraptionEntity contraptionEntity =
				newFacing == null ? OrientedContraptionEntity.create(world, mountedContraption, intialOrientation)
					: OrientedContraptionEntity.createAtYaw(world, mountedContraption, intialOrientation,
						newFacing.asRotation());

			contraptionEntity.startRiding(cart);
			contraptionEntity.setPosition(cart.getX(), cart.getY(), cart.getZ());
			world.spawnEntity(contraptionEntity);
		}
	}

	@Override
	public String getTranslationKey(ItemStack stack) {
		return "item.create.minecart_contraption";
	}

	public static ActionResult wrenchCanBeUsedToPickUpMinecartContraptions(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (player == null || entity == null)
			return ActionResult.PASS;
		if (!AllConfigs.server().kinetics.survivalContraptionPickup.get() && !player.isCreative())
			return ActionResult.PASS;

		if (player.isSpectator()) // forge checks this, fabric does not
			return ActionResult.PASS;
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		ItemStack wrench = player.getStackInHand(hand);
		if (!AllItems.WRENCH.isIn(wrench))
			return ActionResult.PASS;
		if (entity instanceof AbstractContraptionEntity)
			entity = entity.getVehicle();
		if (!(entity instanceof AbstractMinecartEntity))
			return ActionResult.PASS;
		if (!entity.isAlive())
			return ActionResult.PASS;
		if (player instanceof DeployerFakePlayer dfp && dfp.onMinecartContraption)
			return ActionResult.PASS;
		AbstractMinecartEntity cart = (AbstractMinecartEntity) entity;
		Type type = cart.getMinecartType();
		if (type != Type.RIDEABLE && type != Type.FURNACE && type != Type.CHEST)
			return ActionResult.PASS;
		List<Entity> passengers = cart.getPassengerList();
		if (passengers.isEmpty() || !(passengers.get(0) instanceof OrientedContraptionEntity))
			return ActionResult.PASS;
		OrientedContraptionEntity oce = (OrientedContraptionEntity) passengers.get(0);
		Contraption contraption = oce.getContraption();

		if (ContraptionMovementSetting.isNoPickup(contraption.getBlocks()
			.values())) {
			player.sendMessage(Lang.translateDirect("contraption.minecart_contraption_illegal_pickup")
				.formatted(Formatting.RED), true);
			return ActionResult.PASS;
		}

		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		contraption.stop(world);

		for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors())
			if (AllMovementBehaviours.getBehaviour(pair.left.state())instanceof PortableStorageInterfaceMovement psim)
				psim.reset(pair.right);

		ItemStack generatedStack = create(type, oce).setCustomName(entity.getCustomName());

		if (ContraptionData.isTooLargeForPickup(generatedStack.writeNbt(new NbtCompound()))) {
			MutableText message = Lang.translateDirect("contraption.minecart_contraption_too_big")
					.formatted(Formatting.RED);
			player.sendMessage(message, true);
			return ActionResult.PASS;
		}

		if (contraption.getBlocks()
			.size() > 200)
			AllAdvancements.CART_PICKUP.awardTo(player);

		player.getInventory()
			.offerOrDrop(generatedStack);
		oce.discard();
		entity.discard();
		return ActionResult.SUCCESS;
	}

	public static ItemStack create(Type type, OrientedContraptionEntity entity) {
		ItemStack stack = ItemStack.EMPTY;

		switch (type) {
		case RIDEABLE:
			stack = AllItems.MINECART_CONTRAPTION.asStack();
			break;
		case FURNACE:
			stack = AllItems.FURNACE_MINECART_CONTRAPTION.asStack();
			break;
		case CHEST:
			stack = AllItems.CHEST_MINECART_CONTRAPTION.asStack();
			break;
		default:
			break;
		}

		if (stack.isEmpty())
			return stack;

		NbtCompound tag = entity.getContraption()
			.writeNBT(false);
		tag.remove("UUID");
		tag.remove("Pos");
		tag.remove("Motion");

		NBTHelper.writeEnum(tag, "InitialOrientation", entity.getInitialOrientation());

		stack.getOrCreateNbt()
			.put("Contraption", tag);
		return stack;
	}
}
