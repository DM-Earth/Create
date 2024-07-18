package com.simibubi.create.content.redstone.link.controller;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class LinkedControllerStopLecternPacket extends LinkedControllerPacketBase {

	public LinkedControllerStopLecternPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	public LinkedControllerStopLecternPacket(BlockPos lecternPos) {
		super(lecternPos);
	}

	@Override
	protected void handleLectern(ServerPlayerEntity player, LecternControllerBlockEntity lectern) {
		lectern.tryStopUsing(player);
	}

	@Override
	protected void handleItem(ServerPlayerEntity player, ItemStack heldItem) { }

}
