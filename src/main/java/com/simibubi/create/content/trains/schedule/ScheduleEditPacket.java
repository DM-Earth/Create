package com.simibubi.create.content.trains.schedule;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class ScheduleEditPacket extends SimplePacketBase {

	private Schedule schedule;

	public ScheduleEditPacket(Schedule schedule) {
		this.schedule = schedule;
	}

	public ScheduleEditPacket(PacketByteBuf buffer) {
		schedule = Schedule.fromTag(buffer.readNbt());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeNbt(schedule.write());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			ItemStack mainHandItem = sender.getMainHandStack();
			if (!AllItems.SCHEDULE.isIn(mainHandItem))
				return;
			
			NbtCompound tag = mainHandItem.getOrCreateNbt();
			if (schedule.entries.isEmpty()) {
				tag.remove("Schedule");
				if (tag.isEmpty())
					mainHandItem.setNbt(null);
			} else
				tag.put("Schedule", schedule.write());
			
			sender.getItemCooldownManager()
				.set(mainHandItem.getItem(), 5);
		});
		return true;
	}

}
