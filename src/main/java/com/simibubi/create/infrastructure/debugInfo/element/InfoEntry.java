package com.simibubi.create.infrastructure.debugInfo.element;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.infrastructure.debugInfo.DebugInformation;
import com.simibubi.create.infrastructure.debugInfo.InfoProvider;

public record InfoEntry(String name, InfoProvider provider) implements InfoElement {
	public InfoEntry(String name, String info) {
		this(name, player -> info);
	}

	@Override
	public void write(PlayerEntity player, PacketByteBuf buffer) {
		buffer.writeBoolean(false);
		buffer.writeString(name);
		buffer.writeString(provider.getInfoSafe(player));
	}

	@Override
	public void print(int depth, @Nullable PlayerEntity player, Consumer<String> lineConsumer) {
		String value = provider.getInfoSafe(player);
		String indent = DebugInformation.getIndent(depth);
		if (value.contains("\n")) {
			String[] lines = value.split("\n");
			String firstLine = lines[0];
			String lineStart = name + ": ";
			lineConsumer.accept(indent + lineStart + firstLine);
			String extraIndent = Stream.generate(() -> " ").limit(lineStart.length()).collect(Collectors.joining());

			for (int i = 1; i < lines.length; i++) {
				lineConsumer.accept(indent + extraIndent + lines[i]);
			}
		} else {
			lineConsumer.accept(indent + name + ": " + value);
		}

	}

	public static InfoEntry read(PacketByteBuf buffer) {
		String name = buffer.readString();
		String value = buffer.readString();
		return new InfoEntry(name, value);
	}
}
