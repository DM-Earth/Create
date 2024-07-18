package com.simibubi.create.foundation.blockEntity.behaviour;

import java.util.ConcurrentModificationException;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

public abstract class BlockEntityBehaviour {

	public SmartBlockEntity blockEntity;
	private int lazyTickRate;
	private int lazyTickCounter;

	public BlockEntityBehaviour(SmartBlockEntity be) {
		blockEntity = be;
		setLazyTickRate(10);
	}

	public abstract BehaviourType<?> getType();

	public void initialize() {

	}

	public void tick() {
		if (lazyTickCounter-- <= 0) {
			lazyTickCounter = lazyTickRate;
			lazyTick();
		}

	}

	public void read(NbtCompound nbt, boolean clientPacket) {

	}

	public void write(NbtCompound nbt, boolean clientPacket) {

	}

	public boolean isSafeNBT() {
		return false;
	}

	public ItemRequirement getRequiredItems() {
		return ItemRequirement.NONE;
	}

	public void onBlockChanged(BlockState oldState) {

	}

	public void onNeighborChanged(BlockPos neighborPos) {

	}

	/**
	 * Block destroyed or Chunk unloaded. Usually invalidates capabilities
	 */
	public void unload() {}

	/**
	 * Block destroyed or removed. Requires block to call ITE::onRemove
	 */
	public void destroy() {}

	public void setLazyTickRate(int slowTickRate) {
		this.lazyTickRate = slowTickRate;
		this.lazyTickCounter = slowTickRate;
	}

	public void lazyTick() {

	}

	public BlockPos getPos() {
		return blockEntity.getPos();
	}

	public World getWorld() {
		return blockEntity.getWorld();
	}

	public static <T extends BlockEntityBehaviour> T get(BlockView reader, BlockPos pos, BehaviourType<T> type) {
		BlockEntity be;
		try {
			be = reader.getBlockEntity(pos);
		} catch (ConcurrentModificationException e) {
			be = null;
		}
		return get(be, type);
	}

	public static <T extends BlockEntityBehaviour> T get(BlockEntity be, BehaviourType<T> type) {
		if (be == null)
			return null;
		if (!(be instanceof SmartBlockEntity))
			return null;
		SmartBlockEntity ste = (SmartBlockEntity) be;
		return ste.getBehaviour(type);
	}
}
