package com.simibubi.create.content.trains.signal;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.block.WeakPowerCheckingBlock;

public class SignalBlock extends Block implements IBE<SignalBlockEntity>, IWrenchable, WeakPowerCheckingBlock {

	public static final EnumProperty<SignalType> TYPE = EnumProperty.of("type", SignalType.class);
	public static final BooleanProperty POWERED = Properties.POWERED;

	public enum SignalType implements StringIdentifiable {
		ENTRY_SIGNAL, CROSS_SIGNAL;

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	public SignalBlock(Settings p_53182_) {
		super(p_53182_);
		setDefaultState(getDefaultState().with(TYPE, SignalType.ENTRY_SIGNAL)
			.with(POWERED, false));
	}

	@Override
	public Class<SignalBlockEntity> getBlockEntityClass() {
		return SignalBlockEntity.class;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(TYPE, POWERED));
	}

	@Override
	public boolean shouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side) {
		return false;
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		return this.getDefaultState()
			.with(POWERED, Boolean.valueOf(pContext.getWorld()
				.isReceivingRedstonePower(pContext.getBlockPos())));
	}

	@Override
	public void neighborUpdate(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos,
		boolean pIsMoving) {
		if (pLevel.isClient)
			return;
		boolean powered = pState.get(POWERED);
		if (powered == pLevel.isReceivingRedstonePower(pPos))
			return;
		if (powered) {
			pLevel.scheduleBlockTick(pPos, this, 4);
		} else {
			pLevel.setBlockState(pPos, pState.cycle(POWERED), 2);
		}
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRand) {
		if (pState.get(POWERED) && !pLevel.isReceivingRedstonePower(pPos))
			pLevel.setBlockState(pPos, pState.cycle(POWERED), 2);
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, worldIn, pos, newState);
	}

	@Override
	public BlockEntityType<? extends SignalBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.TRACK_SIGNAL.get();
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World level = context.getWorld();
		BlockPos pos = context.getBlockPos();
		if (level.isClient)
			return ActionResult.SUCCESS;
		withBlockEntityDo(level, pos, ste -> {
			SignalBoundary signal = ste.getSignal();
			PlayerEntity player = context.getPlayer();
			if (signal != null) {
				signal.cycleSignalType(pos);
				if (player != null)
					player.sendMessage(Lang.translateDirect("track_signal.mode_change." + signal.getTypeFor(pos)
						.asString()), true);
			} else if (player != null)
				player.sendMessage(Lang.translateDirect("track_signal.cannot_change_mode"), true);
		});
		return ActionResult.SUCCESS;
	}

	@Override
	public boolean hasComparatorOutput(BlockState pState) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState pState, World blockAccess, BlockPos pPos) {
		return getBlockEntityOptional(blockAccess, pPos).filter(SignalBlockEntity::isPowered)
			.map($ -> 15)
			.orElse(0);
	}

}
