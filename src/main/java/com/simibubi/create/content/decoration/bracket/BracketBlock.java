package com.simibubi.create.content.decoration.bracket;

import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.utility.Lang;

public class BracketBlock extends WrenchableDirectionalBlock {

	public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE =
		DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
	public static final EnumProperty<BracketType> TYPE = EnumProperty.of("type", BracketType.class);

	public static enum BracketType implements StringIdentifiable {
		PIPE, COG, SHAFT;

		@Override
		public String asString() {
			return Lang.asId(name());
		}

	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(AXIS_ALONG_FIRST_COORDINATE)
			.add(TYPE));
	}

	public BracketBlock(Settings properties) {
		super(properties);
	}

	public Optional<BlockState> getSuitableBracket(BlockState blockState, Direction direction) {
		if (blockState.getBlock() instanceof AbstractSimpleShaftBlock)
			return getSuitableBracket(blockState.get(RotatedPillarKineticBlock.AXIS), direction,
				blockState.getBlock() instanceof CogWheelBlock ? BracketType.COG : BracketType.SHAFT);
		return getSuitableBracket(FluidPropagator.getStraightPipeAxis(blockState), direction, BracketType.PIPE);
	}

	private Optional<BlockState> getSuitableBracket(Axis targetBlockAxis, Direction direction, BracketType type) {
		Axis axis = direction.getAxis();
		if (targetBlockAxis == null || targetBlockAxis == axis)
			return Optional.empty();

		boolean alongFirst = axis != Axis.Z ? targetBlockAxis == Axis.Z : targetBlockAxis == Axis.Y;
		return Optional.of(getDefaultState().with(TYPE, type)
			.with(FACING, direction)
			.with(AXIS_ALONG_FIRST_COORDINATE, !alongFirst));
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rot) {
		if (rot.ordinal() % 2 == 1)
			state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
		return super.rotate(state, rot);
	}

}
