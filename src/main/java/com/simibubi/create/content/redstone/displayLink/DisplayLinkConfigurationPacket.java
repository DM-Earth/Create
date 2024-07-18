package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class DisplayLinkConfigurationPacket extends BlockEntityConfigurationPacket<DisplayLinkBlockEntity> {

	private NbtCompound configData;
	private int targetLine;

	public DisplayLinkConfigurationPacket(BlockPos pos, NbtCompound configData, int targetLine) {
		super(pos);
		this.configData = configData;
		this.targetLine = targetLine;
	}

	public DisplayLinkConfigurationPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeNbt(configData);
		buffer.writeInt(targetLine);
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		configData = buffer.readNbt();
		targetLine = buffer.readInt();
	}

	@Override
	protected void applySettings(DisplayLinkBlockEntity be) {
		be.targetLine = targetLine;

		if (!configData.contains("Id")) {
			be.notifyUpdate();
			return;
		}

		Identifier id = new Identifier(configData.getString("Id"));
		DisplaySource source = AllDisplayBehaviours.getSource(id);
		if (source == null) {
			be.notifyUpdate();
			return;
		}

		if (be.activeSource == null || be.activeSource != source) {
			be.activeSource = source;
			be.setSourceConfig(configData.copy());
		} else {
			be.getSourceConfig()
				.copyFrom(configData);
		}

		be.updateGatheredData();
		be.notifyUpdate();
	}

}
