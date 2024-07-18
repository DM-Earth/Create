package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public abstract class ConfigureZapperPacket extends SimplePacketBase {

	protected Hand hand;
	protected PlacementPatterns pattern;

	public ConfigureZapperPacket(Hand hand, PlacementPatterns pattern) {
		this.hand = hand;
		this.pattern = pattern;
	}

	public ConfigureZapperPacket(PacketByteBuf buffer) {
		hand = buffer.readEnumConstant(Hand.class);
		pattern = buffer.readEnumConstant(PlacementPatterns.class);
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeEnumConstant(hand);
		buffer.writeEnumConstant(pattern);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getStackInHand(hand);
			if (stack.getItem() instanceof ZapperItem) {
				configureZapper(stack);
			}
		});
		return true;
	}

	public abstract void configureZapper(ItemStack stack);

}
