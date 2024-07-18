package com.simibubi.create.content.trains.track;

import com.google.common.base.Objects;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem.OverlapResult;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;

public class TrackTargetingClient {

	static BlockPos lastHovered;
	static boolean lastDirection;
	static EdgePointType<?> lastType;
	static BezierTrackPointLocation lastHoveredBezierSegment;

	static OverlapResult lastResult;
	static TrackGraphLocation lastLocation;

	public static void clientTick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		Vec3d lookAngle = player.getRotationVector();

		BlockPos hovered = null;
		boolean direction = false;
		EdgePointType<?> type = null;
		BezierTrackPointLocation hoveredBezier = null;

		ItemStack stack = player.getMainHandStack();
		if (stack.getItem() instanceof TrackTargetingBlockItem ttbi)
			type = ttbi.getType(stack);

		if (type == EdgePointType.SIGNAL)
			Create.RAILWAYS.sided(null)
				.tickSignalOverlay();

		boolean alreadySelected = stack.hasNbt() && stack.getNbt()
			.contains("SelectedPos");

		if (type != null) {
			BezierPointSelection bezierSelection = TrackBlockOutline.result;

			if (alreadySelected) {
				NbtCompound tag = stack.getNbt();
				hovered = NbtHelper.toBlockPos(tag.getCompound("SelectedPos"));
				direction = tag.getBoolean("SelectedDirection");
				if (tag.contains("Bezier")) {
					NbtCompound bezierNbt = tag.getCompound("Bezier");
					BlockPos key = NbtHelper.toBlockPos(bezierNbt.getCompound("Key"));
					hoveredBezier = new BezierTrackPointLocation(key, bezierNbt.getInt("Segment"));
				}

			} else if (bezierSelection != null) {
				hovered = bezierSelection.blockEntity()
					.getPos();
				hoveredBezier = bezierSelection.loc();
				direction = lookAngle.dotProduct(bezierSelection.direction()) < 0;

			} else {
				HitResult hitResult = mc.crosshairTarget;
				if (hitResult != null && hitResult.getType() == Type.BLOCK) {
					BlockHitResult blockHitResult = (BlockHitResult) hitResult;
					BlockPos pos = blockHitResult.getBlockPos();
					BlockState blockState = mc.world.getBlockState(pos);
					if (blockState.getBlock() instanceof ITrackBlock track) {
						direction = track.getNearestTrackAxis(mc.world, pos, blockState, lookAngle)
							.getSecond() == AxisDirection.POSITIVE;
						hovered = pos;
					}
				}
			}
		}

		if (hovered == null) {
			lastHovered = null;
			lastResult = null;
			lastLocation = null;
			lastHoveredBezierSegment = null;
			return;
		}

		if (Objects.equal(hovered, lastHovered) && Objects.equal(hoveredBezier, lastHoveredBezierSegment)
			&& direction == lastDirection && type == lastType)
			return;

		lastType = type;
		lastHovered = hovered;
		lastDirection = direction;
		lastHoveredBezierSegment = hoveredBezier;

		TrackTargetingBlockItem.withGraphLocation(mc.world, hovered, direction, hoveredBezier, type,
			(result, location) -> {
				lastResult = result;
				lastLocation = location;
			});
	}

	public static void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera) {
		if (lastLocation == null || lastResult.feedback != null)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		BlockPos pos = lastHovered;
		int light = WorldRenderer.getLightmapCoordinates(mc.world, pos);
		AxisDirection direction = lastDirection ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;

		RenderedTrackOverlayType type = lastType == EdgePointType.SIGNAL ? RenderedTrackOverlayType.SIGNAL
			: lastType == EdgePointType.OBSERVER ? RenderedTrackOverlayType.OBSERVER : RenderedTrackOverlayType.STATION;

		ms.push();
		TransformStack.cast(ms)
			.translate(Vec3d.of(pos)
				.subtract(camera));
		TrackTargetingBehaviour.render(mc.world, pos, direction, lastHoveredBezierSegment, ms, buffer, light,
			OverlayTexture.DEFAULT_UV, type, 1 + 1 / 16f);
		ms.pop();
	}

}
