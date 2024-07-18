package com.simibubi.create.content.contraptions;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.mixin.accessor.NbtAccounterAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;

public class ContraptionData {
	/**
	 * A sane, default maximum for contraption data size.
	 */
	public static final int DEFAULT_LIMIT = 2_000_000;
	/**
	 * Connectivity expands the NBT packet limit to 2 GB.
	 */
	public static final int CONNECTIVITY_LIMIT = Integer.MAX_VALUE;
	/**
	 * Packet Fixer expands the NBT packet limit to 200 MB.
	 */
	public static final int PACKET_FIXER_LIMIT = 209_715_200;
	/**
	 * XL Packets expands the NBT packet limit to 2 GB.
	 */
	public static final int XL_PACKETS_LIMIT = 2_000_000_000;
	/**
	 * Minecart item sizes are limited by the vanilla slot change packet ({@link ScreenHandlerSlotUpdateS2CPacket}).
	 * {@link #DEFAULT_LIMIT} is used as the default.
	 * Connectivity, PacketFixer, and XL Packets expand the size limit.
	 * If one of these mods is loaded, we take advantage of it and use the higher limit.
	 */
	public static final int PICKUP_LIMIT;

	static {
		int limit = DEFAULT_LIMIT;

		// Check from largest to smallest to use the smallest limit if multiple mods are loaded.
		// It is necessary to use the smallest limit because even if multiple mods are loaded,
		// not all of their mixins may be applied. Therefore, it is safest to only assume that
		// the mod with the smallest limit is actually active.
		if (Mods.CONNECTIVITY.isLoaded()) {
			limit = CONNECTIVITY_LIMIT;
		}
		if (Mods.XLPACKETS.isLoaded()) {
			limit = XL_PACKETS_LIMIT;
		}
		if (Mods.PACKETFIXER.isLoaded()) {
			limit = PACKET_FIXER_LIMIT;
		}

		PICKUP_LIMIT = limit;
	}

	/**
	 * @return true if the given NBT is too large for a contraption to be synced to clients.
	 */
	public static boolean isTooLargeForSync(NbtCompound data) {
		int max = AllConfigs.server().kinetics.maxDataSize.get();
		return max != 0 && packetSize(data) > max;
	}

	/**
	 * @return true if the given NBT is too large for a contraption to be picked up with a wrench.
	 */
	public static boolean isTooLargeForPickup(NbtCompound data) {
		return packetSize(data) > PICKUP_LIMIT;
	}

	/**
	 * @return the size of the given NBT when put through a packet, in bytes.
	 */
	public static long packetSize(NbtCompound data) {
		PacketByteBuf test = new PacketByteBuf(Unpooled.buffer());
		test.writeNbt(data);
		NbtTagSizeTracker sizeTracker = new NbtTagSizeTracker(Long.MAX_VALUE);
		test.readNbt(sizeTracker);
		long size = ((NbtAccounterAccessor) sizeTracker).create$getAllocatedBytes();
		test.release();
		return size;
	}
}
