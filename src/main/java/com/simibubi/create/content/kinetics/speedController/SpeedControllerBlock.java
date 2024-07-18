package com.simibubi.create.content.kinetics.speedController;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpeedControllerBlock extends HorizontalAxisKineticBlock implements IBE<SpeedControllerBlockEntity> {

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public SpeedControllerBlock(Settings properties) {
		super(properties);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState above = context.getWorld()
			.getBlockState(context.getBlockPos()
				.up());
		if (ICogWheel.isLargeCog(above) && above.get(CogWheelBlock.AXIS)
			.isHorizontal())
			return getDefaultState().with(HORIZONTAL_AXIS, above.get(CogWheelBlock.AXIS) == Axis.X ? Axis.Z : Axis.X);
		return super.getPlacementState(context);
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block p_220069_4_, BlockPos neighbourPos,
		boolean p_220069_6_) {
		if (neighbourPos.equals(pos.up()))
			withBlockEntityDo(world, pos, SpeedControllerBlockEntity::updateBracket);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {

		ItemStack heldItem = player.getStackInHand(hand);
		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray).placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		return ActionResult.PASS;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.SPEED_CONTROLLER;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ((Predicate<ItemStack>) ICogWheel::isLargeCogItem).and(ICogWheel::isDedicatedCogItem);
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return AllBlocks.ROTATION_SPEED_CONTROLLER::has;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos, BlockHitResult ray) {
			BlockPos newPos = pos.up();
			if (!world.getBlockState(newPos)
				.isReplaceable())
				return PlacementOffset.fail();

			Axis newAxis = state.get(HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X;

			if (!CogWheelBlock.isValidCogwheelPosition(true, world, newPos, newAxis))
				return PlacementOffset.fail();

			return PlacementOffset.success(newPos, s -> s.with(CogWheelBlock.AXIS, newAxis));
		}
	}

	@Override
	public Class<SpeedControllerBlockEntity> getBlockEntityClass() {
		return SpeedControllerBlockEntity.class;
	}
	
	@Override
	public BlockEntityType<? extends SpeedControllerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ROTATION_SPEED_CONTROLLER.get();
	}
}
