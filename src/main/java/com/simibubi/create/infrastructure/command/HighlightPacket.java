package com.simibubi.create.infrastructure.command;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

public class HighlightPacket extends SimplePacketBase {

	private final BlockPos pos;

	public HighlightPacket(BlockPos pos) {
		this.pos = pos;
	}

	public HighlightPacket(PacketByteBuf buffer) {
		this.pos = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			performHighlight(pos);
		}));
		return true;
	}

	@Environment(EnvType.CLIENT)
	public static void performHighlight(BlockPos pos) {
		if (MinecraftClient.getInstance().world == null || !MinecraftClient.getInstance().world.canSetBlock(pos))
			return;

		CreateClient.OUTLINER.showAABB("highlightCommand", VoxelShapes.fullCube()
				.getBoundingBox()
				.offset(pos), 200)
				.lineWidth(1 / 32f)
				.colored(0xEeEeEe)
				// .colored(0x243B50)
				.withFaceTexture(AllSpecialTextures.SELECTION);
	}

}
