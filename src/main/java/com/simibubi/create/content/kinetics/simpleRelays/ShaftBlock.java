package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.google.common.base.Predicates;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.placement.PoleHelper;

public class ShaftBlock extends AbstractSimpleShaftBlock implements EncasableBlock {

	public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public ShaftBlock(Settings properties) {
		super(properties);
	}

	public static boolean isShaft(BlockState state) {
		return AllBlocks.SHAFT.has(state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState stateForPlacement = super.getPlacementState(context);
		return pickCorrectShaftType(stateForPlacement, context.getWorld(), context.getBlockPos());
	}

	public static BlockState pickCorrectShaftType(BlockState stateForPlacement, World level, BlockPos pos) {
		if (PoweredShaftBlock.stillValid(stateForPlacement, level, pos))
			return PoweredShaftBlock.getEquivalent(stateForPlacement);
		return stateForPlacement;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.SIX_VOXEL_POLE.get(state.get(AXIS));
	}

	@Override
	public float getParticleTargetRadius() {
		return .35f;
	}

	@Override
	public float getParticleInitialRadius() {
		return .125f;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		if (player.isSneaking() || !player.canModifyBlocks())
			return ActionResult.PASS;

		ItemStack heldItem = player.getStackInHand(hand);
		ActionResult result = tryEncase(state, world, pos, heldItem, player, hand, ray);
		if (result.isAccepted())
			return result;

		if (AllBlocks.METAL_GIRDER.isIn(heldItem) && state.get(AXIS) != Axis.Y) {
			KineticBlockEntity.switchToBlockState(world, pos, AllBlocks.METAL_GIRDER_ENCASED_SHAFT.getDefaultState()
				.with(WATERLOGGED, state.get(WATERLOGGED))
				.with(GirderEncasedShaftBlock.HORIZONTAL_AXIS, state.get(AXIS) == Axis.Z ? Axis.Z : Axis.X));
			if (!world.isClient && !player.isCreative()) {
				heldItem.decrement(1);
				if (heldItem.isEmpty())
					player.setStackInHand(hand, ItemStack.EMPTY);
			}
			return ActionResult.SUCCESS;
		}

		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		return ActionResult.PASS;
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper extends PoleHelper<Direction.Axis> {
		// used for extending a shaft in its axis, like the piston poles. works with
		// shafts and cogs

		private PlacementHelper() {
			super(state -> state.getBlock() instanceof AbstractSimpleShaftBlock
				|| state.getBlock() instanceof PoweredShaftBlock, state -> state.get(AXIS), AXIS);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof AbstractSimpleShaftBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(AllBlocks.SHAFT::has, AllBlocks.POWERED_SHAFT::has);
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful())
				offset.withTransform(offset.getTransform()
					.andThen(s -> ShaftBlock.pickCorrectShaftType(s, world, offset.getBlockPos())));
			return offset;
		}

	}
}
