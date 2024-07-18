package com.simibubi.create.content.contraptions.actors.roller;

import java.util.List;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

public class RollerBlockEntity extends SmartBlockEntity {

	// For simulations such as Ponder
	private float manuallyAnimatedSpeed;

	public FilteringBehaviour filtering;
	public ScrollOptionBehaviour<RollingMode> mode;

	private boolean dontPropagate;

	public RollerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		dontPropagate = false;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = new FilteringBehaviour(this, new RollerValueBox(3)));
		behaviours.add(mode = new ScrollOptionBehaviour<RollingMode>(RollingMode.class,
			Lang.translateDirect("contraptions.roller_mode"), this, new RollerValueBox(-3)));

		filtering.setLabel(Lang.translateDirect("contraptions.mechanical_roller.pave_material"));
		filtering.withCallback(this::onFilterChanged);
		filtering.withPredicate(this::isValidMaterial);
		mode.withCallback(this::onModeChanged);
	}

	protected void onModeChanged(int mode) {
		shareValuesToAdjacent();
	}

	protected void onFilterChanged(ItemStack newFilter) {
		shareValuesToAdjacent();
	}

	protected boolean isValidMaterial(ItemStack newFilter) {
		if (newFilter.isEmpty())
			return true;
		BlockState appliedState = RollerMovementBehaviour.getStateToPaveWith(newFilter);
		if (appliedState.isAir())
			return false;
		if (appliedState.getBlock() instanceof BlockEntityProvider)
			return false;
		if (appliedState.getBlock() instanceof StairsBlock)
			return false;
		VoxelShape shape = appliedState.getOutlineShape(world, pos);
		if (shape.isEmpty() || !shape.getBoundingBox()
			.equals(VoxelShapes.fullCube()
				.getBoundingBox()))
			return false;
		VoxelShape collisionShape = appliedState.getCollisionShape(world, pos);
		if (collisionShape.isEmpty())
			return false;
		return true;
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}

	public float getAnimatedSpeed() {
		return manuallyAnimatedSpeed;
	}

	public void setAnimatedSpeed(float speed) {
		manuallyAnimatedSpeed = speed;
	}

	public void searchForSharedValues() {
		BlockState blockState = getCachedState();
		Direction facing = blockState.getOrEmpty(RollerBlock.FACING)
			.orElse(Direction.SOUTH);

		for (int side : Iterate.positiveAndNegative) {
			BlockPos blockPos = pos.offset(facing.rotateYClockwise(), side);
			if (world.getBlockState(blockPos) != blockState)
				continue;
			if (!(world.getBlockEntity(blockPos) instanceof RollerBlockEntity otherRoller))
				continue;
			acceptSharedValues(otherRoller.mode.getValue(), otherRoller.filtering.getFilter());
			shareValuesToAdjacent();
			break;
		}
	}

	protected void acceptSharedValues(int mode, ItemStack filter) {
		dontPropagate = true;
		this.filtering.setFilter(filter.copy());
		this.mode.setValue(mode);
		dontPropagate = false;
		notifyUpdate();
	}

	public void shareValuesToAdjacent() {
		if (dontPropagate || world.isClient())
			return;
		BlockState blockState = getCachedState();
		Direction facing = blockState.getOrEmpty(RollerBlock.FACING)
			.orElse(Direction.SOUTH);

		for (int side : Iterate.positiveAndNegative) {
			for (int i = 1; i < 100; i++) {
				BlockPos blockPos = pos.offset(facing.rotateYClockwise(), side * i);
				if (world.getBlockState(blockPos) != blockState)
					break;
				if (!(world.getBlockEntity(blockPos) instanceof RollerBlockEntity otherRoller))
					break;
				otherRoller.acceptSharedValues(mode.getValue(), filtering.getFilter());
			}
		}
	}

	static enum RollingMode implements INamedIconOptions {

		TUNNEL_PAVE(AllIcons.I_ROLLER_PAVE),
		STRAIGHT_FILL(AllIcons.I_ROLLER_FILL),
		WIDE_FILL(AllIcons.I_ROLLER_WIDE_FILL),

		;

		private String translationKey;
		private AllIcons icon;

		private RollingMode(AllIcons icon) {
			this.icon = icon;
			translationKey = "contraptions.roller_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}

	}

	private final class RollerValueBox extends ValueBoxTransform {

		private int hOffset;

		public RollerValueBox(int hOffset) {
			this.hOffset = hOffset;
		}

		@Override
		public void rotate(BlockState state, MatrixStack ms) {
			Direction facing = state.get(RollerBlock.FACING);
			float yRot = AngleHelper.horizontalAngle(facing) + 180;
			TransformStack.cast(ms)
				.rotateY(yRot)
				.rotateX(90);
		}

		@Override
		public boolean testHit(BlockState state, Vec3d localHit) {
			Vec3d offset = getLocalOffset(state);
			if (offset == null)
				return false;
			return localHit.distanceTo(offset) < scale / 3;
		}

		@Override
		public Vec3d getLocalOffset(BlockState state) {
			Direction facing = state.get(RollerBlock.FACING);
			float stateAngle = AngleHelper.horizontalAngle(facing) + 180;
			return VecHelper.rotateCentered(VecHelper.voxelSpace(8 + hOffset, 15.5f, 11), stateAngle, Axis.Y);
		}

	}

}
