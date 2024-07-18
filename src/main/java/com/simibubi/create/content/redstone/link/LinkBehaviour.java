package com.simibubi.create.content.redstone.link;

import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.Couple;

public class LinkBehaviour extends BlockEntityBehaviour implements IRedstoneLinkable, ClipboardCloneable {

	public static final BehaviourType<LinkBehaviour> TYPE = new BehaviourType<>();

	enum Mode {
		TRANSMIT, RECEIVE
	}

	Frequency frequencyFirst;
	Frequency frequencyLast;
	ValueBoxTransform firstSlot;
	ValueBoxTransform secondSlot;
	Vec3d textShift;

	public boolean newPosition;
	private Mode mode;
	private IntSupplier transmission;
	private IntConsumer signalCallback;

	protected LinkBehaviour(SmartBlockEntity be, Pair<ValueBoxTransform, ValueBoxTransform> slots) {
		super(be);
		frequencyFirst = Frequency.EMPTY;
		frequencyLast = Frequency.EMPTY;
		firstSlot = slots.getLeft();
		secondSlot = slots.getRight();
		textShift = Vec3d.ZERO;
		newPosition = true;
	}

	public static LinkBehaviour receiver(SmartBlockEntity be, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		IntConsumer signalCallback) {
		LinkBehaviour behaviour = new LinkBehaviour(be, slots);
		behaviour.signalCallback = signalCallback;
		behaviour.mode = Mode.RECEIVE;
		return behaviour;
	}

	public static LinkBehaviour transmitter(SmartBlockEntity be, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		IntSupplier transmission) {
		LinkBehaviour behaviour = new LinkBehaviour(be, slots);
		behaviour.transmission = transmission;
		behaviour.mode = Mode.TRANSMIT;
		return behaviour;
	}

	public LinkBehaviour moveText(Vec3d shift) {
		textShift = shift;
		return this;
	}

	public void copyItemsFrom(LinkBehaviour behaviour) {
		if (behaviour == null)
			return;
		frequencyFirst = behaviour.frequencyFirst;
		frequencyLast = behaviour.frequencyLast;
	}

	@Override
	public boolean isListening() {
		return mode == Mode.RECEIVE;
	}

	@Override
	public int getTransmittedStrength() {
		return mode == Mode.TRANSMIT ? transmission.getAsInt() : 0;
	}

	@Override
	public void setReceivedStrength(int networkPower) {
		if (!newPosition)
			return;
		signalCallback.accept(networkPower);
	}

	public void notifySignalChange() {
		Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(getWorld(), this);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (getWorld().isClient)
			return;
		getHandler().addToNetwork(getWorld(), this);
		newPosition = true;
	}

	@Override
	public Couple<Frequency> getNetworkKey() {
		return Couple.create(frequencyFirst, frequencyLast);
	}

	@Override
	public void unload() {
		super.unload();
		if (getWorld().isClient)
			return;
		getHandler().removeFromNetwork(getWorld(), this);
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		nbt.put("FrequencyFirst", frequencyFirst.getStack()
			.writeNbt(new NbtCompound()));
		nbt.put("FrequencyLast", frequencyLast.getStack()
			.writeNbt(new NbtCompound()));
		nbt.putLong("LastKnownPosition", blockEntity.getPos()
			.asLong());
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		long positionInTag = blockEntity.getPos()
			.asLong();
		long positionKey = nbt.getLong("LastKnownPosition");
		newPosition = positionInTag != positionKey;

		super.read(nbt, clientPacket);
		frequencyFirst = Frequency.of(ItemStack.fromNbt(nbt.getCompound("FrequencyFirst")));
		frequencyLast = Frequency.of(ItemStack.fromNbt(nbt.getCompound("FrequencyLast")));
	}

	public void setFrequency(boolean first, ItemStack stack) {
		stack = stack.copy();
		stack.setCount(1);
		ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
		boolean changed = !ItemStack.canCombine(stack, toCompare);

		if (changed)
			getHandler().removeFromNetwork(getWorld(), this);

		if (first)
			frequencyFirst = Frequency.of(stack);
		else
			frequencyLast = Frequency.of(stack);

		if (!changed)
			return;

		blockEntity.sendData();
		getHandler().addToNetwork(getWorld(), this);
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	private RedstoneLinkNetworkHandler getHandler() {
		return Create.REDSTONE_LINK_NETWORK_HANDLER;
	}

	public static class SlotPositioning {
		Function<BlockState, Pair<Vec3d, Vec3d>> offsets;
		Function<BlockState, Vec3d> rotation;
		float scale;

		public SlotPositioning(Function<BlockState, Pair<Vec3d, Vec3d>> offsetsForState,
			Function<BlockState, Vec3d> rotationForState) {
			offsets = offsetsForState;
			rotation = rotationForState;
			scale = 1;
		}

		public SlotPositioning scale(float scale) {
			this.scale = scale;
			return this;
		}

	}

	public boolean testHit(Boolean first, Vec3d hit) {
		BlockState state = blockEntity.getCachedState();
		Vec3d localHit = hit.subtract(Vec3d.of(blockEntity.getPos()));
		return (first ? firstSlot : secondSlot).testHit(state, localHit);
	}

	@Override
	public boolean isAlive() {
		World level = getWorld();
		BlockPos pos = getPos();
		if (blockEntity.isChunkUnloaded())
			return false;
		if (blockEntity.isRemoved())
			return false;
		if (!level.canSetBlock(pos))
			return false;
		return level.getBlockEntity(pos) == blockEntity;
	}

	@Override
	public BlockPos getLocation() {
		return getPos();
	}

	@Override
	public String getClipboardKey() {
		return "Frequencies";
	}

	@Override
	public boolean writeToClipboard(NbtCompound tag, Direction side) {
		tag.put("First", frequencyFirst.getStack()
			.writeNbt(new NbtCompound()));
		tag.put("Last", frequencyLast.getStack()
			.writeNbt(new NbtCompound()));
		return true;
	}

	@Override
	public boolean readFromClipboard(NbtCompound tag, PlayerEntity player, Direction side, boolean simulate) {
		if (!tag.contains("First") || !tag.contains("Last"))
			return false;
		if (simulate)
			return true;
		setFrequency(true, ItemStack.fromNbt(tag.getCompound("First")));
		setFrequency(false, ItemStack.fromNbt(tag.getCompound("Last")));
		return true;
	}

}
