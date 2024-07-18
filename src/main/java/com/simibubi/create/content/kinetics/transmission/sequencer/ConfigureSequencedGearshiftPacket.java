package com.simibubi.create.content.kinetics.transmission.sequencer;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class ConfigureSequencedGearshiftPacket extends BlockEntityConfigurationPacket<SequencedGearshiftBlockEntity> {

	private NbtList instructions;

	public ConfigureSequencedGearshiftPacket(BlockPos pos, NbtList instructions) {
		super(pos);
		this.instructions = instructions;
	}

	public ConfigureSequencedGearshiftPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		instructions = buffer.readNbt().getList("data", NbtElement.COMPOUND_TYPE);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		NbtCompound tag = new NbtCompound();
		tag.put("data", instructions);
		buffer.writeNbt(tag);
	}

	@Override
	protected void applySettings(SequencedGearshiftBlockEntity be) {
		if (be.computerBehaviour.hasAttachedComputer())
			return;

		be.run(-1);
		be.instructions = Instruction.deserializeAll(instructions);
		be.sendData();
	}

}
