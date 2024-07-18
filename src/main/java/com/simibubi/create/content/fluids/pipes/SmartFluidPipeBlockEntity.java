package com.simibubi.create.content.fluids.pipes;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.PipeAttachmentBlockEntity;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

public class SmartFluidPipeBlockEntity extends SmartBlockEntity implements PipeAttachmentBlockEntity {

	private FilteringBehaviour filter;

	public SmartFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new SmartPipeBehaviour(this));
		behaviours.add(filter = new FilteringBehaviour(this, new SmartPipeFilterSlot()).forFluids()
			.withCallback(this::onFilterChanged));
		registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
	}

	private void onFilterChanged(ItemStack newFilter) {
		if (!world.isClient)
			FluidPropagator.propagateChangedPipe(world, pos, getCachedState());
	}

	@Override
	@Nullable
	public Object getRenderAttachmentData() {
		return PipeAttachmentBlockEntity.getAttachments(this);
	}

	class SmartPipeBehaviour extends StraightPipeFluidTransportBehaviour {

		public SmartPipeBehaviour(SmartBlockEntity be) {
			super(be);
		}

		@Override
		public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
			if (fluid.isEmpty() || filter != null && filter.test(fluid))
				return super.canPullFluidFrom(fluid, state, direction);
			return false;
		}

		@Override
		public boolean canHaveFlowToward(BlockState state, Direction direction) {
			return state.getBlock() instanceof SmartFluidPipeBlock
				&& SmartFluidPipeBlock.getPipeAxis(state) == direction.getAxis();
		}

	}

	class SmartPipeFilterSlot extends ValueBoxTransform {

		@Override
		public Vec3d getLocalOffset(BlockState state) {
			WallMountLocation face = state.get(SmartFluidPipeBlock.FACE);
			float y = face == WallMountLocation.CEILING ? 0.55f : face == WallMountLocation.WALL ? 11.4f : 15.45f;
			float z = face == WallMountLocation.CEILING ? 4.6f : face == WallMountLocation.WALL ? 0.55f : 4.625f;
			return VecHelper.rotateCentered(VecHelper.voxelSpace(8, y, z), angleY(state), Axis.Y);
		}

		@Override
		public float getScale() {
			return super.getScale() * 1.02f;
		}

		@Override
		public void rotate(BlockState state, MatrixStack ms) {
			WallMountLocation face = state.get(SmartFluidPipeBlock.FACE);
			TransformStack.cast(ms)
				.rotateY(angleY(state))
				.rotateX(face == WallMountLocation.CEILING ? -45 : 45);
		}

		protected float angleY(BlockState state) {
			WallMountLocation face = state.get(SmartFluidPipeBlock.FACE);
			float horizontalAngle = AngleHelper.horizontalAngle(state.get(SmartFluidPipeBlock.FACING));
			if (face == WallMountLocation.WALL)
				horizontalAngle += 180;
			return horizontalAngle;
		}

	}

}
