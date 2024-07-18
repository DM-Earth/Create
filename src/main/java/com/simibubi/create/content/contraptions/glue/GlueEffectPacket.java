package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class GlueEffectPacket extends SimplePacketBase {

	private BlockPos pos;
	private Direction direction;
	private boolean fullBlock;

	public GlueEffectPacket(BlockPos pos, Direction direction, boolean fullBlock) {
		this.pos = pos;
		this.direction = direction;
		this.fullBlock = fullBlock;
	}

	public GlueEffectPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
		direction = Direction.byId(buffer.readByte());
		fullBlock = buffer.readBoolean();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeByte(direction.getId());
		buffer.writeBoolean(fullBlock);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::handleClient));
		return true;
	}

	@Environment(EnvType.CLIENT)
	public void handleClient() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!mc.player.getBlockPos().isWithinDistance(pos, 100))
			return;
		SuperGlueItem.spawnParticles(mc.world, pos, direction, fullBlock);
	}

}
