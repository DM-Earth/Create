package com.simibubi.create.content.redstone.link;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

public class RedstoneLinkBlockEntity extends SmartBlockEntity {

	private boolean receivedSignalChanged;
	private int receivedSignal;
	private int transmittedSignal;
	protected LinkBehaviour link;
	private boolean transmitter;

	public RedstoneLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void addBehavioursDeferred(List<BlockEntityBehaviour> behaviours) {
		createLink();
		behaviours.add(link);
	}

	protected void createLink() {
		Pair<ValueBoxTransform, ValueBoxTransform> slots =
			ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);
		link = transmitter ? LinkBehaviour.transmitter(this, slots, this::getSignal)
			: LinkBehaviour.receiver(this, slots, this::setSignal);
	}

	public int getSignal() {
		return transmittedSignal;
	}

	public void setSignal(int power) {
		if (receivedSignal != power)
			receivedSignalChanged = true;
		receivedSignal = power;
	}

	public void transmit(int strength) {
		transmittedSignal = strength;
		if (link != null)
			link.notifySignalChange();
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putBoolean("Transmitter", transmitter);
		compound.putInt("Receive", getReceivedSignal());
		compound.putBoolean("ReceivedChanged", receivedSignalChanged);
		compound.putInt("Transmit", transmittedSignal);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		transmitter = compound.getBoolean("Transmitter");
		super.read(compound, clientPacket);

		receivedSignal = compound.getInt("Receive");
		receivedSignalChanged = compound.getBoolean("ReceivedChanged");
		if (world == null || world.isClient || !link.newPosition)
			transmittedSignal = compound.getInt("Transmit");
	}

	@Override
	public void tick() {
		super.tick();

		if (isTransmitterBlock() != transmitter) {
			transmitter = isTransmitterBlock();
			LinkBehaviour prevlink = link;
			removeBehaviour(LinkBehaviour.TYPE);
			createLink();
			link.copyItemsFrom(prevlink);
			attachBehaviourLate(link);
		}

		if (transmitter)
			return;
		if (world.isClient)
			return;

		BlockState blockState = getCachedState();
		if (!AllBlocks.REDSTONE_LINK.has(blockState))
			return;

		if ((getReceivedSignal() > 0) != blockState.get(RedstoneLinkBlock.POWERED)) {
			receivedSignalChanged = true;
			world.setBlockState(pos, blockState.cycle(RedstoneLinkBlock.POWERED));
		}

		if (receivedSignalChanged) {
			Direction attachedFace = blockState.get(RedstoneLinkBlock.FACING)
				.getOpposite();
			BlockPos attachedPos = pos.offset(attachedFace);
			world.updateNeighbors(pos, world.getBlockState(pos)
				.getBlock());
			world.updateNeighbors(attachedPos, world.getBlockState(attachedPos)
				.getBlock());
			receivedSignalChanged = false;
		}
	}

	protected Boolean isTransmitterBlock() {
		return !getCachedState().get(RedstoneLinkBlock.RECEIVER);
	}

	public int getReceivedSignal() {
		return receivedSignal;
	}

}
