package com.simibubi.create.content.contraptions.actors.roller;

import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.AttachedActorBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PoleHelper;

public class RollerBlock extends AttachedActorBlock implements IBE<RollerBlockEntity> {
	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public RollerBlock(Settings p_i48377_1_) {
		super(p_i48377_1_);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return withWater(getDefaultState().with(FACING, context.getHorizontalPlayerFacing()
			.getOpposite()), context);
	}

	@Override
	public Class<RollerBlockEntity> getBlockEntityClass() {
		return RollerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RollerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MECHANICAL_ROLLER.get();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return VoxelShapes.fullCube();
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
		return true;
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		withBlockEntityDo(pLevel, pPos, RollerBlockEntity::searchForSharedValues);
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

	private static class PlacementHelper extends PoleHelper<Direction> {

		public PlacementHelper() {
			super(AllBlocks.MECHANICAL_ROLLER::has, state -> state.get(FACING)
				.rotateYClockwise()
				.getAxis(), FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.MECHANICAL_ROLLER::isIn;
		}

	}

}
