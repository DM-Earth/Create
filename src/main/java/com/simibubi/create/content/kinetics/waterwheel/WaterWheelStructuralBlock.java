package com.simibubi.create.content.kinetics.waterwheel;

import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.foundation.utility.AdventureUtil;

import io.github.fabricators_of_create.porting_lib.block.CustomHitEffectsBlock;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;

import io.github.fabricators_of_create.porting_lib.block.CustomDestroyEffectsBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomLandingEffectsBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class WaterWheelStructuralBlock extends FacingBlock implements IWrenchable, IProxyHoveringInformation, MultiPosDestructionHandler, CustomLandingEffectsBlock, CustomDestroyEffectsBlock, CustomHitEffectsBlock {

	public WaterWheelStructuralBlock(Settings p_52591_) {
		super(p_52591_);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(FACING));
	}

	@Override
	public BlockRenderType getRenderType(BlockState pState) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return ActionResult.PASS;
	}

	@Override
	public ItemStack getPickStack(BlockView pLevel, BlockPos pPos, BlockState pState) {
		return AllBlocks.LARGE_WATER_WHEEL.asStack();
	}

	@Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		BlockPos clickedPos = context.getBlockPos();
		World level = context.getWorld();

		if (stillValid(level, clickedPos, state, false)) {
			BlockPos masterPos = getMaster(level, clickedPos, state);
			context = new ItemUsageContext(level, context.getPlayer(), context.getHand(), context.getStack(),
				new BlockHitResult(context.getHitPos(), context.getSide(), masterPos,
					context.hitsInsideBlock()));
			state = level.getBlockState(masterPos);
		}

		return IWrenchable.super.onSneakWrenched(state, context);
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (AdventureUtil.isAdventure(pPlayer))
			return ActionResult.PASS;
		if (!stillValid(pLevel, pPos, pState, false))
			return ActionResult.FAIL;
		if (!(pLevel.getBlockEntity(getMaster(pLevel, pPos, pState))instanceof WaterWheelBlockEntity wwt))
			return ActionResult.FAIL;
		return wwt.applyMaterialIfValid(pPlayer.getStackInHand(pHand));
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (stillValid(pLevel, pPos, pState, false))
			pLevel.breakBlock(getMaster(pLevel, pPos, pState), true);
	}

	public void onBreak(World pLevel, BlockPos pPos, BlockState pState, PlayerEntity pPlayer) {
		if (stillValid(pLevel, pPos, pState, false)) {
			BlockPos masterPos = getMaster(pLevel, pPos, pState);
			pLevel.setBlockBreakingInfo(masterPos.hashCode(), masterPos, -1);
			if (!pLevel.isClient() && pPlayer.isCreative())
				pLevel.breakBlock(masterPos, false);
		}
		super.onBreak(pLevel, pPos, pState, pPlayer);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		if (stillValid(pLevel, pCurrentPos, pState, false)) {
			BlockPos masterPos = getMaster(pLevel, pCurrentPos, pState);
			if (!pLevel.getBlockTickScheduler()
				.isQueued(masterPos, AllBlocks.LARGE_WATER_WHEEL.get()))
				pLevel.scheduleBlockTick(masterPos, AllBlocks.LARGE_WATER_WHEEL.get(), 1);
			return pState;
		}
		if (!(pLevel instanceof World level) || level.isClient())
			return pState;
		if (!level.getBlockTickScheduler()
			.isQueued(pCurrentPos, this))
			level.scheduleBlockTick(pCurrentPos, this, 1);
		return pState;
	}

	public static BlockPos getMaster(BlockView level, BlockPos pos, BlockState state) {
		Direction direction = state.get(FACING);
		BlockPos targetedPos = pos.offset(direction);
		BlockState targetedState = level.getBlockState(targetedPos);
		if (targetedState.isOf(AllBlocks.WATER_WHEEL_STRUCTURAL.get()))
			return getMaster(level, targetedPos, targetedState);
		return targetedPos;
	}

	public boolean stillValid(BlockView level, BlockPos pos, BlockState state, boolean directlyAdjacent) {
		if (!state.isOf(this))
			return false;

		Direction direction = state.get(FACING);
		BlockPos targetedPos = pos.offset(direction);
		BlockState targetedState = level.getBlockState(targetedPos);

		if (!directlyAdjacent && stillValid(level, targetedPos, targetedState, true))
			return true;
		return targetedState.getBlock() instanceof LargeWaterWheelBlock
			&& targetedState.get(LargeWaterWheelBlock.AXIS) != direction.getAxis();
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		if (!stillValid(pLevel, pPos, pState, false))
			pLevel.setBlockState(pPos, Blocks.AIR.getDefaultState());
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerWorld level, BlockPos pos, BlockState state2,
		LivingEntity entity, int numberOfParticles) {
		return true;
	}

	// fabric: Don't add destroy effects, it'll create missingno particles
	@Override
	public boolean addDestroyEffects(BlockState state, ClientWorld Level, BlockPos pos, ParticleManager manager) {
		return false;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean addHitEffects(BlockState state, World level, HitResult target, ParticleManager engine) {
		if (target instanceof BlockHitResult bhr) {
			BlockPos targetPos = bhr.getBlockPos();
			WaterWheelStructuralBlock waterWheelStructuralBlock = AllBlocks.WATER_WHEEL_STRUCTURAL.get();
			if (waterWheelStructuralBlock.stillValid(level, targetPos, state, false))
				engine.addBlockBreakingParticles(WaterWheelStructuralBlock.getMaster(level, targetPos, state), bhr.getSide());
			return true;
		}
		return false;
	}

	@Override
	@Nullable
	@Environment(EnvType.CLIENT)
	public Set<BlockPos> getExtraPositions(ClientWorld level, BlockPos pos, BlockState blockState, int progress) {
		WaterWheelStructuralBlock waterWheelStructuralBlock = AllBlocks.WATER_WHEEL_STRUCTURAL.get();
		if (!waterWheelStructuralBlock.stillValid(level, pos, blockState, false))
			return null;
		HashSet<BlockPos> set = new HashSet<>();
		set.add(WaterWheelStructuralBlock.getMaster(level, pos, blockState));
		return set;
	}

	@Override
	public BlockPos getInformationSource(World level, BlockPos pos, BlockState state) {
		return stillValid(level, pos, state, false) ? getMaster(level, pos, state) : pos;
	}

}
