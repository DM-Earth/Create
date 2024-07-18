package com.simibubi.create.content.contraptions.actors.seat;

import java.util.Map;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.util.EntityHelper;

public class SeatMovementBehaviour implements MovementBehaviour {

	@Override
	public void startMoving(MovementContext context) {
		MovementBehaviour.super.startMoving(context);
		int indexOf = context.contraption.getSeats()
			.indexOf(context.localPos);
		context.data.putInt("SeatIndex", indexOf);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		MovementBehaviour.super.visitNewPosition(context, pos);
		AbstractContraptionEntity contraptionEntity = context.contraption.entity;
		if (contraptionEntity == null)
			return;
		int index = context.data.getInt("SeatIndex");
		if (index == -1)
			return;

		Map<UUID, Integer> seatMapping = context.contraption.getSeatMapping();
		BlockState blockState = context.world.getBlockState(pos);
		boolean slab =
			blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.BOTTOM;
		boolean solid = blockState.isOpaque() || slab;

		// Occupied
		if (!seatMapping.containsValue(index))
			return;
		if (!solid)
			return;
		Entity toDismount = null;
		for (Map.Entry<UUID, Integer> entry : seatMapping.entrySet()) {
			if (entry.getValue() != index)
				continue;
			for (Entity entity : contraptionEntity.getPassengerList()) {
				if (!entry.getKey()
					.equals(entity.getUuid()))
					continue;
				toDismount = entity;
			}
		}
		if (toDismount == null)
			return;
		toDismount.stopRiding();
		Vec3d position = VecHelper.getCenterOf(pos)
			.add(0, slab ? .5f : 1f, 0);
		toDismount.requestTeleport(position.x, position.y, position.z);
		toDismount.getCustomData()
			.remove("ContraptionDismountLocation");
	}

}
