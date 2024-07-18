package com.simibubi.create.content.fluids;

import static net.minecraft.state.property.Properties.WATERLOGGED;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.tropheusj.milk.Milk;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class OpenEndedPipe extends FlowSource {

	private static final List<IEffectHandler> EFFECT_HANDLERS = new ArrayList<>();

	static {
		registerEffectHandler(new PotionEffectHandler());
		registerEffectHandler(new MilkEffectHandler());
		registerEffectHandler(new WaterEffectHandler());
		registerEffectHandler(new LavaEffectHandler());
		registerEffectHandler(new TeaEffectHandler());
	}

	private World world;
	private BlockPos pos;
	private Box aoe;

	private OpenEndFluidHandler fluidHandler;
	private BlockPos outputPos;
	private boolean wasPulling;

	private FluidStack cachedFluid;
	private List<StatusEffectInstance> cachedEffects;

	public OpenEndedPipe(BlockFace face) {
		super(face);
		fluidHandler = new OpenEndFluidHandler();
		outputPos = face.getConnectedPos();
		pos = face.getPos();
		aoe = new Box(outputPos).stretch(0, -1, 0);
		if (face.getFace() == Direction.DOWN)
			aoe = aoe.stretch(0, -1, 0);
	}

	public static void registerEffectHandler(IEffectHandler handler) {
		EFFECT_HANDLERS.add(handler);
	}

	public World getWorld() {
		return world;
	}

	public BlockPos getPos() {
		return pos;
	}

	public BlockPos getOutputPos() {
		return outputPos;
	}

	public Box getAOE() {
		return aoe;
	}

	@Override
	public void manageSource(World world) {
		this.world = world;
	}

	@Override
	public Storage<FluidVariant> provideHandler() {
		return fluidHandler;
	}

	@Override
	public boolean isEndpoint() {
		return true;
	}

	public NbtCompound serializeNBT() {
		NbtCompound compound = new NbtCompound();
		fluidHandler.writeToNBT(compound);
		compound.putBoolean("Pulling", wasPulling);
		compound.put("Location", location.serializeNBT());
		return compound;
	}

	public static OpenEndedPipe fromNBT(NbtCompound compound, BlockPos blockEntityPos) {
		BlockFace fromNBT = BlockFace.fromNBT(compound.getCompound("Location"));
		OpenEndedPipe oep = new OpenEndedPipe(new BlockFace(blockEntityPos, fromNBT.getFace()));
		oep.fluidHandler.readFromNBT(compound);
		oep.wasPulling = compound.getBoolean("Pulling");
		return oep;
	}

	private FluidStack removeFluidFromSpace(TransactionContext ctx) {
		FluidStack empty = FluidStack.EMPTY;
		if (world == null)
			return empty;
		if (!world.canSetBlock(outputPos))
			return empty;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.contains(WATERLOGGED);

		FluidStack drainBlock = VanillaFluidTargets.drainBlock(world, outputPos, state, ctx);
		if (!drainBlock.isEmpty()) {
			if (state.contains(Properties.HONEY_LEVEL)
				&& AllFluids.HONEY.is(drainBlock.getFluid()))
				TransactionCallback.onSuccess(ctx, () -> AdvancementBehaviour.tryAward(world, pos, AllAdvancements.HONEY_DRAIN));
			return drainBlock;
		}

		if (!waterlog && !state.isReplaceable())
			return empty;
		if (fluidState.isEmpty() || !fluidState.isStill())
			return empty;

		FluidStack stack = new FluidStack(fluidState.getFluid(), FluidConstants.BUCKET);

		if (FluidHelper.isWater(stack.getFluid()))
			AdvancementBehaviour.tryAward(world, pos, AllAdvancements.WATER_SUPPLY);

		world.updateSnapshots(ctx);
		if (waterlog) {
			world.setBlockState(outputPos, state.with(WATERLOGGED, false), 3);
			TransactionCallback.onSuccess(ctx, () -> world.scheduleFluidTick(outputPos, Fluids.WATER, 1));
			return stack;
		}
		world.setBlockState(outputPos, fluidState.getBlockState()
			.with(FluidBlock.LEVEL, 14), 3);
		return stack;
	}

	private boolean provideFluidToSpace(FluidStack fluid, TransactionContext ctx) {
		if (world == null)
			return false;
		if (!world.canSetBlock(outputPos))
			return false;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.contains(WATERLOGGED);

		if (!waterlog && !state.isReplaceable())
			return false;
		if (fluid.isEmpty())
			return false;
		if (!FluidHelper.hasBlockState(fluid.getFluid()) || fluid.getFluid().isIn(Milk.MILK_FLUID_TAG)) // fabric: milk logic is different
			return true;

		// fabric: note - this is possibly prone to issues but follows what forge does.
		// collisions completely ignore simulation / transactions.
		if (!fluidState.isEmpty() && fluidState.getFluid() != fluid.getFluid()) {
			FluidReactions.handlePipeSpillCollision(world, outputPos, fluid.getFluid(), fluidState);
			return false;
		}

		if (fluidState.isStill())
			return false;
		if (waterlog && fluid.getFluid() != Fluids.WATER)
			return false;

		if (world.getDimension()
			.ultrawarm() && FluidHelper.isTag(fluid, FluidTags.WATER)) {
			int i = outputPos.getX();
			int j = outputPos.getY();
			int k = outputPos.getZ();
			TransactionCallback.onSuccess(ctx, () -> world.playSound(null, i, j, k, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
					2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F));
			return true;
		}

		world.updateSnapshots(ctx);
		if (waterlog) {
			world.setBlockState(outputPos, state.with(WATERLOGGED, true), 3);
			TransactionCallback.onSuccess(ctx, () -> world.scheduleFluidTick(outputPos, Fluids.WATER, 1));
			return true;
		}

		if (!AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get())
			return true;

		world.setBlockState(outputPos, fluid.getFluid()
			.getDefaultState()
			.getBlockState(), 3);
		return true;
	}

	private boolean canApplyEffects(FluidStack fluid) {
		for (IEffectHandler handler : EFFECT_HANDLERS) {
			if (handler.canApplyEffects(this, fluid)) {
				return true;
			}
		}
		return false;
	}

	private void applyEffects(FluidStack fluid) {
		for (IEffectHandler handler : EFFECT_HANDLERS) {
			if (handler.canApplyEffects(this, fluid)) {
				handler.applyEffects(this, fluid);
			}
		}
	}

	private class OpenEndFluidHandler extends FluidTank {

		public OpenEndFluidHandler() {
			super(FluidConstants.BUCKET);
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			// Never allow being filled when a source is attached
			if (world == null)
				return 0;
			if (!world.canSetBlock(outputPos))
				return 0;
			if (resource.isBlank())
				return 0;
			FluidStack stack = new FluidStack(resource, 81);
			updateSnapshots(transaction);
			try (Transaction provideTest = transaction.openNested()) {
				if (!provideFluidToSpace(stack, provideTest))
					return 0;
			}

			FluidStack containedFluidStack = getFluid();
			boolean hasBlockState = FluidHelper.hasBlockState(containedFluidStack.getFluid());

			if (!containedFluidStack.isEmpty() && !containedFluidStack.canFill(resource))
				setFluid(FluidStack.EMPTY);
			if (wasPulling)
				wasPulling = false;

			if (canApplyEffects(stack) && !hasBlockState)
				maxAmount = 81; // fabric: deplete fluids 81 times faster to account for larger amounts
			long fill = super.insert(resource, maxAmount, transaction);
			if (!stack.isEmpty())
				TransactionCallback.onSuccess(transaction, () -> applyEffects(stack));
			if (getFluidAmount() == FluidConstants.BUCKET || (!FluidHelper.hasBlockState(containedFluidStack.getFluid()) || containedFluidStack.getFluid().isIn(Milk.MILK_FLUID_TAG))) { // fabric: milk logic is different
				if (provideFluidToSpace(containedFluidStack, transaction))
					setFluid(FluidStack.EMPTY);
			}
			return fill;
		}

		@Override
		public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction) {
			if (world == null)
				return 0;
			if (!world.canSetBlock(outputPos))
				return 0;
			if (maxAmount == 0)
				return 0;
			if (maxAmount > FluidConstants.BUCKET) {
				maxAmount = FluidConstants.BUCKET;
			}

			if (!wasPulling)
				wasPulling = true;

			updateSnapshots(transaction);
			long drainedFromInternal = super.extract(extractedVariant, maxAmount, transaction);
			if (drainedFromInternal != 0)
				return drainedFromInternal;

			FluidStack drainedFromWorld = removeFluidFromSpace(transaction);
			if (drainedFromWorld.isEmpty())
				return 0;
			if (!drainedFromWorld.canFill(extractedVariant))
				return 0;

			long remainder = drainedFromWorld.getAmount() - maxAmount;
			drainedFromWorld.setAmount(maxAmount);

			if (remainder > 0) {
				if (!getFluid().isEmpty() && !getFluid().isFluidEqual(drainedFromWorld))
					setFluid(FluidStack.EMPTY);
				super.insert(drainedFromWorld.getType(), remainder, transaction);
			}
			return drainedFromWorld.getAmount();
		}

		@Override
		public boolean isResourceBlank() {
			if (!super.isResourceBlank()) return false;
			return getResource().isBlank();
		}

		@Override
		public FluidVariant getResource() {
			if (!super.isResourceBlank()) return super.getResource();
			try (Transaction t = TransferUtil.getTransaction()) {
				FluidStack stack = removeFluidFromSpace(t);
				return stack.getType();
			}
		}

		@Override
		public long getAmount() {
			long amount = super.getAmount();
			if (amount != 0) return amount;
			return isResourceBlank() ? 0 : FluidConstants.BUCKET;
		}
	}

	public interface IEffectHandler {
		boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid);

		void applyEffects(OpenEndedPipe pipe, FluidStack fluid);
	}

	public static class PotionEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid()
				.matchesType(AllFluids.POTION.get());
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			if (pipe.cachedFluid == null || pipe.cachedEffects == null || !fluid.isFluidEqual(pipe.cachedFluid)) {
				FluidStack copy = fluid.copy();
				copy.setAmount(FluidConstants.BOTTLE);
				ItemStack bottle = PotionFluidHandler.fillBottle(new ItemStack(Items.GLASS_BOTTLE), fluid);
				pipe.cachedEffects = PotionUtil.getPotionEffects(bottle);
			}

			if (pipe.cachedEffects.isEmpty())
				return;

			List<LivingEntity> entities = pipe.getWorld()
				.getEntitiesByClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedBySplashPotions);
			for (LivingEntity entity : entities) {
				for (StatusEffectInstance effectInstance : pipe.cachedEffects) {
					StatusEffect effect = effectInstance.getEffectType();
					if (effect.isInstant()) {
						effect.applyInstantEffect(null, null, entity, effectInstance.getAmplifier(), 0.5D);
					} else {
						entity.addStatusEffect(new StatusEffectInstance(effectInstance));
					}
				}
			}
		}
	}

	public static class MilkEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return FluidHelper.isTag(fluid, Tags.Fluids.MILK);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			World world = pipe.getWorld();
			if (world.getTime() % 5 != 0)
				return;
			List<LivingEntity> entities =
				world.getEntitiesByClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedBySplashPotions);
//			ItemStack curativeItem = new ItemStack(Items.MILK_BUCKET);
			for (LivingEntity entity : entities)
				entity.clearStatusEffects();
		}
	}

	public static class WaterEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return FluidHelper.isTag(fluid, FluidTags.WATER);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			World world = pipe.getWorld();
			if (world.getTime() % 5 != 0)
				return;
			List<Entity> entities = world.getOtherEntities((Entity) null, pipe.getAOE(), Entity::isOnFire);
			for (Entity entity : entities)
				entity.extinguish();
			BlockPos.stream(pipe.getAOE())
				.forEach(pos -> dowseFire(world, pos));
		}

		// Adapted from ThrownPotion
		private static void dowseFire(World level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			if (state.isIn(BlockTags.FIRE)) {
				level.removeBlock(pos, false);
			} else if (AbstractCandleBlock.isLitCandle(state)) {
				AbstractCandleBlock.extinguish(null, state, level, pos);
			} else if (CampfireBlock.isLitCampfire(state)) {
				level.syncWorldEvent(null, 1009, pos, 0);
				CampfireBlock.extinguish(null, level, pos, state);
				level.setBlockState(pos, state.with(CampfireBlock.LIT, false));
			}
		}
	}

	public static class LavaEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return FluidHelper.isTag(fluid, FluidTags.LAVA);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			World world = pipe.getWorld();
			if (world.getTime() % 5 != 0)
				return;
			List<Entity> entities = world.getOtherEntities((Entity) null, pipe.getAOE(), entity -> !entity.isFireImmune());
			for (Entity entity : entities)
				entity.setOnFireFor(3);
		}
	}

	public static class TeaEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid().matchesType(AllFluids.TEA.get());
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			World world = pipe.getWorld();
			if (world.getTime() % 5 != 0)
				return;
			List<LivingEntity> entities = world
					.getEntitiesByClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedBySplashPotions);
			for (LivingEntity entity : entities) {
					entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 21, 0, false, false, false));
			}
		}
	}

}
