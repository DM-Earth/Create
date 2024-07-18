package com.simibubi.create.content.contraptions.pulley;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class PulleyBlock extends HorizontalAxisKineticBlock implements IBE<PulleyBlockEntity> {

    public PulleyBlock(Settings properties) {
        super(properties);
    }

    private static void onRopeBroken(World world, BlockPos pulleyPos) {
		BlockEntity be = world.getBlockEntity(pulleyPos);
		if (be instanceof PulleyBlockEntity) {
			PulleyBlockEntity pulley = (PulleyBlockEntity) be;
			pulley.initialOffset = 0;
			pulley.onLengthBroken();
		}
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onStateReplaced(state, worldIn, pos, newState, isMoving);
		if (state.isOf(newState.getBlock()))
			return;
		if (worldIn.isClient)
			return;
		BlockState below = worldIn.getBlockState(pos.down());
		if (below.getBlock() instanceof RopeBlockBase)
			worldIn.breakBlock(pos.down(), true);
	}

    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
                                  BlockHitResult hit) {
        if (!player.canModifyBlocks())
            return ActionResult.PASS;
        if (player.isSneaking())
            return ActionResult.PASS;
        if (player.getStackInHand(handIn)
                .isEmpty()) {
            withBlockEntityDo(worldIn, pos, be -> be.assembleNextTick = true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return AllShapes.PULLEY.get(state.get(HORIZONTAL_AXIS));
    }

    @Override
    public Class<PulleyBlockEntity> getBlockEntityClass() {
        return PulleyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PulleyBlockEntity> getBlockEntityType() {
    	return AllBlockEntityTypes.ROPE_PULLEY.get();
    }

	private static class RopeBlockBase extends Block implements Waterloggable, BlockPickInteractionAware {

        public RopeBlockBase(Settings properties) {
            super(properties);
            setDefaultState(super.getDefaultState().with(Properties.WATERLOGGED, false));
        }

		@Override
    	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
    		return false;
    	}

		@Override
		public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
			return AllBlocks.ROPE_PULLEY.asStack();
		}

        @Override
        public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
            if (!isMoving && (!state.contains(Properties.WATERLOGGED) || !newState.contains(Properties.WATERLOGGED) || state.get(Properties.WATERLOGGED) == newState.get(Properties.WATERLOGGED))) {
                onRopeBroken(worldIn, pos.up());
                if (!worldIn.isClient) {
                    BlockState above = worldIn.getBlockState(pos.up());
                    BlockState below = worldIn.getBlockState(pos.down());
                    if (above.getBlock() instanceof RopeBlockBase)
                        worldIn.breakBlock(pos.up(), true);
                    if (below.getBlock() instanceof RopeBlockBase)
                        worldIn.breakBlock(pos.down(), true);
                }
            }
            if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
                worldIn.removeBlockEntity(pos);
            }
        }


        @Override
        public FluidState getFluidState(BlockState state) {
            return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
        }

        @Override
        protected void appendProperties(Builder<Block, BlockState> builder) {
            builder.add(Properties.WATERLOGGED);
            super.appendProperties(builder);
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

    }

    public static class MagnetBlock extends RopeBlockBase {

        public MagnetBlock(Settings properties) {
            super(properties);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
            return AllShapes.PULLEY_MAGNET;
        }

    }

    public static class RopeBlock extends RopeBlockBase {

        public RopeBlock(Settings properties) {
            super(properties);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
            return AllShapes.FOUR_VOXEL_POLE.get(Direction.UP);
        }
    }

}
