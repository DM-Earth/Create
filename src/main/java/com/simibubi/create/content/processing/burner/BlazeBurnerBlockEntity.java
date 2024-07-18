package com.simibubi.create.content.processing.burner;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class BlazeBurnerBlockEntity extends SmartBlockEntity {

	public static final int MAX_HEAT_CAPACITY = 10000;
	public static final int INSERTION_THRESHOLD = 500;

	protected FuelType activeFuel;
	protected int remainingBurnTime;
	protected LerpedFloat headAnimation;
	protected LerpedFloat headAngle;
	protected boolean isCreative;
	protected boolean goggles;
	protected boolean hat;

	public BlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		headAnimation = LerpedFloat.linear();
		headAngle = LerpedFloat.angular();
		isCreative = false;
		goggles = false;

		headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOrEmpty(BlazeBurnerBlock.FACING)
			.orElse(Direction.SOUTH)) + 180) % 360);
	}

	public FuelType getActiveFuel() {
		return activeFuel;
	}

	public int getRemainingBurnTime() {
		return remainingBurnTime;
	}

	public boolean isCreative() {
		return isCreative;
	}

	@Override
	public void tick() {
		super.tick();

		if (world.isClient) {
			tickAnimation();
			if (!isVirtual())
				spawnParticles(getHeatLevelFromBlock(), 1);
			return;
		}

		if (isCreative)
			return;

		if (remainingBurnTime > 0)
			remainingBurnTime--;

		if (activeFuel == FuelType.NORMAL)
			updateBlockState();
		if (remainingBurnTime > 0)
			return;

		if (activeFuel == FuelType.SPECIAL) {
			activeFuel = FuelType.NORMAL;
			remainingBurnTime = MAX_HEAT_CAPACITY / 2;
		} else
			activeFuel = FuelType.NONE;

		updateBlockState();
	}

	@Environment(EnvType.CLIENT)
	private void tickAnimation() {
		boolean active = getHeatLevelFromBlock().isAtLeast(HeatLevel.FADING) && isValidBlockAbove();

		if (!active) {
			float target = 0;
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null && !player.isInvisible()) {
				double x;
				double z;
				if (isVirtual()) {
					x = -4;
					z = -10;
				} else {
					x = player.getX();
					z = player.getZ();
				}
				double dx = x - (getPos().getX() + 0.5);
				double dz = z - (getPos().getZ() + 0.5);
				target = AngleHelper.deg(-MathHelper.atan2(dz, dx)) - 90;
			}
			target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
			headAngle.chase(target, .25f, Chaser.exp(5));
			headAngle.tickChaser();
		} else {
			headAngle.chase((AngleHelper.horizontalAngle(getCachedState().getOrEmpty(BlazeBurnerBlock.FACING)
				.orElse(Direction.SOUTH)) + 180) % 360, .125f, Chaser.EXP);
			headAngle.tickChaser();
		}

		headAnimation.chase(active ? 1 : 0, .25f, Chaser.exp(.25f));
		headAnimation.tickChaser();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		if (!isCreative) {
			compound.putInt("fuelLevel", activeFuel.ordinal());
			compound.putInt("burnTimeRemaining", remainingBurnTime);
		} else
			compound.putBoolean("isCreative", true);
		if (goggles)
			compound.putBoolean("Goggles", true);
		if (hat)
			compound.putBoolean("TrainHat", true);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		activeFuel = FuelType.values()[compound.getInt("fuelLevel")];
		remainingBurnTime = compound.getInt("burnTimeRemaining");
		isCreative = compound.getBoolean("isCreative");
		goggles = compound.contains("Goggles");
		hat = compound.contains("TrainHat");
		super.read(compound, clientPacket);
	}

	public BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock() {
		return BlazeBurnerBlock.getHeatLevelOf(getCachedState());
	}

	public void updateBlockState() {
		setBlockHeat(getHeatLevel());
	}

	protected void setBlockHeat(HeatLevel heat) {
		HeatLevel inBlockState = getHeatLevelFromBlock();
		if (inBlockState == heat)
			return;
		world.setBlockState(pos, getCachedState().with(BlazeBurnerBlock.HEAT_LEVEL, heat));
		notifyUpdate();
	}

	/**
	 * @return true if the heater updated its burn time and an item should be
	 *         consumed
	 */
	protected boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, TransactionContext ctx) {
		if (isCreative)
			return false;

		FuelType newFuel = FuelType.NONE;
		int newBurnTime;

		if (AllItemTags.BLAZE_BURNER_FUEL_SPECIAL.matches(itemStack)) {
			newBurnTime = 3200;
			newFuel = FuelType.SPECIAL;
		} else {
			Integer fuel = FuelRegistry.INSTANCE.get(itemStack.getItem());
			newBurnTime = fuel == null ? 0 : fuel;
			if (newBurnTime > 0) {
				newFuel = FuelType.NORMAL;
			} else if (AllItemTags.BLAZE_BURNER_FUEL_REGULAR.matches(itemStack)) {
				newBurnTime = 1600; // Same as coal
				newFuel = FuelType.NORMAL;
			}
		}

		if (newFuel == FuelType.NONE)
			return false;
		if (newFuel.ordinal() < activeFuel.ordinal())
			return false;

		if (newFuel == activeFuel) {
			if (remainingBurnTime <= INSERTION_THRESHOLD) {
				newBurnTime += remainingBurnTime;
			} else if (forceOverflow && newFuel == FuelType.NORMAL) {
				if (remainingBurnTime < MAX_HEAT_CAPACITY) {
					newBurnTime = Math.min(remainingBurnTime + newBurnTime, MAX_HEAT_CAPACITY);
				} else {
					newBurnTime = remainingBurnTime;
				}
			} else {
				return false;
			}
		}

		FuelType finalNewFuel = newFuel;
		int finalNewBurnTime = newBurnTime;
		TransactionCallback.onSuccess(ctx, () -> {
			activeFuel = finalNewFuel;
			remainingBurnTime = finalNewBurnTime;
			if (world.isClient) {
				spawnParticleBurst(activeFuel == FuelType.SPECIAL);
				return;
			}
			HeatLevel prev = getHeatLevelFromBlock();
			playSound();
			updateBlockState();

			if (prev != getHeatLevelFromBlock())
				world.playSound(null, pos, SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.BLOCKS,
						.125f + world.random.nextFloat() * .125f, 1.15f - world.random.nextFloat() * .25f);
		});

		return true;
	}

	protected void applyCreativeFuel() {
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		isCreative = true;

		HeatLevel next = getHeatLevelFromBlock().nextActiveLevel();

		if (world.isClient) {
			spawnParticleBurst(next.isAtLeast(HeatLevel.SEETHING));
			return;
		}

		playSound();
		if (next == HeatLevel.FADING)
			next = next.nextActiveLevel();
		setBlockHeat(next);
	}

	public boolean isCreativeFuel(ItemStack stack) {
		return AllItems.CREATIVE_BLAZE_CAKE.isIn(stack);
	}

	public boolean isValidBlockAbove() {
		if (isVirtual())
			return false;
		BlockState blockState = world.getBlockState(pos.up());
		return AllBlocks.BASIN.has(blockState) || blockState.getBlock() instanceof FluidTankBlock;
	}

	protected void playSound() {
		world.playSound(null, pos, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.BLOCKS,
			.125f + world.random.nextFloat() * .125f, .75f - world.random.nextFloat() * .25f);
	}

	protected HeatLevel getHeatLevel() {
		HeatLevel level = HeatLevel.SMOULDERING;
		switch (activeFuel) {
		case SPECIAL:
			level = HeatLevel.SEETHING;
			break;
		case NORMAL:
			boolean lowPercent = (double) remainingBurnTime / MAX_HEAT_CAPACITY < 0.0125;
			level = lowPercent ? HeatLevel.FADING : HeatLevel.KINDLED;
			break;
		default:
		case NONE:
			break;
		}
		return level;
	}

	protected void spawnParticles(HeatLevel heatLevel, double burstMult) {
		if (world == null)
			return;
		if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE)
			return;

		Random r = world.getRandom();

		Vec3d c = VecHelper.getCenterOf(pos);
		Vec3d v = c.add(VecHelper.offsetRandomly(Vec3d.ZERO, r, .125f)
			.multiply(1, 0, 1));

		if (r.nextInt(4) != 0)
			return;

		boolean empty = world.getBlockState(pos.up())
			.getCollisionShape(world, pos.up())
			.isEmpty();

		if (empty || r.nextInt(8) == 0)
			world.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0, 0, 0);

		double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
		Vec3d v2 = c.add(VecHelper.offsetRandomly(Vec3d.ZERO, r, .5f)
			.multiply(1, .25f, 1)
			.normalize()
			.multiply((empty ? .25f : .5) + r.nextDouble() * .125f))
			.add(0, .5, 0);

		if (heatLevel.isAtLeast(HeatLevel.SEETHING)) {
			world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
		} else if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			world.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
		}
		return;
	}

	public void spawnParticleBurst(boolean soulFlame) {
		Vec3d c = VecHelper.getCenterOf(pos);
		Random r = world.random;
		for (int i = 0; i < 20; i++) {
			Vec3d offset = VecHelper.offsetRandomly(Vec3d.ZERO, r, .5f)
				.multiply(1, .25f, 1)
				.normalize();
			Vec3d v = c.add(offset.multiply(.5 + r.nextDouble() * .125f))
				.add(0, .125, 0);
			Vec3d m = offset.multiply(1 / 32f);

			world.addParticle(soulFlame ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, v.x, v.y, v.z, m.x, m.y,
				m.z);
		}
	}

	public enum FuelType {
		NONE, NORMAL, SPECIAL
	}

}
