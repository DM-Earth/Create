package com.simibubi.create.content.equipment.bell;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.utility.IntAttached;
import com.simibubi.create.foundation.utility.LongAttached;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HauntedBellPulser {

	public static final int DISTANCE = 3;
	public static final int RECHARGE_TICKS = 8;
	public static final int WARMUP_TICKS = 10;

	public static final Cache<UUID, IntAttached<Entity>> WARMUP = CacheBuilder.newBuilder()
		.expireAfterAccess(250, TimeUnit.MILLISECONDS)
		.build();

	public static void hauntedBellCreatesPulse(ServerWorld world) {
//		if (event.phase != TickEvent.Phase.END)
//			return;
//		if (event.side != LogicalSide.SERVER)
//			return;
		PlayerLookup.world(world).forEach(player -> {
			if (player.isSpectator())
				return;
			if (!player.isHolding(AllBlocks.HAUNTED_BELL::isIn))
				return;

//		Entity player = event.player;
			boolean firstPulse = false;

			try {
				IntAttached<Entity> ticker = WARMUP.get(player.getUuid(), () -> IntAttached.with(WARMUP_TICKS, player));
				firstPulse = ticker.getFirst()
						.intValue() == 1;
				ticker.decrement();
				if (!ticker.isOrBelowZero())
					return;
			} catch (ExecutionException e) {
			}

			long gameTime = player.getWorld().getTime();
			if (firstPulse || gameTime % RECHARGE_TICKS != 0)
				sendPulse(player.getWorld(), player.getBlockPos(), DISTANCE, false);
		});
	}

	public static void sendPulse(World world, BlockPos pos, int distance, boolean canOverlap) {
//		LevelChunk chunk = world.getChunkAt(pos);
		AllPackets.getChannel().sendToClientsTracking(new SoulPulseEffectPacket(pos, distance, canOverlap), (ServerWorld) world, pos);
	}

}
