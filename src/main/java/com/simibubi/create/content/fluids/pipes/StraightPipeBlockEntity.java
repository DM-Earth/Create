package com.simibubi.create.content.fluids.pipes;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeAttachmentBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

public class StraightPipeBlockEntity extends SmartBlockEntity implements PipeAttachmentBlockEntity {

	public StraightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new StraightPipeFluidTransportBehaviour(this));
		behaviours.add(new BracketedBlockEntityBehaviour(this));
		registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
	}

	@Override
	@Nullable
	public Object getRenderAttachmentData() {
		return PipeAttachmentBlockEntity.getAttachments(this);
	}

	public static class StraightPipeFluidTransportBehaviour extends FluidTransportBehaviour {

		public StraightPipeFluidTransportBehaviour(SmartBlockEntity be) {
			super(be);
		}

		@Override
		public boolean canHaveFlowToward(BlockState state, Direction direction) {
			return state.contains(AxisPipeBlock.AXIS) && state.get(AxisPipeBlock.AXIS) == direction.getAxis();
		}

		@Override
		public AttachmentTypes getRenderedRimAttachment(BlockRenderView world, BlockPos pos, BlockState state,
			Direction direction) {
			AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
			BlockState otherState = world.getBlockState(pos.offset(direction));

			Axis axis = IAxisPipe.getAxisOf(state);
			Axis otherAxis = IAxisPipe.getAxisOf(otherState);

			if (attachment == AttachmentTypes.RIM && state.getBlock() instanceof FluidValveBlock)
				return AttachmentTypes.NONE;
			if (attachment == AttachmentTypes.RIM && FluidPipeBlock.isPipe(otherState))
				return AttachmentTypes.PARTIAL_RIM;
			if (axis == otherAxis && axis != null)
				return AttachmentTypes.NONE;

			if (otherState.getBlock() instanceof FluidValveBlock
				&& FluidValveBlock.getPipeAxis(otherState) == direction.getAxis())
				return AttachmentTypes.NONE;

			return attachment.withoutConnector();
		}

	}

}
