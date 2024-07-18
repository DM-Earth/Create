package com.simibubi.create.content.contraptions.piston;

import static com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.isExtensionPole;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.enums.PistonType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class MechanicalPistonHeadBlock extends WrenchableDirectionalBlock implements Waterloggable, BlockPickInteractionAware {

    public static final EnumProperty<PistonType> TYPE = Properties.PISTON_TYPE;

    public MechanicalPistonHeadBlock(Settings p_i48415_1_) {
        super(p_i48415_1_);
        setDefaultState(super.getDefaultState().with(Properties.WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(TYPE, Properties.WATERLOGGED);
        super.appendProperties(builder);
    }

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		return AllBlocks.PISTON_EXTENSION_POLE.asStack();
	}

    @Override
    public void onBreak(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        Direction direction = state.get(FACING);
        BlockPos pistonHead = pos;
        BlockPos pistonBase = null;

        for (int offset = 1; offset < MechanicalPistonBlock.maxAllowedPistonPoles(); offset++) {
            BlockPos currentPos = pos.offset(direction.getOpposite(), offset);
            BlockState block = worldIn.getBlockState(currentPos);

            if (isExtensionPole(block) && direction.getAxis() == block.get(Properties.FACING)
                    .getAxis())
                continue;

            if (MechanicalPistonBlock.isPiston(block) && block.get(Properties.FACING) == direction)
                pistonBase = currentPos;

            break;
        }

        if (pistonHead != null && pistonBase != null) {
            final BlockPos basePos = pistonBase;
            BlockPos.stream(pistonBase, pistonHead)
                    .filter(p -> !p.equals(pos) && !p.equals(basePos))
                    .forEach(p -> worldIn.breakBlock(p, !player.isCreative()));
            worldIn.setBlockState(basePos, worldIn.getBlockState(basePos)
                    .with(MechanicalPistonBlock.STATE, PistonState.RETRACTED));
        }

        super.onBreak(worldIn, pos, state, player);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return AllShapes.MECHANICAL_PISTON_HEAD.get(state.get(FACING));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState,
                                          WorldAccess world, BlockPos pos, BlockPos neighbourPos) {
        if (state.get(Properties.WATERLOGGED))
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return state;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        FluidState FluidState = context.getWorld().getFluidState(context.getBlockPos());
        return super.getPlacementState(context).with(Properties.WATERLOGGED, Boolean.valueOf(FluidState.getFluid() == Fluids.WATER));
    }

    @Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}
}
