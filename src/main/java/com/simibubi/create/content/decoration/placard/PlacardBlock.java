package com.simibubi.create.content.decoration.placard;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

public class PlacardBlock extends WallMountedBlock
	implements ProperWaterloggedBlock, IBE<PlacardBlockEntity>, ISpecialBlockItemRequirement, IWrenchable {

	public static final BooleanProperty POWERED = Properties.POWERED;

	public PlacardBlock(Settings p_53182_) {
		super(p_53182_);
		setDefaultState(getDefaultState().with(WATERLOGGED, false)
			.with(POWERED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(FACE, FACING, WATERLOGGED, POWERED));
	}

	@Override
	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		return canAttachLenient(pLevel, pPos, getDirection(pState).getOpposite());
	}

	public static boolean canAttachLenient(WorldView pReader, BlockPos pPos, Direction pDirection) {
		BlockPos blockpos = pPos.offset(pDirection);
		return !pReader.getBlockState(blockpos)
			.getCollisionShape(pReader, blockpos)
			.isEmpty();
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		BlockState stateForPlacement = super.getPlacementState(pContext);
		if (stateForPlacement == null)
			return null;
		if (stateForPlacement.get(FACE) == WallMountLocation.FLOOR)
			stateForPlacement = stateForPlacement.with(FACING, stateForPlacement.get(FACING)
				.getOpposite());
		return withWater(stateForPlacement, pContext);
	}

	@Override
	public boolean emitsRedstonePower(BlockState pState) {
		return true;
	}

	@Override
	public int getWeakRedstonePower(BlockState pBlockState, BlockView pBlockAccess, BlockPos pPos, Direction pSide) {
		return pBlockState.get(POWERED) ? 15 : 0;
	}

	@Override
	public int getStrongRedstonePower(BlockState pBlockState, BlockView pBlockAccess, BlockPos pPos, Direction pSide) {
		return pBlockState.get(POWERED) && getDirection(pBlockState) == pSide ? 15 : 0;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.PLACARD.get(getDirection(pState));
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return super.getStateForNeighborUpdate(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity player, Hand pHand,
		BlockHitResult pHit) {
		if (player.isSneaking())
			return ActionResult.PASS;
		if (pLevel.isClient)
			return ActionResult.SUCCESS;

		ItemStack inHand = player.getStackInHand(pHand);
		return onBlockEntityUse(pLevel, pPos, pte -> {
			ItemStack inBlock = pte.getHeldItem();

			if (!player.canModifyBlocks() || inHand.isEmpty() || !inBlock.isEmpty()) {
				if (inBlock.isEmpty())
					return ActionResult.FAIL;
				if (inHand.isEmpty())
					return ActionResult.FAIL;
				if (pState.get(POWERED))
					return ActionResult.FAIL;

				boolean test = inBlock.getItem() instanceof FilterItem ? FilterItemStack.of(inBlock)
					.test(pLevel, inHand) : ItemHandlerHelper.canItemStacksStack(inHand, inBlock);
				if (!test) {
					AllSoundEvents.DENY.play(pLevel, null, pPos, 1, 1);
					return ActionResult.SUCCESS;
				}

				AllSoundEvents.CONFIRM.play(pLevel, null, pPos, 1, 1);
				pLevel.setBlockState(pPos, pState.with(POWERED, true), 3);
				updateNeighbours(pState, pLevel, pPos);
				pte.poweredTicks = 19;
				pte.notifyUpdate();
				return ActionResult.SUCCESS;
			}

			pLevel.playSound(null, pPos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
			pte.setHeldItem(ItemHandlerHelper.copyStackWithSize(inHand, 1));

			if (!player.isCreative()) {
				inHand.decrement(1);
				if (inHand.isEmpty())
					player.setStackInHand(pHand, ItemStack.EMPTY);
			}

			return ActionResult.SUCCESS;
		});
	}

	public static Direction connectedDirection(BlockState state) {
		return getDirection(state);
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		boolean blockChanged = !pState.isOf(pNewState.getBlock());
		if (!pIsMoving && blockChanged)
			if (pState.get(POWERED))
				updateNeighbours(pState, pLevel, pPos);

		if (pState.hasBlockEntity() && (blockChanged || !pNewState.hasBlockEntity())) {
			if (!pIsMoving)
				withBlockEntityDo(pLevel, pPos, be -> Block.dropStack(pLevel, pPos, be.getHeldItem()));
			pLevel.removeBlockEntity(pPos);
		}
	}

	public static void updateNeighbours(BlockState pState, World pLevel, BlockPos pPos) {
		pLevel.updateNeighborsAlways(pPos, pState.getBlock());
		pLevel.updateNeighborsAlways(pPos.offset(getDirection(pState).getOpposite()), pState.getBlock());
	}

	@Override
	public void onBlockBreakStart(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer) {
		if (pLevel.isClient)
			return;
		withBlockEntityDo(pLevel, pPos, pte -> {
			ItemStack heldItem = pte.getHeldItem();
			if (heldItem.isEmpty())
				return;
			pPlayer.getInventory()
				.offerOrDrop(heldItem);
			pLevel.playSound(null, pPos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1, 1);
			pte.setHeldItem(ItemStack.EMPTY);
		});
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		ItemStack placardStack = AllBlocks.PLACARD.asStack();
		if (be instanceof PlacardBlockEntity pbe) {
			ItemStack heldItem = pbe.getHeldItem();
			if (!heldItem.isEmpty()) {
				return new ItemRequirement(List.of(
					new ItemRequirement.StackRequirement(placardStack, ItemUseType.CONSUME),
					new ItemRequirement.StrictNbtStackRequirement(heldItem, ItemUseType.CONSUME)
				));
			}
		}
		return new ItemRequirement(ItemUseType.CONSUME, placardStack);
	}

	@Override
	public Class<PlacardBlockEntity> getBlockEntityClass() {
		return PlacardBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PlacardBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.PLACARD.get();
	}

}
