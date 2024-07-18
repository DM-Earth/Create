package com.simibubi.create.content.logistics.tunnel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock.Shape;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;

public class BrassTunnelBlockEntity extends BeltTunnelBlockEntity implements IHaveGoggleInformation, SidedStorageBlockEntity {

	SidedFilteringBehaviour filtering;

	boolean connectedLeft;
	boolean connectedRight;

	ItemStack stackToDistribute;
	Direction stackEnteredFrom;

	float distributionProgress;
	int distributionDistanceLeft;
	int distributionDistanceRight;
	int previousOutputIndex;

	// <filtered, non-filtered>
	Couple<List<Pair<BlockPos, Direction>>> distributionTargets;

	private boolean syncedOutputActive;
	private Set<BrassTunnelBlockEntity> syncSet;

	protected ScrollOptionBehaviour<SelectionMode> selectionMode;
	private BrassTunnelItemHandler tunnelCapability;

	public final SnapshotParticipant<Data> snapshotParticipant = new SnapshotParticipant<>() {

		@Override
		protected Data createSnapshot() {
			return new Data(stackToDistribute.copy(), distributionProgress, stackEnteredFrom);
		}

		@Override
		protected void readSnapshot(Data snapshot) {
			stackToDistribute = snapshot.stack;
			distributionProgress = snapshot.progress;
			stackEnteredFrom = snapshot.enteredFrom;
		}
	};

	private record Data(ItemStack stack, float progress, Direction enteredFrom) {
	}

	public BrassTunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		distributionTargets = Couple.create(ArrayList::new);
		syncSet = new HashSet<>();
		stackToDistribute = ItemStack.EMPTY;
		stackEnteredFrom = null;
		// fabric: beltCapability moved to cache, initialized on level set
		tunnelCapability = new BrassTunnelItemHandler(this);
		previousOutputIndex = 0;
		syncedOutputActive = false;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(selectionMode = new ScrollOptionBehaviour<>(SelectionMode.class,
			Lang.translateDirect("logistics.when_multiple_outputs_available"), this, new BrassTunnelModeSlot()));

		selectionMode.onlyActiveWhen(this::hasDistributionBehaviour);

		// Propagate settings across connected tunnels
		selectionMode.withCallback(setting -> {
			for (boolean side : Iterate.trueAndFalse) {
				if (!isConnected(side))
					continue;
				BrassTunnelBlockEntity adjacent = getAdjacent(side);
				if (adjacent != null)
					adjacent.selectionMode.setValue(setting);
			}
		});
	}

	@Override
	public void tick() {
		super.tick();
		BeltBlockEntity beltBelow = BeltHelper.getSegmentBE(world, pos.down());

		if (distributionProgress > 0)
			distributionProgress--;
		if (beltBelow == null || beltBelow.getSpeed() == 0)
			return;
		if (stackToDistribute.isEmpty() && !syncedOutputActive)
			return;
		if (world.isClient && !isVirtual())
			return;

		if (distributionProgress == -1) {
			distributionTargets.forEach(List::clear);
			distributionDistanceLeft = 0;
			distributionDistanceRight = 0;

			syncSet.clear();
			List<Pair<BrassTunnelBlockEntity, Direction>> validOutputs = gatherValidOutputs();
			if (selectionMode.get() == SelectionMode.SYNCHRONIZE) {
				boolean allEmpty = true;
				boolean allFull = true;
				for (BrassTunnelBlockEntity be : syncSet) {
					boolean hasStack = !be.stackToDistribute.isEmpty();
					allEmpty &= !hasStack;
					allFull &= hasStack;
				}
				final boolean notifySyncedOut = !allEmpty;
				if (allFull || allEmpty)
					syncSet.forEach(be -> be.syncedOutputActive = notifySyncedOut);
			}

			if (validOutputs == null)
				return;
			if (stackToDistribute.isEmpty())
				return;

			for (Pair<BrassTunnelBlockEntity, Direction> pair : validOutputs) {
				BrassTunnelBlockEntity tunnel = pair.getKey();
				Direction output = pair.getValue();
				if (insertIntoTunnel(tunnel, output, stackToDistribute, true) == null)
					continue;
				distributionTargets.get(!tunnel.flapFilterEmpty(output))
					.add(Pair.of(tunnel.pos, output));
				int distance = tunnel.pos.getX() + tunnel.pos.getZ() - pos.getX()
					- pos.getZ();
				if (distance < 0)
					distributionDistanceLeft = Math.max(distributionDistanceLeft, -distance);
				else
					distributionDistanceRight = Math.max(distributionDistanceRight, distance);
			}

			if (distributionTargets.getFirst()
				.isEmpty()
				&& distributionTargets.getSecond()
					.isEmpty())
				return;

			if (selectionMode.get() != SelectionMode.SYNCHRONIZE || syncedOutputActive) {
				distributionProgress = AllConfigs.server().logistics.brassTunnelTimer.get();
				sendData();
			}
			return;
		}

		if (distributionProgress != 0)
			return;

		distributionTargets.forEach(list -> {
			if (stackToDistribute.isEmpty())
				return;
			List<Pair<BrassTunnelBlockEntity, Direction>> validTargets = new ArrayList<>();
			for (Pair<BlockPos, Direction> pair : list) {
				BlockPos tunnelPos = pair.getKey();
				Direction output = pair.getValue();
				if (tunnelPos.equals(pos) && output == stackEnteredFrom)
					continue;
				BlockEntity be = world.getBlockEntity(tunnelPos);
				if (!(be instanceof BrassTunnelBlockEntity))
					continue;
				validTargets.add(Pair.of((BrassTunnelBlockEntity) be, output));
			}
			distribute(validTargets);
			distributionProgress = -1;
		});
	}

	private static Random rand = new Random();
	private static Map<Pair<BrassTunnelBlockEntity, Direction>, ItemStack> distributed = new IdentityHashMap<>();
	private static Set<Pair<BrassTunnelBlockEntity, Direction>> full = new HashSet<>();

	private void distribute(List<Pair<BrassTunnelBlockEntity, Direction>> validTargets) {
		int amountTargets = validTargets.size();
		if (amountTargets == 0)
			return;

		distributed.clear();
		full.clear();

		int indexStart = previousOutputIndex % amountTargets;
		SelectionMode mode = selectionMode.get();
		boolean force = mode == SelectionMode.FORCED_ROUND_ROBIN || mode == SelectionMode.FORCED_SPLIT;
		boolean split = mode == SelectionMode.FORCED_SPLIT || mode == SelectionMode.SPLIT;
		boolean robin = mode == SelectionMode.FORCED_ROUND_ROBIN || mode == SelectionMode.ROUND_ROBIN;

		if (mode == SelectionMode.RANDOMIZE)
			indexStart = rand.nextInt(amountTargets);
		if (mode == SelectionMode.PREFER_NEAREST || mode == SelectionMode.SYNCHRONIZE)
			indexStart = 0;

		ItemStack toDistribute = stackToDistribute.copy();
		for (boolean distributeAgain : Iterate.trueAndFalse) {
			ItemStack toDistributeThisCycle = null;
			int remainingOutputs = amountTargets;
			int leftovers = 0;

			for (boolean simulate : Iterate.trueAndFalse) {
				if (remainingOutputs == 0)
					break;

				leftovers = 0;
				int index = indexStart;
				int stackSize = toDistribute.getCount();
				int splitStackSize = stackSize / remainingOutputs;
				int splitRemainder = stackSize % remainingOutputs;
				int visited = 0;

				toDistributeThisCycle = toDistribute.copy();
				if (!(force || split) && simulate)
					continue;

				while (visited < amountTargets) {
					Pair<BrassTunnelBlockEntity, Direction> pair = validTargets.get(index);
					BrassTunnelBlockEntity tunnel = pair.getKey();
					Direction side = pair.getValue();
					index = (index + 1) % amountTargets;
					visited++;

					if (full.contains(pair)) {
						if (split && simulate)
							remainingOutputs--;
						continue;
					}

					int count = split ? splitStackSize + (splitRemainder > 0 ? 1 : 0) : stackSize;
					ItemStack toOutput = ItemHandlerHelper.copyStackWithSize(toDistributeThisCycle, count);

					// Grow by 1 to determine if target is full even after a successful transfer
					boolean testWithIncreasedCount = distributed.containsKey(pair);
					int increasedCount = testWithIncreasedCount ? distributed.get(pair)
						.getCount() : 0;
					if (testWithIncreasedCount)
						toOutput.increment(increasedCount);

					ItemStack remainder = insertIntoTunnel(tunnel, side, toOutput, true);

					if (remainder == null || remainder.getCount() == (testWithIncreasedCount ? count + 1 : count)) {
						if (force)
							return;
						if (split && simulate)
							remainingOutputs--;
						if (!simulate)
							full.add(pair);
						if (robin)
							break;
						continue;
					} else if (!remainder.isEmpty() && !simulate) {
						full.add(pair);
					}

					if (!simulate) {
						toOutput.decrement(remainder.getCount());
						distributed.put(pair, toOutput);
					}

					leftovers += remainder.getCount();
					toDistributeThisCycle.decrement(count);
					if (toDistributeThisCycle.isEmpty())
						break;
					splitRemainder--;
					if (!split)
						break;
				}
			}

			toDistribute.setCount(toDistributeThisCycle.getCount() + leftovers);
			if (leftovers == 0 && distributeAgain)
				break;
			if (!split)
				break;
		}

		int failedTransferrals = 0;
		for (Entry<Pair<BrassTunnelBlockEntity, Direction>, ItemStack> entry : distributed.entrySet()) {
			Pair<BrassTunnelBlockEntity, Direction> pair = entry.getKey();
			failedTransferrals += insertIntoTunnel(pair.getKey(), pair.getValue(), entry.getValue(), false).getCount();
		}

		toDistribute.increment(failedTransferrals);
		stackToDistribute = ItemHandlerHelper.copyStackWithSize(stackToDistribute, toDistribute.getCount());
		if (stackToDistribute.isEmpty())
			stackEnteredFrom = null;
		previousOutputIndex++;
		previousOutputIndex %= amountTargets;
		notifyUpdate();
	}

	public void setStackToDistribute(ItemStack stack, @Nullable Direction enteredFrom, @Nullable TransactionContext ctx) {
		if (ctx != null) {
			snapshotParticipant.updateSnapshots(ctx);
		}
		stackToDistribute = stack;
		stackEnteredFrom = enteredFrom;
		distributionProgress = -1;
	}

	public ItemStack getStackToDistribute() {
		return stackToDistribute;
	}

	public List<ItemStack> grabAllStacksOfGroup(boolean simulate) {
		List<ItemStack> list = new ArrayList<>();

		ItemStack own = getStackToDistribute();
		if (!own.isEmpty()) {
			list.add(own);
			if (!simulate)
				setStackToDistribute(ItemStack.EMPTY, null, null);
		}

		for (boolean left : Iterate.trueAndFalse) {
			BrassTunnelBlockEntity adjacent = this;
			while (adjacent != null) {
				if (!world.canSetBlock(adjacent.getPos()))
					return null;
				adjacent = adjacent.getAdjacent(left);
				if (adjacent == null)
					continue;
				ItemStack other = adjacent.getStackToDistribute();
				if (other.isEmpty())
					continue;
				list.add(other);
				if (!simulate)
					adjacent.setStackToDistribute(ItemStack.EMPTY, null, null);
			}
		}

		return list;
	}

	@Nullable
	protected ItemStack insertIntoTunnel(BrassTunnelBlockEntity tunnel, Direction side, ItemStack stack,
		boolean simulate) {
		if (stack.isEmpty())
			return stack;
		if (!tunnel.testFlapFilter(side, stack))
			return null;

		BeltBlockEntity below = BeltHelper.getSegmentBE(world, tunnel.pos.down());
		if (below == null)
			return null;
		BlockPos offset = tunnel.getPos()
			.down()
			.offset(side);
		DirectBeltInputBehaviour sideOutput = BlockEntityBehaviour.get(world, offset, DirectBeltInputBehaviour.TYPE);
		if (sideOutput != null) {
			if (!sideOutput.canInsertFromSide(side))
				return null;
			ItemStack result = sideOutput.handleInsertion(stack, side, simulate);
			if (result.isEmpty() && !simulate)
				tunnel.flap(side, false);
			return result;
		}

		Direction movementFacing = below.getMovementFacing();
		if (side == movementFacing)
			if (!BlockHelper.hasBlockSolidSide(world.getBlockState(offset), world, offset, side.getOpposite())) {
				BeltBlockEntity controllerBE = below.getControllerBE();
				if (controllerBE == null)
					return null;

				if (!simulate) {
					tunnel.flap(side, true);
					ItemStack ejected = stack;
					float beltMovementSpeed = below.getDirectionAwareBeltMovementSpeed();
					float movementSpeed = Math.max(Math.abs(beltMovementSpeed), 1 / 8f);
					int additionalOffset = beltMovementSpeed > 0 ? 1 : 0;
					Vec3d outPos = BeltHelper.getVectorForOffset(controllerBE, below.index + additionalOffset);
					Vec3d outMotion = Vec3d.of(side.getVector())
						.multiply(movementSpeed)
						.add(0, 1 / 8f, 0);
					outPos.add(outMotion.normalize());
					ItemEntity entity = new ItemEntity(world, outPos.x, outPos.y + 6 / 16f, outPos.z, ejected);
					entity.setVelocity(outMotion);
					entity.setToDefaultPickupDelay();
					entity.velocityModified = true;
					world.spawnEntity(entity);
				}

				return ItemStack.EMPTY;
			}

		return null;
	}

	public boolean testFlapFilter(Direction side, ItemStack stack) {
		if (filtering == null)
			return false;
		if (filtering.get(side) == null) {
			FilteringBehaviour adjacentFilter =
				BlockEntityBehaviour.get(world, pos.offset(side), FilteringBehaviour.TYPE);
			if (adjacentFilter == null)
				return true;
			return adjacentFilter.test(stack);
		}
		return filtering.test(side, stack);
	}

	public boolean flapFilterEmpty(Direction side) {
		if (filtering == null)
			return false;
		if (filtering.get(side) == null) {
			FilteringBehaviour adjacentFilter =
				BlockEntityBehaviour.get(world, pos.offset(side), FilteringBehaviour.TYPE);
			if (adjacentFilter == null)
				return true;
			return adjacentFilter.getFilter()
				.isEmpty();
		}
		return filtering.getFilter(side)
			.isEmpty();
	}

	@Override
	public void initialize() {
		if (filtering == null) {
			filtering = createSidedFilter();
			attachBehaviourLate(filtering);
		}
		super.initialize();
	}

	public boolean canInsert(Direction side, ItemStack stack) {
		if (filtering != null && !filtering.test(side, stack))
			return false;
		if (!hasDistributionBehaviour())
			return true;
		if (!stackToDistribute.isEmpty())
			return false;
		return true;
	}

	public boolean hasDistributionBehaviour() {
		if (flaps.isEmpty())
			return false;
		if (connectedLeft || connectedRight)
			return true;
		BlockState blockState = getCachedState();
		if (!AllBlocks.BRASS_TUNNEL.has(blockState))
			return false;
		Axis axis = blockState.get(BrassTunnelBlock.HORIZONTAL_AXIS);
		for (Direction direction : flaps.keySet())
			if (direction.getAxis() != axis)
				return true;
		return false;
	}

	private List<Pair<BrassTunnelBlockEntity, Direction>> gatherValidOutputs() {
		List<Pair<BrassTunnelBlockEntity, Direction>> validOutputs = new ArrayList<>();
		boolean synchronize = selectionMode.get() == SelectionMode.SYNCHRONIZE;
		addValidOutputsOf(this, validOutputs);

		for (boolean left : Iterate.trueAndFalse) {
			BrassTunnelBlockEntity adjacent = this;
			while (adjacent != null) {
				if (!world.canSetBlock(adjacent.getPos()))
					return null;
				adjacent = adjacent.getAdjacent(left);
				if (adjacent == null)
					continue;
				addValidOutputsOf(adjacent, validOutputs);
			}
		}

		if (!syncedOutputActive && synchronize)
			return null;
		return validOutputs;
	}

	private void addValidOutputsOf(BrassTunnelBlockEntity tunnelBE,
		List<Pair<BrassTunnelBlockEntity, Direction>> validOutputs) {
		syncSet.add(tunnelBE);
		BeltBlockEntity below = BeltHelper.getSegmentBE(world, tunnelBE.pos.down());
		if (below == null)
			return;
		Direction movementFacing = below.getMovementFacing();
		BlockState blockState = getCachedState();
		if (!AllBlocks.BRASS_TUNNEL.has(blockState))
			return;

		boolean prioritizeSides = tunnelBE == this;

		for (boolean sidePass : Iterate.trueAndFalse) {
			if (!prioritizeSides && sidePass)
				continue;
			for (Direction direction : Iterate.horizontalDirections) {
				if (direction == movementFacing && below.getSpeed() == 0)
					continue;
				if (prioritizeSides && sidePass == (direction.getAxis() == movementFacing.getAxis()))
					continue;
				if (direction == movementFacing.getOpposite())
					continue;
				if (!tunnelBE.sides.contains(direction))
					continue;

				BlockPos offset = tunnelBE.pos.down()
					.offset(direction);

				BlockState potentialFunnel = world.getBlockState(offset.up());
				if (potentialFunnel.getBlock() instanceof BeltFunnelBlock
					&& potentialFunnel.get(BeltFunnelBlock.SHAPE) == Shape.PULLING
					&& FunnelBlock.getFunnelFacing(potentialFunnel) == direction)
					continue;

				DirectBeltInputBehaviour inputBehaviour =
					BlockEntityBehaviour.get(world, offset, DirectBeltInputBehaviour.TYPE);
				if (inputBehaviour == null) {
					if (direction == movementFacing)
						if (!BlockHelper.hasBlockSolidSide(world.getBlockState(offset), world, offset,
							direction.getOpposite()))
							validOutputs.add(Pair.of(tunnelBE, direction));
					continue;
				}
				if (inputBehaviour.canInsertFromSide(direction))
					validOutputs.add(Pair.of(tunnelBE, direction));
				continue;
			}
		}
	}

	@Override
	public void addBehavioursDeferred(List<BlockEntityBehaviour> behaviours) {
		super.addBehavioursDeferred(behaviours);
		filtering = createSidedFilter();
		behaviours.add(filtering);
	}

	protected SidedFilteringBehaviour createSidedFilter() {
		return new SidedFilteringBehaviour(this, new BrassTunnelFilterSlot(), this::makeFilter,
			this::isValidFaceForFilter);
	}

	private FilteringBehaviour makeFilter(Direction side, FilteringBehaviour filter) {
		return filter;
	}

	private boolean isValidFaceForFilter(Direction side) {
		return sides.contains(side);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putBoolean("SyncedOutput", syncedOutputActive);
		compound.putBoolean("ConnectedLeft", connectedLeft);
		compound.putBoolean("ConnectedRight", connectedRight);

		compound.put("StackToDistribute", NBTSerializer.serializeNBT(stackToDistribute));
		if (stackEnteredFrom != null)
			NBTHelper.writeEnum(compound, "StackEnteredFrom", stackEnteredFrom);

		compound.putFloat("DistributionProgress", distributionProgress);
		compound.putInt("PreviousIndex", previousOutputIndex);
		compound.putInt("DistanceLeft", distributionDistanceLeft);
		compound.putInt("DistanceRight", distributionDistanceRight);

		for (boolean filtered : Iterate.trueAndFalse) {
			compound.put(filtered ? "FilteredTargets" : "Targets",
				NBTHelper.writeCompoundList(distributionTargets.get(filtered), pair -> {
					NbtCompound nbt = new NbtCompound();
					nbt.put("Pos", NbtHelper.fromBlockPos(pair.getKey()));
					nbt.putInt("Face", pair.getValue()
						.getId());
					return nbt;
				}));
		}

		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		boolean wasConnectedLeft = connectedLeft;
		boolean wasConnectedRight = connectedRight;

		syncedOutputActive = compound.getBoolean("SyncedOutput");
		connectedLeft = compound.getBoolean("ConnectedLeft");
		connectedRight = compound.getBoolean("ConnectedRight");

		stackToDistribute = ItemStack.fromNbt(compound.getCompound("StackToDistribute"));
		stackEnteredFrom =
			compound.contains("StackEnteredFrom") ? NBTHelper.readEnum(compound, "StackEnteredFrom", Direction.class)
				: null;

		distributionProgress = compound.getFloat("DistributionProgress");
		previousOutputIndex = compound.getInt("PreviousIndex");
		distributionDistanceLeft = compound.getInt("DistanceLeft");
		distributionDistanceRight = compound.getInt("DistanceRight");

		for (boolean filtered : Iterate.trueAndFalse) {
			distributionTargets.set(filtered, NBTHelper
				.readCompoundList(compound.getList(filtered ? "FilteredTargets" : "Targets", NbtElement.COMPOUND_TYPE), nbt -> {
					BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("Pos"));
					Direction face = Direction.byId(nbt.getInt("Face"));
					return Pair.of(pos, face);
				}));
		}

		super.read(compound, clientPacket);

		if (!clientPacket)
			return;
		if (wasConnectedLeft != connectedLeft || wasConnectedRight != connectedRight) {
//			requestModelDataUpdate();
			if (hasWorld())
				world.updateListeners(getPos(), getCachedState(), getCachedState(), 16);
		}
		filtering.updateFilterPresence();
	}

	public boolean isConnected(boolean leftSide) {
		return leftSide ? connectedLeft : connectedRight;
	}

	@Override
	public void updateTunnelConnections() {
		super.updateTunnelConnections();
		boolean connectivityChanged = false;
		boolean nowConnectedLeft = determineIfConnected(true);
		boolean nowConnectedRight = determineIfConnected(false);

		if (connectedLeft != nowConnectedLeft) {
			connectedLeft = nowConnectedLeft;
			connectivityChanged = true;
			BrassTunnelBlockEntity adjacent = getAdjacent(true);
			if (adjacent != null && !world.isClient) {
				adjacent.updateTunnelConnections();
				adjacent.selectionMode.setValue(selectionMode.getValue());
			}
		}

		if (connectedRight != nowConnectedRight) {
			connectedRight = nowConnectedRight;
			connectivityChanged = true;
			BrassTunnelBlockEntity adjacent = getAdjacent(false);
			if (adjacent != null && !world.isClient) {
				adjacent.updateTunnelConnections();
				adjacent.selectionMode.setValue(selectionMode.getValue());
			}
		}

		if (filtering != null)
			filtering.updateFilterPresence();
		if (connectivityChanged)
			sendData();
	}

	protected boolean determineIfConnected(boolean leftSide) {
		if (flaps.isEmpty())
			return false;
		BrassTunnelBlockEntity adjacentTunnelBE = getAdjacent(leftSide);
		return adjacentTunnelBE != null && !adjacentTunnelBE.flaps.isEmpty();
	}

	@Nullable
	protected BrassTunnelBlockEntity getAdjacent(boolean leftSide) {
		if (!hasWorld())
			return null;

		BlockState blockState = getCachedState();
		if (!AllBlocks.BRASS_TUNNEL.has(blockState))
			return null;

		Axis axis = blockState.get(BrassTunnelBlock.HORIZONTAL_AXIS);
		Direction baseDirection = Direction.get(AxisDirection.POSITIVE, axis);
		Direction direction = leftSide ? baseDirection.rotateYCounterclockwise() : baseDirection.rotateYClockwise();
		BlockPos adjacentPos = pos.offset(direction);
		BlockState adjacentBlockState = world.getBlockState(adjacentPos);

		if (!AllBlocks.BRASS_TUNNEL.has(adjacentBlockState))
			return null;
		if (adjacentBlockState.get(BrassTunnelBlock.HORIZONTAL_AXIS) != axis)
			return null;
		BlockEntity adjacentBE = world.getBlockEntity(adjacentPos);
		if (adjacentBE.isRemoved())
			return null;
		if (!(adjacentBE instanceof BrassTunnelBlockEntity))
			return null;
		return (BrassTunnelBlockEntity) adjacentBE;
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Override
	public void destroy() {
		super.destroy();
		Block.dropStack(world, pos, stackToDistribute);
		stackEnteredFrom = null;
	}

	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return tunnelCapability;
	}

	public Storage<ItemVariant> getBeltCapability() {
		return belowProvider != null ? belowProvider.get(Direction.UP) : null;
	}

	public enum SelectionMode implements INamedIconOptions {
		SPLIT(AllIcons.I_TUNNEL_SPLIT),
		FORCED_SPLIT(AllIcons.I_TUNNEL_FORCED_SPLIT),
		ROUND_ROBIN(AllIcons.I_TUNNEL_ROUND_ROBIN),
		FORCED_ROUND_ROBIN(AllIcons.I_TUNNEL_FORCED_ROUND_ROBIN),
		PREFER_NEAREST(AllIcons.I_TUNNEL_PREFER_NEAREST),
		RANDOMIZE(AllIcons.I_TUNNEL_RANDOMIZE),
		SYNCHRONIZE(AllIcons.I_TUNNEL_SYNCHRONIZE),

		;

		private final String translationKey;
		private final AllIcons icon;

		SelectionMode(AllIcons icon) {
			this.icon = icon;
			this.translationKey = "tunnel.selection_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}
	}

	public boolean canTakeItems() {
		return stackToDistribute.isEmpty() && !syncedOutputActive;
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		List<ItemStack> allStacks = grabAllStacksOfGroup(true);
		if (allStacks.isEmpty())
			return false;

		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("tooltip.brass_tunnel.contains"))
			.formatted(Formatting.WHITE));
		for (ItemStack item : allStacks) {
			tooltip.add(componentSpacing.copyContentOnly()
				.append(Lang.translateDirect("tooltip.brass_tunnel.contains_entry",
					Components.translatable(item.getTranslationKey())
						.getString(),
					item.getCount()))
				.formatted(Formatting.GRAY));
		}
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("tooltip.brass_tunnel.retrieve"))
			.formatted(Formatting.DARK_GRAY));

		return true;
	}
}
