package com.simibubi.create.content.fluids.drain;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ItemDrainBlock extends Block implements IWrenchable, IBE<ItemDrainBlockEntity> {

	public ItemDrainBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		ItemStack heldItem = player.getStackInHand(handIn);

		if (heldItem.getItem() instanceof BlockItem
			&& ContainerItemContext.withConstant(heldItem).find(FluidStorage.ITEM) == null)
			return ActionResult.PASS;

		return onBlockEntityUse(worldIn, pos, be -> {
			if (!heldItem.isEmpty()) {
				be.internalTank.allowInsertion();
				ActionResult tryExchange = tryExchange(worldIn, player, handIn, heldItem, be, Direction.DOWN); // up prohibits insertion
				be.internalTank.forbidInsertion();
				if (tryExchange.isAccepted())
					return tryExchange;
			}

			ItemStack heldItemStack = be.getHeldItemStack();
			if (!worldIn.isClient && !heldItemStack.isEmpty()) {
				player.getInventory()
					.offerOrDrop(heldItemStack);
				be.heldItem = null;
				be.notifyUpdate();
			}
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		if (!(entityIn instanceof ItemEntity))
			return;
		if (!entityIn.isAlive())
			return;
		if (entityIn.getWorld().isClient)
			return;

		ItemEntity itemEntity = (ItemEntity) entityIn;
		DirectBeltInputBehaviour inputBehaviour =
			BlockEntityBehaviour.get(worldIn, entityIn.getBlockPos(), DirectBeltInputBehaviour.TYPE);
		if (inputBehaviour == null)
			return;
		Vec3d deltaMovement = entityIn.getVelocity()
			.multiply(1, 0, 1)
			.normalize();
		Direction nearest = Direction.getFacing(deltaMovement.x, deltaMovement.y, deltaMovement.z);
		ItemStack remainder = inputBehaviour.handleInsertion(itemEntity.getStack(), nearest, false);
		itemEntity.setStack(remainder);
		if (remainder.isEmpty())
			itemEntity.discard();
	}

	protected ActionResult tryExchange(World worldIn, PlayerEntity player, Hand handIn, ItemStack heldItem,
		ItemDrainBlockEntity be, Direction side) {
		if (FluidHelper.tryEmptyItemIntoBE(worldIn, player, handIn, heldItem, be, side))
			return ActionResult.SUCCESS;
		if (GenericItemEmptying.canItemBeEmptied(worldIn, heldItem))
			return ActionResult.SUCCESS;
		return ActionResult.PASS;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState p_220053_1_, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return AllShapes.CASING_13PX.get(Direction.UP);
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			ItemStack heldItemStack = be.getHeldItemStack();
			if (!heldItemStack.isEmpty())
				ItemScatterer.spawn(worldIn, pos.getX(), pos.getY(), pos.getZ(), heldItemStack);
		});
		worldIn.removeBlockEntity(pos);
	}

	@Override
	public Class<ItemDrainBlockEntity> getBlockEntityClass() {
		return ItemDrainBlockEntity.class;
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public BlockEntityType<? extends ItemDrainBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ITEM_DRAIN.get();
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
		return ComparatorUtil.levelOfSmartFluidTank(worldIn, pos);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
