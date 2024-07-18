package com.simibubi.create.content.logistics.chute;

import java.util.function.Consumer;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Iterate;

public abstract class AbstractChuteBlock extends Block implements IWrenchable, IBE<ChuteBlockEntity>, ReducedDestroyEffects {

	public AbstractChuteBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
	}

//	@OnlyIn(Dist.CLIENT)
//	public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
//		consumer.accept(new ReducedDestroyEffects());
//	}

	public static boolean isChute(BlockState state) {
		return state.getBlock() instanceof AbstractChuteBlock;
	}

	public static boolean isOpenChute(BlockState state) {
		return isChute(state) && ((AbstractChuteBlock) state.getBlock()).isOpen(state);
	}

	public static boolean isTransparentChute(BlockState state) {
		return isChute(state) && ((AbstractChuteBlock) state.getBlock()).isTransparent(state);
	}

	@Nullable
	public static Direction getChuteFacing(BlockState state) {
		return !isChute(state) ? null : ((AbstractChuteBlock) state.getBlock()).getFacing(state);
	}

	public Direction getFacing(BlockState state) {
		return Direction.DOWN;
	}

	public boolean isOpen(BlockState state) {
		return true;
	}

	public boolean isTransparent(BlockState state) {
		return false;
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		if (!(entityIn instanceof ItemEntity))
			return;
		if (entityIn.getWorld().isClient)
			return;
		if (!entityIn.isAlive())
			return;
		DirectBeltInputBehaviour input = BlockEntityBehaviour.get(entityIn.getWorld(),
			BlockPos.ofFloored(entityIn.getPos()
				.add(0, 0.5f, 0))
				.down(),
			DirectBeltInputBehaviour.TYPE);
		if (input == null)
			return;
		if (!input.canInsertFromSide(Direction.UP))
			return;

		ItemEntity itemEntity = (ItemEntity) entityIn;
		ItemStack toInsert = itemEntity.getStack();
		ItemStack remainder = input.handleInsertion(toInsert, Direction.UP, false);

		if (remainder.isEmpty())
			itemEntity.discard();
		if (remainder.getCount() < toInsert.getCount())
			itemEntity.setStack(remainder);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
		withBlockEntityDo(world, pos, ChuteBlockEntity::onAdded);
		updateDiagonalNeighbour(state, world, pos);
	}

	protected void updateDiagonalNeighbour(BlockState state, World world, BlockPos pos) {
		if (!isChute(state))
			return;
		AbstractChuteBlock block = (AbstractChuteBlock) state.getBlock();
		Direction facing = block.getFacing(state);
		BlockPos toUpdate = pos.down();
		if (facing.getAxis()
			.isHorizontal())
			toUpdate = toUpdate.offset(facing.getOpposite());

		BlockState stateToUpdate = world.getBlockState(toUpdate);
		if (isChute(stateToUpdate) && !world.getBlockTickScheduler()
			.isQueued(toUpdate, stateToUpdate.getBlock()))
			world.scheduleBlockTick(toUpdate, stateToUpdate.getBlock(), 1);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, world, pos, newState);

		if (state.isOf(newState.getBlock()))
			return;

		updateDiagonalNeighbour(state, world, pos);

		for (Direction direction : Iterate.horizontalDirections) {
			BlockPos toUpdate = pos.up()
				.offset(direction);
			BlockState stateToUpdate = world.getBlockState(toUpdate);
			if (isChute(stateToUpdate) && !world.getBlockTickScheduler()
				.isQueued(toUpdate, stateToUpdate.getBlock()))
				world.scheduleBlockTick(toUpdate, stateToUpdate.getBlock(), 1);
		}
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		BlockState updated = updateChuteState(pState, pLevel.getBlockState(pPos.up()), pLevel, pPos);
		if (pState != updated)
			pLevel.setBlockState(pPos, updated);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState above, WorldAccess world,
		BlockPos pos, BlockPos p_196271_6_) {
		if (direction != Direction.UP)
			return state;
		return updateChuteState(state, above, world, pos);
	}

	@Override
	public void neighborUpdate(BlockState p_220069_1_, World world, BlockPos pos, Block p_220069_4_,
		BlockPos neighbourPos, boolean p_220069_6_) {
		if (pos.down()
			.equals(neighbourPos))
			withBlockEntityDo(world, pos, ChuteBlockEntity::blockBelowChanged);
		// fabric: unnecessary, not how it works here
//		else if (pos.above()
//				.equals(neighbourPos))
//			withBlockEntityDo(world, pos, chute -> chute.capAbove = LazyOptional.empty());
	}

	public abstract BlockState updateChuteState(BlockState state, BlockState above, BlockView world, BlockPos pos);

	@Override
	public VoxelShape getOutlineShape(BlockState p_220053_1_, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return ChuteShapes.getShape(p_220053_1_);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockView p_220071_2_, BlockPos p_220071_3_,
		ShapeContext p_220071_4_) {
		return ChuteShapes.getCollisionShape(p_220071_1_);
	}

	@Override
	public Class<ChuteBlockEntity> getBlockEntityClass() {
		return ChuteBlockEntity.class;
	}

	@Override
	public ActionResult onUse(BlockState p_225533_1_, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult p_225533_6_) {
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		if (!player.getStackInHand(hand)
			.isEmpty())
			return ActionResult.PASS;
		if (world.isClient)
			return ActionResult.SUCCESS;

		return onBlockEntityUse(world, pos, be -> {
			if (be.item.isEmpty())
				return ActionResult.PASS;
			player.getInventory()
				.offerOrDrop(be.item);
			be.setItem(ItemStack.EMPTY);
			return ActionResult.SUCCESS;
		});
	}

}
