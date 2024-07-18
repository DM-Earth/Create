package com.simibubi.create.content.logistics.chute;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;

public class ChuteBlock extends AbstractChuteBlock implements ProperWaterloggedBlock {

	public static final Property<Shape> SHAPE = EnumProperty.of("shape", Shape.class);
	public static final DirectionProperty FACING = Properties.HOPPER_FACING;

	public ChuteBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
		setDefaultState(getDefaultState().with(SHAPE, Shape.NORMAL)
			.with(FACING, Direction.DOWN)
			.with(WATERLOGGED, false));
	}

	public enum Shape implements StringIdentifiable {
		INTERSECTION, WINDOW, NORMAL, ENCASED;

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	@Override
	public Direction getFacing(BlockState state) {
		return state.get(FACING);
	}

	@Override
	public boolean isOpen(BlockState state) {
		return state.get(FACING) == Direction.DOWN || state.get(SHAPE) == Shape.INTERSECTION;
	}

	@Override
	public boolean isTransparent(BlockState state) {
		return state.get(SHAPE) == Shape.WINDOW;
	}
	
	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		Shape shape = state.get(SHAPE);
		boolean down = state.get(FACING) == Direction.DOWN;
		if (shape == Shape.INTERSECTION)
			return ActionResult.PASS;
		World level = context.getWorld();
		if (level.isClient)
			return ActionResult.SUCCESS;
		if (shape == Shape.ENCASED) {
			level.setBlockState(context.getBlockPos(), state.with(SHAPE, Shape.NORMAL));
			level.syncWorldEvent(2001, context.getBlockPos(),
				Block.getRawIdFromState(AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState()));
			return ActionResult.SUCCESS;
		}
		if (down)
			level.setBlockState(context.getBlockPos(),
				state.with(SHAPE, shape != Shape.NORMAL ? Shape.NORMAL : Shape.WINDOW));
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult hitResult) {
		Shape shape = state.get(SHAPE);
		if (!AllBlocks.INDUSTRIAL_IRON_BLOCK.isIn(player.getStackInHand(hand)))
			return super.onUse(state, level, pos, player, hand, hitResult);
		if (shape == Shape.INTERSECTION || shape == Shape.ENCASED)
			return super.onUse(state, level, pos, player, hand, hitResult);
		if (player == null || level.isClient)
			return ActionResult.SUCCESS;

		level.setBlockState(pos, state.with(SHAPE, Shape.ENCASED));
		level.playSound(null, pos, SoundEvents.BLOCK_NETHERITE_BLOCK_HIT, SoundCategory.BLOCKS, 0.5f, 1.05f);
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState state = withWater(super.getPlacementState(ctx), ctx);
		Direction face = ctx.getSide();
		if (face.getAxis()
			.isHorizontal() && !ctx.shouldCancelInteraction()) {
			World world = ctx.getWorld();
			BlockPos pos = ctx.getBlockPos();
			return updateChuteState(state.with(FACING, face), world.getBlockState(pos.up()), world, pos);
		}
		return state;
	}
	
	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState above, WorldAccess world,
		BlockPos pos, BlockPos p_196271_6_) {
		updateWater(world, state, pos);
		return super.getStateForNeighborUpdate(state, direction, above, world, pos, p_196271_6_);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		super.appendProperties(p_206840_1_.add(SHAPE, FACING, WATERLOGGED));
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState above = world.getBlockState(pos.up());
		return !isChute(above) || getChuteFacing(above) == Direction.DOWN;
	}

	@Override
	public BlockState updateChuteState(BlockState state, BlockState above, BlockView world, BlockPos pos) {
		if (!(state.getBlock() instanceof ChuteBlock))
			return state;

		Map<Direction, Boolean> connections = new HashMap<>();
		int amtConnections = 0;
		Direction facing = state.get(FACING);
		boolean vertical = facing == Direction.DOWN;

		if (!vertical) {
			BlockState target = world.getBlockState(pos.down()
				.offset(facing.getOpposite()));
			if (!isChute(target))
				return state.with(FACING, Direction.DOWN)
					.with(SHAPE, Shape.NORMAL);
		}

		for (Direction direction : Iterate.horizontalDirections) {
			BlockState diagonalInputChute = world.getBlockState(pos.up()
				.offset(direction));
			boolean value =
				diagonalInputChute.getBlock() instanceof ChuteBlock && diagonalInputChute.get(FACING) == direction;
			connections.put(direction, value);
			if (value)
				amtConnections++;
		}

		boolean noConnections = amtConnections == 0;
		if (vertical)
			return state.with(SHAPE,
				noConnections ? state.get(SHAPE) == Shape.INTERSECTION ? Shape.NORMAL : state.get(SHAPE)
					: Shape.INTERSECTION);
		if (noConnections)
			return state.with(SHAPE, Shape.INTERSECTION);
		if (connections.get(Direction.NORTH) && connections.get(Direction.SOUTH))
			return state.with(SHAPE, Shape.INTERSECTION);
		if (connections.get(Direction.EAST) && connections.get(Direction.WEST))
			return state.with(SHAPE, Shape.INTERSECTION);
		if (amtConnections == 1 && connections.get(facing) && !(getChuteFacing(above) == Direction.DOWN)
			&& !(above.getBlock() instanceof FunnelBlock && FunnelBlock.getFunnelFacing(above) == Direction.DOWN))
			return state.with(SHAPE, state.get(SHAPE) == Shape.ENCASED ? Shape.ENCASED : Shape.NORMAL);
		return state.with(SHAPE, Shape.INTERSECTION);
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

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public BlockEntityType<? extends ChuteBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CHUTE.get();
	}

}
