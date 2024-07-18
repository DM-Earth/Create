package com.simibubi.create.content.redstone.diodes;

import static com.simibubi.create.content.redstone.diodes.BrassDiodeBlock.POWERING;

import java.util.List;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.Lang;

public abstract class BrassDiodeBlockEntity extends SmartBlockEntity implements ClipboardCloneable {

	protected int state;
	ScrollValueBehaviour maxState;

	public BrassDiodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		maxState = new BrassDiodeScrollValueBehaviour(Lang.translateDirect("logistics.redstone_interval"), this,
			new BrassDiodeScrollSlot()).between(2, 60 * 20 * 60);
		maxState.withFormatter(this::format);
		maxState.withCallback(this::onMaxDelayChanged);
		maxState.setValue(2);
		behaviours.add(maxState);
	}

	public float getProgress() {
		int max = Math.max(2, maxState.getValue());
		return MathHelper.clamp(state, 0, max) / (float) max;
	}

	public boolean isIdle() {
		return state == 0;
	}

	@Override
	public void tick() {
		super.tick();
		boolean powered = getCachedState().get(AbstractRedstoneGateBlock.POWERED);
		boolean powering = getCachedState().get(POWERING);
		boolean atMax = state >= maxState.getValue();
		boolean atMin = state <= 0;
		updateState(powered, powering, atMax, atMin);
	}

	protected abstract void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin);

	private void onMaxDelayChanged(int newMax) {
		state = MathHelper.clamp(state, 0, newMax);
		sendData();
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		state = compound.getInt("State");
		super.read(compound, clientPacket);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putInt("State", state);
		super.write(compound, clientPacket);
	}

	private String format(int value) {
		if (value < 60)
			return value + "t";
		if (value < 20 * 60)
			return (value / 20) + "s";
		return (value / 20 / 60) + "m";
	}

	@Override
	public String getClipboardKey() {
		return "Block";
	}
	
	@Override
	public boolean readFromClipboard(NbtCompound tag, PlayerEntity player, Direction side, boolean simulate) {
		if (!tag.contains("Inverted"))
			return false;
		if (simulate)
			return true;
		BlockState blockState = getCachedState();
		if (blockState.get(BrassDiodeBlock.INVERTED) != tag.getBoolean("Inverted"))
			world.setBlockState(pos, blockState.cycle(BrassDiodeBlock.INVERTED));
		return true;
	}
	
	@Override
	public boolean writeToClipboard(NbtCompound tag, Direction side) {
		tag.putBoolean("Inverted", getCachedState().getOrEmpty(BrassDiodeBlock.INVERTED)
			.orElse(false));
		return true;
	}
	
}
