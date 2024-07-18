package com.simibubi.create.infrastructure.debugInfo.element;

import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

public sealed interface InfoElement permits DebugInfoSection, InfoEntry {
	void write(PlayerEntity player, PacketByteBuf buffer);

	void print(int depth, @Nullable PlayerEntity player, Consumer<String> lineConsumer);

	default void print(@Nullable PlayerEntity player, Consumer<String> lineConsumer) {
		print(0, player, lineConsumer);
	}

	static InfoElement read(PacketByteBuf buffer) {
		boolean section = buffer.readBoolean();
		if (section) {
			return DebugInfoSection.read(buffer);
		} else {
			return InfoEntry.read(buffer);
		}
	}
}
