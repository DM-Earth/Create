package com.simibubi.create.content.kinetics.simpleRelays;

import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.item.UseFirstBehaviorItem;

public class CogwheelBlockItem extends BlockItem implements UseFirstBehaviorItem {

	boolean large;

	private final int placementHelperId;
	private final int integratedCogHelperId;

	public CogwheelBlockItem(CogWheelBlock block, Settings builder) {
		super(block, builder);
		large = block.isLarge;

		placementHelperId = PlacementHelpers.register(large ? new LargeCogHelper() : new SmallCogHelper());
		integratedCogHelperId =
			PlacementHelpers.register(large ? new IntegratedLargeCogHelper() : new IntegratedSmallCogHelper());
	}

	@Override
	public ActionResult onItemUseFirst(ItemStack stack, ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);

		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		PlayerEntity player = context.getPlayer();
		BlockHitResult ray = new BlockHitResult(context.getHitPos(), context.getSide(), pos, true);
		if (helper.matchesState(state) && player != null && !player.isSneaking()) {
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, this, player, context.getHand(), ray);
		}

		if (integratedCogHelperId != -1) {
			helper = PlacementHelpers.get(integratedCogHelperId);

			if (helper.matchesState(state) && player != null && !player.isSneaking()) {
				return helper.getOffset(player, world, state, pos, ray)
					.placeInWorld(world, this, player, context.getHand(), ray);
			}
		}

		return ActionResult.PASS;
	}

	@MethodsReturnNonnullByDefault
	private static class SmallCogHelper extends DiagonalCogHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ((Predicate<ItemStack>) ICogWheel::isSmallCogItem).and(ICogWheel::isDedicatedCogItem);
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			if (hitOnShaft(state, ray))
				return PlacementOffset.fail();

			if (!ICogWheel.isLargeCog(state)) {
				Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
				List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(), axis);

				for (Direction dir : directions) {
					BlockPos newPos = pos.offset(dir);

					if (!CogWheelBlock.isValidCogwheelPosition(false, world, newPos, axis))
						continue;

					if (!world.getBlockState(newPos)
						.isReplaceable())
						continue;

					return PlacementOffset.success(newPos, s -> s.with(AXIS, axis));

				}

				return PlacementOffset.fail();
			}

			return super.getOffset(player, world, state, pos, ray);
		}
	}

	@MethodsReturnNonnullByDefault
	private static class LargeCogHelper extends DiagonalCogHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ((Predicate<ItemStack>) ICogWheel::isLargeCogItem).and(ICogWheel::isDedicatedCogItem);
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			if (hitOnShaft(state, ray))
				return PlacementOffset.fail();

			if (ICogWheel.isLargeCog(state)) {
				Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
				Direction side = IPlacementHelper.orderedByDistanceOnlyAxis(pos, ray.getPos(), axis)
					.get(0);
				List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(), axis);
				for (Direction dir : directions) {
					BlockPos newPos = pos.offset(dir)
						.offset(side);

					if (!CogWheelBlock.isValidCogwheelPosition(true, world, newPos, dir.getAxis()))
						continue;

					if (!world.getBlockState(newPos)
						.isReplaceable())
						continue;

					return PlacementOffset.success(newPos, s -> s.with(AXIS, dir.getAxis()));
				}

				return PlacementOffset.fail();
			}

			return super.getOffset(player, world, state, pos, ray);
		}
	}

	@MethodsReturnNonnullByDefault
	public abstract static class DiagonalCogHelper implements IPlacementHelper {

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> ICogWheel.isSmallCog(s) || ICogWheel.isLargeCog(s);
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			// diagonal gears of different size
			Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
			Direction closest = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(), axis)
				.get(0);
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(), axis,
				d -> d.getAxis() != closest.getAxis());

			for (Direction dir : directions) {
				BlockPos newPos = pos.offset(dir)
					.offset(closest);
				if (!world.getBlockState(newPos)
					.isReplaceable())
					continue;

				if (!CogWheelBlock.isValidCogwheelPosition(ICogWheel.isLargeCog(state), world, newPos, axis))
					continue;

				return PlacementOffset.success(newPos, s -> s.with(AXIS, axis));
			}

			return PlacementOffset.fail();
		}

		protected boolean hitOnShaft(BlockState state, BlockHitResult ray) {
			return AllShapes.SIX_VOXEL_POLE.get(((IRotate) state.getBlock()).getRotationAxis(state))
				.getBoundingBox()
				.expand(0.001)
				.contains(ray.getPos()
					.subtract(ray.getPos()
						.floorAlongAxes(Iterate.axisSet)));
		}
	}

	@MethodsReturnNonnullByDefault
	public static class IntegratedLargeCogHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ((Predicate<ItemStack>) ICogWheel::isLargeCogItem).and(ICogWheel::isDedicatedCogItem);
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> !ICogWheel.isDedicatedCogWheel(s.getBlock()) && ICogWheel.isSmallCog(s);
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			Direction face = ray.getSide();
			Axis newAxis;

			if (state.contains(HorizontalKineticBlock.HORIZONTAL_FACING))
				newAxis = state.get(HorizontalKineticBlock.HORIZONTAL_FACING)
					.getAxis();
			else if (state.contains(DirectionalKineticBlock.FACING))
				newAxis = state.get(DirectionalKineticBlock.FACING)
					.getAxis();
			else if (state.contains(RotatedPillarKineticBlock.AXIS))
				newAxis = state.get(RotatedPillarKineticBlock.AXIS);
			else
				newAxis = Axis.Y;

			if (face.getAxis() == newAxis)
				return PlacementOffset.fail();

			List<Direction> directions =
				IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(), face.getAxis(), newAxis);

			for (Direction d : directions) {
				BlockPos newPos = pos.offset(face)
					.offset(d);

				if (!world.getBlockState(newPos)
					.isReplaceable())
					continue;

				if (!CogWheelBlock.isValidCogwheelPosition(false, world, newPos, newAxis))
					return PlacementOffset.fail();

				return PlacementOffset.success(newPos, s -> s.with(CogWheelBlock.AXIS, newAxis));
			}

			return PlacementOffset.fail();
		}

	}

	@MethodsReturnNonnullByDefault
	public static class IntegratedSmallCogHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ((Predicate<ItemStack>) ICogWheel::isSmallCogItem).and(ICogWheel::isDedicatedCogItem);
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> !ICogWheel.isDedicatedCogWheel(s.getBlock()) && ICogWheel.isSmallCog(s);
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			Direction face = ray.getSide();
			Axis newAxis;

			if (state.contains(HorizontalKineticBlock.HORIZONTAL_FACING))
				newAxis = state.get(HorizontalKineticBlock.HORIZONTAL_FACING)
					.getAxis();
			else if (state.contains(DirectionalKineticBlock.FACING))
				newAxis = state.get(DirectionalKineticBlock.FACING)
					.getAxis();
			else if (state.contains(RotatedPillarKineticBlock.AXIS))
				newAxis = state.get(RotatedPillarKineticBlock.AXIS);
			else
				newAxis = Axis.Y;

			if (face.getAxis() == newAxis)
				return PlacementOffset.fail();

			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getPos(), newAxis);

			for (Direction d : directions) {
				BlockPos newPos = pos.offset(d);

				if (!world.getBlockState(newPos)
					.isReplaceable())
					continue;

				if (!CogWheelBlock.isValidCogwheelPosition(false, world, newPos, newAxis))
					return PlacementOffset.fail();

				return PlacementOffset.success()
					.at(newPos)
					.withTransform(s -> s.with(CogWheelBlock.AXIS, newAxis));
			}

			return PlacementOffset.fail();
		}

	}
}