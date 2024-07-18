package com.simibubi.create.content.fluids.transfer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.BBHelper;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.LiquidBlockAccessor;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class FluidDrainingBehaviour extends FluidManipulationBehaviour {

	public static final BehaviourType<FluidDrainingBehaviour> TYPE = new BehaviourType<>();

	Fluid fluid;

	// Execution
	Set<BlockPos> validationSet;
	// fabric: we need to save the queue for snapshots, so it must be a copyable type.
	SortedArraySet<BlockPosEntry> queue;
	boolean isValid;

	// Validation
	List<BlockPosEntry> validationFrontier;
	Set<BlockPos> validationVisited;
	Set<BlockPos> newValidationSet;

	SnapshotParticipant<Data> snapshotParticipant = new SnapshotParticipant<>() {
		@Override
		protected Data createSnapshot() {
			FluidDrainingBehaviour b = FluidDrainingBehaviour.this;
			BlockPos rootPos = b.rootPos == null ? null : b.rootPos.toImmutable();
			BlockBox box = b.affectedArea == null ? null : new BlockBox(
					affectedArea.getMinX(), affectedArea.getMinY(), affectedArea.getMinZ(),
					affectedArea.getMaxX(), affectedArea.getMaxY(), affectedArea.getMaxZ()
			);

			return new Data(
					fluid, isValid, rootPos, new ArrayList<>(validationFrontier), new HashSet<>(validationVisited),
					new HashSet<>(newValidationSet), revalidateIn, box, copySet(queue)
			);
		}

		@Override
		protected void readSnapshot(Data snapshot) {
			fluid = snapshot.fluid;
			isValid = snapshot.valid;
			validationFrontier = snapshot.validationFrontier;
			validationVisited = snapshot.validationVisited;
			newValidationSet = snapshot.newValidationSet;
			revalidateIn = snapshot.revalidateIn;
			affectedArea = snapshot.affectedArea;
			rootPos = snapshot.rootPos;
			queue = snapshot.queue;
		}
	};

	@Override
	protected SnapshotParticipant<?> snapshotParticipant() {
		return snapshotParticipant;
	}

	record Data(Fluid fluid, boolean valid, BlockPos rootPos, List<BlockPosEntry> validationFrontier,
				Set<BlockPos> validationVisited, Set<BlockPos> newValidationSet, int revalidateIn,
				BlockBox affectedArea, SortedArraySet<BlockPosEntry> queue) {
	}

	public FluidDrainingBehaviour(SmartBlockEntity be) {
		super(be);
		validationVisited = new HashSet<>();
		validationFrontier = new ArrayList<>();
		validationSet = new HashSet<>();
		newValidationSet = new HashSet<>();
		queue = SortedArraySet.create(this::comparePositions);
	}

	@Nullable
	public boolean pullNext(BlockPos root, TransactionContext ctx) {
		if (!frontier.isEmpty())
			return false;
		if (!Objects.equals(root, rootPos)) {
			rebuildContext(root, ctx);
			return false;
		}

		if (counterpartActed) {
			counterpartActed = false;
			softReset(root, ctx);
			return false;
		}

		if (affectedArea == null)
			affectedArea = BlockBox.create(root, root);

		World world = getWorld();
		if (!queue.isEmpty() && !isValid) {
			rebuildContext(root, ctx);
			return false;
		}

		snapshotParticipant.updateSnapshots(ctx);
		if (validationFrontier.isEmpty() && !queue.isEmpty() && revalidateIn == 0)
			revalidate(root);

		if (infinite) {
			blockEntity.award(AllAdvancements.HOSE_PULLEY);
			if (FluidHelper.isLava(fluid))
				blockEntity.award(AllAdvancements.HOSE_PULLEY_LAVA);

			playEffect(world, root, fluid, true);
			return true;
		}

		while (!queue.isEmpty()) {
			// Dont dequeue here, so we can decide not to dequeue a valid entry when
			// simulating
			BlockPos currentPos = queue.first()
				.pos();

			BlockState blockState = world.getBlockState(currentPos);
			BlockState emptied = blockState;
			Fluid fluid = Fluids.EMPTY;

			if (blockState.contains(Properties.WATERLOGGED)
				&& blockState.get(Properties.WATERLOGGED)) {
				emptied = blockState.with(Properties.WATERLOGGED, Boolean.valueOf(false));
				fluid = Fluids.WATER;
			} else if (blockState.getBlock() instanceof FluidBlock) {
				FluidBlock flowingFluid = (FluidBlock) blockState.getBlock();
				emptied = Blocks.AIR.getDefaultState();
				if (blockState.get(FluidBlock.LEVEL) == 0)
					fluid = ((LiquidBlockAccessor) flowingFluid).port_lib$getFluid().getStill();
				else {
					affectedArea = BBHelper.encapsulate(affectedArea, BlockBox.create(currentPos, currentPos));
					if (!blockEntity.isVirtual())
						world.setBlockState(currentPos, emptied, 2 | 16);
					dequeue(queue);
					if (queue.isEmpty()) {
						isValid = checkValid(world, rootPos);
						reset(ctx);
					}
					continue;
				}
			} else if (blockState.getFluidState()
				.getFluid() != Fluids.EMPTY && blockState.getCollisionShape(world, currentPos, ShapeContext.absent())
					.isEmpty()) {
				fluid = blockState.getFluidState()
					.getFluid();
				emptied = Blocks.AIR.getDefaultState();
			}

			if (this.fluid == null)
				this.fluid = fluid;

			if (!this.fluid.matchesType(fluid)) {
				dequeue(queue);
				if (queue.isEmpty()) {
					isValid = checkValid(world, rootPos);
					reset(ctx);
				}
				continue;
			}

			Fluid finalFluid = fluid;
			TransactionCallback.onSuccess(ctx, () -> {
				playEffect(world, currentPos, finalFluid, true);
				blockEntity.award(AllAdvancements.HOSE_PULLEY);
				if (infinite && FluidHelper.isLava(finalFluid))
					blockEntity.award(AllAdvancements.HOSE_PULLEY_LAVA);
			});

			if (infinite) {
				return true;
			}

			if (!blockEntity.isVirtual()) {
				world.updateSnapshots(ctx);
				world.setBlockState(currentPos, emptied, 2 | 16);
			}
			affectedArea = BBHelper.encapsulate(affectedArea, currentPos);

			dequeue(queue);
			if (queue.isEmpty()) {
				isValid = checkValid(world, rootPos);
				reset(ctx);
			} else if (!validationSet.contains(currentPos)) {
				reset(ctx);
			}
			return true;
		}

		if (rootPos == null)
			return false;

		if (isValid)
			rebuildContext(root, ctx);

		return false;
	}

	protected void softReset(BlockPos root, TransactionContext ctx) {
		queue.clear();
		validationSet.clear();
		newValidationSet.clear();
		validationFrontier.clear();
		validationVisited.clear();
		visited.clear();
		infinite = false;
		setValidationTimer();
		frontier.add(new BlockPosEntry(root, 0));
		TransactionCallback.onSuccess(ctx, blockEntity::sendData);
	}

	protected boolean checkValid(World world, BlockPos root) {
		BlockPos currentPos = root;
		for (int timeout = 1000; timeout > 0 && !root.equals(blockEntity.getPos()); timeout--) {
			FluidBlockType canPullFluidsFrom = canPullFluidsFrom(world.getBlockState(currentPos), currentPos);
			if (canPullFluidsFrom == FluidBlockType.FLOWING) {
				for (Direction d : Iterate.directions) {
					BlockPos side = currentPos.offset(d);
					if (canPullFluidsFrom(world.getBlockState(side), side) == FluidBlockType.SOURCE)
						return true;
				}
				currentPos = currentPos.up();
				continue;
			}
			if (canPullFluidsFrom == FluidBlockType.SOURCE)
				return true;
			break;
		}
		return false;
	}

	enum FluidBlockType {
		NONE, SOURCE, FLOWING;
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		super.read(nbt, clientPacket);
		if (!clientPacket && affectedArea != null)
			frontier.add(new BlockPosEntry(rootPos, 0));
	}

	protected FluidBlockType canPullFluidsFrom(BlockState blockState, BlockPos pos) {
		if (blockState.contains(Properties.WATERLOGGED)
			&& blockState.get(Properties.WATERLOGGED))
			return FluidBlockType.SOURCE;
		if (blockState.getBlock() instanceof FluidBlock)
			return blockState.get(FluidBlock.LEVEL) == 0 ? FluidBlockType.SOURCE : FluidBlockType.FLOWING;
		if (blockState.getFluidState()
			.getFluid() != Fluids.EMPTY && blockState.getCollisionShape(getWorld(), pos, ShapeContext.absent())
				.isEmpty())
			return FluidBlockType.SOURCE;
		return FluidBlockType.NONE;
	}

	@Override
	public void tick() {
		super.tick();
		if (rootPos != null)
			isValid = checkValid(getWorld(), rootPos);
		if (!frontier.isEmpty()) {
			continueSearch();
			return;
		}
		if (!validationFrontier.isEmpty()) {
			continueValidation();
			return;
		}
		if (revalidateIn > 0)
			revalidateIn--;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
	}

	public void rebuildContext(BlockPos root, TransactionContext ctx) {
		reset(ctx);
		rootPos = root;
		affectedArea = BlockBox.create(rootPos, rootPos);
		if (isValid)
			frontier.add(new BlockPosEntry(root, 0));
	}

	public void revalidate(BlockPos root) {
		validationFrontier.clear();
		validationVisited.clear();
		newValidationSet.clear();
		validationFrontier.add(new BlockPosEntry(root, 0));
		setValidationTimer();
	}

	private void continueSearch() {
		try {
			fluid = search(fluid, frontier, visited, (e, d) -> {
				BlockPosEntry entry = new BlockPosEntry(e, d);
				queue.add(entry);
				validationSet.add(e);
			}, false);
		} catch (ChunkNotLoadedException e) {
			blockEntity.sendData();
			frontier.clear();
			visited.clear();
		}

		int maxBlocks = maxBlocks();
		if (visited.size() > maxBlocks && canDrainInfinitely(fluid) && !queue.isEmpty()) {
			infinite = true;
			BlockPos firstValid = queue.first()
				.pos();
			frontier.clear();
			visited.clear();
			queue.clear();
			queue.add(new BlockPosEntry(firstValid, 0));
			blockEntity.sendData();
			return;
		}

		if (!frontier.isEmpty())
			return;

		blockEntity.sendData();
		visited.clear();
	}

	private void continueValidation() {
		try {
			search(fluid, validationFrontier, validationVisited, (e, d) -> newValidationSet.add(e), false);
		} catch (ChunkNotLoadedException e) {
			validationFrontier.clear();
			validationVisited.clear();
			setLongValidationTimer();
			return;
		}

		int maxBlocks = maxBlocks();
		if (validationVisited.size() > maxBlocks && canDrainInfinitely(fluid)) {
			if (!infinite)
				reset(null);
			validationFrontier.clear();
			setLongValidationTimer();
			return;
		}

		if (!validationFrontier.isEmpty())
			return;
		if (infinite) {
			reset(null);
			return;
		}

		validationSet = newValidationSet;
		newValidationSet = new HashSet<>();
		validationVisited.clear();
	}

	@Override
	public void reset(@Nullable TransactionContext ctx) {
		super.reset(ctx);

		fluid = null;
		rootPos = null;
		queue.clear();
		validationSet.clear();
		newValidationSet.clear();
		validationFrontier.clear();
		validationVisited.clear();
		if (ctx != null) TransactionCallback.onSuccess(ctx, blockEntity::sendData);
		else blockEntity.sendData();
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	protected boolean isSearching() {
		return !frontier.isEmpty();
	}

	public FluidStack getDrainableFluid(BlockPos rootPos) {
		try (Transaction t = TransferUtil.getTransaction()) { // simulate pullNext
			if (fluid == null || isSearching() || !pullNext(rootPos, t)) {
				return FluidStack.EMPTY;
			}
		}
		return new FluidStack(fluid, FluidConstants.BUCKET);
	}
}
