package com.simibubi.create.content.kinetics.drill;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DrillBlock extends DirectionalKineticBlock implements IBE<DrillBlockEntity>, Waterloggable {
	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public DrillBlock(Settings properties) {
		super(properties);
		setDefaultState(super.getDefaultState().with(Properties.WATERLOGGED, false));
	}

	@Override
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		if (entityIn instanceof ItemEntity)
			return;
		if (!new Box(pos).contract(.1f)
			.intersects(entityIn.getBoundingBox()))
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			if (be.getSpeed() == 0)
				return;
			entityIn.damage(CreateDamageSources.drill(worldIn), (float) getDamage(be.getSpeed()));
		});
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.CASING_12PX.get(state.get(FACING));
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		withBlockEntityDo(worldIn, pos, DrillBlockEntity::destroyNextTick);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(FACING)
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face == state.get(FACING)
			.getOpposite();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
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

	public static double getDamage(float speed) {
		float speedAbs = Math.abs(speed);
		double sub1 = Math.min(speedAbs / 16, 2);
		double sub2 = Math.min(speedAbs / 32, 4);
		double sub3 = Math.min(speedAbs / 64, 4);
		return MathHelper.clamp(sub1 + sub2 + sub3, 1, 10);
	}

	@Override
	public Class<DrillBlockEntity> getBlockEntityClass() {
		return DrillBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DrillBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.DRILL.get();
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		ItemStack heldItem = player.getStackInHand(hand);

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (!player.isSneaking() && player.canModifyBlocks()) {
			if (placementHelper.matchesItem(heldItem)) {
				placementHelper.getOffset(player, world, state, pos, ray)
					.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
				return ActionResult.SUCCESS;
			}
		}

		return ActionResult.PASS;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.MECHANICAL_DRILL::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return AllBlocks.MECHANICAL_DRILL::has;
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
