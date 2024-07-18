package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ToolboxEquipPacket extends SimplePacketBase {

	private BlockPos toolboxPos;
	private int slot;
	private int hotbarSlot;

	public ToolboxEquipPacket(BlockPos toolboxPos, int slot, int hotbarSlot) {
		this.toolboxPos = toolboxPos;
		this.slot = slot;
		this.hotbarSlot = hotbarSlot;
	}

	public ToolboxEquipPacket(PacketByteBuf buffer) {
		if (buffer.readBoolean())
			toolboxPos = buffer.readBlockPos();
		slot = buffer.readVarInt();
		hotbarSlot = buffer.readVarInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBoolean(toolboxPos != null);
		if (toolboxPos != null)
			buffer.writeBlockPos(toolboxPos);
		buffer.writeVarInt(slot);
		buffer.writeVarInt(hotbarSlot);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			World world = player.getWorld();

			if (toolboxPos == null) {
				ToolboxHandler.unequip(player, hotbarSlot, false);
				ToolboxHandler.syncData(player);
				return;
			}

			BlockEntity blockEntity = world.getBlockEntity(toolboxPos);

			double maxRange = ToolboxHandler.getMaxRange(player);
			if (player.squaredDistanceTo(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
				* maxRange)
				return;
			if (!(blockEntity instanceof ToolboxBlockEntity))
				return;

			ToolboxHandler.unequip(player, hotbarSlot, false);

			if (slot < 0 || slot >= 8) {
				ToolboxHandler.syncData(player);
				return;
			}

			ToolboxBlockEntity toolboxBlockEntity = (ToolboxBlockEntity) blockEntity;

			ItemStack playerStack = player.getInventory().getStack(hotbarSlot);
			if (!playerStack.isEmpty() && !ToolboxInventory.canItemsShareCompartment(playerStack,
				toolboxBlockEntity.inventory.filters.get(slot))) {
				toolboxBlockEntity.inventory.inLimitedMode(inventory -> {
					try (Transaction t = TransferUtil.getTransaction()) {
						ItemVariant stack = ItemVariant.of(playerStack);
						long count = playerStack.getCount();
						long inserted = inventory.insert(stack, count, t);
						if (inserted != count)
							inserted += TransferUtil.insertToMainInv(player, stack, count - inserted);
						long remainder = count - inserted;
						if (remainder != count) {
							t.commit();
							ItemStack newStack = player.getStackReference(hotbarSlot).get().copy();
							newStack.setCount((int) remainder);
							player.getInventory().setStack(hotbarSlot, newStack);
						}
					}
				});
			}

			NbtCompound compound = player.getCustomData()
				.getCompound("CreateToolboxData");
			String key = String.valueOf(hotbarSlot);

			NbtCompound data = new NbtCompound();
			data.putInt("Slot", slot);
			data.put("Pos", NbtHelper.fromBlockPos(toolboxPos));
			compound.put(key, data);

			player.getCustomData()
				.put("CreateToolboxData", compound);

			toolboxBlockEntity.connectPlayer(slot, player, hotbarSlot);
			ToolboxHandler.syncData(player);
		});
		return true;
	}

}
