package com.simibubi.create.content.trains.station;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.gui.ScreenOpener;

import com.simibubi.create.foundation.utility.AdventureUtil;
import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
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

public class StationBlock extends Block implements IBE<StationBlockEntity>, IWrenchable, ProperWaterloggedBlock {

	public static final BooleanProperty ASSEMBLING = BooleanProperty.of("assembling");

	public StationBlock(Settings p_54120_) {
		super(p_54120_);
		setDefaultState(getDefaultState().with(ASSEMBLING, false)
			.with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(ASSEMBLING, WATERLOGGED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		return withWater(super.getPlacementState(pContext), pContext);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public boolean hasComparatorOutput(BlockState pState) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState pState, World pLevel, BlockPos pPos) {
		return getBlockEntityOptional(pLevel, pPos).map(ste -> ste.trainPresent ? 15 : 0)
			.orElse(0);
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, worldIn, pos, newState);
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		SharedDepotBlockMethods.onLanded(worldIn, entityIn);
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {

		if (pPlayer == null || pPlayer.isSneaking())
			return ActionResult.PASS;
		ItemStack itemInHand = pPlayer.getStackInHand(pHand);
		if (AllItems.WRENCH.isIn(itemInHand))
			return ActionResult.PASS;

		if (itemInHand.getItem() == Items.FILLED_MAP) {
			return onBlockEntityUse(pLevel, pPos, station -> {
				if (pLevel.isClient)
					return ActionResult.SUCCESS;

				if (station.getStation() == null || station.getStation().getId() == null)
					return ActionResult.FAIL;

				MapState savedData = FilledMapItem.getMapState(itemInHand, pLevel);
				if (!(savedData instanceof StationMapData stationMapData))
					return ActionResult.FAIL;

				if (!stationMapData.toggleStation(pLevel, pPos, station))
					return ActionResult.FAIL;

				return ActionResult.SUCCESS;
			});
		}

		ActionResult result = onBlockEntityUse(pLevel, pPos, station -> {
			ItemStack autoSchedule = station.getAutoSchedule();
			if (autoSchedule.isEmpty())
				return ActionResult.PASS;
			if (pLevel.isClient)
				return ActionResult.SUCCESS;
			pPlayer.getInventory()
				.offerOrDrop(autoSchedule.copy());
			station.depotBehaviour.removeHeldItem();
			station.notifyUpdate();
			AllSoundEvents.playItemPickup(pPlayer);
			return ActionResult.SUCCESS;
		});

		if (result == ActionResult.PASS)
			EnvExecutor.runWhenOn(EnvType.CLIENT,
				() -> () -> withBlockEntityDo(pLevel, pPos, be -> this.displayScreen(be, pPlayer)));
		return ActionResult.SUCCESS;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayScreen(StationBlockEntity be, PlayerEntity player) {
		if (!(player instanceof ClientPlayerEntity))
			return;
		GlobalStation station = be.getStation();
		BlockState blockState = be.getCachedState();
		if (station == null || blockState == null)
			return;
		boolean assembling = blockState.getBlock() == this && blockState.get(ASSEMBLING);
		ScreenOpener.open(assembling ? new AssemblyScreen(be, station) : new StationScreen(be, station));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.STATION;
	}

	@Override
	public Class<StationBlockEntity> getBlockEntityClass() {
		return StationBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StationBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.TRACK_STATION.get();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
