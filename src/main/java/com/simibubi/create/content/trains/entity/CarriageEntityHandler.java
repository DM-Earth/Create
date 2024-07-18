package com.simibubi.create.content.trains.entity;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

/**
 * Removes all Carriage entities in chunks that aren't ticking
 */
public class CarriageEntityHandler {

	public static void onEntityEnterSection(Entity entity, long packedOldPos, long packedNewPos) {
		if (!(ChunkSectionPos.unpackX(packedOldPos) != ChunkSectionPos.unpackX(packedNewPos) || ChunkSectionPos.unpackZ(packedOldPos) != ChunkSectionPos.unpackZ(packedNewPos)))
			return;
		if (!(entity instanceof CarriageContraptionEntity cce))
			return;
		ChunkSectionPos newPos = ChunkSectionPos.from(packedNewPos);
		World level = entity.getWorld();
		if (level.isClient)
			return;
		if (!isActiveChunk(level, newPos.getCenterPos()))
			cce.leftTickingChunks = true;
	}

	public static void validateCarriageEntity(CarriageContraptionEntity entity) {
		if (!entity.isAlive())
			return;
		World level = entity.getWorld();
		if (level.isClient)
			return;
		if (!isActiveChunk(level, entity.getBlockPos()))
			entity.leftTickingChunks = true;
	}

	public static boolean isActiveChunk(World level, BlockPos pos) {
		if (level instanceof ServerWorld serverLevel)
			return serverLevel.shouldTickEntity(pos);
		return false;
	}

}
