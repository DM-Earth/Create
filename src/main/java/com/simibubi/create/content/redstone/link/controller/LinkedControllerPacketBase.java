package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public abstract class LinkedControllerPacketBase extends SimplePacketBase {

	private BlockPos lecternPos;

	public LinkedControllerPacketBase(BlockPos lecternPos) {
		this.lecternPos = lecternPos;
	}

	public LinkedControllerPacketBase(PacketByteBuf buffer) {
		if (buffer.readBoolean()) {
			lecternPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
		}
	}

	protected boolean inLectern() {
		return lecternPos != null;
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBoolean(inLectern());
		if (inLectern()) {
			buffer.writeInt(lecternPos.getX());
			buffer.writeInt(lecternPos.getY());
			buffer.writeInt(lecternPos.getZ());
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;

			if (inLectern()) {
				BlockEntity be = player.getWorld().getBlockEntity(lecternPos);
				if (!(be instanceof LecternControllerBlockEntity))
					return;
				handleLectern(player, (LecternControllerBlockEntity) be);
			} else {
				ItemStack controller = player.getMainHandStack();
				if (!AllItems.LINKED_CONTROLLER.isIn(controller)) {
					controller = player.getOffHandStack();
					if (!AllItems.LINKED_CONTROLLER.isIn(controller))
						return;
				}
				handleItem(player, controller);
			}
		});
		return true;
	}

	protected abstract void handleItem(ServerPlayerEntity player, ItemStack heldItem);
	protected abstract void handleLectern(ServerPlayerEntity player, LecternControllerBlockEntity lectern);

}
