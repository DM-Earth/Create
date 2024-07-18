package com.simibubi.create.content.equipment.armor;

import java.util.Optional;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class BacktankBlock extends HorizontalKineticBlock
	implements IBE<BacktankBlockEntity>, Waterloggable {

	public BacktankBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(Properties.WATERLOGGED, false));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false)
			: Fluids.EMPTY.getDefaultState();
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(Properties.WATERLOGGED);
		super.appendProperties(builder);
	}
	@Override
	public boolean hasComparatorOutput(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return getBlockEntityOptional(world, pos).map(BacktankBlockEntity::getComparatorOutput)
			.orElse(0);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState,
		WorldAccess world, BlockPos pos, BlockPos neighbourPos) {
		if (state.get(Properties.WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		return state;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		FluidState fluidState = context.getWorld()
			.getFluidState(context.getBlockPos());
		return super.getPlacementState(context).with(Properties.WATERLOGGED,
			fluidState.getFluid() == Fluids.WATER);
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.UP;
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return Axis.Y;
	}

	@Override
	public void onPlaced(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.onPlaced(worldIn, pos, state, placer, stack);
		if (worldIn.isClient)
			return;
		if (stack == null)
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			int level = EnchantmentHelper.getLevel(AllEnchantments.CAPACITY.get(), stack);
			be.setCapacityEnchantLevel(level);
			be.setAirLevel(stack.getOrCreateNbt()
				.getInt("Air"));
			if (stack.hasEnchantments())
				be.setEnchantmentTag(stack.getEnchantments());
			if (stack.hasCustomName())
				be.setCustomName(stack.getName());
			// fabric: forge mangles item placement logic, so this isn't needed there.
			// here, we need to do this manually so neighboring blocks are updated (comparators, #1396)
			// this isn't needed for other items with block entity data (ex. chests) since they use the BlockEntityTag
			// nbt, and that system calls this after updating it.
			be.markDirty();
		});
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult hit) {
		if (player == null)
			return ActionResult.PASS;
		if (player instanceof FakePlayer)
			return ActionResult.PASS;
		if (player.isSneaking())
			return ActionResult.PASS;
		if (player.getMainHandStack()
			.getItem() instanceof BlockItem)
			return ActionResult.PASS;
		if (!player.getEquippedStack(EquipmentSlot.CHEST)
			.isEmpty())
			return ActionResult.PASS;
		if (!world.isClient) {
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .75f, 1);
			player.equipStack(EquipmentSlot.CHEST, getPickStack(world, pos, state));
			world.breakBlock(pos, false);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public ItemStack getPickStack(BlockView blockGetter, BlockPos pos, BlockState state) {
		Item item = asItem();
		if (item instanceof BacktankItem.BacktankBlockItem placeable) {
			item = placeable.getActualItem();
		}

		ItemStack stack = new ItemStack(item);
		Optional<BacktankBlockEntity> blockEntityOptional = getBlockEntityOptional(blockGetter, pos);

		int air = blockEntityOptional.map(BacktankBlockEntity::getAirLevel)
			.orElse(0);
		NbtCompound tag = stack.getOrCreateNbt();
		tag.putInt("Air", air);

		NbtList enchants = blockEntityOptional.map(BacktankBlockEntity::getEnchantmentTag)
			.orElse(new NbtList());
		if (!enchants.isEmpty()) {
			NbtList enchantmentTagList = stack.getEnchantments();
			enchantmentTagList.addAll(enchants);
			tag.put("Enchantments", enchantmentTagList);
		}

		Text customName = blockEntityOptional.map(BacktankBlockEntity::getCustomName)
			.orElse(null);
		if (customName != null)
			stack.setCustomName(customName);
		return stack;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState p_220053_1_, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return AllShapes.BACKTANK;
	}

	@Override
	public Class<BacktankBlockEntity> getBlockEntityClass() {
		return BacktankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BacktankBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.BACKTANK.get();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
