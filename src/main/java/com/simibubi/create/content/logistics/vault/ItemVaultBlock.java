package com.simibubi.create.content.logistics.vault;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.block.CustomSoundTypeBlock;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ItemVaultBlock extends Block implements IWrenchable, IBE<ItemVaultBlockEntity>, CustomSoundTypeBlock {

	public static final Property<Axis> HORIZONTAL_AXIS = Properties.HORIZONTAL_AXIS;
	public static final BooleanProperty LARGE = BooleanProperty.of("large");

	public ItemVaultBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
		setDefaultState(getDefaultState().with(LARGE, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		pBuilder.add(HORIZONTAL_AXIS, LARGE);
		super.appendProperties(pBuilder);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		if (pContext.getPlayer() == null || !pContext.getPlayer()
			.isSneaking()) {
			BlockState placedOn = pContext.getWorld()
				.getBlockState(pContext.getBlockPos()
					.offset(pContext.getSide()
						.getOpposite()));
			Axis preferredAxis = getVaultBlockAxis(placedOn);
			if (preferredAxis != null)
				return this.getDefaultState()
					.with(HORIZONTAL_AXIS, preferredAxis);
		}
		return this.getDefaultState()
			.with(HORIZONTAL_AXIS, pContext.getHorizontalPlayerFacing()
				.getAxis());
	}

	@Override
	public void onBlockAdded(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		if (pOldState.getBlock() == pState.getBlock())
			return;
		if (pIsMoving)
			return;
		// fabric: see comment in FluidTankItem
		Consumer<ItemVaultBlockEntity> consumer = ItemVaultItem.IS_PLACING_NBT
				? ItemVaultBlockEntity::queueConnectivityUpdate
				: ItemVaultBlockEntity::updateConnectivity;
		withBlockEntityDo(pLevel, pPos, consumer);
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (context.getSide()
			.getAxis()
			.isVertical()) {
			BlockEntity be = context.getWorld()
				.getBlockEntity(context.getBlockPos());
			if (be instanceof ItemVaultBlockEntity) {
				ItemVaultBlockEntity vault = (ItemVaultBlockEntity) be;
				ConnectivityHandler.splitMulti(vault);
				vault.removeController(true);
			}
			state = state.with(LARGE, false);
		}
		ActionResult onWrenched = IWrenchable.super.onWrenched(state, context);
		return onWrenched;
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean pIsMoving) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (!(be instanceof ItemVaultBlockEntity))
				return;
			ItemVaultBlockEntity vaultBE = (ItemVaultBlockEntity) be;
			ItemHelper.dropContents(world, pos, vaultBE.inventory);
			world.removeBlockEntity(pos);
			ConnectivityHandler.splitMulti(vaultBE);
		}
	}

	public static boolean isVault(BlockState state) {
		return AllBlocks.ITEM_VAULT.has(state);
	}

	@Nullable
	public static Axis getVaultBlockAxis(BlockState state) {
		if (!isVault(state))
			return null;
		return state.get(HORIZONTAL_AXIS);
	}

	public static boolean isLarge(BlockState state) {
		if (!isVault(state))
			return false;
		return state.get(LARGE);
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rot) {
		Axis axis = state.get(HORIZONTAL_AXIS);
		return state.with(HORIZONTAL_AXIS, rot.rotate(Direction.from(axis, AxisDirection.POSITIVE))
			.getAxis());
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
		return state;
	}

	// Vaults are less noisy when placed in batch
	public static final BlockSoundGroup SILENCED_METAL =
		new BlockSoundGroup(0.1F, 1.5F, SoundEvents.BLOCK_NETHERITE_BLOCK_BREAK, SoundEvents.BLOCK_NETHERITE_BLOCK_STEP,
			SoundEvents.BLOCK_NETHERITE_BLOCK_PLACE, SoundEvents.BLOCK_NETHERITE_BLOCK_HIT,
			SoundEvents.BLOCK_NETHERITE_BLOCK_FALL);

	@Override
	public BlockSoundGroup getSoundType(BlockState state, WorldView world, BlockPos pos, Entity entity) {
		BlockSoundGroup soundType = getSoundGroup(state);
		if (entity != null && entity.getCustomData()
			.contains("SilenceVaultSound"))
			return SILENCED_METAL;
		return soundType;
	}

	@Override
	public boolean hasComparatorOutput(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState pState, World pLevel, BlockPos pPos) {
		return getBlockEntityOptional(pLevel, pPos)
			.filter(vte -> !Transaction.isOpen()) // fabric: hack fix for comparators updating when they shouldn't
			.map(vte -> vte.getItemStorage(null))
			.map(ItemHelper::calcRedstoneFromInventory)
			.orElse(0);
	}

	@Override
	public BlockEntityType<? extends ItemVaultBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ITEM_VAULT.get();
	}

	@Override
	public Class<ItemVaultBlockEntity> getBlockEntityClass() {
		return ItemVaultBlockEntity.class;
	}
}
