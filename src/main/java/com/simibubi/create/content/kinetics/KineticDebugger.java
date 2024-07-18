package com.simibubi.create.content.kinetics;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class KineticDebugger {

	public static void tick() {
		if (!isActive()) {
			if (KineticBlockEntityRenderer.rainbowMode) {
				KineticBlockEntityRenderer.rainbowMode = false;
				CreateClient.BUFFER_CACHE.invalidate();
			}
			return;
		}

		KineticBlockEntity be = getSelectedBE();
		if (be == null)
			return;

		World world = MinecraftClient.getInstance().world;
		BlockPos toOutline = be.hasSource() ? be.source : be.getPos();
		BlockState state = be.getCachedState();
		VoxelShape shape = world.getBlockState(toOutline)
			.getSidesShape(world, toOutline);

		if (be.getTheoreticalSpeed() != 0 && !shape.isEmpty())
			CreateClient.OUTLINER.chaseAABB("kineticSource", shape.getBoundingBox()
					.offset(toOutline))
					.lineWidth(1 / 16f)
					.colored(be.hasSource() ? Color.generateFromLong(be.network).getRGB() : 0xffcc00);

		if (state.getBlock() instanceof IRotate) {
			Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
			Vec3d vec = Vec3d.of(Direction.get(AxisDirection.POSITIVE, axis)
					.getVector());
			Vec3d center = VecHelper.getCenterOf(be.getPos());
			CreateClient.OUTLINER.showLine("rotationAxis", center.add(vec), center.subtract(vec))
					.lineWidth(1 / 16f);
		}

	}

	public static boolean isActive() {
		return isF3DebugModeActive() && AllConfigs.client().rainbowDebug.get();
	}

	public static boolean isF3DebugModeActive() {
		return MinecraftClient.getInstance().options.debugEnabled;
	}

	public static KineticBlockEntity getSelectedBE() {
		HitResult obj = MinecraftClient.getInstance().crosshairTarget;
		ClientWorld world = MinecraftClient.getInstance().world;
		if (obj == null)
			return null;
		if (world == null)
			return null;
		if (!(obj instanceof BlockHitResult))
			return null;

		BlockHitResult ray = (BlockHitResult) obj;
		BlockEntity be = world.getBlockEntity(ray.getBlockPos());
		if (!(be instanceof KineticBlockEntity))
			return null;

		return (KineticBlockEntity) be;
	}

}
