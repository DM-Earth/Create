package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CurvedTrackDestroyPacket extends BlockEntityConfigurationPacket<TrackBlockEntity> {

	private BlockPos targetPos;
	private BlockPos soundSource;
	private boolean wrench;

	public CurvedTrackDestroyPacket(BlockPos pos, BlockPos targetPos, BlockPos soundSource, boolean wrench) {
		super(pos);
		this.targetPos = targetPos;
		this.soundSource = soundSource;
		this.wrench = wrench;
	}

	public CurvedTrackDestroyPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeBlockPos(targetPos);
		buffer.writeBlockPos(soundSource);
		buffer.writeBoolean(wrench);
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		targetPos = buffer.readBlockPos();
		soundSource = buffer.readBlockPos();
		wrench = buffer.readBoolean();
	}

	@Override
	protected void applySettings(ServerPlayerEntity player, TrackBlockEntity be) {
		int verifyDistance = AllConfigs.server().trains.maxTrackPlacementLength.get() * 4;
		if (!be.getPos()
			.isWithinDistance(player.getBlockPos(), verifyDistance)) {
			Create.LOGGER.warn(player.getEntityName() + " too far away from destroyed Curve track");
			return;
		}

		World level = be.getWorld();
		BezierConnection bezierConnection = be.getConnections()
			.get(targetPos);

		be.removeConnection(targetPos);
		if (level.getBlockEntity(targetPos)instanceof TrackBlockEntity other)
			other.removeConnection(pos);

		BlockState blockState = be.getCachedState();
		TrackPropagator.onRailRemoved(level, pos, blockState);

		if (wrench) {
			AllSoundEvents.WRENCH_REMOVE.playOnServer(player.getWorld(), soundSource, 1,
				Create.RANDOM.nextFloat() * .5f + .5f);
			if (!player.isCreative() && bezierConnection != null)
				bezierConnection.addItemsToPlayer(player);
		} else if (!player.isCreative() && bezierConnection != null)
			bezierConnection.spawnItems(level);

		bezierConnection.spawnDestroyParticles(level);
		BlockSoundGroup soundtype = blockState.getSoundGroup();
		if (soundtype == null)
			return;

		level.playSound(null, soundSource, soundtype.getBreakSound(), SoundCategory.BLOCKS,
			(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
	}

	@Override
	protected int maxRange() {
		return 64;
	}

	@Override
	protected void applySettings(TrackBlockEntity be) {}

}
