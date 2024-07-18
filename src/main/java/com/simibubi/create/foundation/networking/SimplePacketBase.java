package com.simibubi.create.foundation.networking;

import com.simibubi.create.foundation.mixin.fabric.BlockableEventLoopAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.simplenetworking.C2SPacket;
import me.pepperbell.simplenetworking.S2CPacket;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.thread.ThreadExecutor;

public abstract class SimplePacketBase implements C2SPacket, S2CPacket {

	public abstract void write(PacketByteBuf buffer);

	public abstract boolean handle(Context context);

	@Override
	public final void encode(PacketByteBuf buffer) {
		write(buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void handle(MinecraftClient client, ClientPlayNetworkHandler listener, PacketSender responseSender, SimpleChannel channel) {
		handle(new Context(client, listener, null));
	}

	@Override
	public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler listener, PacketSender responseSender, SimpleChannel channel) {
		handle(new Context(server, listener, player));
	}

	public enum NetworkDirection {
		PLAY_TO_CLIENT,
		PLAY_TO_SERVER
	}

	public record Context(ThreadExecutor<? extends Runnable> executor, PacketListener listener, @Nullable ServerPlayerEntity sender) {
		public void enqueueWork(Runnable runnable) {
			// Matches Forge's enqueueWork behavior.
			// MC will sometimes defer tasks even if already on the right thread.
			if (executor.isOnThread()) {
				runnable.run();
			} else {
				// skip extra checks
				((BlockableEventLoopAccessor) executor).callSubmitAsync(runnable);
			}
		}

		@Nullable
		public ServerPlayerEntity getSender() {
			return sender();
		}

		public NetworkDirection getDirection() {
			return sender() == null ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
		}
	}
}
