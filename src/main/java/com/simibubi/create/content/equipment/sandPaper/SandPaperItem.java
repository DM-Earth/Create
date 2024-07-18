package com.simibubi.create.content.equipment.sandPaper;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.CustomUseEffectsItem;
import com.simibubi.create.foundation.mixin.accessor.LivingEntityAccessor;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.AxeItemAccessor;
import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SandPaperItem extends Item implements CustomUseEffectsItem {

	public SandPaperItem(Settings properties) {
		super(properties.maxDamageIfAbsent(8));
	}

	@Override
	public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack itemstack = playerIn.getStackInHand(handIn);
		TypedActionResult<ItemStack> FAIL = new TypedActionResult<>(ActionResult.FAIL, itemstack);

		if (itemstack.getOrCreateNbt()
			.contains("Polishing")) {
			playerIn.setCurrentHand(handIn);
			return new TypedActionResult<>(ActionResult.PASS, itemstack);
		}

		Hand otherHand =
			handIn == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
		ItemStack itemInOtherHand = playerIn.getStackInHand(otherHand);
		if (SandPaperPolishingRecipe.canPolish(worldIn, itemInOtherHand)) {
			ItemStack item = itemInOtherHand.copy();
			ItemStack toPolish = item.split(1);
			playerIn.setCurrentHand(handIn);
			itemstack.getOrCreateNbt()
				.put("Polishing", NBTSerializer.serializeNBT(toPolish));
			playerIn.setStackInHand(otherHand, item);
			return new TypedActionResult<>(ActionResult.SUCCESS, itemstack);
		}

		HitResult raytraceresult = raycast(worldIn, playerIn, RaycastContext.FluidHandling.NONE);
		if (!(raytraceresult instanceof BlockHitResult))
			return FAIL;
		BlockHitResult ray = (BlockHitResult) raytraceresult;
		Vec3d hitVec = ray.getPos();

		Box bb = new Box(hitVec, hitVec).expand(1f);
		ItemEntity pickUp = null;
		for (ItemEntity itemEntity : worldIn.getNonSpectatingEntities(ItemEntity.class, bb)) {
			if (!itemEntity.isAlive())
				continue;
			if (itemEntity.getPos()
				.distanceTo(playerIn.getPos()) > 3)
				continue;
			ItemStack stack = itemEntity.getStack();
			if (!SandPaperPolishingRecipe.canPolish(worldIn, stack))
				continue;
			pickUp = itemEntity;
			break;
		}

		if (pickUp == null)
			return FAIL;

		ItemStack item = pickUp.getStack()
			.copy();
		ItemStack toPolish = item.split(1);

		playerIn.setCurrentHand(handIn);

		if (!worldIn.isClient) {
			itemstack.getOrCreateNbt()
				.put("Polishing", NBTSerializer.serializeNBT(toPolish));
			if (item.isEmpty())
				pickUp.discard();
			else
				pickUp.setStack(item);
		}

		return new TypedActionResult<>(ActionResult.SUCCESS, itemstack);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		if (!(entityLiving instanceof PlayerEntity))
			return stack;
		PlayerEntity player = (PlayerEntity) entityLiving;
		NbtCompound tag = stack.getOrCreateNbt();
		if (tag.contains("Polishing")) {
			ItemStack toPolish = ItemStack.fromNbt(tag.getCompound("Polishing"));
			ItemStack polished =
				SandPaperPolishingRecipe.applyPolish(worldIn, entityLiving.getPos(), toPolish, stack);

			if (worldIn.isClient) {
				spawnParticles(entityLiving.getCameraPosVec(1)
					.add(entityLiving.getRotationVector()
						.multiply(.5f)),
					toPolish, worldIn);
				return stack;
			}

			if (!polished.isEmpty()) {
				if (player instanceof FakePlayer) {
					player.dropItem(polished, false, false);
				} else {
					player.getInventory()
						.offerOrDrop(polished);
				}
			}
			tag.remove("Polishing");
			stack.damage(1, entityLiving, p -> p.sendToolBreakStatus(p.getActiveHand()));
		}

		return stack;
	}

	public static void spawnParticles(Vec3d location, ItemStack polishedStack, World world) {
		for (int i = 0; i < 20; i++) {
			Vec3d motion = VecHelper.offsetRandomly(Vec3d.ZERO, world.random, 1 / 8f);
			world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, polishedStack), location.x, location.y,
				location.z, motion.x, motion.y, motion.z);
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
		if (!(entityLiving instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) entityLiving;
		NbtCompound tag = stack.getOrCreateNbt();
		if (tag.contains("Polishing")) {
			ItemStack toPolish = ItemStack.fromNbt(tag.getCompound("Polishing"));
			player.getInventory()
				.offerOrDrop(toPolish);
			tag.remove("Polishing");
		}
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		World level = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = level.getBlockState(pos);
		AxeItemAccessor access = (AxeItemAccessor) Items.DIAMOND_AXE;
		Optional<BlockState> newState = access.porting_lib$getStripped(state);
		if (newState.isPresent()) {
			AllSoundEvents.SANDING_LONG.play(level, player, pos, 1, 1 + (level.random.nextFloat() * 0.5f - 1f) / 5f);
			level.syncWorldEvent(player, 3005, pos, 0); // Spawn particles
		} else {
			newState = Oxidizable.getDecreasedOxidationState(state);
			if (newState.isEmpty()) { // fabric: account for waxing
				newState = Optional.ofNullable((HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get()).get(state.getBlock()))
						.map(block -> block.getStateWithProperties(state));
			}
			if (newState.isPresent()) {
				AllSoundEvents.SANDING_LONG.play(level, player, pos, 1,
					1 + (level.random.nextFloat() * 0.5f - 1f) / 5f);
				level.syncWorldEvent(player, 3004, pos, 0); // Spawn particles
			}
		}

		if (newState.isPresent()) {
			level.setBlockState(pos, newState.get());
			if (player != null)
				stack.damage(1, player, p -> p.sendToolBreakStatus(p.getActiveHand()));
			return ActionResult.success(level.isClient);
		}

		return ActionResult.PASS;
	}

//	@Override
//	public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
//		return toolAction == ToolActions.AXE_SCRAPE || toolAction == ToolActions.AXE_WAX_OFF;
//	}

	@Override
	public Boolean shouldTriggerUseEffects(ItemStack stack, LivingEntity entity) {
		// Trigger every tick so that we have more fine grain control over the animation
		return true;
	}

	@Override
	public boolean triggerUseEffects(ItemStack stack, LivingEntity entity, int count, Random random) {
		NbtCompound tag = stack.getOrCreateNbt();
		if (tag.contains("Polishing")) {
			ItemStack polishing = ItemStack.fromNbt(tag.getCompound("Polishing"));
			((LivingEntityAccessor) entity).create$callSpawnItemParticles(polishing, 1);
		}

		// After 6 ticks play the sound every 7th
		if ((entity.getItemUseTime() - 6) % 7 == 0)
			entity.playSound(entity.getEatSound(stack), 0.9F + 0.2F * random.nextFloat(),
				random.nextFloat() * 0.2F + 0.9F);

		return true;
	}

	@Override
	public SoundEvent getEatSound() {
		return AllSoundEvents.SANDING_SHORT.getMainEvent();
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.EAT;
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 32;
	}

	@Override
	public int getEnchantability() {
		return 1;
	}

//	@Override
//	@OnlyIn(Dist.CLIENT)
//	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new SandPaperItemRenderer()));
//	}

}
