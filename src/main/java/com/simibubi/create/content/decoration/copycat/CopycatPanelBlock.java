package com.simibubi.create.content.decoration.copycat;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
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
import com.simibubi.create.foundation.placement.PlacementOffset;

public class CopycatPanelBlock extends WaterloggedCopycatBlock {

	public static final DirectionProperty FACING = Properties.FACING;

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public CopycatPanelBlock(Settings pProperties) {
		super(pProperties);
		setDefaultState(getDefaultState().with(FACING, Direction.UP));
	}

	@Override
	public boolean isAcceptedRegardless(BlockState material) {
		return CopycatSpecialCases.isBarsMaterial(material) || CopycatSpecialCases.isTrapdoorMaterial(material);
	}

	@Override
	public BlockState prepareMaterial(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity pPlayer,
		Hand pHand, BlockHitResult pHit, BlockState material) {
		if (!CopycatSpecialCases.isTrapdoorMaterial(material))
			return super.prepareMaterial(pLevel, pPos, pState, pPlayer, pHand, pHit, material);

		Direction panelFacing = pState.get(FACING);
		if (panelFacing == Direction.DOWN)
			material = material.with(TrapdoorBlock.HALF, BlockHalf.TOP);
		if (panelFacing.getAxis() == Axis.Y)
			return material.with(TrapdoorBlock.FACING, pPlayer.getHorizontalFacing())
				.with(TrapdoorBlock.OPEN, false);

		boolean clickedNearTop = pHit.getPos().y - .5 > pPos.getY();
		return material.with(TrapdoorBlock.OPEN, true)
			.with(TrapdoorBlock.HALF, clickedNearTop ? BlockHalf.TOP : BlockHalf.BOTTOM)
			.with(TrapdoorBlock.FACING, panelFacing);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {

		if (!player.isSneaking() && player.canModifyBlocks()) {
			ItemStack heldItem = player.getStackInHand(hand);
			IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
			if (placementHelper.matchesItem(heldItem)) {
				placementHelper.getOffset(player, world, state, pos, ray)
					.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
				return ActionResult.SUCCESS;
			}
		}

		return super.onUse(state, world, pos, player, hand, ray);
	}

	@Override
	public boolean isIgnoredConnectivitySide(BlockRenderView reader, BlockState state, Direction face,
		BlockPos fromPos, BlockPos toPos) {
		Direction facing = state.get(FACING);
		BlockState toState = reader.getBlockState(toPos);

		if (!toState.isOf(this))
			return facing != face.getOpposite();

		BlockPos diff = fromPos.subtract(toPos);
		int coord = facing.getAxis()
			.choose(diff.getX(), diff.getY(), diff.getZ());
		return facing == toState.get(FACING)
			.getOpposite()
			&& !(coord != 0 && coord == facing.getDirection()
				.offset());
	}

	@Override
	public boolean canConnectTexturesToward(BlockRenderView reader, BlockPos fromPos, BlockPos toPos,
		BlockState state) {
		Direction facing = state.get(FACING);
		BlockState toState = reader.getBlockState(toPos);

		if (toPos.equals(fromPos.offset(facing)))
			return false;

		BlockPos diff = fromPos.subtract(toPos);
		int coord = facing.getAxis()
			.choose(diff.getX(), diff.getY(), diff.getZ());

		if (!toState.isOf(this))
			return coord != -facing.getDirection()
				.offset();

		if (isOccluded(state, toState, facing))
			return true;
		if (toState.with(WATERLOGGED, false) == state.with(WATERLOGGED, false) && coord == 0)
			return true;

		return false;
	}

	@Override
	public boolean canFaceBeOccluded(BlockState state, Direction face) {
		return state.get(FACING)
			.getOpposite() == face;
	}

	@Override
	public boolean shouldFaceAlwaysRender(BlockState state, Direction face) {
		return canFaceBeOccluded(state, face.getOpposite());
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		BlockState stateForPlacement = super.getPlacementState(pContext);
		return stateForPlacement.with(FACING, pContext.getPlayerLookDirection()
			.getOpposite());
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(FACING));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.CASING_3PX.get(pState.get(FACING));
	}

	@Override
	public boolean canPathfindThrough(BlockState pState, BlockView pLevel, BlockPos pPos, NavigationType pType) {
		return false;
	}

	@Override
	public boolean supportsExternalFaceHiding(BlockState state) {
		return true;
	}

	@Override
	public boolean hidesNeighborFace(BlockView level, BlockPos pos, BlockState state, BlockState neighborState,
		Direction dir) {
		if (state.isOf(this) == neighborState.isOf(this)) {
			if (CopycatSpecialCases.isBarsMaterial(getMaterial(level, pos))
				&& CopycatSpecialCases.isBarsMaterial(getMaterial(level, pos.offset(dir))))
				return state.get(FACING) == neighborState.get(FACING);
			if (getMaterial(level, pos).isSideInvisible(getMaterial(level, pos.offset(dir)), dir.getOpposite()))
				return isOccluded(state, neighborState, dir.getOpposite());
		}

		return state.get(FACING) == dir.getOpposite()
			&& getMaterial(level, pos).isSideInvisible(neighborState, dir.getOpposite());
	}

	public static boolean isOccluded(BlockState state, BlockState other, Direction pDirection) {
		state = state.with(WATERLOGGED, false);
		other = other.with(WATERLOGGED, false);
		Direction facing = state.get(FACING);
		if (facing.getOpposite() == other.get(FACING) && pDirection == facing)
			return true;
		if (other.get(FACING) != facing)
			return false;
		return pDirection.getAxis() != facing.getAxis();
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rot) {
		return state.with(FACING, rot.rotate(state.get(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.get(FACING)));
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.COPYCAT_PANEL::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return AllBlocks.COPYCAT_PANEL::has;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(),
				state.get(FACING)
					.getAxis(),
				dir -> world.getBlockState(pos.offset(dir))
					.isReplaceable());

			if (directions.isEmpty())
				return PlacementOffset.fail();
			else {
				return PlacementOffset.success(pos.offset(directions.get(0)),
					s -> s.with(FACING, state.get(FACING)));
			}
		}
	}

}
