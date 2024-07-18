package com.simibubi.create.content.processing.burner;

import net.fabricmc.fabric.api.entity.FakePlayer;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlazeBurnerBlock extends HorizontalFacingBlock implements IBE<BlazeBurnerBlockEntity>, IWrenchable {

	public static final EnumProperty<HeatLevel> HEAT_LEVEL = EnumProperty.of("blaze", HeatLevel.class);

	public BlazeBurnerBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(HEAT_LEVEL, HeatLevel.NONE));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(HEAT_LEVEL, FACING);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
		if (world.isClient)
			return;
		BlockEntity blockEntity = world.getBlockEntity(pos.up());
		if (!(blockEntity instanceof BasinBlockEntity))
			return;
		BasinBlockEntity basin = (BasinBlockEntity) blockEntity;
		basin.notifyChangeOfContents();
	}

	@Override
	public Class<BlazeBurnerBlockEntity> getBlockEntityClass() {
		return BlazeBurnerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.HEATER.get();
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		if (state.get(HEAT_LEVEL) == HeatLevel.NONE)
			return null;
		return IBE.super.createBlockEntity(pos, state);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult blockRayTraceResult) {
		ItemStack heldItem = player.getStackInHand(hand);
		HeatLevel heat = state.get(HEAT_LEVEL);

		if (AllItems.GOGGLES.isIn(heldItem) && heat != HeatLevel.NONE)
			return onBlockEntityUse(world, pos, bbte -> {
				if (bbte.goggles)
					return ActionResult.PASS;
				bbte.goggles = true;
				bbte.notifyUpdate();
				return ActionResult.SUCCESS;
			});

		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		if (heldItem.isEmpty() && heat != HeatLevel.NONE)
			return onBlockEntityUse(world, pos, bbte -> {
				if (!bbte.goggles)
					return ActionResult.PASS;
				bbte.goggles = false;
				bbte.notifyUpdate();
				return ActionResult.SUCCESS;
			});

		if (heat == HeatLevel.NONE) {
			if (heldItem.getItem() instanceof FlintAndSteelItem) {
				world.playSound(player, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F,
					world.random.nextFloat() * 0.4F + 0.8F);
				if (world.isClient)
					return ActionResult.SUCCESS;
				heldItem.damage(1, player, p -> p.sendToolBreakStatus(hand));
				world.setBlockState(pos, AllBlocks.LIT_BLAZE_BURNER.getDefaultState());
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		}

		boolean doNotConsume = player.isCreative();
		boolean forceOverflow = !(player instanceof FakePlayer);
		try (Transaction t = TransferUtil.getTransaction()) {
			TypedActionResult<ItemStack> res =
					tryInsert(state, world, pos, heldItem, doNotConsume, forceOverflow, t);
			t.commit();
			ItemStack leftover = res.getValue();
			if (!world.isClient && !doNotConsume && !leftover.isEmpty()) {
				if (heldItem.isEmpty()) {
					player.setStackInHand(hand, leftover);
				} else if (!player.getInventory()
						.insertStack(leftover)) {
					player.dropItem(leftover, false);
				}
			}

			return res.getResult() == ActionResult.SUCCESS ? ActionResult.SUCCESS : ActionResult.PASS;
		}
	}

	public static TypedActionResult<ItemStack> tryInsert(BlockState state, World world, BlockPos pos,
		ItemStack stack, boolean doNotConsume, boolean forceOverflow, TransactionContext ctx) {
		if (!state.hasBlockEntity())
			return TypedActionResult.fail(ItemStack.EMPTY);

		BlockEntity be = world.getBlockEntity(pos);
		if (!(be instanceof BlazeBurnerBlockEntity))
			return TypedActionResult.fail(ItemStack.EMPTY);
		BlazeBurnerBlockEntity burnerBE = (BlazeBurnerBlockEntity) be;

		if (burnerBE.isCreativeFuel(stack)) {
			TransactionCallback.onSuccess(ctx, burnerBE::applyCreativeFuel);
			return TypedActionResult.success(ItemStack.EMPTY);
		}
		if (!burnerBE.tryUpdateFuel(stack, forceOverflow, ctx))
			return TypedActionResult.fail(ItemStack.EMPTY);

		if (!doNotConsume) {
			ItemStack container = stack.getRecipeRemainder();
			if (!world.isClient) {
				stack.decrement(1);
			}
			return TypedActionResult.success(container);
		}
		return TypedActionResult.success(ItemStack.EMPTY);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		ItemStack stack = context.getStack();
		Item item = stack.getItem();
		BlockState defaultState = getDefaultState();
		if (!(item instanceof BlazeBurnerBlockItem))
			return defaultState;
		HeatLevel initialHeat =
			((BlazeBurnerBlockItem) item).hasCapturedBlaze() ? HeatLevel.SMOULDERING : HeatLevel.NONE;
		return defaultState.with(HEAT_LEVEL, initialHeat)
			.with(FACING, context.getHorizontalPlayerFacing()
				.getOpposite());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView reader, BlockPos pos, ShapeContext context) {
		return AllShapes.HEATER_BLOCK_SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockView p_220071_2_, BlockPos p_220071_3_,
		ShapeContext p_220071_4_) {
		if (p_220071_4_ == ShapeContext.absent())
			return AllShapes.HEATER_BLOCK_SPECIAL_COLLISION_SHAPE;
		return getOutlineShape(p_220071_1_, p_220071_2_, p_220071_3_, p_220071_4_);
	}

	@Override
	public boolean hasComparatorOutput(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState state, World p_180641_2_, BlockPos p_180641_3_) {
		return Math.max(0, state.get(HEAT_LEVEL)
			.ordinal() - 1);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Environment(EnvType.CLIENT)
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(10) != 0)
			return;
		if (!state.get(HEAT_LEVEL)
			.isAtLeast(HeatLevel.SMOULDERING))
			return;
		world.playSound((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F),
			(double) ((float) pos.getZ() + 0.5F), SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS,
			0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
	}

	public static HeatLevel getHeatLevelOf(BlockState blockState) {
		return blockState.contains(BlazeBurnerBlock.HEAT_LEVEL) ? blockState.get(BlazeBurnerBlock.HEAT_LEVEL)
			: HeatLevel.NONE;
	}

	public static int getLight(BlockState state) {
		HeatLevel level = state.get(HEAT_LEVEL);
		return switch (level) {
		case NONE -> 0;
		case SMOULDERING -> 8;
		default -> 15;
		};
	}

	public static LootTable.Builder buildLootTable() {
		LootCondition.Builder survivesExplosion = SurvivesExplosionLootCondition.builder();
		BlazeBurnerBlock block = AllBlocks.BLAZE_BURNER.get();
		LootTable.Builder builder = LootTable.builder();
		LootPool.Builder poolBuilder = LootPool.builder();
		for (HeatLevel level : HeatLevel.values()) {
			ItemConvertible drop = level == HeatLevel.NONE ? AllItems.EMPTY_BLAZE_BURNER.get() : AllBlocks.BLAZE_BURNER.get();
			poolBuilder.with(ItemEntry.builder(drop)
				.conditionally(survivesExplosion)
				.conditionally(BlockStatePropertyLootCondition.builder(block)
					.properties(StatePredicate.Builder.create()
						.exactMatch(HEAT_LEVEL, level))));
		}
		builder.pool(poolBuilder.rolls(ConstantLootNumberProvider.create(1)));
		return builder;
	}

	public enum HeatLevel implements StringIdentifiable {
		NONE, SMOULDERING, FADING, KINDLED, SEETHING,;

		public static HeatLevel byIndex(int index) {
			return values()[index];
		}

		public HeatLevel nextActiveLevel() {
			return byIndex(ordinal() % (values().length - 1) + 1);
		}

		public boolean isAtLeast(HeatLevel heatLevel) {
			return this.ordinal() >= heatLevel.ordinal();
		}

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

}
