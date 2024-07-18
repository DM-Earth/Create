package com.simibubi.create.content.schematics.packet;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

public class SchematicSyncPacket extends SimplePacketBase {

	public int slot;
	public boolean deployed;
	public BlockPos anchor;
	public BlockRotation rotation;
	public BlockMirror mirror;

	public SchematicSyncPacket(int slot, StructurePlacementData settings,
			BlockPos anchor, boolean deployed) {
		this.slot = slot;
		this.deployed = deployed;
		this.anchor = anchor;
		this.rotation = settings.getRotation();
		this.mirror = settings.getMirror();
	}

	public SchematicSyncPacket(PacketByteBuf buffer) {
		slot = buffer.readVarInt();
		deployed = buffer.readBoolean();
		anchor = buffer.readBlockPos();
		rotation = buffer.readEnumConstant(BlockRotation.class);
		mirror = buffer.readEnumConstant(BlockMirror.class);
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeVarInt(slot);
		buffer.writeBoolean(deployed);
		buffer.writeBlockPos(anchor);
		buffer.writeEnumConstant(rotation);
		buffer.writeEnumConstant(mirror);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;
			ItemStack stack = ItemStack.EMPTY;
			if (slot == -1) {
				stack = player.getMainHandStack();
			} else {
				stack = player.getInventory().getStack(slot);
			}
			if (!AllItems.SCHEMATIC.isIn(stack)) {
				return;
			}
			NbtCompound tag = stack.getOrCreateNbt();
			tag.putBoolean("Deployed", deployed);
			tag.put("Anchor", NbtHelper.fromBlockPos(anchor));
			tag.putString("Rotation", rotation.name());
			tag.putString("Mirror", mirror.name());
			SchematicInstances.clearHash(stack);
		});
		return true;
	}

}
