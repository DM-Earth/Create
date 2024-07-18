package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.content.logistics.filter.AttributeFilterMenu.WhitelistMode;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class FilterScreenPacket extends SimplePacketBase {

	public enum Option {
		WHITELIST, WHITELIST2, BLACKLIST, RESPECT_DATA, IGNORE_DATA, UPDATE_FILTER_ITEM, ADD_TAG, ADD_INVERTED_TAG;
	}

	private final Option option;
	private final NbtCompound data;

	public FilterScreenPacket(Option option) {
		this(option, new NbtCompound());
	}

	public FilterScreenPacket(Option option, NbtCompound data) {
		this.option = option;
		this.data = data;
	}

	public FilterScreenPacket(PacketByteBuf buffer) {
		option = Option.values()[buffer.readInt()];
		data = buffer.readNbt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(option.ordinal());
		buffer.writeNbt(data);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;

			if (player.currentScreenHandler instanceof FilterMenu) {
				FilterMenu c = (FilterMenu) player.currentScreenHandler;
				if (option == Option.WHITELIST)
					c.blacklist = false;
				if (option == Option.BLACKLIST)
					c.blacklist = true;
				if (option == Option.RESPECT_DATA)
					c.respectNBT = true;
				if (option == Option.IGNORE_DATA)
					c.respectNBT = false;
				if (option == Option.UPDATE_FILTER_ITEM)
					c.ghostInventory.setStackInSlot(
							data.getInt("Slot"),
							net.minecraft.item.ItemStack.fromNbt(data.getCompound("Item")));
			}

			if (player.currentScreenHandler instanceof AttributeFilterMenu) {
				AttributeFilterMenu c = (AttributeFilterMenu) player.currentScreenHandler;
				if (option == Option.WHITELIST)
					c.whitelistMode = WhitelistMode.WHITELIST_DISJ;
				if (option == Option.WHITELIST2)
					c.whitelistMode = WhitelistMode.WHITELIST_CONJ;
				if (option == Option.BLACKLIST)
					c.whitelistMode = WhitelistMode.BLACKLIST;
				if (option == Option.ADD_TAG)
					c.appendSelectedAttribute(ItemAttribute.fromNBT(data), false);
				if (option == Option.ADD_INVERTED_TAG)
					c.appendSelectedAttribute(ItemAttribute.fromNBT(data), true);
			}

		});
		return true;
	}

}
