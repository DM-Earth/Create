package com.simibubi.create.content.kinetics.crank;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ValveHandleBlockEntity extends HandCrankBlockEntity {

	public ScrollValueBehaviour angleInput;
	public int cooldown;

	protected int startAngle;
	protected int targetAngle;
	protected int totalUseTicks;

	public ValveHandleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(angleInput = new ValveHandleScrollValueBehaviour(this).between(-180, 180));
		angleInput.onlyActiveWhen(this::showValue);
		angleInput.setValue(45);
	}

	@Override
	protected boolean clockwise() {
		return angleInput.getValue() < 0 ^ backwards;
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putInt("TotalUseTicks", totalUseTicks);
		compound.putInt("StartAngle", startAngle);
		compound.putInt("TargetAngle", targetAngle);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		totalUseTicks = compound.getInt("TotalUseTicks");
		startAngle = compound.getInt("StartAngle");
		targetAngle = compound.getInt("TargetAngle");
	}

	@Override
	public void tick() {
		super.tick();
		if (inUse == 0 && cooldown > 0)
			cooldown--;
		independentAngle = world.isClient() ? getIndependentAngle(0) : 0;
	}

	@Override
	public float getIndependentAngle(float partialTicks) {
		if (inUse == 0 && source != null && getSpeed() != 0)
			return KineticBlockEntityRenderer.getAngleForTe(this, pos,
				KineticBlockEntityRenderer.getRotationAxisOf(this));

		int step = getCachedState().getOrEmpty(ValveHandleBlock.FACING)
			.orElse(Direction.SOUTH)
			.getDirection()
			.offset();

		return (inUse > 0 && totalUseTicks > 0
			? MathHelper.lerp(Math.min(totalUseTicks, totalUseTicks - inUse + partialTicks) / (float) totalUseTicks,
				startAngle, targetAngle)
			: targetAngle) * MathHelper.RADIANS_PER_DEGREE * (backwards ? -1 : 1) * step;
	}

	public boolean showValue() {
		return inUse == 0;
	}

	public boolean activate(boolean sneak) {
		if (getTheoreticalSpeed() != 0)
			return false;
		if (inUse > 0 || cooldown > 0)
			return false;
		if (world.isClient)
			return true;

		// Always overshoot, target will stop early
		int value = angleInput.getValue();
		int target = Math.abs(value);
		int rotationSpeed = AllBlocks.COPPER_VALVE_HANDLE.get()
			.getRotationSpeed();
		double degreesPerTick = KineticBlockEntity.convertToAngular(rotationSpeed);
		inUse = (int) Math.ceil(target / degreesPerTick) + 2;

		startAngle = (int) ((independentAngle) % 90 + 360) % 90;
		targetAngle = Math.round((startAngle + (target > 135 ? 180 : 90) * MathHelper.sign(value)) / 90f) * 90;
		totalUseTicks = inUse;
		backwards = sneak;

		sequenceContext = SequenceContext.fromGearshift(SequencerInstructions.TURN_ANGLE, rotationSpeed, target);
		updateGeneratedRotation();
		cooldown = 4;

		return true;
	}

	@Override
	protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}

	@Override
	@Environment(EnvType.CLIENT)
	public SuperByteBuffer getRenderedHandle() {
		return CachedBufferer.block(getCachedState());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Instancer<ModelData> getRenderedHandleInstance(Material<ModelData> material) {
		return material.getModel(getCachedState());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean shouldRenderShaft() {
		return false;
	}

	public static class ValveHandleScrollValueBehaviour extends ScrollValueBehaviour {

		public ValveHandleScrollValueBehaviour(SmartBlockEntity be) {
			super(Lang.translateDirect("kinetics.valve_handle.rotated_angle"), be, new ValveHandleValueBox());
			withFormatter(v -> String.valueOf(Math.abs(v)) + Lang.translateDirect("generic.unit.degrees")
				.getString());
		}

		@Override
		public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult) {
			ImmutableList<Text> rows = ImmutableList.of(Components.literal("\u27f3")
				.formatted(Formatting.BOLD),
				Components.literal("\u27f2")
					.formatted(Formatting.BOLD));
			return new ValueSettingsBoard(label, 180, 45, rows, new ValueSettingsFormatter(this::formatValue));
		}

		@Override
		public void setValueSettings(PlayerEntity player, ValueSettings valueSetting, boolean ctrlHeld) {
			int value = Math.max(1, valueSetting.value());
			if (!valueSetting.equals(getValueSettings()))
				playFeedbackSound(this);
			setValue(valueSetting.row() == 0 ? -value : value);
		}

		@Override
		public ValueSettings getValueSettings() {
			return new ValueSettings(value < 0 ? 0 : 1, Math.abs(value));
		}

		public MutableText formatValue(ValueSettings settings) {
			return Lang.number(Math.max(1, Math.abs(settings.value())))
				.add(Lang.translateDirect("generic.unit.degrees"))
				.component();
		}

		@Override
		public void onShortInteract(PlayerEntity player, Hand hand, Direction side) {
			BlockState blockState = blockEntity.getCachedState();
			if (blockState.getBlock() instanceof ValveHandleBlock vhb)
				vhb.clicked(getWorld(), getPos(), blockState, player, hand);
		}

	}

	public static class ValveHandleValueBox extends ValueBoxTransform.Sided {

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return direction == state.get(ValveHandleBlock.FACING);
		}

		@Override
		protected Vec3d getSouthLocation() {
			return VecHelper.voxelSpace(8, 8, 4.5);
		}

		@Override
		public boolean testHit(BlockState state, Vec3d localHit) {
			Vec3d offset = getLocalOffset(state);
			if (offset == null)
				return false;
			return localHit.distanceTo(offset) < scale / 1.5f;
		}

	}

}
