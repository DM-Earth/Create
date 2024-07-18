package com.simibubi.create.content.contraptions.elevator;

import java.util.Optional;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.BlockHelper;
import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.block.WeakPowerCheckingBlock;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class ElevatorContactBlock extends WrenchableDirectionalBlock
	implements IBE<ElevatorContactBlockEntity>, ISpecialBlockItemRequirement, WeakPowerCheckingBlock, ConnectableRedstoneBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final BooleanProperty CALLING = BooleanProperty.of("calling");
	public static final BooleanProperty POWERING = BrassDiodeBlock.POWERING;

	public ElevatorContactBlock(Settings pProperties) {
		super(pProperties);
		setDefaultState(getDefaultState().with(CALLING, false)
			.with(POWERING, false)
			.with(POWERED, false)
			.with(FACING, Direction.SOUTH));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(CALLING, POWERING, POWERED));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		ActionResult onWrenched = super.onWrenched(state, context);
		if (onWrenched != ActionResult.SUCCESS)
			return onWrenched;

		World level = context.getWorld();
		if (level.isClient())
			return onWrenched;

		BlockPos pos = context.getBlockPos();
		state = level.getBlockState(pos);
		Direction facing = state.get(RedstoneContactBlock.FACING);
		if (facing.getAxis() != Axis.Y
			&& ElevatorColumn.get(level, new ColumnCoords(pos.getX(), pos.getZ(), facing)) != null)
			return onWrenched;

		level.setBlockState(pos, BlockHelper.copyProperties(state, AllBlocks.REDSTONE_CONTACT.getDefaultState()));

		return onWrenched;
	}

	@Nullable
	public static ColumnCoords getColumnCoords(WorldAccess level, BlockPos pos) {
		BlockState blockState = level.getBlockState(pos);
		if (!AllBlocks.ELEVATOR_CONTACT.has(blockState) && !AllBlocks.REDSTONE_CONTACT.has(blockState))
			return null;
		Direction facing = blockState.get(FACING);
		BlockPos target = pos;
		return new ColumnCoords(target.getX(), target.getZ(), facing);
	}

	@Override
	public void neighborUpdate(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos,
		boolean pIsMoving) {
		if (pLevel.isClient)
			return;

		boolean isPowered = pState.get(POWERED);
		if (isPowered == pLevel.isReceivingRedstonePower(pPos))
			return;

		pLevel.setBlockState(pPos, pState.cycle(POWERED), 2);

		if (isPowered)
			return;
		if (pState.get(CALLING))
			return;

		ElevatorColumn elevatorColumn = ElevatorColumn.getOrCreate(pLevel, getColumnCoords(pLevel, pPos));
		callToContactAndUpdate(elevatorColumn, pState, pLevel, pPos, true);
	}

	public void callToContactAndUpdate(ElevatorColumn elevatorColumn, BlockState pState, World pLevel, BlockPos pPos,
		boolean powered) {
		pLevel.setBlockState(pPos, pState.cycle(CALLING), 2);

		for (BlockPos otherPos : elevatorColumn.getContacts()) {
			if (otherPos.equals(pPos))
				continue;
			BlockState otherState = pLevel.getBlockState(otherPos);
			if (!AllBlocks.ELEVATOR_CONTACT.has(otherState))
				continue;
			pLevel.setBlockState(otherPos, otherState.with(CALLING, false), 2 | 16);
			scheduleActivation(pLevel, otherPos);
		}

		if (powered)
			pState = pState.with(POWERED, true);
		pLevel.setBlockState(pPos, pState.with(CALLING, true), 2);
		pLevel.updateNeighborsAlways(pPos, this);

		elevatorColumn.target(pPos.getY());
		elevatorColumn.markDirty();
	}

	public void scheduleActivation(WorldAccess pLevel, BlockPos pPos) {
		if (!pLevel.getBlockTickScheduler()
			.isQueued(pPos, this))
			pLevel.scheduleBlockTick(pPos, this, 1);
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRand) {
		boolean wasPowering = pState.get(POWERING);

		Optional<ElevatorContactBlockEntity> optionalBE = getBlockEntityOptional(pLevel, pPos);
		boolean shouldBePowering = optionalBE.map(be -> {
			boolean activateBlock = be.activateBlock;
			be.activateBlock = false;
			be.markDirty();
			return activateBlock;
		})
			.orElse(false);

		shouldBePowering |= RedstoneContactBlock.hasValidContact(pLevel, pPos, pState.get(FACING));

		if (wasPowering || shouldBePowering)
			pLevel.setBlockState(pPos, pState.with(POWERING, shouldBePowering), 2 | 16);

		pLevel.updateNeighborsAlways(pPos, this);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn,
		BlockPos currentPos, BlockPos facingPos) {
		if (facing != stateIn.get(FACING))
			return stateIn;
		boolean hasValidContact = RedstoneContactBlock.hasValidContact(worldIn, currentPos, facing);
		if (stateIn.get(POWERING) != hasValidContact)
			scheduleActivation(worldIn, currentPos);
		return stateIn;
	}

	@Override
    public boolean shouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side) {
        return false;
    }

	@Override
	public boolean emitsRedstonePower(BlockState state) {
		return state.get(POWERING);
	}

	@Override
	public ItemStack getPickStack(BlockView pLevel, BlockPos pPos, BlockState pState) {
		return AllBlocks.REDSTONE_CONTACT.asStack();
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, @Nullable Direction side) {
		if (side == null)
			return true;
		return state.get(FACING) != side.getOpposite();
	}

	@Override
	public int getWeakRedstonePower(BlockState state, BlockView blockAccess, BlockPos pos, Direction side) {
		if (side == null)
			return 0;
		BlockState toState = blockAccess.getBlockState(pos.offset(side.getOpposite()));
		if (toState.isOf(this))
			return 0;
		return state.get(POWERING) ? 15 : 0;
	}

	@Override
	public Class<ElevatorContactBlockEntity> getBlockEntityClass() {
		return ElevatorContactBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ElevatorContactBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ELEVATOR_CONTACT.get();
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(AllBlocks.REDSTONE_CONTACT.getDefaultState(), be);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (player != null && AllItems.WRENCH.isIn(player.getStackInHand(handIn)))
			return ActionResult.PASS;
		EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> withBlockEntityDo(worldIn, pos, be -> this.displayScreen(be, player)));
		return ActionResult.SUCCESS;
	}

	@Environment(EnvType.CLIENT)
	protected void displayScreen(ElevatorContactBlockEntity be, PlayerEntity player) {
		if (player instanceof ClientPlayerEntity)
			ScreenOpener
				.open(new ElevatorContactScreen(be.getPos(), be.shortName, be.longName, be.doorControls.mode));
	}

	public static int getLight(BlockState state) {
		return state.get(POWERING) ? 10 : 0;
	}

}
