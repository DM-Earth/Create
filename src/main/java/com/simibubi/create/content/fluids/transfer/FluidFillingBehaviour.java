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
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Unit;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.WorldTickScheduler;

public class FluidFillingBehaviour extends FluidManipulationBehaviour {

	public static final BehaviourType<FluidFillingBehaviour> TYPE = new BehaviourType<>();

	// fabric: we need to save the queue for snapshots, so it must be a copyable type.
	SortedArraySet<BlockPosEntry> queue;

	List<BlockPosEntry> infinityCheckFrontier;
	Set<BlockPos> infinityCheckVisited;

	SnapshotParticipant<Data> snapshotParticipant = new SnapshotParticipant<>() {
		@Override
		protected Data createSnapshot() {
			return new Data(new HashSet<>(visited), copySet(queue), counterpartActed);
		}

		@Override
		protected void readSnapshot(Data snapshot) {
			visited = snapshot.visited;
			queue = snapshot.queue;
			counterpartActed = snapshot.counterpartActed;
		}
	};

	@Override
	protected SnapshotParticipant<?> snapshotParticipant() {
		return snapshotParticipant;
	}

	record Data(Set<BlockPos> visited, SortedArraySet<BlockPosEntry> queue, boolean counterpartActed) {
	}

	public FluidFillingBehaviour(SmartBlockEntity be) {
		super(be);
		queue = SortedArraySet.create((p, p2) -> -comparePositions(p, p2));
		revalidateIn = 1;
		infinityCheckFrontier = new ArrayList<>();
		infinityCheckVisited = new HashSet<>();
	}

	@Override
	public void tick() {
		super.tick();
		if (!infinityCheckFrontier.isEmpty() && rootPos != null) {
			Fluid fluid = getWorld().getFluidState(rootPos)
				.getFluid();
			if (fluid != Fluids.EMPTY)
				continueValidation(fluid);
		}
		if (revalidateIn > 0)
			revalidateIn--;
	}

	protected void continueValidation(Fluid fluid) {
		try {
			search(fluid, infinityCheckFrontier, infinityCheckVisited,
				(p, d) -> infinityCheckFrontier.add(new BlockPosEntry(p, d)), true);
		} catch (ChunkNotLoadedException e) {
			infinityCheckFrontier.clear();
			infinityCheckVisited.clear();
			setLongValidationTimer();
			return;
		}

		int maxBlocks = maxBlocks();

		if (infinityCheckVisited.size() > maxBlocks && maxBlocks != -1 && !fillInfinite()) {
			if (!infinite) {
				reset(null);
				infinite = true;
				blockEntity.sendData();
			}
			infinityCheckFrontier.clear();
			setLongValidationTimer();
			return;
		}

		if (!infinityCheckFrontier.isEmpty())
			return;
		if (infinite) {
			reset(null);
			return;
		}

		infinityCheckVisited.clear();
	}

	public boolean tryDeposit(Fluid fluid, BlockPos root, TransactionContext ctx) {
		if (!Objects.equals(root, rootPos)) {
			reset(ctx);
			rootPos = root;
			BlockPosEntry e = new BlockPosEntry(root, 0);
			queue.add(e);
			affectedArea = BlockBox.create(rootPos, rootPos);
			return false;
		}

		if (counterpartActed) {
			counterpartActed = false;
			softReset(root);
			return false;
		}

		if (affectedArea == null)
			affectedArea = BlockBox.create(root, root);

		if (revalidateIn == 0) {
			visited.clear();
			infinityCheckFrontier.clear();
			infinityCheckVisited.clear();
			infinityCheckFrontier.add(new BlockPosEntry(root, 0));
			setValidationTimer();
			softReset(root);
		}

		World world = getWorld();
		int maxRange = maxRange();
		int maxRangeSq = maxRange * maxRange;
		int maxBlocks = maxBlocks();
		boolean evaporate = world.getDimension()
			.ultrawarm() && FluidHelper.isTag(fluid, FluidTags.WATER);
		boolean canPlaceSources = AllConfigs.server().fluids.fluidFillPlaceFluidSourceBlocks.get();

		if ((!fillInfinite() && infinite) || evaporate || !canPlaceSources) {
			FluidState fluidState = world.getFluidState(rootPos);
			boolean equivalentTo = fluidState.getFluid()
				.matchesType(fluid);
			if (!equivalentTo && !evaporate && canPlaceSources)
				return false;

			TransactionCallback.onSuccess(ctx, () -> {
				playEffect(world, root, fluid, false);
				if (evaporate) {
					int i = root.getX();
					int j = root.getY();
					int k = root.getZ();
					world.playSound(null, i, j, k, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
							2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
				} else if (!canPlaceSources)
					blockEntity.award(AllAdvancements.HOSE_PULLEY);
			});
			return true;
		}

		boolean success = false;
		for (int i = 0; !success && !queue.isEmpty() && i < searchedPerTick; i++) {
			BlockPosEntry entry = queue.first();
			BlockPos currentPos = entry.pos();

			if (visited.contains(currentPos)) {
				dequeue(queue);
				continue;
			}

			snapshotParticipant.updateSnapshots(ctx);
			visited.add(currentPos);

			if (visited.size() >= maxBlocks && maxBlocks != -1) {
				infinite = true;
				if (!fillInfinite()) {
					visited.clear();
					queue.clear();
				return false;}
			}

			SpaceType spaceType = getAtPos(world, currentPos, fluid);
			if (spaceType == SpaceType.BLOCKING)
				continue;
			if (spaceType == SpaceType.FILLABLE) {
				success = true;
				BlockState blockState = world.getBlockState(currentPos);

				if (!blockEntity.isVirtual())
					world.updateSnapshots(ctx);

				new SnapshotParticipant<Unit>() { // can't be a typical TransactionCallback because ordering refuses to cooperate
					@Override protected Unit createSnapshot() { return Unit.INSTANCE; }
					@Override protected void readSnapshot(Unit snapshot) {}

					@Override
					protected void onFinalCommit() {
						playEffect(world, currentPos, fluid, false);
						QueryableTickScheduler<Fluid> pendingFluidTicks = world.getFluidTickScheduler();
						if (pendingFluidTicks instanceof WorldTickScheduler) {
							WorldTickScheduler<Fluid> serverTickList = (WorldTickScheduler<Fluid>) pendingFluidTicks;
							serverTickList.clearNextTicks(new BlockBox(currentPos));
						}
						affectedArea = BBHelper.encapsulate(affectedArea, currentPos);
					}
				}.updateSnapshots(ctx);

				if (blockState.contains(Properties.WATERLOGGED) && fluid.matchesType(Fluids.WATER)) {
					if (!blockEntity.isVirtual())
						world.setBlockState(currentPos,
								updatePostWaterlogging(blockState.with(Properties.WATERLOGGED, true)),
								2 | 16);
				} else {
					replaceBlock(world, currentPos, blockState, ctx);
					if (!blockEntity.isVirtual())
						world.setBlockState(currentPos, FluidHelper.convertToStill(fluid)
								.getDefaultState()
								.getBlockState(), 2 | 16);
				}
			}

			visited.add(currentPos);
			dequeue(queue);

			for (Direction side : Iterate.directions) {
				if (side == Direction.UP)
					continue;

				BlockPos offsetPos = currentPos.offset(side);
				if (visited.contains(offsetPos))
					continue;
				if (offsetPos.getSquaredDistance(rootPos) > maxRangeSq)
					continue;

				SpaceType nextSpaceType = getAtPos(world, offsetPos, fluid);
				if (nextSpaceType != SpaceType.BLOCKING)
					queue.add(new BlockPosEntry(offsetPos, entry.distance() + 1));
			}
		}

		if (success)
			TransactionCallback.onSuccess(ctx, () -> blockEntity.award(AllAdvancements.HOSE_PULLEY));
		return success;
	}

	protected void softReset(BlockPos root) {
		visited.clear();
		queue.clear();
		queue.add(new BlockPosEntry(root, 0));
		infinite = false;
		setValidationTimer();
		blockEntity.sendData();
	}

	enum SpaceType {
		FILLABLE, FILLED, BLOCKING
	}

	protected SpaceType getAtPos(World world, BlockPos pos, Fluid toFill) {
		BlockState blockState = world.getBlockState(pos);
		FluidState fluidState = blockState.getFluidState();

		if (blockState.contains(Properties.WATERLOGGED))
			return toFill.matchesType(Fluids.WATER)
				? blockState.get(Properties.WATERLOGGED) ? SpaceType.FILLED : SpaceType.FILLABLE
				: SpaceType.BLOCKING;

		if (blockState.getBlock() instanceof FluidBlock)
			return blockState.get(FluidBlock.LEVEL) == 0
				? toFill.matchesType(fluidState.getFluid()) ? SpaceType.FILLED : SpaceType.BLOCKING
				: SpaceType.FILLABLE;

		if (fluidState.getFluid() != Fluids.EMPTY
			&& blockState.getCollisionShape(getWorld(), pos, ShapeContext.absent())
				.isEmpty())
			return toFill.matchesType(fluidState.getFluid()) ? SpaceType.FILLED : SpaceType.BLOCKING;

		return canBeReplacedByFluid(world, pos, blockState) ? SpaceType.FILLABLE : SpaceType.BLOCKING;
	}

	protected void replaceBlock(World world, BlockPos pos, BlockState state, TransactionContext ctx) {
		BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
		TransactionCallback.onSuccess(ctx, () -> {
			Block.dropStacks(state, world, pos, blockEntity);
		});
	}

	// From FlowingFluidBlock#isBlocked
	protected boolean canBeReplacedByFluid(BlockView world, BlockPos pos, BlockState pState) {
		Block block = pState.getBlock();
		if (!(block instanceof DoorBlock) && !pState.isIn(BlockTags.SIGNS) && !pState.isOf(Blocks.LADDER)
			&& !pState.isOf(Blocks.SUGAR_CANE) && !pState.isOf(Blocks.BUBBLE_COLUMN)) {
			if (!pState.isOf(Blocks.NETHER_PORTAL) && !pState.isOf(Blocks.END_PORTAL) && !pState.isOf(Blocks.END_GATEWAY)
				&& !pState.isOf(Blocks.STRUCTURE_VOID)) {
				return !pState.blocksMovement();
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	protected BlockState updatePostWaterlogging(BlockState state) {
		if (state.contains(Properties.LIT))
			state = state.with(Properties.LIT, false);
		return state;
	}

	@Override
	public void reset(@Nullable TransactionContext ctx) {
		super.reset(ctx);
		queue.clear();
		infinityCheckFrontier.clear();
		infinityCheckVisited.clear();
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

}
