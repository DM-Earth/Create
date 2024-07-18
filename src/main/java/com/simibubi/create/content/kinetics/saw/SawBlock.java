package com.simibubi.create.content.kinetics.saw;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SawBlock extends DirectionalAxisKineticBlock implements IBE<SawBlockEntity> {
	public static final BooleanProperty FLIPPED = BooleanProperty.of("flipped");

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public SawBlock(Settings properties) {
		super(properties);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(FLIPPED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState stateForPlacement = super.getPlacementState(context);
		Direction facing = stateForPlacement.get(FACING);
		return stateForPlacement.with(FLIPPED, facing.getAxis() == Axis.Y && context.getHorizontalPlayerFacing()
			.getDirection() == AxisDirection.POSITIVE);
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		BlockState newState = super.getRotatedBlockState(originalState, targetedFace);
		if (newState.get(FACING)
			.getAxis() != Axis.Y)
			return newState;
		if (targetedFace.getAxis() != Axis.Y)
			return newState;
		if (!originalState.get(AXIS_ALONG_FIRST_COORDINATE))
			newState = newState.cycle(FLIPPED);
		return newState;
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rot) {
		BlockState newState = super.rotate(state, rot);
		if (state.get(FACING)
			.getAxis() != Axis.Y)
			return newState;

		if (rot.ordinal() % 2 == 1 && (rot == BlockRotation.CLOCKWISE_90) != state.get(AXIS_ALONG_FIRST_COORDINATE))
			newState = newState.cycle(FLIPPED);
		if (rot == BlockRotation.CLOCKWISE_180)
			newState = newState.cycle(FLIPPED);

		return newState;
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
		BlockState newState = super.mirror(state, mirrorIn);
		if (state.get(FACING)
			.getAxis() != Axis.Y)
			return newState;

		boolean alongX = state.get(AXIS_ALONG_FIRST_COORDINATE);
		if (alongX && mirrorIn == BlockMirror.FRONT_BACK)
			newState = newState.cycle(FLIPPED);
		if (!alongX && mirrorIn == BlockMirror.LEFT_RIGHT)
			newState = newState.cycle(FLIPPED);

		return newState;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.CASING_12PX.get(state.get(FACING));
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		ItemStack heldItem = player.getStackInHand(handIn);
		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (!player.isSneaking() && player.canModifyBlocks()) {
			if (placementHelper.matchesItem(heldItem) && placementHelper.getOffset(player, worldIn, state, pos, hit)
				.placeInWorld(worldIn, (BlockItem) heldItem.getItem(), player, handIn, hit)
				.isAccepted())
				return ActionResult.SUCCESS;
		}

		if (player.isSpectator() || !player.getStackInHand(handIn)
			.isEmpty())
			return ActionResult.PASS;
		if (state.getOrEmpty(FACING)
			.orElse(Direction.WEST) != Direction.UP)
			return ActionResult.PASS;

		return onBlockEntityUse(worldIn, pos, be -> {
			for (int i = 0; i < be.inventory.getSlotCount(); i++) {
				ItemStack heldItemStack = be.inventory.getStackInSlot(i);
				if (!worldIn.isClient && !heldItemStack.isEmpty())
					player.getInventory()
						.offerOrDrop(heldItemStack);
			}
			be.inventory.clear();
			be.notifyUpdate();
			return ActionResult.SUCCESS;
		});
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
			entityIn.damage(CreateDamageSources.saw(worldIn), (float) DrillBlock.getDamage(be.getSpeed()));
		});
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		if (!(entityIn instanceof ItemEntity))
			return;
		if (entityIn.getWorld().isClient)
			return;

		BlockPos pos = entityIn.getBlockPos();
		withBlockEntityDo(entityIn.getWorld(), pos, be -> {
			if (be.getSpeed() == 0)
				return;
			be.insertItem((ItemEntity) entityIn);
		});
	}

	public static boolean isHorizontal(BlockState state) {
		return state.get(FACING)
			.getAxis()
			.isHorizontal();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return isHorizontal(state) ? state.get(FACING)
			.getAxis() : super.getRotationAxis(state);
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return isHorizontal(state) ? face == state.get(FACING)
			.getOpposite() : super.hasShaftTowards(world, pos, state, face);
	}

	@Override
	public Class<SawBlockEntity> getBlockEntityClass() {
		return SawBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SawBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SAW.get();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.MECHANICAL_SAW::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return state -> AllBlocks.MECHANICAL_SAW.has(state);
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
					s -> s.with(FACING, state.get(FACING))
					.with(AXIS_ALONG_FIRST_COORDINATE, state.get(AXIS_ALONG_FIRST_COORDINATE))
					.with(FLIPPED, state.get(FLIPPED)));
			}
		}

	}

}
