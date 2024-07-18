package com.simibubi.create.compat.computercraft;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.nbt.NbtCompound;

public class AbstractComputerBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<AbstractComputerBehaviour> TYPE = new BehaviourType<>();

	boolean hasAttachedComputer;

	public AbstractComputerBehaviour(SmartBlockEntity te) {
		super(te);
		this.hasAttachedComputer = false;
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		hasAttachedComputer = nbt.getBoolean("HasAttachedComputer");
		super.read(nbt, clientPacket);
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		nbt.putBoolean("HasAttachedComputer", hasAttachedComputer);
		super.write(nbt, clientPacket);
	}

	@Nullable
	public <T> T getPeripheral() {
		return null;
	}

	public void setHasAttachedComputer(boolean hasAttachedComputer) {
		this.hasAttachedComputer = hasAttachedComputer;
	}

	public boolean hasAttachedComputer() {
		return hasAttachedComputer;
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

}
