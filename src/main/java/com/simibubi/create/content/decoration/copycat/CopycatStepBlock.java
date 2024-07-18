package com.simibubi.create.content.decoration.copycat;

import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PoleHelper;
import com.simibubi.create.foundation.utility.VoxelShaper;

public class CopycatStepBlock extends WaterloggedCopycatBlock {

	public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public CopycatStepBlock(Settings pProperties) {
		super(pProperties);
		setDefaultState(getDefaultState().with(HALF, BlockHalf.BOTTOM)
			.with(FACING, Direction.SOUTH));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {

		if (!player.isSneaking() && player.canModifyBlocks()) {
			ItemStack heldItem = player.getStackInHand(hand);
			IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
			if (helper.matchesItem(heldItem))
				return helper.getOffset(player, world, state, pos, ray)
					.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
		}

		return super.onUse(state, world, pos, player, hand, ray);
	}

	@Override
	public boolean isIgnoredConnectivitySide(BlockRenderView reader, BlockState state, Direction face,
		BlockPos fromPos, BlockPos toPos) {
		BlockState toState = reader.getBlockState(toPos);

		if (!toState.isOf(this))
			return true;

		Direction facing = state.get(FACING);
		BlockPos diff = fromPos.subtract(toPos);
		int coord = facing.getAxis()
			.choose(diff.getX(), diff.getY(), diff.getZ());

		BlockHalf half = state.get(HALF);
		if (half != toState.get(HALF))
			return diff.getY() == 0;

		return facing == toState.get(FACING)
			.getOpposite()
			&& !(coord != 0 && coord != facing.getDirection()
				.offset());
	}

	@Override
	public boolean canConnectTexturesToward(BlockRenderView reader, BlockPos fromPos, BlockPos toPos,
		BlockState state) {
		Direction facing = state.get(FACING);
		BlockState toState = reader.getBlockState(toPos);
		BlockPos diff = fromPos.subtract(toPos);

		if (fromPos.equals(toPos.offset(facing)))
			return false;
		if (!toState.isOf(this))
			return false;

		if (diff.getY() != 0) {
			if (isOccluded(toState, state, diff.getY() > 0 ? Direction.UP : Direction.DOWN))
				return true;
			return false;
		}

		if (isOccluded(state, toState, facing))
			return true;

		int coord = facing.getAxis()
			.choose(diff.getX(), diff.getY(), diff.getZ());
		if (state.with(WATERLOGGED, false) == toState.with(WATERLOGGED, false) && coord == 0)
			return true;

		return false;
	}

	@Override
	public boolean canFaceBeOccluded(BlockState state, Direction face) {
		if (face.getAxis() == Axis.Y)
			return (state.get(HALF) == BlockHalf.TOP) == (face == Direction.UP);
		return state.get(FACING) == face;
	}

	@Override
	public boolean shouldFaceAlwaysRender(BlockState state, Direction face) {
		return canFaceBeOccluded(state, face.getOpposite());
	}

	@Override
	public boolean canPathfindThrough(BlockState pState, BlockView pLevel, BlockPos pPos, NavigationType pType) {
		return false;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		BlockState stateForPlacement =
			super.getPlacementState(pContext).with(FACING, pContext.getHorizontalPlayerFacing());
		Direction direction = pContext.getSide();
		if (direction == Direction.UP)
			return stateForPlacement;
		if (direction == Direction.DOWN || (pContext.getHitPos().y - pContext.getBlockPos()
			.getY() > 0.5D))
			return stateForPlacement.with(HALF, BlockHalf.TOP);
		return stateForPlacement;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(HALF, FACING));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		VoxelShaper voxelShaper = pState.get(HALF) == BlockHalf.BOTTOM ? AllShapes.STEP_BOTTOM : AllShapes.STEP_TOP;
		return voxelShaper.get(pState.get(FACING));
	}

	@Override
	public boolean supportsExternalFaceHiding(BlockState state) {
		return true;
	}

	@Override
	public boolean hidesNeighborFace(BlockView level, BlockPos pos, BlockState state, BlockState neighborState,
		Direction dir) {
		if (state.isOf(this) == neighborState.isOf(this)
			&& getMaterial(level, pos).isSideInvisible(getMaterial(level, pos.offset(dir)), dir.getOpposite()))
			return isOccluded(state, neighborState, dir);
		return false;
	}

	public static boolean isOccluded(BlockState state, BlockState other, Direction pDirection) {
		state = state.with(WATERLOGGED, false);
		other = other.with(WATERLOGGED, false);

		BlockHalf half = state.get(HALF);
		boolean vertical = pDirection.getAxis() == Axis.Y;
		if (half != other.get(HALF))
			return vertical && (pDirection == Direction.UP) == (half == BlockHalf.TOP);
		if (vertical)
			return false;

		Direction facing = state.get(FACING);
		if (facing.getOpposite() == other.get(FACING) && pDirection == facing)
			return true;
		if (other.get(FACING) != facing)
			return false;
		return pDirection.getAxis() != facing.getAxis();
	}

	@Override
	public BlockState rotate(BlockState pState, BlockRotation pRot) {
		return pState.with(FACING, pRot.rotate(pState.get(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState pState, BlockMirror pMirror) {
		return pState.rotate(pMirror.getRotation(pState.get(FACING)));
	}

	private static class PlacementHelper extends PoleHelper<Direction> {

		public PlacementHelper() {
			super(AllBlocks.COPYCAT_STEP::has, state -> state.get(FACING)
				.rotateYClockwise()
				.getAxis(), FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.COPYCAT_STEP::isIn;
		}

	}

}
