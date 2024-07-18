package com.simibubi.create.content.equipment.clipboard;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.networking.SimplePacketBase;

public class ClipboardEditPacket extends SimplePacketBase {

	private int hotbarSlot;
	private NbtCompound data;
	private BlockPos targetedBlock;

	public ClipboardEditPacket(int hotbarSlot, NbtCompound data, @Nullable BlockPos targetedBlock) {
		this.hotbarSlot = hotbarSlot;
		this.data = data;
		this.targetedBlock = targetedBlock;
	}

	public ClipboardEditPacket(PacketByteBuf buffer) {
		hotbarSlot = buffer.readVarInt();
		data = buffer.readNbt();
		if (buffer.readBoolean())
			targetedBlock = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeVarInt(hotbarSlot);
		buffer.writeNbt(data);
		buffer.writeBoolean(targetedBlock != null);
		if (targetedBlock != null)
			buffer.writeBlockPos(targetedBlock);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();

			if (targetedBlock != null) {
				World world = sender.getWorld();
				if (world == null || !world.canSetBlock(targetedBlock))
					return;
				if (!targetedBlock.isWithinDistance(sender.getBlockPos(), 20))
					return;
				if (world.getBlockEntity(targetedBlock) instanceof ClipboardBlockEntity cbe) {
					cbe.dataContainer.setNbt(data.isEmpty() ? null : data);
					cbe.onEditedBy(sender);
				}
				return;
			}

			ItemStack itemStack = sender.getInventory()
				.getStack(hotbarSlot);
			if (!AllBlocks.CLIPBOARD.isIn(itemStack))
				return;
			itemStack.setNbt(data.isEmpty() ? null : data);
		});

		return true;
	}

}
