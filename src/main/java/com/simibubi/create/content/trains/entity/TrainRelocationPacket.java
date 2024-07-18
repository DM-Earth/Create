package com.simibubi.create.content.trains.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.ContraptionRelocationPacket;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class TrainRelocationPacket extends SimplePacketBase {

	UUID trainId;
	BlockPos pos;
	Vec3d lookAngle;
	int entityId;
	private boolean direction;
	private BezierTrackPointLocation hoveredBezier;

	public TrainRelocationPacket(PacketByteBuf buffer) {
		trainId = buffer.readUuid();
		pos = buffer.readBlockPos();
		lookAngle = VecHelper.read(buffer);
		entityId = buffer.readInt();
		direction = buffer.readBoolean();
		if (buffer.readBoolean())
			hoveredBezier = new BezierTrackPointLocation(buffer.readBlockPos(), buffer.readInt());
	}

	public TrainRelocationPacket(UUID trainId, BlockPos pos, BezierTrackPointLocation hoveredBezier, boolean direction,
		Vec3d lookAngle, int entityId) {
		this.trainId = trainId;
		this.pos = pos;
		this.hoveredBezier = hoveredBezier;
		this.direction = direction;
		this.lookAngle = lookAngle;
		this.entityId = entityId;
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeUuid(trainId);
		buffer.writeBlockPos(pos);
		VecHelper.write(lookAngle, buffer);
		buffer.writeInt(entityId);
		buffer.writeBoolean(direction);
		buffer.writeBoolean(hoveredBezier != null);
		if (hoveredBezier != null) {
			buffer.writeBlockPos(hoveredBezier.curveTarget());
			buffer.writeInt(hoveredBezier.segment());
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			Train train = Create.RAILWAYS.trains.get(trainId);
			Entity entity = sender.getWorld().getEntityById(entityId);

			String messagePrefix = sender.getName()
				.getString() + " could not relocate Train ";

			if (train == null || !(entity instanceof CarriageContraptionEntity cce)) {
				Create.LOGGER.warn(messagePrefix + train.id.toString()
					.substring(0, 5) + ": not present on server");
				return;
			}

			if (!train.id.equals(cce.trainId))
				return;

			int verifyDistance = AllConfigs.server().trains.maxTrackPlacementLength.get() * 2;
			if (!sender.getPos()
				.isInRange(Vec3d.ofCenter(pos), verifyDistance)) {
				Create.LOGGER.warn(messagePrefix + train.name.getString() + ": player too far from clicked pos");
				return;
			}
			if (!sender.getPos()
				.isInRange(cce.getPos(), verifyDistance + cce.getBoundingBox()
					.getXLength() / 2)) {
				Create.LOGGER.warn(messagePrefix + train.name.getString() + ": player too far from carriage entity");
				return;
			}

			if (TrainRelocator.relocate(train, sender.getWorld(), pos, hoveredBezier, direction, lookAngle, false)) {
				sender.sendMessage(Lang.translateDirect("train.relocate.success")
					.formatted(Formatting.GREEN), true);
				train.carriages.forEach(c -> c.forEachPresentEntity(e -> {
					e.nonDamageTicks = 10;
					AllPackets.getChannel().sendToClientsTracking(new ContraptionRelocationPacket(e.getId()), e);
				}));
				return;
			}

			Create.LOGGER.warn(messagePrefix + train.name.getString() + ": relocation failed server-side");
		});
		return true;
	}

}
