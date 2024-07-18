package com.simibubi.create.content.decoration.slidingDoor;

import java.util.function.Supplier;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import net.minecraft.world.event.GameEvent;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

public class SlidingDoorBlock extends DoorBlock implements IWrenchable, IBE<SlidingDoorBlockEntity> {

	public static final Supplier<BlockSetType> TRAIN_SET_TYPE =
		() -> new BlockSetType("train", true, BlockSoundGroup.NETHERITE, SoundEvents.BLOCK_IRON_DOOR_CLOSE,
			SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,
			SoundEvents.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON,
			SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON);

	public static final Supplier<BlockSetType> GLASS_SET_TYPE =
		() -> new BlockSetType("train", true, BlockSoundGroup.NETHERITE, SoundEvents.BLOCK_IRON_DOOR_CLOSE,
			SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,
			SoundEvents.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON,
			SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON);

	public static final BooleanProperty VISIBLE = BooleanProperty.of("visible");
	private boolean folds;

	public static SlidingDoorBlock metal(Settings p_52737_, boolean folds) {
		return new SlidingDoorBlock(p_52737_, TRAIN_SET_TYPE.get(), folds);
	}
	
	public static SlidingDoorBlock glass(Settings p_52737_, boolean folds) {
		return new SlidingDoorBlock(p_52737_, GLASS_SET_TYPE.get(), folds);
	}
	
	public SlidingDoorBlock(Settings p_52737_, BlockSetType type, boolean folds) {
		super(p_52737_, type);
		this.folds = folds;
	}

	public boolean isFoldingDoor() {
		return folds;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(VISIBLE));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		if (!pState.get(OPEN) && (pState.get(VISIBLE) || pLevel instanceof ContraptionWorld))
			return super.getOutlineShape(pState, pLevel, pPos, pContext);

		Direction direction = pState.get(FACING);
		boolean hinge = pState.get(HINGE) == DoorHinge.RIGHT;
		return SlidingDoorShapes.get(direction, hinge, isFoldingDoor());
	}

	@Override
	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		return pState.get(HALF) == DoubleBlockHalf.LOWER || pLevel.getBlockState(pPos.down())
			.isOf(this);
	}

	@Override
	public VoxelShape getRaycastShape(BlockState pState, BlockView pLevel, BlockPos pPos) {
		return getOutlineShape(pState, pLevel, pPos, ShapeContext.absent());
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		BlockState stateForPlacement = super.getPlacementState(pContext);
		if (stateForPlacement != null && stateForPlacement.get(OPEN))
			return stateForPlacement.with(OPEN, false)
				.with(POWERED, false);
		return stateForPlacement;
	}

	@Override
	public void onBlockAdded(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		if (!pOldState.isOf(this))
			deferUpdate(pLevel, pPos);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		BlockState blockState = super.getStateForNeighborUpdate(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
		if (blockState.isAir())
			return blockState;
		DoubleBlockHalf doubleblockhalf = blockState.get(HALF);
		if (pFacing.getAxis() == Direction.Axis.Y
			&& doubleblockhalf == DoubleBlockHalf.LOWER == (pFacing == Direction.UP)) {
			return pFacingState.isOf(this) && pFacingState.get(HALF) != doubleblockhalf
				? blockState.with(VISIBLE, pFacingState.get(VISIBLE))
				: Blocks.AIR.getDefaultState();
		}
		return blockState;
	}

	@Override
	public void setOpen(@Nullable Entity entity, World level, BlockState state, BlockPos pos, boolean open) {
		if (!state.isOf(this))
			return;
		if (state.get(OPEN) == open)
			return;
		BlockState changedState = state.with(OPEN, open);
		if (open)
			changedState = changedState.with(VISIBLE, false);
		level.setBlockState(pos, changedState, 10);

		DoorHinge hinge = changedState.get(HINGE);
		Direction facing = changedState.get(FACING);
		BlockPos otherPos =
			pos.offset(hinge == DoorHinge.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
		BlockState otherDoor = level.getBlockState(otherPos);
		if (isDoubleDoor(changedState, hinge, facing, otherDoor))
			setOpen(entity, level, otherDoor, otherPos, open);

		this.playOpenCloseSound(entity, level, pos, open);
		level.emitGameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
	}

	@Override
	public void neighborUpdate(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos,
		boolean pIsMoving) {
		boolean lower = pState.get(HALF) == DoubleBlockHalf.LOWER;
		boolean isPowered = isDoorPowered(pLevel, pPos, pState);
		if (getDefaultState().isOf(pBlock))
			return;
		if (isPowered == pState.get(POWERED))
			return;

		SlidingDoorBlockEntity be = getBlockEntity(pLevel, lower ? pPos : pPos.down());
		if (be != null && be.deferUpdate)
			return;

		BlockState changedState = pState.with(POWERED, Boolean.valueOf(isPowered))
			.with(OPEN, Boolean.valueOf(isPowered));
		if (isPowered)
			changedState = changedState.with(VISIBLE, false);

		if (isPowered != pState.get(OPEN)) {
			this.playOpenCloseSound(null, pLevel, pPos, isPowered);
			pLevel.emitGameEvent(null, isPowered ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);

			DoorHinge hinge = changedState.get(HINGE);
			Direction facing = changedState.get(FACING);
			BlockPos otherPos =
				pPos.offset(hinge == DoorHinge.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
			BlockState otherDoor = pLevel.getBlockState(otherPos);

			if (isDoubleDoor(changedState, hinge, facing, otherDoor)) {
				otherDoor = otherDoor.with(POWERED, Boolean.valueOf(isPowered))
					.with(OPEN, Boolean.valueOf(isPowered));
				if (isPowered)
					otherDoor = otherDoor.with(VISIBLE, false);
				pLevel.setBlockState(otherPos, otherDoor, 2);
			}
		}

		pLevel.setBlockState(pPos, changedState, 2);
	}

	public static boolean isDoorPowered(World pLevel, BlockPos pPos, BlockState state) {
		boolean lower = state.get(HALF) == DoubleBlockHalf.LOWER;
		DoorHinge hinge = state.get(HINGE);
		Direction facing = state.get(FACING);
		BlockPos otherPos =
			pPos.offset(hinge == DoorHinge.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
		BlockState otherDoor = pLevel.getBlockState(otherPos);

		if (isDoubleDoor(state.cycle(OPEN), hinge, facing, otherDoor) && (pLevel.isReceivingRedstonePower(otherPos)
			|| pLevel.isReceivingRedstonePower(otherPos.offset(lower ? Direction.UP : Direction.DOWN))))
			return true;

		return pLevel.isReceivingRedstonePower(pPos)
			|| pLevel.isReceivingRedstonePower(pPos.offset(lower ? Direction.UP : Direction.DOWN));
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {

		pState = pState.cycle(OPEN);
		if (pState.get(OPEN))
			pState = pState.with(VISIBLE, false);
		pLevel.setBlockState(pPos, pState, 10);
		pLevel.emitGameEvent(pPlayer, isOpen(pState) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);

		DoorHinge hinge = pState.get(HINGE);
		Direction facing = pState.get(FACING);
		BlockPos otherPos =
			pPos.offset(hinge == DoorHinge.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
		BlockState otherDoor = pLevel.getBlockState(otherPos);
		if (isDoubleDoor(pState, hinge, facing, otherDoor))
			onUse(otherDoor, pLevel, otherPos, pPlayer, pHand, pHit);
		else if (pState.get(OPEN))
			pLevel.emitGameEvent(pPlayer, GameEvent.BLOCK_OPEN, pPos);

		return ActionResult.success(pLevel.isClient);
	}

	public void deferUpdate(WorldAccess level, BlockPos pos) {
		withBlockEntityDo(level, pos, sdte -> sdte.deferUpdate = true);
	}

	public static boolean isDoubleDoor(BlockState pState, DoorHinge hinge, Direction facing, BlockState otherDoor) {
		return otherDoor.getBlock() == pState.getBlock() && otherDoor.get(HINGE) != hinge
			&& otherDoor.get(FACING) == facing && otherDoor.get(OPEN) != pState.get(OPEN)
			&& otherDoor.get(HALF) == pState.get(HALF);
	}

	@Override
	public BlockRenderType getRenderType(BlockState pState) {
		return pState.get(VISIBLE) ? BlockRenderType.MODEL : BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	private void playOpenCloseSound(@Nullable Entity pSource, World pLevel, BlockPos pPos, boolean pIsOpening) {
		pLevel.playSound(pSource, pPos, pIsOpening ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
			SoundCategory.BLOCKS, 1.0F, pLevel.getRandom()
				.nextFloat() * 0.1F + 0.9F);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		if (state.get(HALF) == DoubleBlockHalf.UPPER)
			return null;
		return IBE.super.createBlockEntity(pos, state);
	}

	@Override
	public Class<SlidingDoorBlockEntity> getBlockEntityClass() {
		return SlidingDoorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SLIDING_DOOR.get();
	}

}
