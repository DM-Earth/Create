package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GhostItemSubmitPacket extends SimplePacketBase {

	private final ItemStack item;
	private final int slot;

	public GhostItemSubmitPacket(ItemStack item, int slot) {
		this.item = item;
		this.slot = slot;
	}

	public GhostItemSubmitPacket(PacketByteBuf buffer) {
		item = buffer.readItemStack();
		slot = buffer.readInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeItemStack(item);
		buffer.writeInt(slot);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;

			if (player.currentScreenHandler instanceof GhostItemMenu<?> menu) {
				menu.ghostInventory.setStackInSlot(slot, item);
				menu.getSlot(36 + slot).markDirty();
			}
		});
		return true;
	}

}
