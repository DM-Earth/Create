package com.simibubi.create.content.kinetics.chainDrive;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class ChainGearshiftBlockEntity extends KineticBlockEntity {

	int signal;
	boolean signalChanged;

	public ChainGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		signal = 0;
		setLazyTickRate(40);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putInt("Signal", signal);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		signal = compound.getInt("Signal");
		super.read(compound, clientPacket);
	}

	public float getModifier() {
		return getModifierForSignal(signal);
	}

	public void neighbourChanged() {
		if (!hasWorld())
			return;
		int power = world.getReceivedRedstonePower(pos);
		if (power != signal) 
			signalChanged = true;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		neighbourChanged();
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isClient)
			return;
		if (signalChanged) {
			signalChanged = false;
			analogSignalChanged(world.getReceivedRedstonePower(pos));
		}
	}

	protected void analogSignalChanged(int newSignal) {
		detachKinetics();
		removeSource();
		signal = newSignal;
		attachKinetics();
	}

	protected float getModifierForSignal(int newPower) {
		if (newPower == 0)
			return 1;
		return 1 + ((newPower + 1) / 16f);
	}

}
