package com.simibubi.create.content.contraptions.actors.contraptionControls;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

public class ContraptionControlsBlockEntity extends SmartBlockEntity {

	public FilteringBehaviour filtering;
	public boolean disabled;
	public boolean powered;

	public LerpedFloat indicator;
	public LerpedFloat button;

	public ContraptionControlsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		indicator = LerpedFloat.angular()
			.startWithValue(0);
		button = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, 0.125f, Chaser.EXP);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = new FilteringBehaviour(this, new ControlsSlot()));
		filtering.setLabel(Lang.translateDirect("contraptions.contoller.target"));
		filtering.withPredicate(AllItemTags.CONTRAPTION_CONTROLLED::matches);
	}

	public void pressButton() {
		button.setValue(1);
	}

	public void updatePoweredState() {
		if (world.isClient())
			return;
		boolean powered = world.isReceivingRedstonePower(pos);
		if (this.powered == powered)
			return;
		this.powered = powered;
		this.disabled = powered;
		notifyUpdate();
	}

	@Override
	public void initialize() {
		super.initialize();
		updatePoweredState();
	}

	@Override
	public void tick() {
		super.tick();
		if (!world.isClient())
			return;
		tickAnimations();
		int value = disabled ? 4 * 45 : 0;
		indicator.setValue(value);
		indicator.updateChaseTarget(value);
	}

	public void tickAnimations() {
		button.tickChaser();
		indicator.tickChaser();
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		disabled = tag.getBoolean("Disabled");
		powered = tag.getBoolean("Powered");
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putBoolean("Disabled", disabled);
		tag.putBoolean("Powered", powered);
	}

	public static void sendStatus(PlayerEntity player, ItemStack filter, boolean enabled) {
		MutableText state = Lang.translate("contraption.controls.actor_toggle." + (enabled ? "on" : "off"))
			.color(DyeHelper.DYE_TABLE.get(enabled ? DyeColor.LIME : DyeColor.ORANGE)
				.getFirst())
			.component();

		if (filter.isEmpty()) {
			Lang.translate("contraption.controls.all_actor_toggle", state)
				.sendStatus(player);
			return;
		}

		Lang.translate("contraption.controls.specific_actor_toggle", filter.getName()
			.getString(), state)
			.sendStatus(player);
	}

	public static class ControlsSlot extends ValueBoxTransform.Sided {

		@Override
		public Vec3d getLocalOffset(BlockState state) {
			Direction facing = state.get(ControlsBlock.FACING);
			float yRot = AngleHelper.horizontalAngle(facing);
			return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 12f, 5.5f), yRot, Axis.Y);
		}

		@Override
		public void rotate(BlockState state, MatrixStack ms) {
			Direction facing = state.get(ControlsBlock.FACING);
			float yRot = AngleHelper.horizontalAngle(facing);
			TransformStack.cast(ms)
				.rotateY(yRot + 180)
				.rotateX(67.5f);
		}

		@Override
		public float getScale() {
			return .508f;
		}

		@Override
		protected Vec3d getSouthLocation() {
			return Vec3d.ZERO;
		}

	}

}
