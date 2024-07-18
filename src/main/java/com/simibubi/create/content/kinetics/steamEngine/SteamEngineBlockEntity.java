package com.simibubi.create.content.kinetics.steamEngine;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SteamEngineBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	protected ScrollOptionBehaviour<RotationDirection> movementDirection;

	public WeakReference<PoweredShaftBlockEntity> target;
	public WeakReference<FluidTankBlockEntity> source;

	public SteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		source = new WeakReference<>(null);
		target = new WeakReference<>(null);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class,
			Lang.translateDirect("contraptions.windmill.rotation_direction"), this, new SteamEngineValueBox());
		movementDirection.onlyActiveWhen(() -> {
			PoweredShaftBlockEntity shaft = getShaft();
			return shaft == null || !shaft.hasSource();
		});
		movementDirection.withCallback($ -> onDirectionChanged());
		behaviours.add(movementDirection);

		registerAwardables(behaviours, AllAdvancements.STEAM_ENGINE);
	}

	private void onDirectionChanged() {}

	@Override
	public void tick() {
		super.tick();
		FluidTankBlockEntity tank = getTank();
		PoweredShaftBlockEntity shaft = getShaft();

		if (tank == null || shaft == null) {
			if (world.isClient())
				return;
			if (shaft == null)
				return;
			if (!shaft.getPos()
				.subtract(pos)
				.equals(shaft.enginePos))
				return;
			if (shaft.engineEfficiency == 0)
				return;
			Direction facing = SteamEngineBlock.getFacing(getCachedState());
			if (world.canSetBlock(pos.offset(facing.getOpposite())))
				shaft.update(pos, 0, 0);
			return;
		}

		boolean verticalTarget = false;
		BlockState shaftState = shaft.getCachedState();
		Axis targetAxis = Axis.X;
		if (shaftState.getBlock()instanceof IRotate ir)
			targetAxis = ir.getRotationAxis(shaftState);
		verticalTarget = targetAxis == Axis.Y;

		BlockState blockState = getCachedState();
		if (!AllBlocks.STEAM_ENGINE.has(blockState))
			return;
		Direction facing = SteamEngineBlock.getFacing(blockState);
		if (facing.getAxis() == Axis.Y)
			facing = blockState.get(SteamEngineBlock.FACING);

		float efficiency = MathHelper.clamp(tank.boiler.getEngineEfficiency(tank.getTotalTankSize()), 0, 1);
		if (efficiency > 0)

			award(AllAdvancements.STEAM_ENGINE);

		int conveyedSpeedLevel =
			efficiency == 0 ? 1 : verticalTarget ? 1 : (int) GeneratingKineticBlockEntity.convertToDirection(1, facing);
		if (targetAxis == Axis.Z)
			conveyedSpeedLevel *= -1;
		if (movementDirection.get() == RotationDirection.COUNTER_CLOCKWISE)
			conveyedSpeedLevel *= -1;

		float shaftSpeed = shaft.getTheoreticalSpeed();
		if (shaft.hasSource() && shaftSpeed != 0 && conveyedSpeedLevel != 0
			&& (shaftSpeed > 0) != (conveyedSpeedLevel > 0)) {
			movementDirection.setValue(1 - movementDirection.get()
				.ordinal());
			conveyedSpeedLevel *= -1;
		}

		shaft.update(pos, conveyedSpeedLevel, efficiency);

		if (!world.isClient)
			return;

		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::spawnParticles);
	}

	@Override
	public void remove() {
		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft != null)
			shaft.remove(pos);
		super.remove();
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected Box createRenderBoundingBox() {
		return super.createRenderBoundingBox().expand(2);
	}

	public PoweredShaftBlockEntity getShaft() {
		PoweredShaftBlockEntity shaft = target.get();
		if (shaft == null || shaft.isRemoved() || !shaft.canBePoweredBy(pos)) {
			if (shaft != null)
				target = new WeakReference<>(null);
			Direction facing = SteamEngineBlock.getFacing(getCachedState());
			BlockEntity anyShaftAt = world.getBlockEntity(pos.offset(facing, 2));
			if (anyShaftAt instanceof PoweredShaftBlockEntity ps && ps.canBePoweredBy(pos))
				target = new WeakReference<>(shaft = ps);
		}
		return shaft;
	}

	public FluidTankBlockEntity getTank() {
		FluidTankBlockEntity tank = source.get();
		if (tank == null || tank.isRemoved()) {
			if (tank != null)
				source = new WeakReference<>(null);
			Direction facing = SteamEngineBlock.getFacing(getCachedState());
			BlockEntity be = world.getBlockEntity(pos.offset(facing.getOpposite()));
			if (be instanceof FluidTankBlockEntity tankBe)
				source = new WeakReference<>(tank = tankBe);
		}
		if (tank == null)
			return null;
		return tank.getControllerBE();
	}

	float prevAngle = 0;

	@Environment(EnvType.CLIENT)
	private void spawnParticles() {
		Float targetAngle = getTargetAngle();
		PoweredShaftBlockEntity ste = target.get();
		if (ste == null)
			return;
		if (!ste.isPoweredBy(pos) || ste.engineEfficiency == 0)
			return;
		if (targetAngle == null)
			return;

		float angle = AngleHelper.deg(targetAngle);
		angle += (angle < 0) ? -180 + 75 : 360 - 75;
		angle %= 360;

		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft == null || shaft.getSpeed() == 0)
			return;

		if (angle >= 0 && !(prevAngle > 180 && angle < 180)) {
			prevAngle = angle;
			return;
		}
		if (angle < 0 && !(prevAngle < -180 && angle > -180)) {
			prevAngle = angle;
			return;
		}

		FluidTankBlockEntity sourceBE = source.get();
		if (sourceBE != null) {
			FluidTankBlockEntity controller = sourceBE.getControllerBE();
			if (controller != null && controller.boiler != null) {
				float volume = 3f / Math.max(2, controller.boiler.attachedEngines / 6);
				float pitch = 1.18f - world.random.nextFloat() * .25f;
				world.playSound(pos.getX(), pos.getY(), pos.getZ(),
					SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.BLOCKS, volume, pitch, false);
				AllSoundEvents.STEAM.playAt(world, pos, volume / 16, .8f, false);
			}
		}

		Direction facing = SteamEngineBlock.getFacing(getCachedState());

		Vec3d offset = VecHelper.rotate(new Vec3d(0, 0, 1).add(VecHelper.offsetRandomly(Vec3d.ZERO, world.random, 1)
			.multiply(1, 1, 0)
			.normalize()
			.multiply(.5f)), AngleHelper.verticalAngle(facing), Axis.X);
		offset = VecHelper.rotate(offset, AngleHelper.horizontalAngle(facing), Axis.Y);
		Vec3d v = offset.multiply(.5f)
			.add(Vec3d.ofCenter(pos));
		Vec3d m = offset.subtract(Vec3d.of(facing.getVector())
			.multiply(.75f));
		world.addParticle(new SteamJetParticleData(1), v.x, v.y, v.z, m.x, m.y, m.z);

		prevAngle = angle;
	}

	@Nullable
	@Environment(EnvType.CLIENT)
	public Float getTargetAngle() {
		float angle = 0;
		BlockState blockState = getCachedState();
		if (!AllBlocks.STEAM_ENGINE.has(blockState))
			return null;

		Direction facing = SteamEngineBlock.getFacing(blockState);
		PoweredShaftBlockEntity shaft = getShaft();
		Axis facingAxis = facing.getAxis();
		Axis axis = Axis.Y;

		if (shaft == null)
			return null;

		axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
		angle = KineticBlockEntityRenderer.getAngleForTe(shaft, shaft.getPos(), axis);

		if (axis == facingAxis)
			return null;
		if (axis.isHorizontal() && (facingAxis == Axis.X ^ facing.getDirection() == AxisDirection.POSITIVE))
			angle *= -1;
		if (axis == Axis.X && facing == Direction.DOWN)
			angle *= -1;
		return angle;
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		PoweredShaftBlockEntity shaft = getShaft();
		return shaft == null ? false : shaft.addToEngineTooltip(tooltip, isPlayerSneaking);
	}

}
