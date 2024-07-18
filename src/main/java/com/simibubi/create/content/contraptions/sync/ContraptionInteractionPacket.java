package com.simibubi.create.content.contraptions.sync;

import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

public class ContraptionInteractionPacket extends SimplePacketBase {

	private Hand interactionHand;
	private int target;
	private BlockPos localPos;
	private Direction face;

	public ContraptionInteractionPacket(AbstractContraptionEntity target, Hand hand, BlockPos localPos, Direction side) {
		this.interactionHand = hand;
		this.localPos = localPos;
		this.target = target.getId();
		this.face = side;
	}

	public ContraptionInteractionPacket(PacketByteBuf buffer) {
		target = buffer.readInt();
		int handId = buffer.readInt();
		interactionHand = handId == -1 ? null : Hand.values()[handId];
		localPos = buffer.readBlockPos();
		face = Direction.byId(buffer.readShort());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(target);
		buffer.writeInt(interactionHand == null ? -1 : interactionHand.ordinal());
		buffer.writeBlockPos(localPos);
		buffer.writeShort(face.getId());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			if (sender == null)
				return;
			Entity entityByID = sender.getWorld().getEntityById(target);
			if (!(entityByID instanceof AbstractContraptionEntity))
				return;
			AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) entityByID;
			Box bb = contraptionEntity.getBoundingBox();
			double boundsExtra = Math.max(bb.getXLength(), bb.getYLength());
			double d = ReachUtil.reach(sender) + 10 + boundsExtra;
			if (!sender.canSee(entityByID))
				d -= 3;
			d *= d;
			if (sender.squaredDistanceTo(entityByID) > d)
				return;
			if (contraptionEntity.handlePlayerInteraction(sender, localPos, face, interactionHand))
				sender.swingHand(interactionHand, true);
		});
		return true;
	}

}
