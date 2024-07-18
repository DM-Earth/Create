package com.simibubi.create.content.equipment.extendoGrip;

import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

public class ExtendoGripInteractionPacket extends SimplePacketBase {

	private Hand interactionHand;
	private int target;
	private Vec3d specificPoint;

	public ExtendoGripInteractionPacket(Entity target) {
		this(target, null);
	}

	public ExtendoGripInteractionPacket(Entity target, Hand hand) {
		this(target, hand, null);
	}

	public ExtendoGripInteractionPacket(Entity target, Hand hand, Vec3d specificPoint) {
		interactionHand = hand;
		this.specificPoint = specificPoint;
		this.target = target.getId();
	}

	public ExtendoGripInteractionPacket(PacketByteBuf buffer) {
		target = buffer.readInt();
		int handId = buffer.readInt();
		interactionHand = handId == -1 ? null : Hand.values()[handId];
		if (buffer.readBoolean())
			specificPoint = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(target);
		buffer.writeInt(interactionHand == null ? -1 : interactionHand.ordinal());
		buffer.writeBoolean(specificPoint != null);
		if (specificPoint != null) {
			buffer.writeDouble(specificPoint.x);
			buffer.writeDouble(specificPoint.y);
			buffer.writeDouble(specificPoint.z);
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			if (sender == null)
				return;
			Entity entityByID = sender.getWorld()
				.getEntityById(target);
			if (entityByID != null && ExtendoGripItem.isHoldingExtendoGrip(sender)) {
				double d = ReachUtil.reach(sender);
				if (!sender.canSee(entityByID))
					d -= 3;
				d *= d;
				if (sender.squaredDistanceTo(entityByID) > d)
					return;
				if (interactionHand == null)
					sender.attack(entityByID);
				else if (specificPoint == null)
					sender.interact(entityByID, interactionHand);
				else
					entityByID.interactAt(sender, specificPoint, interactionHand);
			}
		});
		return true;
	}

}
