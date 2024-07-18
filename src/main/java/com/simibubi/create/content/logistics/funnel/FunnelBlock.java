package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class FunnelBlock extends AbstractDirectionalFunnelBlock {

	public static final BooleanProperty EXTRACTING = BooleanProperty.of("extracting");

	public FunnelBlock(Settings p_i48415_1_) {
		super(p_i48415_1_);
		setDefaultState(getDefaultState().with(EXTRACTING, false));
	}

	public abstract BlockState getEquivalentBeltFunnel(BlockView world, BlockPos pos, BlockState state);

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);

		boolean sneak = context.getPlayer() != null && context.getPlayer()
			.isSneaking();
		state = state.with(EXTRACTING, !sneak);

		for (Direction direction : context.getPlacementDirections()) {
			BlockState blockstate = state.with(FACING, direction.getOpposite());
			if (blockstate.canPlaceAt(context.getWorld(), context.getBlockPos()))
				return blockstate.with(POWERED, state.get(POWERED));
		}

		return state;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(EXTRACTING));
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (newState.getBlock() instanceof BeltFunnelBlock bfb && bfb.isOfSameType(this))
			return;
		super.onStateReplaced(state, world, pos, newState, isMoving);
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(handIn);
		boolean shouldntInsertItem = heldItem.isEmpty() || AllBlocks.MECHANICAL_ARM.isIn(heldItem) || !canInsertIntoFunnel(state);

		if (AllItems.WRENCH.isIn(heldItem))
			return ActionResult.PASS;

		if (hit.getSide() == getFunnelFacing(state) && !shouldntInsertItem) {
			if (!worldIn.isClient)
				withBlockEntityDo(worldIn, pos, be -> {
					ItemStack toInsert = heldItem.copy();
					ItemStack remainder = tryInsert(worldIn, pos, toInsert, false);
					if (!ItemStack.areEqual(remainder, toInsert))
						player.setStackInHand(handIn, remainder);
				});
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		if (!world.isClient)
			world.setBlockState(context.getBlockPos(), state.cycle(EXTRACTING));
		return ActionResult.SUCCESS;
	}

	@Override
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		if (worldIn.isClient)
			return;
		if (!(entityIn instanceof ItemEntity))
			return;
		if (!canInsertIntoFunnel(state))
			return;
		if (!entityIn.isAlive())
			return;
		ItemEntity itemEntity = (ItemEntity) entityIn;

		Direction direction = getFunnelFacing(state);
		Vec3d diff = entityIn.getPos()
			.subtract(VecHelper.getCenterOf(pos)
				.add(Vec3d.of(direction.getVector())
					.multiply(-.325f)));
		double projectedDiff = direction.getAxis()
			.choose(diff.x, diff.y, diff.z);
		if (projectedDiff < 0 == (direction.getDirection() == AxisDirection.POSITIVE))
			return;

		ItemStack toInsert = itemEntity.getStack();
		ItemStack remainder = tryInsert(worldIn, pos, toInsert, false);

		if (remainder.isEmpty())
			itemEntity.discard();
		if (remainder.getCount() < toInsert.getCount())
			itemEntity.setStack(remainder);
	}

	protected boolean canInsertIntoFunnel(BlockState state) {
		return !state.get(POWERED) && !state.get(EXTRACTING);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Direction facing = state.get(FACING);
		return facing == Direction.DOWN ? AllShapes.FUNNEL_CEILING
			: facing == Direction.UP ? AllShapes.FUNNEL_FLOOR : AllShapes.FUNNEL_WALL.get(facing);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if (context instanceof EntityShapeContext
			&& ((EntityShapeContext) context).getEntity() instanceof ItemEntity && getFacing(state).getAxis()
				.isHorizontal())
			return AllShapes.FUNNEL_COLLISION.get(getFacing(state));
		return getOutlineShape(state, world, pos, context);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState p_196271_3_, WorldAccess world,
		BlockPos pos, BlockPos p_196271_6_) {
		updateWater(world, state, pos);
		if (getFacing(state).getAxis()
			.isVertical() || direction != Direction.DOWN)
			return state;
		BlockState equivalentFunnel =
			ProperWaterloggedBlock.withWater(world, getEquivalentBeltFunnel(null, null, state), pos);
		if (BeltFunnelBlock.isOnValidBelt(equivalentFunnel, world, pos))
			return equivalentFunnel.with(BeltFunnelBlock.SHAPE,
				BeltFunnelBlock.getShapeForPosition(world, pos, getFacing(state), state.get(EXTRACTING)));
		return state;
	}

}
