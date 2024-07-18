package com.simibubi.create.foundation.blockEntity.behaviour.simple;

import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

public class DeferralBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<DeferralBehaviour> TYPE = new BehaviourType<>();

	private boolean needsUpdate;
	private Supplier<Boolean> callback;

	public DeferralBehaviour(SmartBlockEntity be, Supplier<Boolean> callback) {
		super(be);
		this.callback = callback;
	}

	@Override
	public boolean isSafeNBT() { return true; }

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		nbt.putBoolean("NeedsUpdate", needsUpdate);
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		needsUpdate = nbt.getBoolean("NeedsUpdate");
		super.read(nbt, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();
		if (needsUpdate && callback.get())
			needsUpdate = false;
	}

	public void scheduleUpdate() {
		needsUpdate = true;
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

}