package com.simibubi.create.content.equipment.clipboard;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.gui.ScreenOpener;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
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
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ClipboardBlock extends WallMountedBlock
	implements IBE<ClipboardBlockEntity>, IWrenchable, ProperWaterloggedBlock {

	public static final BooleanProperty WRITTEN = BooleanProperty.of("written");

	public ClipboardBlock(Settings pProperties) {
		super(pProperties);
		setDefaultState(getDefaultState().with(WATERLOGGED, false)
			.with(WRITTEN, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(WRITTEN, FACE, FACING, WATERLOGGED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		BlockState stateForPlacement = super.getPlacementState(pContext);
		if (stateForPlacement == null)
			return null;
		if (stateForPlacement.get(FACE) != WallMountLocation.WALL)
			stateForPlacement = stateForPlacement.with(FACING, stateForPlacement.get(FACING)
				.getOpposite());
		return withWater(stateForPlacement, pContext).with(WRITTEN, pContext.getStack()
			.hasNbt());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return (switch (pState.get(FACE)) {
		case FLOOR -> AllShapes.CLIPBOARD_FLOOR;
		case CEILING -> AllShapes.CLIPBOARD_CEILING;
		default -> AllShapes.CLIPBOARD_WALL;
		}).get(pState.get(FACING));
	}

	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		return !pLevel.getBlockState(pPos.offset(getDirection(pState).getOpposite()))
			.isReplaceable();
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (pPlayer.isSneaking()) {
			breakAndCollect(pState, pLevel, pPos, pPlayer);
			return ActionResult.SUCCESS;
		}

		return onBlockEntityUse(pLevel, pPos, cbe -> {
			if (pLevel.isClient())
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> openScreen(pPlayer, cbe.dataContainer, pPos));
			return ActionResult.SUCCESS;
		});
	}

	@Environment(EnvType.CLIENT)
	private void openScreen(PlayerEntity player, ItemStack stack, BlockPos pos) {
		if (MinecraftClient.getInstance().player == player)
			ScreenOpener.open(new ClipboardScreen(player.getInventory().selectedSlot, stack, pos));
	}

	@Override
	public void onBlockBreakStart(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer) {
		breakAndCollect(pState, pLevel, pPos, pPlayer);
	}

	private void breakAndCollect(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer) {
		if (pPlayer instanceof FakePlayer)
			return;
		if (pLevel.isClient)
			return;
		ItemStack cloneItemStack = getPickStack(pLevel, pPos, pState);
		pLevel.breakBlock(pPos, false);
		if (pLevel.getBlockState(pPos) != pState)
			pPlayer.getInventory()
				.offerOrDrop(cloneItemStack);
	}

	@Override
	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		if (world.getBlockEntity(pos) instanceof ClipboardBlockEntity cbe)
			return cbe.dataContainer;
		return new ItemStack(this);
	}

	@Override
	public void onBreak(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity pPlayer) {
		if (!(pLevel.getBlockEntity(pPos) instanceof ClipboardBlockEntity cbe))
			return;
		if (pLevel.isClient || pPlayer.isCreative())
			return;
		Block.dropStack(pLevel, pPos, cbe.dataContainer.copy());
	}

	@Override
	@SuppressWarnings("deprecation")
	public List<ItemStack> getDroppedStacks(BlockState pState, LootContextParameterSet.Builder pBuilder) {
		if (!(pBuilder.getOptional(LootContextParameters.BLOCK_ENTITY) instanceof ClipboardBlockEntity cbe))
			return super.getDroppedStacks(pState, pBuilder);
		pBuilder.addDynamicDrop(ShulkerBoxBlock.CONTENTS_DYNAMIC_DROP_ID, p_56219_ -> p_56219_.accept(cbe.dataContainer.copy()));
		return ImmutableList.of(cbe.dataContainer.copy());
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return super.getStateForNeighborUpdate(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
	}

	@Override
	public Class<ClipboardBlockEntity> getBlockEntityClass() {
		return ClipboardBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ClipboardBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CLIPBOARD.get();
	}

}
