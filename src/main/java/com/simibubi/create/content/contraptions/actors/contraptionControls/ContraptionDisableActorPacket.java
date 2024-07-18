package com.simibubi.create.content.contraptions.actors.contraptionControls;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.networking.SimplePacketBase;

public class ContraptionDisableActorPacket extends SimplePacketBase {

	private int entityID;
	private ItemStack filter;
	private boolean enable;

	public ContraptionDisableActorPacket(int entityID, ItemStack filter, boolean enable) {
		this.entityID = entityID;
		this.filter = filter;
		this.enable = enable;
	}

	public ContraptionDisableActorPacket(PacketByteBuf buffer) {
		entityID = buffer.readInt();
		enable = buffer.readBoolean();
		filter = buffer.readItemStack();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityID);
		buffer.writeBoolean(enable);
		buffer.writeItemStack(filter);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Entity entityByID = MinecraftClient.getInstance().world.getEntityById(entityID);
			if (!(entityByID instanceof AbstractContraptionEntity ace))
				return;

			Contraption contraption = ace.getContraption();
			List<ItemStack> disabledActors = contraption.getDisabledActors();
			if (filter.isEmpty())
				disabledActors.clear();

			if (!enable) {
				disabledActors.add(filter);
				contraption.setActorsActive(filter, false);
				return;
			}

			for (Iterator<ItemStack> iterator = disabledActors.iterator(); iterator.hasNext();) {
				ItemStack next = iterator.next();
				if (ContraptionControlsMovement.isSameFilter(next, filter) || next.isEmpty())
					iterator.remove();
			}

			contraption.setActorsActive(filter, true);
		});
		return true;
	}

}
