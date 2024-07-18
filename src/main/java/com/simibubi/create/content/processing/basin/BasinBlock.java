package com.simibubi.create.content.processing.basin;

import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class BasinBlock extends Block implements IBE<BasinBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = Properties.HOPPER_FACING;

	public BasinBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
		setDefaultState(getDefaultState().with(FACING, Direction.DOWN));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		super.appendProperties(p_206840_1_.add(FACING));
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos.up());
		if (blockEntity instanceof BasinOperatingBlockEntity)
			return false;
		return true;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (!context.getWorld().isClient)
			withBlockEntityDo(context.getWorld(), context.getBlockPos(),
				bte -> bte.onWrenched(context.getSide()));
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		ItemStack heldItem = player.getStackInHand(handIn);

		return onBlockEntityUse(worldIn, pos, be -> {
			if (!heldItem.isEmpty()) {
				Direction direction = hit.getSide();
				if (FluidHelper.tryEmptyItemIntoBE(worldIn, player, handIn, heldItem, be, direction))
					return ActionResult.SUCCESS;
				if (FluidHelper.tryFillItemFromBE(worldIn, player, handIn, heldItem, be, direction))
					return ActionResult.SUCCESS;

				if (GenericItemEmptying.canItemBeEmptied(worldIn, heldItem)
					|| GenericItemFilling.canItemBeFilled(worldIn, heldItem))
					return ActionResult.SUCCESS;
				if (heldItem.getItem()
					.equals(Items.SPONGE)) {
					Storage<FluidVariant> storage = be.getFluidStorage(direction);
					if (storage != null && !TransferUtil.extractAnyFluid(storage, Long.MAX_VALUE).isEmpty()) {
						return ActionResult.SUCCESS;
					}
				}
				return ActionResult.PASS;
			}

			Storage<ItemVariant> inv = be.itemCapability;
			if (inv == null) return ActionResult.PASS;
			List<ItemStack> extracted = TransferUtil.extractAllAsStacks(inv);
			if (extracted.size() > 0) {
				extracted.forEach(s -> player.getInventory().offerOrDrop(s));
				worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f,
						1f + Create.RANDOM.nextFloat());
			}
			be.onEmptied();
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		if (!AllBlocks.BASIN.has(worldIn.getBlockState(entityIn.getBlockPos())))
			return;
		if (!(entityIn instanceof ItemEntity))
			return;
		if (!entityIn.isAlive())
			return;
		ItemEntity itemEntity = (ItemEntity) entityIn;
		withBlockEntityDo(worldIn, entityIn.getBlockPos(), be -> {

			// Tossed items bypass the quarter-stack limit
			be.inputInventory.withMaxStackSize(64);
			ItemStack stack = itemEntity.getStack().copy();
			try (Transaction t = TransferUtil.getTransaction()) {
				long inserted = be.inputInventory.insert(ItemVariant.of(stack), stack.getCount(), t);
				be.inputInventory.withMaxStackSize(16);
				t.commit();

				if (inserted == stack.getCount()) {
					itemEntity.discard();

					return;
				}

				stack.setCount((int) (stack.getCount() - inserted));
				itemEntity.setStack(stack);
			}
		});
	}

	@Override
	public VoxelShape getRaycastShape(BlockState p_199600_1_, BlockView p_199600_2_, BlockPos p_199600_3_) {
		return AllShapes.BASIN_RAYTRACE_SHAPE;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.BASIN_BLOCK_SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView reader, BlockPos pos, ShapeContext ctx) {
		if (ctx instanceof EntityShapeContext && ((EntityShapeContext) ctx).getEntity() instanceof ItemEntity)
			return AllShapes.BASIN_COLLISION_SHAPE;
		return getOutlineShape(state, reader, pos, ctx);
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, worldIn, pos, newState);
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
		return getBlockEntityOptional(worldIn, pos).map(BasinBlockEntity::getInputInventory)
				.filter(basin -> !Transaction.isOpen()) // fabric: hack fix for comparators updating when they shouldn't
			.map(ItemHelper::calcRedstoneFromInventory)
			.orElse(0);
	}

	@Override
	public Class<BasinBlockEntity> getBlockEntityClass() {
		return BasinBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BasinBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.BASIN.get();
	}

	public static boolean canOutputTo(BlockView world, BlockPos basinPos, Direction direction) {
		BlockPos neighbour = basinPos.offset(direction);
		BlockPos output = neighbour.down();
		BlockState blockState = world.getBlockState(neighbour);

		if (FunnelBlock.isFunnel(blockState)) {
			if (FunnelBlock.getFunnelFacing(blockState) == direction)
				return false;
		} else if (!blockState.getCollisionShape(world, neighbour)
			.isEmpty()) {
			return false;
		} else {
			BlockEntity blockEntity = world.getBlockEntity(output);
			if (blockEntity instanceof BeltBlockEntity) {
				BeltBlockEntity belt = (BeltBlockEntity) blockEntity;
				return belt.getSpeed() == 0 || belt.getMovementFacing() != direction.getOpposite();
			}
		}

		DirectBeltInputBehaviour directBeltInputBehaviour =
			BlockEntityBehaviour.get(world, output, DirectBeltInputBehaviour.TYPE);
		if (directBeltInputBehaviour != null)
			return directBeltInputBehaviour.canInsertFromSide(direction);
		return false;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
