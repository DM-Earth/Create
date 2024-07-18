package com.simibubi.create.foundation.utility.worldWrappers;

import javax.annotation.Nullable;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

public class DummyStatusListener implements WorldGenerationProgressListener {

	@Override
	public void start(ChunkPos pCenter) {}

	@Override
	public void setChunkStatus(ChunkPos pChunkPosition, @Nullable ChunkStatus pNewStatus) {}

	@Override
	public void start() {}

	@Override
	public void stop() {}

}
