package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public class ConfigureSymmetryWandPacket extends SimplePacketBase {

	protected Hand hand;
	protected SymmetryMirror mirror;

	public ConfigureSymmetryWandPacket(Hand hand, SymmetryMirror mirror) {
		this.hand = hand;
		this.mirror = mirror;
	}

	public ConfigureSymmetryWandPacket(PacketByteBuf buffer) {
		hand = buffer.readEnumConstant(Hand.class);
		mirror = SymmetryMirror.fromNBT(buffer.readNbt());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeEnumConstant(hand);
		buffer.writeNbt(mirror.writeToNbt());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getStackInHand(hand);
			if (stack.getItem() instanceof SymmetryWandItem) {
				SymmetryWandItem.configureSettings(stack, mirror);
			}
		});
		return true;
	}

}
