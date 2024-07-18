package com.simibubi.create.content.redstone.link.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LinkedControllerInputPacket extends LinkedControllerPacketBase {

	private Collection<Integer> activatedButtons;
	private boolean press;

	public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
		this(activatedButtons, press, null);
	}

	public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
		super(lecternPos);
		this.activatedButtons = activatedButtons;
		this.press = press;
	}

	public LinkedControllerInputPacket(PacketByteBuf buffer) {
		super(buffer);
		activatedButtons = new ArrayList<>();
		press = buffer.readBoolean();
		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			activatedButtons.add(buffer.readVarInt());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		super.write(buffer);
		buffer.writeBoolean(press);
		buffer.writeVarInt(activatedButtons.size());
		activatedButtons.forEach(buffer::writeVarInt);
	}

	@Override
	protected void handleLectern(ServerPlayerEntity player, LecternControllerBlockEntity lectern) {
		if (lectern.isUsedBy(player))
			handleItem(player, lectern.getController());
	}

	@Override
	protected void handleItem(ServerPlayerEntity player, ItemStack heldItem) {
		World world = player.getEntityWorld();
		UUID uniqueID = player.getUuid();
		BlockPos pos = player.getBlockPos();

		if (player.isSpectator() && press)
			return;
		
		LinkedControllerServerHandler.receivePressed(world, pos, uniqueID, activatedButtons.stream()
			.map(i -> LinkedControllerItem.toFrequency(heldItem, i))
			.collect(Collectors.toList()), press);
	}

}
