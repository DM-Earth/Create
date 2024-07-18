package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class EjectorAwardPacket extends BlockEntityConfigurationPacket<EjectorBlockEntity> {

	public EjectorAwardPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	public EjectorAwardPacket(BlockPos pos) {
		super(pos);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {}

	@Override
	protected void readSettings(PacketByteBuf buffer) {}

	@Override
	protected void applySettings(ServerPlayerEntity player, EjectorBlockEntity be) {
		AllAdvancements.EJECTOR_MAXED.awardTo(player);
	}

	@Override
	protected void applySettings(EjectorBlockEntity be) {}

}
