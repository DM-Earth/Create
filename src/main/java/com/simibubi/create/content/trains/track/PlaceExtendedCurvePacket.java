package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public class PlaceExtendedCurvePacket extends SimplePacketBase {

	boolean mainHand;
	boolean ctrlDown;

	public PlaceExtendedCurvePacket(boolean mainHand, boolean ctrlDown) {
		this.mainHand = mainHand;
		this.ctrlDown = ctrlDown;
	}

	public PlaceExtendedCurvePacket(PacketByteBuf buffer) {
		mainHand = buffer.readBoolean();
		ctrlDown = buffer.readBoolean();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBoolean(mainHand);
		buffer.writeBoolean(ctrlDown);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			ItemStack stack = sender.getStackInHand(mainHand ? Hand.MAIN_HAND : Hand.OFF_HAND);
			if (!AllTags.AllBlockTags.TRACKS.matches(stack) || !stack.hasNbt())
				return;
			NbtCompound tag = stack.getNbt();
			tag.putBoolean("ExtendCurve", true);
			stack.setNbt(tag);
		});
		return true;
	}

}
