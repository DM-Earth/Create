package com.simibubi.create.content.trains.track;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.GameMode;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.WorldAttached;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

public class TrackBlockOutline {

	public static WorldAttached<Map<BlockPos, TrackBlockEntity>> TRACKS_WITH_TURNS =
		new WorldAttached<>(w -> new HashMap<>());

	public static BezierPointSelection result;

	public static void pickCurves() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!(mc.cameraEntity instanceof ClientPlayerEntity player))
			return;
		if (mc.world == null)
			return;

		Vec3d origin = player.getCameraPosVec(AnimationTickHolder.getPartialTicks(mc.world));

		double maxRange = mc.crosshairTarget == null ? Double.MAX_VALUE
			: mc.crosshairTarget.getPos()
				.squaredDistanceTo(origin);

		result = null;

		double range = ReachUtil.reach(mc.player);
		Vec3d target = RaycastHelper.getTraceTarget(player, Math.min(maxRange, range) + 1, origin);
		Map<BlockPos, TrackBlockEntity> turns = TRACKS_WITH_TURNS.get(mc.world);

		for (TrackBlockEntity be : turns.values()) {
			for (BezierConnection bc : be.connections.values()) {
				if (!bc.isPrimary())
					continue;

				Box bounds = bc.getBounds();
				if (!bounds.contains(origin) && bounds.raycast(origin, target)
					.isEmpty())
					continue;

				float[] stepLUT = bc.getStepLUT();
				int segments = (int) (bc.getLength() * 2);
				Box segmentBounds = AllShapes.TRACK_ORTHO.get(Direction.SOUTH)
					.getBoundingBox();
				segmentBounds = segmentBounds.offset(-.5, segmentBounds.getYLength() / -2, -.5);

				int bestSegment = -1;
				double bestDistance = Double.MAX_VALUE;
				double newMaxRange = maxRange;

				for (int i = 0; i < stepLUT.length - 2; i++) {
					float t = stepLUT[i] * i / segments;
					float t1 = stepLUT[i + 1] * (i + 1) / segments;
					float t2 = stepLUT[i + 2] * (i + 2) / segments;

					Vec3d v1 = bc.getPosition(t);
					Vec3d v2 = bc.getPosition(t2);
					Vec3d diff = v2.subtract(v1);
					Vec3d angles = TrackRenderer.getModelAngles(bc.getNormal(t1), diff);

					Vec3d anchor = v1.add(diff.multiply(.5));
					Vec3d localOrigin = origin.subtract(anchor);
					Vec3d localDirection = target.subtract(origin);
					localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.x), Axis.X);
					localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.y), Axis.Y);
					localDirection = VecHelper.rotate(localDirection, AngleHelper.deg(-angles.x), Axis.X);
					localDirection = VecHelper.rotate(localDirection, AngleHelper.deg(-angles.y), Axis.Y);

					Optional<Vec3d> clip = segmentBounds.raycast(localOrigin, localOrigin.add(localDirection));
					if (clip.isEmpty())
						continue;

					if (bestSegment != -1 && bestDistance < clip.get()
						.squaredDistanceTo(0, 0.25f, 0))
						continue;

					double distanceToSqr = clip.get()
						.squaredDistanceTo(localOrigin);
					if (distanceToSqr > maxRange)
						continue;

					bestSegment = i;
					newMaxRange = distanceToSqr;
					bestDistance = clip.get()
						.squaredDistanceTo(0, 0.25f, 0);

					BezierTrackPointLocation location = new BezierTrackPointLocation(bc.getKey(), i);
					result = new BezierPointSelection(be, location, anchor, angles, diff.normalize());
				}

				if (bestSegment != -1)
					maxRange = newMaxRange;
			}
		}

		if (result == null)
			return;

		if (mc.crosshairTarget != null && mc.crosshairTarget.getType() != Type.MISS) {
			Vec3d priorLoc = mc.crosshairTarget.getPos();
			mc.crosshairTarget = BlockHitResult.createMissed(priorLoc, Direction.UP, BlockPos.ofFloored(priorLoc));
		}
	}

	public static void drawCurveSelection(MatrixStack ms, VertexConsumerProvider buffer, Vec3d camera) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;

		BezierPointSelection result = TrackBlockOutline.result;
		if (result == null)
			return;

		VertexConsumer vb = buffer.getBuffer(RenderLayer.getLines());
		Vec3d vec = result.vec()
			.subtract(camera);
		Vec3d angles = result.angles();
		TransformStack.cast(ms)
			.pushPose()
			.translate(vec.x, vec.y + .125f, vec.z)
			.rotateYRadians(angles.y)
			.rotateXRadians(angles.x)
			.translate(-.5, -.125f, -.5);

		boolean holdingTrack = AllTags.AllBlockTags.TRACKS.matches(MinecraftClient.getInstance().player.getMainHandStack());
		renderShape(AllShapes.TRACK_ORTHO.get(Direction.SOUTH), ms, vb, holdingTrack ? false : null);
		ms.pop();
	}

	public static boolean drawCustomBlockSelection(WorldRenderer context, Camera info, HitResult hitResult, float partialTicks, MatrixStack ms, VertexConsumerProvider buffers) {
		if (!(hitResult instanceof BlockHitResult))
			return false;
		MinecraftClient mc = MinecraftClient.getInstance();
		BlockHitResult target = (BlockHitResult) hitResult;
		BlockPos pos = target.getBlockPos();
		BlockState blockstate = mc.world.getBlockState(pos);

		if (!(blockstate.getBlock() instanceof TrackBlock))
			return false;
		if (!mc.world.getWorldBorder()
			.contains(pos))
			return false;

		VertexConsumer vb = buffers
			.getBuffer(RenderLayer.getLines());
		Vec3d camPos = info
			.getPos();

		ms.push();
		ms.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

		boolean holdingTrack = AllTags.AllBlockTags.TRACKS.matches(MinecraftClient.getInstance().player.getMainHandStack());
		TrackShape shape = blockstate.get(TrackBlock.SHAPE);
		boolean canConnectFrom = !shape.isJunction()
			&& !(mc.world.getBlockEntity(pos)instanceof TrackBlockEntity tbe && tbe.isTilted());

		AtomicBoolean cancelled = new AtomicBoolean(false);
		walkShapes(shape, TransformStack.cast(ms), s -> {
			renderShape(s, ms, vb, holdingTrack ? canConnectFrom : null);
			cancelled.set(true);
		});

		ms.pop();
		return cancelled.get();
	}

	public static void renderShape(VoxelShape s, MatrixStack ms, VertexConsumer vb, Boolean valid) {
		MatrixStack.Entry transform = ms.peek();
		s.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
			float xDiff = (float) (x2 - x1);
			float yDiff = (float) (y2 - y1);
			float zDiff = (float) (z2 - z1);
			float length = MathHelper.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

			xDiff /= length;
			yDiff /= length;
			zDiff /= length;

			float r = 0f;
			float g = 0f;
			float b = 0f;

			if (valid != null && valid) {
				g = 1f;
				b = 1f;
				r = 1f;
			}

			if (valid != null && !valid) {
				r = 1f;
				b = 0.125f;
				g = 0.25f;
			}

			vb.vertex(transform.getPositionMatrix(), (float) x1, (float) y1, (float) z1)
				.color(r, g, b, .4f)
				.normal(transform.getNormalMatrix(), xDiff, yDiff, zDiff)
				.next();
			vb.vertex(transform.getPositionMatrix(), (float) x2, (float) y2, (float) z2)
				.color(r, g, b, .4f)
				.normal(transform.getNormalMatrix(), xDiff, yDiff, zDiff)
				.next();

		});
	}

	private static final VoxelShape LONG_CROSS =
		VoxelShapes.union(TrackVoxelShapes.longOrthogonalZ(), TrackVoxelShapes.longOrthogonalX());
	private static final VoxelShape LONG_ORTHO = TrackVoxelShapes.longOrthogonalZ();
	private static final VoxelShape LONG_ORTHO_OFFSET = TrackVoxelShapes.longOrthogonalZOffset();

	private static void walkShapes(TrackShape shape, TransformStack msr, Consumer<VoxelShape> renderer) {
		float angle45 = MathHelper.PI / 4;

		if (shape == TrackShape.XO || shape == TrackShape.CR_NDX || shape == TrackShape.CR_PDX)
			renderer.accept(AllShapes.TRACK_ORTHO.get(Direction.EAST));
		else if (shape == TrackShape.ZO || shape == TrackShape.CR_NDZ || shape == TrackShape.CR_PDZ)
			renderer.accept(AllShapes.TRACK_ORTHO.get(Direction.SOUTH));

		if (shape.isPortal()) {
			for (Direction d : Iterate.horizontalDirections) {
				if (TrackShape.asPortal(d) != shape)
					continue;
				msr.rotateCentered(Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(d)));
				renderer.accept(LONG_ORTHO_OFFSET);
				return;
			}
		}

		if (shape == TrackShape.PD || shape == TrackShape.CR_PDX || shape == TrackShape.CR_PDZ) {
			msr.rotateCentered(Direction.UP, angle45);
			renderer.accept(LONG_ORTHO);
		} else if (shape == TrackShape.ND || shape == TrackShape.CR_NDX || shape == TrackShape.CR_NDZ) {
			msr.rotateCentered(Direction.UP, -MathHelper.PI / 4);
			renderer.accept(LONG_ORTHO);
		}

		if (shape == TrackShape.CR_O)
			renderer.accept(AllShapes.TRACK_CROSS);
		else if (shape == TrackShape.CR_D) {
			msr.rotateCentered(Direction.UP, angle45);
			renderer.accept(LONG_CROSS);
		}

		if (!(shape == TrackShape.AE || shape == TrackShape.AN || shape == TrackShape.AW || shape == TrackShape.AS))
			return;

		msr.translate(0, 1, 0);
		msr.rotateCentered(Direction.UP, MathHelper.PI - AngleHelper.rad(shape.getModelRotation()));
		msr.rotateXRadians(angle45);
		msr.translate(0, -3 / 16f, 1 / 16f);
		renderer.accept(LONG_ORTHO);
	}

	public static record BezierPointSelection(TrackBlockEntity blockEntity, BezierTrackPointLocation loc, Vec3d vec,
		Vec3d angles, Vec3d direction) {
	}

}
