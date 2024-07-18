package com.simibubi.create.content.redstone.smartObserver;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.FilteredDetectorFilterSlot;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase.InterfaceProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Iterate;

public class SmartObserverBlockEntity extends SmartBlockEntity {

	private static final int DEFAULT_DELAY = 6;
	private FilteringBehaviour filtering;
	private InvManipulationBehaviour observedInventory;
	private TankManipulationBehaviour observedTank;
	
	private VersionedInventoryTrackerBehaviour invVersionTracker;
	private boolean sustainSignal;
	
	public int turnOffTicks = 0;

	public SmartObserverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(20);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = new FilteringBehaviour(this, new FilteredDetectorFilterSlot(false))
			.withCallback($ -> invVersionTracker.reset()));
		behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));

		InterfaceProvider towardBlockFacing =
			(w, p, s) -> new BlockFace(p, DirectedDirectionalBlock.getTargetDirection(s));

		behaviours.add(observedInventory = new InvManipulationBehaviour(this, towardBlockFacing).bypassSidedness());
		behaviours.add(observedTank = new TankManipulationBehaviour(this, towardBlockFacing).bypassSidedness());
	}

	@Override
	public void tick() {
		super.tick();
		
		if (world.isClient())
			return;
		
		BlockState state = getCachedState();
		if (turnOffTicks > 0) {
			turnOffTicks--;
			if (turnOffTicks == 0)
				world.scheduleBlockTick(pos, state.getBlock(), 1);
		}

		if (!isActive())
			return;

		BlockPos targetPos = pos.offset(SmartObserverBlock.getTargetDirection(state));
		Block block = world.getBlockState(targetPos)
			.getBlock();

		if (!filtering.getFilter()
			.isEmpty() && block.asItem() != null && filtering.test(new ItemStack(block))) {
			activate(3);
			return;
		}

		// Detect items on belt
		TransportedItemStackHandlerBehaviour behaviour =
			BlockEntityBehaviour.get(world, targetPos, TransportedItemStackHandlerBehaviour.TYPE);
		if (behaviour != null) {
			behaviour.handleCenteredProcessingOnAllItems(.45f, stack -> {
				if (!filtering.test(stack.stack) || turnOffTicks == 6)
					return TransportedResult.doNothing();
				activate();
				return TransportedResult.doNothing();
			});
			return;
		}

		// Detect fluids in pipe
		FluidTransportBehaviour fluidBehaviour =
			BlockEntityBehaviour.get(world, targetPos, FluidTransportBehaviour.TYPE);
		if (fluidBehaviour != null) {
			for (Direction side : Iterate.directions) {
				Flow flow = fluidBehaviour.getFlow(side);
				if (flow == null || !flow.inbound || !flow.complete)
					continue;
				if (!filtering.test(flow.fluid))
					continue;
				activate();
				return;
			}
			return;
		}

		if (observedInventory.hasInventory()) {
			boolean skipInv = invVersionTracker.stillWaiting(observedInventory);
			invVersionTracker.awaitNewVersion(observedInventory);

			if (skipInv && sustainSignal)
				turnOffTicks = DEFAULT_DELAY;

			if (!skipInv) {
				sustainSignal = false;
				if (!observedInventory.simulate()
					.extract()
					.isEmpty()) {
					sustainSignal = true;
					activate();
					return;
				}
			}
		}

		if (!observedTank.simulate()
			.extractAny()
			.isEmpty()) {
			activate();
			return;
		}
	}

	public void activate() {
		activate(DEFAULT_DELAY);
	}

	public void activate(int ticks) {
		BlockState state = getCachedState();
		turnOffTicks = ticks;
		if (state.get(SmartObserverBlock.POWERED))
			return;
		world.setBlockState(pos, state.with(SmartObserverBlock.POWERED, true));
		world.updateNeighborsAlways(pos, state.getBlock());
	}

	private boolean isActive() {
		return true;
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putInt("TurnOff", turnOffTicks);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		turnOffTicks = compound.getInt("TurnOff");
	}

}
