package com.simibubi.create.content.redstone.displayLink;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.RedstonePowerDisplaySource;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;

import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
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

public class DisplayLinkBlock extends WrenchableDirectionalBlock implements IBE<DisplayLinkBlockEntity> {

	public static final BooleanProperty POWERED = Properties.POWERED;

	public DisplayLinkBlock(Settings p_i48415_1_) {
		super(p_i48415_1_);
		setDefaultState(getDefaultState().with(POWERED, false));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState placed = super.getPlacementState(context);
		placed = placed.with(FACING, context.getSide());
		return placed.with(POWERED, shouldBePowered(placed, context.getWorld(), context.getBlockPos()));
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	public static void notifyGatherers(WorldAccess level, BlockPos pos) {
		forEachAttachedGatherer(level, pos, DisplayLinkBlockEntity::updateGatheredData);
	}

	@SuppressWarnings("unchecked")
	public static <T extends DisplaySource> void sendToGatherers(WorldAccess level, BlockPos pos,
		BiConsumer<DisplayLinkBlockEntity, T> callback, Class<T> type) {
		forEachAttachedGatherer(level, pos, dgte -> {
			if (type.isInstance(dgte.activeSource))
				callback.accept(dgte, (T) dgte.activeSource);
		});
	}

	private static void forEachAttachedGatherer(WorldAccess level, BlockPos pos,
		Consumer<DisplayLinkBlockEntity> callback) {
		for (Direction d : Iterate.directions) {
			BlockPos offsetPos = pos.offset(d);
			BlockState blockState = level.getBlockState(offsetPos);
			if (!AllBlocks.DISPLAY_LINK.has(blockState))
				continue;

			BlockEntity blockEntity = level.getBlockEntity(offsetPos);
			if (!(blockEntity instanceof DisplayLinkBlockEntity dlbe))
				continue;
			if (dlbe.activeSource == null)
				continue;
			if (dlbe.getDirection() != d.getOpposite())
				continue;

			callback.accept(dlbe);
		}
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		if (worldIn.isClient)
			return;

		if (fromPos.equals(pos.offset(state.get(FACING)
			.getOpposite())))
			sendToGatherers(worldIn, fromPos, (dlte, p) -> dlte.tickSource(), RedstonePowerDisplaySource.class);

		boolean powered = shouldBePowered(state, worldIn, pos);
		boolean previouslyPowered = state.get(POWERED);
		if (previouslyPowered != powered) {
			worldIn.setBlockState(pos, state.cycle(POWERED), 2);
			if (!powered)
				withBlockEntityDo(worldIn, pos, DisplayLinkBlockEntity::onNoLongerPowered);
		}
	}

	private boolean shouldBePowered(BlockState state, World worldIn, BlockPos pos) {
		boolean powered = false;
		for (Direction d : Iterate.directions) {
			if (d.getOpposite() == state.get(FACING))
				continue;
			if (worldIn.getEmittedRedstonePower(pos.offset(d), d) == 0)
				continue;
			powered = true;
			break;
		}
		return powered;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(POWERED));
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (pPlayer == null || AdventureUtil.isAdventure(pPlayer))
			return ActionResult.PASS;
		if (pPlayer.isSneaking())
			return ActionResult.PASS;
		EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> withBlockEntityDo(pLevel, pPos, be -> this.displayScreen(be, pPlayer)));
		return ActionResult.SUCCESS;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayScreen(DisplayLinkBlockEntity be, PlayerEntity player) {
		if (!(player instanceof ClientPlayerEntity))
			return;
		if (be.targetOffset.equals(BlockPos.ORIGIN)) {
			player.sendMessage(Lang.translateDirect("display_link.invalid"), true);
			return;
		}
		ScreenOpener.open(new DisplayLinkScreen(be));
	}

	@Override
	public boolean canPathfindThrough(BlockState pState, BlockView pLevel, BlockPos pPos, NavigationType pType) {
		return false;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.DATA_GATHERER.get(pState.get(FACING));
	}

	@Override
	public Class<DisplayLinkBlockEntity> getBlockEntityClass() {
		return DisplayLinkBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DisplayLinkBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.DISPLAY_LINK.get();
	}

}
