package com.simibubi.create.content.trains.track;

import static com.simibubi.create.AllPartialModels.GIRDER_SEGMENT_BOTTOM;
import static com.simibubi.create.AllPartialModels.GIRDER_SEGMENT_MIDDLE;
import static com.simibubi.create.AllPartialModels.GIRDER_SEGMENT_TOP;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.content.trains.track.BezierConnection.GirderAngles;
import com.simibubi.create.content.trains.track.BezierConnection.SegmentAngles;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TrackRenderer extends SafeBlockEntityRenderer<TrackBlockEntity> {

	public TrackRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(TrackBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light,
		int overlay) {
		World level = be.getWorld();
		if (Backend.canUseInstancing(level))
			return;
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getCutoutMipped());
		be.connections.values()
			.forEach(bc -> renderBezierTurn(level, bc, ms, vb));
	}

	public static void renderBezierTurn(World level, BezierConnection bc, MatrixStack ms, VertexConsumer vb) {
		if (!bc.isPrimary())
			return;

		ms.push();
		BlockPos tePosition = bc.tePositions.getFirst();
		BlockState air = Blocks.AIR.getDefaultState();
		SegmentAngles[] segments = bc.getBakedSegments();

		renderGirder(level, bc, ms, vb, tePosition);

		for (int i = 1; i < segments.length; i++) {
			SegmentAngles segment = segments[i];
			int light = WorldRenderer.getLightmapCoordinates(level, segment.lightPosition.add(tePosition));

			TrackMaterial.TrackModelHolder modelHolder = bc.getMaterial().getModelHolder();

			CachedBufferer.partial(modelHolder.tie(), air)
				.mulPose(segment.tieTransform.getPositionMatrix())
				.mulNormal(segment.tieTransform.getNormalMatrix())
				.light(light)
				.renderInto(ms, vb);

			for (boolean first : Iterate.trueAndFalse) {
				Entry transform = segment.railTransforms.get(first);
				CachedBufferer.partial(first ? modelHolder.segment_left() : modelHolder.segment_right(), air)
					.mulPose(transform.getPositionMatrix())
					.mulNormal(transform.getNormalMatrix())
					.light(light)
					.renderInto(ms, vb);
			}
		}

		ms.pop();
	}

	private static void renderGirder(World level, BezierConnection bc, MatrixStack ms, VertexConsumer vb,
		BlockPos tePosition) {
		if (!bc.hasGirder)
			return;

		BlockState air = Blocks.AIR.getDefaultState();
		GirderAngles[] girders = bc.getBakedGirders();

		for (int i = 1; i < girders.length; i++) {
			GirderAngles segment = girders[i];
			int light = WorldRenderer.getLightmapCoordinates(level, segment.lightPosition.add(tePosition));

			for (boolean first : Iterate.trueAndFalse) {
				Entry beamTransform = segment.beams.get(first);
				CachedBufferer.partial(GIRDER_SEGMENT_MIDDLE, air)
					.mulPose(beamTransform.getPositionMatrix())
					.mulNormal(beamTransform.getNormalMatrix())
					.light(light)
					.renderInto(ms, vb);

				for (boolean top : Iterate.trueAndFalse) {
					Entry beamCapTransform = segment.beamCaps.get(top)
						.get(first);
					CachedBufferer.partial(top ? GIRDER_SEGMENT_TOP : GIRDER_SEGMENT_BOTTOM, air)
						.mulPose(beamCapTransform.getPositionMatrix())
						.mulNormal(beamCapTransform.getNormalMatrix())
						.light(light)
						.renderInto(ms, vb);
				}
			}
		}
	}

	public static Vec3d getModelAngles(Vec3d normal, Vec3d diff) {
		double diffX = diff.getX();
		double diffY = diff.getY();
		double diffZ = diff.getZ();
		double len = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));
		double yaw = MathHelper.atan2(diffX, diffZ);
		double pitch = MathHelper.atan2(len, diffY) - Math.PI * .5;

		Vec3d yawPitchNormal = VecHelper.rotate(VecHelper.rotate(new Vec3d(0, 1, 0), AngleHelper.deg(pitch), Axis.X),
			AngleHelper.deg(yaw), Axis.Y);

		double signum = Math.signum(yawPitchNormal.dotProduct(normal));
		if (Math.abs(signum) < 0.5f)
			signum = yawPitchNormal.squaredDistanceTo(normal) < 0.5f ? -1 : 1;
		double dot = diff.crossProduct(normal)
			.normalize()
			.dotProduct(yawPitchNormal);
		double roll = Math.acos(MathHelper.clamp(dot, -1, 1)) * signum;
		return new Vec3d(pitch, yaw, roll);
	}

	@Override
	public boolean rendersOutsideBoundingBox(TrackBlockEntity pBlockEntity) {
		return true;
	}

	@Override
	public int getRenderDistance() {
		return 96 * 2;
	}

}
