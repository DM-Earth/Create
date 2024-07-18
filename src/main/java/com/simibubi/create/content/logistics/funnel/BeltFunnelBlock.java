package com.simibubi.create.content.logistics.funnel;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class BeltFunnelBlock extends AbstractHorizontalFunnelBlock implements ISpecialBlockItemRequirement, BlockPickInteractionAware {

	private BlockEntry<? extends FunnelBlock> parent;

	public static final EnumProperty<Shape> SHAPE = EnumProperty.of("shape", Shape.class);

	public enum Shape implements StringIdentifiable {
		RETRACTED(AllShapes.BELT_FUNNEL_RETRACTED),
		EXTENDED(AllShapes.BELT_FUNNEL_EXTENDED),
		PUSHING(AllShapes.BELT_FUNNEL_PERPENDICULAR),
		PULLING(AllShapes.BELT_FUNNEL_PERPENDICULAR);

		VoxelShaper shaper;

		private Shape(VoxelShaper shaper) {
			this.shaper = shaper;
		}

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	public BeltFunnelBlock(BlockEntry<? extends FunnelBlock> parent, Settings p_i48377_1_) {
		super(p_i48377_1_);
		this.parent = parent;
		setDefaultState(getDefaultState().with(SHAPE, Shape.RETRACTED));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		super.appendProperties(p_206840_1_.add(SHAPE));
	}

	public boolean isOfSameType(FunnelBlock otherFunnel) {
		return parent.get() == otherFunnel;
	}
	
	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (newState.getBlock() instanceof FunnelBlock fb && isOfSameType(fb))
			return;
		super.onStateReplaced(state, world, pos, newState, isMoving);
	}
	
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return state.get(SHAPE).shaper.get(state.get(HORIZONTAL_FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockView p_220071_2_, BlockPos p_220071_3_,
		ShapeContext p_220071_4_) {
		if (p_220071_4_ instanceof EntityShapeContext
			&& ((EntityShapeContext) p_220071_4_).getEntity() instanceof ItemEntity
			&& (p_220071_1_.get(SHAPE) == Shape.PULLING || p_220071_1_.get(SHAPE) == Shape.PUSHING))
			return AllShapes.FUNNEL_COLLISION.get(getFacing(p_220071_1_));
		return getOutlineShape(p_220071_1_, p_220071_2_, p_220071_3_, p_220071_4_);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState stateForPlacement = super.getPlacementState(ctx);
		BlockPos pos = ctx.getBlockPos();
		World world = ctx.getWorld();
		Direction facing = ctx.getSide()
			.getAxis()
			.isHorizontal() ? ctx.getSide() : ctx.getHorizontalPlayerFacing();

		BlockState state = stateForPlacement.with(HORIZONTAL_FACING, facing);
		boolean sneaking = ctx.getPlayer() != null && ctx.getPlayer()
			.isSneaking();
		return state.with(SHAPE, getShapeForPosition(world, pos, facing, !sneaking));
	}

	public static Shape getShapeForPosition(BlockView world, BlockPos pos, Direction facing, boolean extracting) {
		BlockPos posBelow = pos.down();
		BlockState stateBelow = world.getBlockState(posBelow);
		Shape perpendicularState = extracting ? Shape.PUSHING : Shape.PULLING;
		if (!AllBlocks.BELT.has(stateBelow))
			return perpendicularState;
		Direction movementFacing = stateBelow.get(BeltBlock.HORIZONTAL_FACING);
		return movementFacing.getAxis() != facing.getAxis() ? perpendicularState : Shape.RETRACTED;
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockView view, BlockPos pos, @Nullable PlayerEntity player, @Nullable HitResult result) {
		return parent.asStack();
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbour, WorldAccess world,
		BlockPos pos, BlockPos p_196271_6_) {
		updateWater(world, state, pos);
		if (!isOnValidBelt(state, world, pos)) {
			BlockState parentState = ProperWaterloggedBlock.withWater(world, parent.getDefaultState(), pos);
			if (state.getOrEmpty(POWERED)
				.orElse(false))
				parentState = parentState.with(POWERED, true);
			if (state.get(SHAPE) == Shape.PUSHING)
				parentState = parentState.with(FunnelBlock.EXTRACTING, true);
			return parentState.with(FunnelBlock.FACING, state.get(HORIZONTAL_FACING));
		}
		Shape updatedShape =
			getShapeForPosition(world, pos, state.get(HORIZONTAL_FACING), state.get(SHAPE) == Shape.PUSHING);
		Shape currentShape = state.get(SHAPE);
		if (updatedShape == currentShape)
			return state;

		// Don't revert wrenched states
		if (updatedShape == Shape.PUSHING && currentShape == Shape.PULLING)
			return state;
		if (updatedShape == Shape.RETRACTED && currentShape == Shape.EXTENDED)
			return state;

		return state.with(SHAPE, updatedShape);
	}

	public static boolean isOnValidBelt(BlockState state, WorldView world, BlockPos pos) {
		BlockState stateBelow = world.getBlockState(pos.down());
		if ((stateBelow.getBlock() instanceof BeltBlock))
			return BeltBlock.canTransportObjects(stateBelow);
		DirectBeltInputBehaviour directBeltInputBehaviour =
			BlockEntityBehaviour.get(world, pos.down(), DirectBeltInputBehaviour.TYPE);
		if (directBeltInputBehaviour == null)
			return false;
		return directBeltInputBehaviour.canSupportBeltFunnels();
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		if (world.isClient)
			return ActionResult.SUCCESS;

		Shape shape = state.get(SHAPE);
		Shape newShape = shape;
		if (shape == Shape.PULLING)
			newShape = Shape.PUSHING;
		else if (shape == Shape.PUSHING)
			newShape = Shape.PULLING;
		else if (shape == Shape.EXTENDED)
			newShape = Shape.RETRACTED;
		else if (shape == Shape.RETRACTED) {
			BlockState belt = world.getBlockState(context.getBlockPos()
				.down());
			if (belt.getBlock() instanceof BeltBlock && belt.get(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL)
				newShape = Shape.RETRACTED;
			else
				newShape = Shape.EXTENDED;
		}

		if (newShape == shape)
			return ActionResult.SUCCESS;

		world.setBlockState(context.getBlockPos(), state.with(SHAPE, newShape));

		if (newShape == Shape.EXTENDED) {
			Direction facing = state.get(HORIZONTAL_FACING);
			BlockState opposite = world.getBlockState(context.getBlockPos()
				.offset(facing));
			if (opposite.getBlock() instanceof BeltFunnelBlock && opposite.get(SHAPE) == Shape.EXTENDED
				&& opposite.get(HORIZONTAL_FACING) == facing.getOpposite())
				AllAdvancements.FUNNEL_KISS.awardTo(context.getPlayer());
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(parent.getDefaultState(), be);
	}

}
