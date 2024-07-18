package com.simibubi.create.foundation.block;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import com.simibubi.create.foundation.utility.fabric.ReachUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Vec3d;

public class BigOutlines {

	static BlockHitResult result = null;

	public static void pick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!(mc.cameraEntity instanceof ClientPlayerEntity player))
			return;
		if (mc.world == null)
			return;

		result = null;

		Vec3d origin = player.getCameraPosVec(AnimationTickHolder.getPartialTicks(mc.world));

		double maxRange = mc.crosshairTarget == null ? Double.MAX_VALUE
			: mc.crosshairTarget.getPos()
				.squaredDistanceTo(origin);

		double range = ReachUtil.reach(player);
		Vec3d target = RaycastHelper.getTraceTarget(player, Math.min(maxRange, range) + 1, origin);

		RaycastHelper.rayTraceUntil(origin, target, pos -> {
			Mutable p = BlockPos.ORIGIN.mutableCopy();

			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					p.set(pos.getX() + x, pos.getY(), pos.getZ() + z);
					BlockState blockState = mc.world.getBlockState(p);

					// Could be a dedicated interface for big blocks
					if (!(blockState.getBlock() instanceof TrackBlock)
						&& !(blockState.getBlock() instanceof SlidingDoorBlock))
						continue;

					BlockHitResult hit = blockState.getRaycastShape(mc.world, p)
						.raycast(origin, target, p.toImmutable());
					if (hit == null)
						continue;

					if (result != null && Vec3d.ofCenter(p)
						.squaredDistanceTo(origin) >= Vec3d.ofCenter(result.getBlockPos())
							.squaredDistanceTo(origin))
						continue;

					Vec3d vec = hit.getPos();
					double interactionDist = vec.squaredDistanceTo(origin);
					if (interactionDist >= maxRange)
						continue;

					BlockPos hitPos = hit.getBlockPos();

					// pacifies ServerGamePacketListenerImpl.handleUseItemOn
					vec = vec.subtract(Vec3d.ofCenter(hitPos));
					vec = VecHelper.clampComponentWise(vec, 1);
					vec = vec.add(Vec3d.ofCenter(hitPos));

					result = new BlockHitResult(vec, hit.getSide(), hitPos, hit.isInsideBlock());
				}
			}

			return result != null;
		});

		if (result != null)
			mc.crosshairTarget = result;
	}

	static boolean isValidPos(PlayerEntity player, BlockPos pos) {
		// verify that the server will accept the fake result
		double x = player.getX() - (pos.getX() + .5);
		double y = player.getY() - (pos.getY() + .5) + 1.5;
		double z = player.getZ() - (pos.getZ() + .5);
		double distSqr = x * x + y * y + z * z;
		double maxDist = ReachUtil.reach(player) + 1;
		maxDist *= maxDist;
		return distSqr <= maxDist;
	}

}
