package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class WhistleExtenderBlock extends Block implements IWrenchable {

	public static final EnumProperty<WhistleExtenderShape> SHAPE =
		EnumProperty.of("shape", WhistleExtenderShape.class);
	public static final EnumProperty<WhistleSize> SIZE = WhistleBlock.SIZE;

	public static enum WhistleExtenderShape implements StringIdentifiable {
		SINGLE, DOUBLE, DOUBLE_CONNECTED;

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	public WhistleExtenderBlock(Settings p_49795_) {
		super(p_49795_);
		setDefaultState(getDefaultState().with(SHAPE, WhistleExtenderShape.SINGLE)
			.with(SIZE, WhistleSize.MEDIUM));
	}

	@Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();

		if (context.getHitPos().y < context.getBlockPos()
			.getY() + .5f || state.get(SHAPE) == WhistleExtenderShape.SINGLE)
			return IWrenchable.super.onSneakWrenched(state, context);
		if (!(world instanceof ServerWorld))
			return ActionResult.SUCCESS;
		world.setBlockState(pos, state.with(SHAPE, WhistleExtenderShape.SINGLE), 3);
		playRemoveSound(world, pos);
		return ActionResult.SUCCESS;
	}

	protected ItemUsageContext relocateContext(ItemUsageContext context, BlockPos target) {
		return new ItemUsageContext(context.getPlayer(), context.getHand(),
			new BlockHitResult(context.getHitPos(), context.getSide(), target, context.hitsInsideBlock()));
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (pPlayer == null || !AllBlocks.STEAM_WHISTLE.isIn(pPlayer.getStackInHand(pHand)))
			return ActionResult.PASS;
		World level = pLevel;
		BlockPos findRoot = findRoot(level, pPos);
		BlockState blockState = level.getBlockState(findRoot);
		if (blockState.getBlock()instanceof WhistleBlock whistle)
			return whistle.onUse(blockState, pLevel, findRoot, pPlayer, pHand,
				new BlockHitResult(pHit.getPos(), pHit.getSide(), findRoot, pHit.isInsideBlock()));
		return ActionResult.PASS;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World level = context.getWorld();
		BlockPos findRoot = findRoot(level, context.getBlockPos());
		BlockState blockState = level.getBlockState(findRoot);
		if (blockState.getBlock()instanceof WhistleBlock whistle)
			return whistle.onWrenched(blockState, relocateContext(context, findRoot));
		return IWrenchable.super.onWrenched(state, context);
	}

	@Override
	public ItemStack getPickStack(BlockView level, BlockPos pos, BlockState state) {
		return AllBlocks.STEAM_WHISTLE.asStack();
	}

	public static BlockPos findRoot(WorldAccess pLevel, BlockPos pPos) {
		BlockPos currentPos = pPos.down();
		while (true) {
			BlockState blockState = pLevel.getBlockState(currentPos);
			if (AllBlocks.STEAM_WHISTLE_EXTENSION.has(blockState)) {
				currentPos = currentPos.down();
				continue;
			}
			return currentPos;
		}
	}

	@Override
	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		BlockState below = pLevel.getBlockState(pPos.down());
		return below.isOf(this) && below.get(SHAPE) != WhistleExtenderShape.SINGLE
			|| AllBlocks.STEAM_WHISTLE.has(below);
	}

	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		if (pFacing.getAxis() != Axis.Y)
			return pState;

		if (pFacing == Direction.UP) {
			boolean connected = pState.get(SHAPE) == WhistleExtenderShape.DOUBLE_CONNECTED;
			boolean shouldConnect = pLevel.getBlockState(pCurrentPos.up())
				.isOf(this);
			if (!connected && shouldConnect)
				return pState.with(SHAPE, WhistleExtenderShape.DOUBLE_CONNECTED);
			if (connected && !shouldConnect)
				return pState.with(SHAPE, WhistleExtenderShape.DOUBLE);
			return pState;
		}

		return !pState.canPlaceAt(pLevel, pCurrentPos) ? Blocks.AIR.getDefaultState()
			: pState.with(SIZE, pLevel.getBlockState(pCurrentPos.down())
				.get(SIZE));
	}

	@Override
	public void onBlockAdded(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		if (pOldState.getBlock() != this || pOldState.get(SHAPE) != pState.get(SHAPE))
			WhistleBlock.queuePitchUpdate(pLevel, findRoot(pLevel, pPos));
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (pNewState.getBlock() != this)
			WhistleBlock.queuePitchUpdate(pLevel, findRoot(pLevel, pPos));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(SHAPE, SIZE));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		WhistleSize size = pState.get(SIZE);
		switch (pState.get(SHAPE)) {
		case DOUBLE:
			return size == WhistleSize.LARGE ? AllShapes.WHISTLE_EXTENDER_LARGE_DOUBLE
				: size == WhistleSize.MEDIUM ? AllShapes.WHISTLE_EXTENDER_MEDIUM_DOUBLE
					: AllShapes.WHISTLE_EXTENDER_SMALL_DOUBLE;
		case DOUBLE_CONNECTED:
			return size == WhistleSize.LARGE ? AllShapes.WHISTLE_EXTENDER_LARGE_DOUBLE_CONNECTED
				: size == WhistleSize.MEDIUM ? AllShapes.WHISTLE_EXTENDER_MEDIUM_DOUBLE_CONNECTED
					: AllShapes.WHISTLE_EXTENDER_SMALL_DOUBLE_CONNECTED;
		case SINGLE:
		default:
			return size == WhistleSize.LARGE ? AllShapes.WHISTLE_EXTENDER_LARGE
				: size == WhistleSize.MEDIUM ? AllShapes.WHISTLE_EXTENDER_MEDIUM : AllShapes.WHISTLE_EXTENDER_SMALL;
		}
	}

//	@Override // fabric: difficult to implement with little to gain
//	public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState,
//		Direction dir) {
//		return AllBlocks.STEAM_WHISTLE.has(neighborState) && dir == Direction.DOWN;
//	}

}