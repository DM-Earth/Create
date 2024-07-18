package com.simibubi.create.content.trains.entity;

import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class TrainPromptPacket extends SimplePacketBase {

	private Text text;
	private boolean shadow;

	public TrainPromptPacket(Text text, boolean shadow) {
		this.text = text;
		this.shadow = shadow;
	}

	public TrainPromptPacket(PacketByteBuf buffer) {
		text = buffer.readText();
		shadow = buffer.readBoolean();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeText(text);
		buffer.writeBoolean(shadow);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::apply));
		return true;
	}

	@Environment(EnvType.CLIENT)
	public void apply() {
		TrainHUD.currentPrompt = text;
		TrainHUD.currentPromptShadow = shadow;
		TrainHUD.promptKeepAlive = 30;
	}

}
