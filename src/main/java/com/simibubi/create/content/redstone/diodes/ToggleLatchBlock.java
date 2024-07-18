package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ToggleLatchBlock extends AbstractDiodeBlock implements ConnectableRedstoneBlock {

	public static BooleanProperty POWERING = BooleanProperty.of("powering");

	public ToggleLatchBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(POWERING, false)
			.with(POWERED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(POWERED, POWERING, FACING);
	}

	@Override
	public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side) {
		return blockState.get(FACING) == side ? this.getOutputLevel(blockAccess, pos, blockState) : 0;
	}

	@Override
	protected int getUpdateDelayInternal(BlockState state) {
		return 1;
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (!player.canModifyBlocks())
			return ActionResult.PASS;
		if (player.isSneaking())
			return ActionResult.PASS;
		if (AllItems.WRENCH.isIn(player.getStackInHand(handIn)))
			return ActionResult.PASS;
		return activated(worldIn, pos, state);
	}

	@Override
	protected int getOutputLevel(BlockView worldIn, BlockPos pos, BlockState state) {
		return state.get(POWERING) ? 15 : 0;
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
		boolean poweredPreviously = state.get(POWERED);
		super.scheduledTick(state, worldIn, pos, random);
		BlockState newState = worldIn.getBlockState(pos);
		if (newState.get(POWERED) && !poweredPreviously)
			worldIn.setBlockState(pos, newState.cycle(POWERING), 2);
	}

	protected ActionResult activated(World worldIn, BlockPos pos, BlockState state) {
		if (!worldIn.isClient) {
			float f = !state.get(POWERING) ? 0.6F : 0.5F;
			worldIn.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, f);
			worldIn.setBlockState(pos, state.cycle(POWERING), 2);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, Direction side) {
		if (side == null)
			return false;
		return side.getAxis() == state.get(FACING)
			.getAxis();
	}

}
