package com.simibubi.create.foundation.blockEntity.behaviour;

import javax.annotation.Nullable;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;

public class ValueSettingsPacket extends BlockEntityConfigurationPacket<SmartBlockEntity> {

	private int row;
	private int value;
	private Hand interactHand;
	private Direction side;
	private boolean ctrlDown;

	public ValueSettingsPacket(BlockPos pos, int row, int value, @Nullable Hand interactHand, Direction side,
		boolean ctrlDown) {
		super(pos);
		this.row = row;
		this.value = value;
		this.interactHand = interactHand;
		this.side = side;
		this.ctrlDown = ctrlDown;
	}

	public ValueSettingsPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeVarInt(value);
		buffer.writeVarInt(row);
		buffer.writeBoolean(interactHand != null);
		if (interactHand != null)
			buffer.writeBoolean(interactHand == Hand.MAIN_HAND);
		buffer.writeVarInt(side.ordinal());
		buffer.writeBoolean(ctrlDown);
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		value = buffer.readVarInt();
		row = buffer.readVarInt();
		if (buffer.readBoolean())
			interactHand = buffer.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
		side = Direction.values()[buffer.readVarInt()];
		ctrlDown = buffer.readBoolean();
	}

	@Override
	protected void applySettings(ServerPlayerEntity player, SmartBlockEntity be) {
		for (BlockEntityBehaviour behaviour : be.getAllBehaviours()) {
			if (!(behaviour instanceof ValueSettingsBehaviour valueSettingsBehaviour))
				continue;
			if (!valueSettingsBehaviour.acceptsValueSettings())
				continue;
			if (interactHand != null) {
				valueSettingsBehaviour.onShortInteract(player, interactHand, side);
				return;
			}
			valueSettingsBehaviour.setValueSettings(player, new ValueSettings(row, value), ctrlDown);
			return;
		}
	}

	@Override
	protected void applySettings(SmartBlockEntity be) {}

}
