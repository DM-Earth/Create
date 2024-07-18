package com.simibubi.create.content.equipment.toolbox;

import static net.minecraft.state.property.Properties.WATERLOGGED;

import java.util.Optional;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.BlockHelper;

import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class ToolboxBlock extends HorizontalFacingBlock implements Waterloggable, IBE<ToolboxBlockEntity> {

	protected final DyeColor color;

	public ToolboxBlock(Settings properties, DyeColor color) {
		super(properties);
		this.color = color;
		setDefaultState(getDefaultState().with(Properties.WATERLOGGED, false));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(WATERLOGGED)
			.add(FACING));
	}

	@Override
	public void onPlaced(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.onPlaced(worldIn, pos, state, placer, stack);
		if (worldIn.isClient)
			return;
		if (stack == null)
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			NbtCompound orCreateTag = stack.getOrCreateNbt();
			be.readInventory(orCreateTag.getCompound("Inventory"));
			if (orCreateTag.contains("UniqueId"))
				be.setUniqueId(orCreateTag.getUuid("UniqueId"));
			if (stack.hasCustomName())
				be.setCustomName(stack.getName());
		});
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moving) {
		if (state.hasBlockEntity() && (!newState.hasBlockEntity() || !(newState.getBlock() instanceof ToolboxBlock)))
			world.removeBlockEntity(pos);
	}

	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (player instanceof FakePlayer)
			return;
		if (world.isClient)
			return;
		withBlockEntityDo(world, pos, ToolboxBlockEntity::unequipTracked);
		if (world instanceof ServerWorld) {
			ItemStack cloneItemStack = getPickStack(world, pos, state);
			world.breakBlock(pos, false);
			if (world.getBlockState(pos) != state)
				player.getInventory().offerOrDrop(cloneItemStack);
		}
	}

	@Override
	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		ItemStack item = new ItemStack(this);
		Optional<ToolboxBlockEntity> blockEntityOptional = getBlockEntityOptional(world, pos);
		NbtCompound tag = item.getOrCreateNbt();

		NbtCompound inv = blockEntityOptional.map(tb -> tb.inventory.serializeNBT())
			.orElse(new NbtCompound());
		tag.put("Inventory", inv);

		blockEntityOptional.map(tb -> tb.getUniqueId())
			.ifPresent(uid -> tag.putUuid("UniqueId", uid));
		blockEntityOptional.map(ToolboxBlockEntity::getCustomName)
			.ifPresent(item::setCustomName);
		return item;
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState, WorldAccess world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.get(WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		return state;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return AllShapes.TOOLBOX.get(state.get(FACING));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {

		if (player == null || player.isInSneakingPose())
			return ActionResult.PASS;

		ItemStack stack = player.getStackInHand(hand);
		DyeColor color = TagUtil.getColorFromStack(stack);
		if (color != null && color != this.color) {
			if (world.isClient)
				return ActionResult.SUCCESS;
			BlockState newState = BlockHelper.copyProperties(state, AllBlocks.TOOLBOXES.get(color)
				.getDefaultState());
			world.setBlockState(pos, newState);
			return ActionResult.SUCCESS;
		}

		if (player instanceof FakePlayer)
			return ActionResult.PASS;
		if (world.isClient)
			return ActionResult.SUCCESS;

		withBlockEntityDo(world, pos,
			toolbox -> NetworkHooks.openScreen((ServerPlayerEntity) player, toolbox, toolbox::sendToMenu));
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		FluidState ifluidstate = context.getWorld()
			.getFluidState(context.getBlockPos());
		return super.getPlacementState(context).with(FACING, context.getHorizontalPlayerFacing()
			.getOpposite())
			.with(WATERLOGGED, Boolean.valueOf(ifluidstate.getFluid() == Fluids.WATER));
	}

	@Override
	public Class<ToolboxBlockEntity> getBlockEntityClass() {
		return ToolboxBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ToolboxBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.TOOLBOX.get();
	}

	public DyeColor getColor() {
		return color;
	}

	public static Ingredient getMainBox() {
		return Ingredient.ofItems(AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
			.get());
	}

}
