package com.simibubi.create.content.kinetics.base;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.WorldView;
import io.github.fabricators_of_create.porting_lib.block.WeakPowerCheckingBlock;

@MethodsReturnNonnullByDefault
public abstract class AbstractEncasedShaftBlock extends RotatedPillarKineticBlock implements WeakPowerCheckingBlock {
    public AbstractEncasedShaftBlock(Settings properties) {
        super(properties);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side) {
        return false;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        if (context.getPlayer() != null && context.getPlayer()
                .isSneaking())
            return super.getPlacementState(context);
        Direction.Axis preferredAxis = getPreferredAxis(context);
        return this.getDefaultState()
                .with(AXIS, preferredAxis == null ? context.getPlayerLookDirection()
                        .getAxis() : preferredAxis);
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.get(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.get(AXIS);
    }
}
