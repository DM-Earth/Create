package com.simibubi.create.foundation.utility;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;

import io.github.fabricators_of_create.porting_lib.common.util.IPlantable;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.IceBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class BlockHelper {

	public static BlockState setZeroAge(BlockState blockState) {
		if (blockState.contains(Properties.AGE_1))
			return blockState.with(Properties.AGE_1, 0);
		if (blockState.contains(Properties.AGE_2))
			return blockState.with(Properties.AGE_2, 0);
		if (blockState.contains(Properties.AGE_3))
			return blockState.with(Properties.AGE_3, 0);
		if (blockState.contains(Properties.AGE_5))
			return blockState.with(Properties.AGE_5, 0);
		if (blockState.contains(Properties.AGE_7))
			return blockState.with(Properties.AGE_7, 0);
		if (blockState.contains(Properties.AGE_15))
			return blockState.with(Properties.AGE_15, 0);
		if (blockState.contains(Properties.AGE_25))
			return blockState.with(Properties.AGE_25, 0);
		if (blockState.contains(Properties.HONEY_LEVEL))
			return blockState.with(Properties.HONEY_LEVEL, 0);
		if (blockState.contains(Properties.HATCH))
			return blockState.with(Properties.HATCH, 0);
		if (blockState.contains(Properties.STAGE))
			return blockState.with(Properties.STAGE, 0);
		if (blockState.isIn(BlockTags.CAULDRONS))
			return Blocks.CAULDRON
				.getDefaultState();
		if (blockState.contains(Properties.LEVEL_8))
			return blockState.with(Properties.LEVEL_8, 0);
		if (blockState.contains(Properties.EXTENDED))
			return blockState.with(Properties.EXTENDED, false);
		return blockState;
	}

	public static int simulateFindAndRemoveInInventory(BlockState block, PlayerEntity player, long amount) {
		try (Transaction t = TransferUtil.getTransaction()) {
			return findAndRemoveInInventory(block, player, amount);
		}
	}

	public static int findAndRemoveInInventory(BlockState block, PlayerEntity player, long amount) {
		ItemVariant required = ItemVariant.of(getRequiredItem(block));

		boolean needsTwo = block.contains(Properties.SLAB_TYPE)
			&& block.get(Properties.SLAB_TYPE) == SlabType.DOUBLE;

		if (needsTwo)
			amount *= 2;

		if (block.contains(Properties.EGGS))
			amount *= block.get(Properties.EGGS);

		if (block.contains(Properties.PICKLES))
			amount *= block.get(Properties.PICKLES);

		try (Transaction t = TransferUtil.getTransaction()) {
			PlayerInventoryStorage storage = PlayerInventoryStorage.of(player);
			int amountFound = (int) storage.extract(required, amount, t);

			if (needsTwo) {
				// Give back 1 if uneven amount was removed
				if (amountFound % 2 != 0)
					storage.insert(required, 1, t);
				amountFound /= 2;
			}

			t.commit();
			return amountFound;
		}
	}

	public static ItemStack getRequiredItem(BlockState state) {
		ItemStack itemStack = new ItemStack(state.getBlock());
		Item item = itemStack.getItem();
		if (item == Items.FARMLAND || item == Items.DIRT_PATH)
			itemStack = new ItemStack(Items.DIRT);
		return itemStack;
	}

	public static void destroyBlock(World world, BlockPos pos, float effectChance) {
		destroyBlock(world, pos, effectChance, stack -> Block.dropStack(world, pos, stack));
	}

	public static void destroyBlock(World world, BlockPos pos, float effectChance,
		Consumer<ItemStack> droppedItemCallback) {
		destroyBlockAs(world, pos, null, ItemStack.EMPTY, effectChance, droppedItemCallback);
	}

	public static void destroyBlockAs(World world, BlockPos pos, @Nullable PlayerEntity player, ItemStack usedTool,
		float effectChance, Consumer<ItemStack> droppedItemCallback) {
		FluidState fluidState = world.getFluidState(pos);
		BlockState state = world.getBlockState(pos);

		if (world.random.nextFloat() < effectChance)
			world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
		BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;

		if (player != null) {
			boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, state, blockEntity);
			if (!allowed) {
				PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(world, player, pos, state, blockEntity);
				return;
			}

			// fabric: exp handed in state.spawnAfterBreak

			usedTool.postMine(world, state, pos, player);
			player.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
		}

		if (world instanceof ServerWorld && world.getGameRules()
			.getBoolean(GameRules.DO_TILE_DROPS)
			&& (player == null || !player.isCreative())) {
			for (ItemStack itemStack : Block.getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, player, usedTool)) {
				if (itemStack.isEmpty())
					continue;
				droppedItemCallback.accept(itemStack);
			}

			// Simulating IceBlock#playerDestroy. Not calling method directly as it would drop item
			// entities as a side-effect
			if (state.getBlock() instanceof IceBlock
				&& EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, usedTool) == 0) {
				if (world.getDimension()
					.ultrawarm())
					return;

				 BlockState blockstate = world.getBlockState(pos.down());
		         if (blockstate.blocksMovement() || blockstate.isLiquid()) {
					world.setBlockState(pos, Blocks.WATER.getDefaultState());
					afterBreak(world, player, pos, state, blockEntity);
				}
				return;
			}

			state.onStacksDropped((ServerWorld) world, pos, ItemStack.EMPTY, true);
		}

		world.setBlockState(pos, fluidState.getBlockState());
		afterBreak(world, player, pos, state, blockEntity);
	}

	// fabric: after break event
	private static void afterBreak(World level, @Nullable PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
		if (player != null)
			PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, be);
	}

	public static boolean isSolidWall(BlockView reader, BlockPos fromPos, Direction toDirection) {
		return hasBlockSolidSide(reader.getBlockState(fromPos.offset(toDirection)), reader,
			fromPos.offset(toDirection), toDirection.getOpposite());
	}

	public static boolean noCollisionInSpace(BlockView reader, BlockPos pos) {
		return reader.getBlockState(pos)
			.getCollisionShape(reader, pos)
			.isEmpty();
	}

	private static void placeRailWithoutUpdate(World world, BlockState state, BlockPos target) {
		WorldChunk chunk = world.getWorldChunk(target);
		int idx = chunk.getSectionIndex(target.getY());
		ChunkSection chunksection = chunk.getSection(idx);
		if (chunksection == null) {
			chunksection = new ChunkSection(world.getRegistryManager()
				.get(RegistryKeys.BIOME));
			chunk.getSectionArray()[idx] = chunksection;
		}
		BlockState old = chunksection.setBlockState(ChunkSectionPos.getLocalCoord(target.getX()),
			ChunkSectionPos.getLocalCoord(target.getY()), ChunkSectionPos.getLocalCoord(target.getZ()), state);
		chunk.setNeedsSaving(true);
		world.markAndNotifyBlock(target, chunk, old, state, 82, 512);

		world.setBlockState(target, state, 82);
		world.updateNeighbor(target, world.getBlockState(target.down())
			.getBlock(), target.down());
	}

	public static NbtCompound prepareBlockEntityData(BlockState blockState, BlockEntity blockEntity) {
		NbtCompound data = null;
		if (blockEntity == null)
			return data;
		if (AllBlockTags.SAFE_NBT.matches(blockState)) {
			data = blockEntity.createNbtWithIdentifyingData();
			data = NBTProcessors.process(blockEntity, data, true);
		} else if (blockEntity instanceof IPartialSafeNBT) {
			data = new NbtCompound();
			((IPartialSafeNBT) blockEntity).writeSafe(data);
			data = NBTProcessors.process(blockEntity, data, true);
		}
		return data;
	}

	public static void placeSchematicBlock(World world, BlockState state, BlockPos target, ItemStack stack,
		@Nullable NbtCompound data) {
		BlockEntity existingBlockEntity = world.getBlockEntity(target);

		// Piston
		if (state.contains(Properties.EXTENDED))
			state = state.with(Properties.EXTENDED, Boolean.FALSE);
		if (state.contains(Properties.WATERLOGGED))
			state = state.with(Properties.WATERLOGGED, Boolean.FALSE);

		if (state.getBlock() == Blocks.COMPOSTER)
			state = Blocks.COMPOSTER.getDefaultState();
		else if (state.getBlock() != Blocks.SEA_PICKLE && state.getBlock() instanceof IPlantable)
			state = ((IPlantable) state.getBlock()).getPlant(world, target);
		else if (state.isIn(BlockTags.CAULDRONS))
			state = Blocks.CAULDRON.getDefaultState();

		if (world.getDimension()
			.ultrawarm() && state.getFluidState().isIn(FluidTags.WATER)) {
			int i = target.getX();
			int j = target.getY();
			int k = target.getZ();
			world.playSound(null, target, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
				2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

			for (int l = 0; l < 8; ++l) {
				world.addParticle(ParticleTypes.LARGE_SMOKE, i + Math.random(), j + Math.random(), k + Math.random(),
					0.0D, 0.0D, 0.0D);
			}
			Block.dropStacks(state, world, target);
			return;
		}

		if (state.getBlock() instanceof AbstractRailBlock) {
			placeRailWithoutUpdate(world, state, target);
		} else if (AllBlocks.BELT.has(state)) {
			world.setBlockState(target, state, 2);
		} else {
			world.setBlockState(target, state, 18);
		}

		if (data != null) {
			if (existingBlockEntity instanceof IMergeableBE mergeable) {
				BlockEntity loaded = BlockEntity.createFromNbt(target, state, data);
				if (existingBlockEntity.getType()
					.equals(loaded.getType())) {
					mergeable.accept(loaded);
					return;
				}
			}
			BlockEntity blockEntity = world.getBlockEntity(target);
			if (blockEntity != null) {
				data.putInt("x", target.getX());
				data.putInt("y", target.getY());
				data.putInt("z", target.getZ());
				if (blockEntity instanceof KineticBlockEntity)
					((KineticBlockEntity) blockEntity).warnOfMovement();
				blockEntity.readNbt(data);
			}
		}

		try {
			state.getBlock()
				.onPlaced(world, target, state, null, stack);
		} catch (Exception e) {
		}
	}

	public static double getBounceMultiplier(Block block) {
		if (block instanceof SlimeBlock)
			return 0.8D;
		if (block instanceof BedBlock)
			return 0.66 * 0.8D;
		return 0;
	}

	public static boolean hasBlockSolidSide(BlockState p_220056_0_, BlockView p_220056_1_, BlockPos p_220056_2_,
		Direction p_220056_3_) {
		return !p_220056_0_.isIn(BlockTags.LEAVES)
			&& Block.isFaceFullSquare(p_220056_0_.getCollisionShape(p_220056_1_, p_220056_2_), p_220056_3_);
	}

	public static boolean extinguishFire(World world, @Nullable PlayerEntity p_175719_1_, BlockPos p_175719_2_,
		Direction p_175719_3_) {
		p_175719_2_ = p_175719_2_.offset(p_175719_3_);
		if (world.getBlockState(p_175719_2_)
			.getBlock() == Blocks.FIRE) {
			world.syncWorldEvent(p_175719_1_, 1009, p_175719_2_, 0);
			world.removeBlock(p_175719_2_, false);
			return true;
		} else {
			return false;
		}
	}

	public static BlockState copyProperties(BlockState fromState, BlockState toState) {
		for (Property<?> property : fromState.getProperties()) {
			toState = copyProperty(property, fromState, toState);
		}
		return toState;
	}

	public static <T extends Comparable<T>> BlockState copyProperty(Property<T> property, BlockState fromState,
		BlockState toState) {
		if (fromState.contains(property) && toState.contains(property)) {
			return toState.with(property, fromState.get(property));
		}
		return toState;
	}

	public static boolean isNotUnheated(BlockState state) {
		if (state.isIn(BlockTags.CAMPFIRES) && state.contains(CampfireBlock.LIT)) {
			return state.get(CampfireBlock.LIT);
		}
		if (state.contains(BlazeBurnerBlock.HEAT_LEVEL)) {
			return state.get(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.NONE;
		}
		return true;
	}

}
