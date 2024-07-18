package com.simibubi.create.infrastructure.gametest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;

import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity.SelectionMode;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.GameTestHelperAccessor;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.test.GameTestState;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * A helper class expanding the functionality of {@link TestContext}.
 * This class may replace the default helper parameter if a test is registered through {@link CreateTestFunction}.
 */
public class CreateGameTestHelper extends TestContext {
	public static final int TICKS_PER_SECOND = 20;
	public static final int TEN_SECONDS = 10 * TICKS_PER_SECOND;
	public static final int FIFTEEN_SECONDS = 15 * TICKS_PER_SECOND;
	public static final int TWENTY_SECONDS = 20 * TICKS_PER_SECOND;

	private CreateGameTestHelper(GameTestState testInfo) {
		super(testInfo);
	}

	public static CreateGameTestHelper of(TestContext original) {
		GameTestHelperAccessor access = (GameTestHelperAccessor) original;
		CreateGameTestHelper helper = new CreateGameTestHelper(access.getTest());
		//noinspection DataFlowIssue // accessor applied at runtime
		GameTestHelperAccessor newAccess = (GameTestHelperAccessor) helper;
		newAccess.setHasFinalClause(access.getHasFinalClause());
		return helper;
	}

	// blocks

	/**
	 * Flip the direction of any block with the {@link Properties#FACING} property.
	 */
	public void flipBlock(BlockPos pos) {
		BlockState original = getBlockState(pos);
		if (!original.contains(Properties.FACING))
			throwGameTestException("FACING property not in block: " + Registries.BLOCK.getRawId(original.getBlock()));
		Direction facing = original.get(Properties.FACING);
		BlockState reversed = original.with(Properties.FACING, facing.getOpposite());
		setBlockState(pos, reversed);
	}

	public void assertNixiePower(BlockPos pos, int strength) {
		NixieTubeBlockEntity nixie = getBlockEntity(AllBlockEntityTypes.NIXIE_TUBE.get(), pos);
		int actualStrength = nixie.getRedstoneStrength();
		if (actualStrength != strength)
			throwGameTestException("Expected nixie tube at %s to have power of %s, got %s".formatted(pos, strength, actualStrength));
	}

	/**
	 * Turn off a lever.
	 */
	public void powerLever(BlockPos pos) {
		expectBlock(Blocks.LEVER, pos);
		if (!getBlockState(pos).get(LeverBlock.POWERED)) {
			toggleLever(pos);
		}
	}

	/**
	 * Turn on a lever.
	 */
	public void unpowerLever(BlockPos pos) {
		expectBlock(Blocks.LEVER, pos);
		if (getBlockState(pos).get(LeverBlock.POWERED)) {
			toggleLever(pos);
		}
	}

	/**
	 * Set the {@link SelectionMode} of a belt tunnel at the given position.
	 * @param pos
	 * @param mode
	 */
	public void setTunnelMode(BlockPos pos, SelectionMode mode) {
		ScrollValueBehaviour behavior = getBehavior(pos, ScrollOptionBehaviour.TYPE);
		behavior.setValue(mode.ordinal());
	}

	public void assertSpeedometerSpeed(BlockPos speedometer, float value) {
		SpeedGaugeBlockEntity be = getBlockEntity(AllBlockEntityTypes.SPEEDOMETER.get(), speedometer);
		assertInRange(be.getSpeed(), value - 0.01, value + 0.01);
	}

	public void assertStressometerCapacity(BlockPos stressometer, float value) {
		StressGaugeBlockEntity be = getBlockEntity(AllBlockEntityTypes.STRESSOMETER.get(), stressometer);
		assertInRange(be.getNetworkCapacity(), value - 0.01, value + 0.01);
	}

	public void toggleActorsOfType(Contraption contraption, ItemConvertible item) {
		AtomicBoolean toggled = new AtomicBoolean(false);
		contraption.getInteractors().forEach((localPos, behavior) -> {
			if (toggled.get() || !(behavior instanceof ContraptionControlsMovingInteraction controls))
				return;
			MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(localPos);
			if (actor == null)
				return;
			ItemStack filter = ContraptionControlsMovement.getFilter(actor.right);
			if (filter != null && filter.isOf(item.asItem())) {
				controls.handlePlayerInteraction(
						createMockCreativePlayer(), Hand.MAIN_HAND, localPos, contraption.entity
				);
				toggled.set(true);
			}
		});
	}

	// block entities

	/**
	 * Get the block entity of the expected type. If the type does not match, this fails the test.
	 */
	public <T extends BlockEntity> T getBlockEntity(BlockEntityType<T> type, BlockPos pos) {
		BlockEntity be = getBlockEntity(pos);
		BlockEntityType<?> actualType = be == null ? null : be.getType();
		if (actualType != type) {
			String actualId = actualType == null ? "null" : RegisteredObjects.getKeyOrThrow(actualType).toString();
			String error = "Expected block entity at pos [%s] with type [%s], got [%s]".formatted(
					pos, RegisteredObjects.getKeyOrThrow(type), actualId
			);
			throwGameTestException(error);
		}
		return (T) be;
	}

	/**
	 * Given any segment of an {@link IMultiBlockEntityContainer}, get the controller for it.
	 */
	public <T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBlockEntity(BlockEntityType<T> type, BlockPos anySegment) {
		T be = getBlockEntity(type, anySegment).getControllerBE();
		if (be == null)
			throwGameTestException("Could not get block entity controller with type [%s] from pos [%s]".formatted(RegisteredObjects.getKeyOrThrow(type), anySegment));
		return be;
	}

	/**
	 * Get the expected {@link BlockEntityBehaviour} from the given position, failing if not present.
	 */
	public <T extends BlockEntityBehaviour> T getBehavior(BlockPos pos, BehaviourType<T> type) {
		T behavior = BlockEntityBehaviour.get(getWorld(), getAbsolutePos(pos), type);
		if (behavior == null)
			throwGameTestException("Behavior at " + pos + " missing, expected " + type.getName());
		return behavior;
	}

	// entities

	/**
	 * Spawn an item entity at the given position with no velocity.
	 */
	public ItemEntity spawnItem(BlockPos pos, ItemStack stack) {
		Vec3d spawn = Vec3d.ofCenter(getAbsolutePos(pos));
		ServerWorld level = getWorld();
		ItemEntity item = new ItemEntity(level, spawn.x, spawn.y, spawn.z, stack, 0, 0, 0);
		level.spawnEntity(item);
		return item;
	}

	/**
	 * Spawn item entities given an item and amount. The amount will be split into multiple entities if
	 * larger than the item's max stack size.
	 */
	public void spawnItems(BlockPos pos, Item item, int amount) {
		while (amount > 0) {
			int toSpawn = Math.min(amount, item.getMaxCount());
			amount -= toSpawn;
			ItemStack stack = new ItemStack(item, toSpawn);
			spawnItem(pos, stack);
		}
	}

	/**
	 * Get the first entity found at the given position.
	 */
	public <T extends Entity> T getFirstEntity(EntityType<T> type, BlockPos pos) {
		List<T> list = getEntitiesBetween(type, pos.north().east().up(), pos.south().west().down());
		if (list.isEmpty())
			throwGameTestException("No entities at pos: " + pos);
		return list.get(0);
	}

	/**
	 * Get a list of all entities between two positions, inclusive.
	 */
	public <T extends Entity> List<T> getEntitiesBetween(EntityType<T> type, BlockPos pos1, BlockPos pos2) {
		BlockBox box = BlockBox.create(getAbsolutePos(pos1), getAbsolutePos(pos2));
		List<? extends T> entities = getWorld().getEntitiesByType(type, e -> box.contains(e.getBlockPos()));
		return (List<T>) entities;
	}


	// transfer - fluids

	public Storage<FluidVariant> fluidStorageAt(BlockPos pos) {
		Storage<FluidVariant> storage = TransferUtil.getFluidStorage(getWorld(), getAbsolutePos(pos));
		if (storage == null)
			throwGameTestException("storage not present");
		return storage;
	}

	/**
	 * Get the content of the tank at the pos.
	 * content is determined by what the tank allows to be extracted.
	 */
	public FluidStack getTankContents(BlockPos tank) {
		Storage<FluidVariant> storage = fluidStorageAt(tank);
		return TransferUtil.firstOrEmpty(storage);
	}

	/**
	 * Get the total capacity of a tank at the given position.
	 */
	public long getTankCapacity(BlockPos pos) {
		Storage<FluidVariant> storage = fluidStorageAt(pos);
		return TransferUtil.totalCapacity(storage);
	}

	/**
	 * Get the total fluid amount across all fluid tanks at the given positions.
	 */
	public long getFluidInTanks(BlockPos... tanks) {
		long total = 0;
		for (BlockPos tank : tanks) {
			total += getTankContents(tank).getAmount();
		}
		return total;
	}

	/**
	 * Assert that the given fluid stack is present in the given tank. The tank might also hold more than the fluid.
	 */
	public void assertFluidPresent(FluidStack fluid, BlockPos pos) {
		FluidStack contained = getTankContents(pos);
		if (!fluid.isFluidEqual(contained))
			throwGameTestException("Different fluids");
		if (fluid.getAmount() != contained.getAmount())
			throwGameTestException("Different amounts");
	}

	/**
	 * Assert that the given tank holds no fluid.
	 */
	public void assertTankEmpty(BlockPos pos) {
		assertFluidPresent(FluidStack.EMPTY, pos);
	}

	public void assertTanksEmpty(BlockPos... tanks) {
		for (BlockPos tank : tanks) {
			assertTankEmpty(tank);
		}
	}

	// transfer - items

	public Storage<ItemVariant> itemStorageAt(BlockPos pos) {
		Storage<ItemVariant> storage = TransferUtil.getItemStorage(getWorld(), getAbsolutePos(pos));
		if (storage == null)
			throwGameTestException("storage not present");
		return storage;
	}

	/**
	 * Get a map of contained items to their amounts. This is not safe for NBT!
	 */
	public Object2LongMap<Item> getItemContent(BlockPos pos) {
		Storage<ItemVariant> storage = itemStorageAt(pos);
		Object2LongMap<Item> map = new Object2LongArrayMap<>();
		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
				ItemVariant resource = view.getResource();
				Item item = resource.getItem();
				long amount = map.getLong(item);
				amount += view.extract(resource, Long.MAX_VALUE, t);
				map.put(item, amount);
			}
		}
		return map;
	}

	/**
	 * Get the combined total of all ItemStacks inside the inventory.
	 */
	public long getTotalItems(BlockPos pos) {
		Storage<ItemVariant> storage = itemStorageAt(pos);
		try (Transaction t = TransferUtil.getTransaction()) {
			return TransferUtil.extractAllAsStacks(storage).stream().mapToLong(ItemStack::getCount).sum();
		}
	}

	/**
	 * Of the provided items, assert that at least one is present in the given inventory.
	 */
	public void assertAnyContained(BlockPos pos, Item... items) {
		Storage<ItemVariant> storage = itemStorageAt(pos);
		boolean noneFound = true;
		try (Transaction t = TransferUtil.getTransaction()) {
			for (Item item : items) {
				if (storage.extract(ItemVariant.of(item), 1, t) > 0) {
					noneFound = false;
					break;
				}
			}
		}
		if (noneFound)
			throwGameTestException("No matching items " + Arrays.toString(items) + " found in handler at pos: " + pos);
	}

	/**
	 * Assert that the inventory contains all the provided content.
	 */
	public void assertContentPresent(Object2LongMap<Item> content, BlockPos pos) {
		Storage<ItemVariant> storage = itemStorageAt(pos);
		Object2LongMap<Item> missing = new Object2LongArrayMap<>();
		try (Transaction t = TransferUtil.getTransaction()) {
			for (Entry<Item> entry : content.object2LongEntrySet()) {
				Item item = entry.getKey();
				long amount = entry.getLongValue();
				long extracted = storage.extract(ItemVariant.of(item), amount, t);
				long remaining = amount - extracted;
				if (remaining > 0) {
					missing.put(item, remaining);
				}
			}
		}
		if (!missing.isEmpty())
			throwGameTestException("Storage missing content: " + missing + ", expected: " + content);
	}

	/**
	 * Assert that all the given inventories hold no items.
	 */
	public void assertContainersEmpty(List<BlockPos> positions) {
		for (BlockPos pos : positions) {
			expectEmptyContainer(pos);
		}
	}

	/**
	 * Assert that the given inventory holds no items.
	 */
	@Override
	public void expectEmptyContainer(@NotNull BlockPos pos) {
		Storage<ItemVariant> storage = itemStorageAt(pos);
		storage.nonEmptyViews().forEach(view -> throwGameTestException("Storage not empty"));
	}

	/** @see CreateGameTestHelper#assertContainerContains(BlockPos, ItemStack) */
	public void assertContainerContains(BlockPos pos, ItemConvertible item) {
		expectContainerWith(pos, item.asItem());
	}

	/** @see CreateGameTestHelper#assertContainerContains(BlockPos, ItemStack) */
	@Override
	public void expectContainerWith(@NotNull BlockPos pos, @NotNull Item item) {
		assertContainerContains(pos, new ItemStack(item));
	}

	/**
	 * Assert that the inventory holds at least the given ItemStack. It may also hold more than the stack.
	 */
	public void assertContainerContains(BlockPos pos, ItemStack item) {
		Storage<ItemVariant> storage = itemStorageAt(pos);
		ItemStack extracted = ItemHelper.extract(storage, stack -> ItemHandlerHelper.canItemStacksStack(stack, item), item.getCount(), true);
		if (extracted.isEmpty())
			throwGameTestException("item not present: " + item);
	}

	// time

	/**
	 * Fail unless the desired number seconds have passed since test start.
	 */
	public void assertSecondsPassed(int seconds) {
		if (getTick() < (long) seconds * TICKS_PER_SECOND)
			throwGameTestException("Waiting for %s seconds to pass".formatted(seconds));
	}

	/**
	 * Get the total number of seconds that have passed since test start.
	 */
	public long secondsPassed() {
		return getTick() % 20;
	}

	/**
	 * Run an action later, once enough time has passed.
	 */
	public void whenSecondsPassed(int seconds, Runnable run) {
		waitAndRun((long) seconds * TICKS_PER_SECOND, run);
	}

	// numbers

	/**
	 * Assert that a number is less than 1 away from its expected value
	 */
	public void assertCloseEnoughTo(double value, double expected) {
		assertInRange(value, expected - 1, expected + 1);
	}

	public void assertInRange(double value, double min, double max) {
		if (value < min)
			throwGameTestException("Value %s below expected min of %s".formatted(value, min));
		if (value > max)
			throwGameTestException("Value %s greater than expected max of %s".formatted(value, max));
	}

	// misc

	@Contract("_->fail") // make IDEA happier
	@Override
	public void throwGameTestException(@NotNull String exceptionMessage) {
		super.throwGameTestException(exceptionMessage);
	}
}
