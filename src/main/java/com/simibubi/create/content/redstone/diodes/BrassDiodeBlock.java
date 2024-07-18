package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IBE;
import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BrassDiodeBlock extends AbstractDiodeBlock implements IBE<BrassDiodeBlockEntity>, ConnectableRedstoneBlock {

	public static final BooleanProperty POWERING = BooleanProperty.of("powering");
	public static final BooleanProperty INVERTED = BooleanProperty.of("inverted");

	public BrassDiodeBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(POWERED, false)
			.with(POWERING, false)
			.with(INVERTED, false));
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity player, Hand pHand,
		BlockHitResult pHit) {
		return toggle(pLevel, pPos, pState, player, pHand);
	}

	public ActionResult toggle(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity player,
		Hand pHand) {
		if (!player.canModifyBlocks())
			return ActionResult.PASS;
		if (player.isSneaking())
			return ActionResult.PASS;
		if (AllItems.WRENCH.isIn(player.getStackInHand(pHand)))
			return ActionResult.PASS;
		if (pLevel.isClient)
			return ActionResult.SUCCESS;
		pLevel.setBlockState(pPos, pState.cycle(INVERTED), 3);
		float f = !pState.get(INVERTED) ? 0.6F : 0.5F;
		pLevel.playSound(null, pPos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, f);
		return ActionResult.SUCCESS;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(POWERED, POWERING, FACING, INVERTED);
		super.appendProperties(builder);
	}

	@Override
	protected int getOutputLevel(BlockView worldIn, BlockPos pos, BlockState state) {
		return state.get(POWERING) ^ state.get(INVERTED) ? 15 : 0;
	}

	@Override
	public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side) {
		return blockState.get(FACING) == side ? this.getOutputLevel(blockAccess, pos, blockState) : 0;
	}

	@Override
	protected int getUpdateDelayInternal(BlockState p_196346_1_) {
		return 2;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, Direction side) {
		if (side == null)
			return false;
		return side.getAxis() == state.get(FACING)
			.getAxis();
	}

	@Override
	public Class<BrassDiodeBlockEntity> getBlockEntityClass() {
		return BrassDiodeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BrassDiodeBlockEntity> getBlockEntityType() {
		return AllBlocks.PULSE_EXTENDER.is(this) ? AllBlockEntityTypes.PULSE_EXTENDER.get()
			: AllBlockEntityTypes.PULSE_REPEATER.get();
	}

}
