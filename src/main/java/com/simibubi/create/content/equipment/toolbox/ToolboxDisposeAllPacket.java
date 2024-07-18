package com.simibubi.create.content.equipment.toolbox;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ToolboxDisposeAllPacket extends SimplePacketBase {

	private BlockPos toolboxPos;

	public ToolboxDisposeAllPacket(BlockPos toolboxPos) {
		this.toolboxPos = toolboxPos;
	}

	public ToolboxDisposeAllPacket(PacketByteBuf buffer) {
		toolboxPos = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(toolboxPos);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			World world = player.getWorld();
			BlockEntity blockEntity = world.getBlockEntity(toolboxPos);

			double maxRange = ToolboxHandler.getMaxRange(player);
			if (player.squaredDistanceTo(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
				* maxRange)
				return;
			if (!(blockEntity instanceof ToolboxBlockEntity))
				return;
			ToolboxBlockEntity toolbox = (ToolboxBlockEntity) blockEntity;

			NbtCompound compound = player.getCustomData()
				.getCompound("CreateToolboxData");
			MutableBoolean sendData = new MutableBoolean(false);

			toolbox.inventory.inLimitedMode(inventory -> {
				try (Transaction t = TransferUtil.getTransaction()) {
					PlayerInventoryStorage playerInv = PlayerInventoryStorage.of(player);
					for (int i = 0; i < 36; i++) {
						String key = String.valueOf(i);
						if (compound.contains(key) && NbtHelper.toBlockPos(compound.getCompound(key)
										.getCompound("Pos"))
								.equals(toolboxPos)) {
							ToolboxHandler.unequip(player, i, true);
							sendData.setTrue();
						}

						SingleSlotStorage<ItemVariant> slot = playerInv.getSlot(i);
						if (slot.isResourceBlank())
							continue;
						long amount = slot.getAmount();
						ItemVariant resource = slot.getResource();

						long inserted = inventory.insert(resource, amount, t);
						if (inserted == 0)
							continue;
						slot.extract(resource, inserted, t);
					}
					t.commit();
				}
			});

			if (sendData.booleanValue())
				ToolboxHandler.syncData(player);
		});
		return true;
	}

}
