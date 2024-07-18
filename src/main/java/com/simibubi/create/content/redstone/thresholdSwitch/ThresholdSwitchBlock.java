package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ThresholdSwitchBlock extends DirectedDirectionalBlock implements IBE<ThresholdSwitchBlockEntity>, ConnectableRedstoneBlock {

	public static final IntProperty LEVEL = IntProperty.of("level", 0, 5);

	public ThresholdSwitchBlock(Settings p_i48377_1_) {
		super(p_i48377_1_);
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		updateObservedInventory(state, worldIn, pos);
	}

	private void updateObservedInventory(BlockState state, WorldView world, BlockPos pos) {
		withBlockEntityDo(world, pos, ThresholdSwitchBlockEntity::updateCurrentLevel);
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, Direction side) {
		return side != null && side.getOpposite() != getTargetDirection(state);
	}

	@Override
	public boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side) {
		if (side == getTargetDirection(blockState)
			.getOpposite())
			return 0;
		return getBlockEntityOptional(blockAccess, pos).filter(ThresholdSwitchBlockEntity::isPowered)
			.map($ -> 15)
			.orElse(0);
	}

	@Override
	public void scheduledTick(BlockState blockState, ServerWorld world, BlockPos pos, Random random) {
		getBlockEntityOptional(world, pos).ifPresent(ThresholdSwitchBlockEntity::updatePowerAfterDelay);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(LEVEL));
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		if (player != null && AllItems.WRENCH.isIn(player.getStackInHand(handIn)))
			return ActionResult.PASS;
		EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> withBlockEntityDo(worldIn, pos, be -> this.displayScreen(be, player)));
		return ActionResult.SUCCESS;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayScreen(ThresholdSwitchBlockEntity be, PlayerEntity player) {
		if (player instanceof ClientPlayerEntity)
			ScreenOpener.open(new ThresholdSwitchScreen(be));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = getDefaultState();

		Direction preferredFacing = null;
		for (Direction face : context.getPlacementDirections()) {
			BlockPos offsetPos = context.getBlockPos().offset(face);
			World world = context.getWorld();
			if (TransferUtil.getItemStorage(world, offsetPos, face.getOpposite()) != null
					|| TransferUtil.getFluidStorage(world, offsetPos, face.getOpposite()) != null) {
				preferredFacing = face;
				break;
			}
		}

		if (preferredFacing == null) {
			Direction facing = context.getPlayerLookDirection();
			preferredFacing = context.getPlayer() != null && context.getPlayer()
				.isSneaking() ? facing : facing.getOpposite();
		}

		if (preferredFacing.getAxis() == Axis.Y) {
			state = state.with(TARGET, preferredFacing == Direction.UP ? WallMountLocation.CEILING : WallMountLocation.FLOOR);
			preferredFacing = context.getHorizontalPlayerFacing();
		}

		return state.with(FACING, preferredFacing);
	}

	@Override
	public Class<ThresholdSwitchBlockEntity> getBlockEntityClass() {
		return ThresholdSwitchBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ThresholdSwitchBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.THRESHOLD_SWITCH.get();
	}

}
