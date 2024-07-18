package com.simibubi.create.content.trains.entity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;
import com.simibubi.create.content.trains.entity.TravellingPoint.ITrackSelector;
import com.simibubi.create.content.trains.entity.TravellingPoint.ITurnListener;
import com.simibubi.create.content.trains.entity.TravellingPoint.SteerDirection;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class TrainRelocator {

	static WeakReference<CarriageContraptionEntity> hoveredEntity = new WeakReference<>(null);
	static UUID relocatingTrain;
	static Vec3d relocatingOrigin;
	static int relocatingEntityId;

	static BlockPos lastHoveredPos;
	static BezierTrackPointLocation lastHoveredBezierSegment;
	static Boolean lastHoveredResult;
	static List<Vec3d> toVisualise;

	public static boolean isRelocating() {
		return relocatingTrain != null;
	}

	@Environment(EnvType.CLIENT)
	public static boolean onClicked() {
		if (relocatingTrain == null)
			return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		if (player == null)
			return false;
		if (player.isSpectator())
			return false;

		if (!player.getPos()
			.isInRange(relocatingOrigin, 24) || player.isSneaking()) {
			relocatingTrain = null;
			player.sendMessage(Lang.translateDirect("train.relocate.abort")
				.formatted(Formatting.RED), true);
			return false;
		}

		if (player.hasVehicle())
			return false;
		if (mc.world == null)
			return false;
		Train relocating = getRelocating(mc.world);
		if (relocating != null) {
			Boolean relocate = relocateClient(relocating, false);
			if (relocate != null && relocate.booleanValue())
				relocatingTrain = null;
			if (relocate != null)
				return true; // cancel
		}
		return false;
	}

	@Nullable
	@Environment(EnvType.CLIENT)
	public static Boolean relocateClient(Train relocating, boolean simulate) {
		MinecraftClient mc = MinecraftClient.getInstance();
		HitResult hitResult = mc.crosshairTarget;
		if (!(hitResult instanceof BlockHitResult blockhit))
			return null;

		BlockPos blockPos = blockhit.getBlockPos();
		BezierTrackPointLocation hoveredBezier = null;

		boolean upsideDown = relocating.carriages.get(0).leadingBogey().isUpsideDown();
		Vec3d offset = upsideDown ? new Vec3d(0, -0.5, 0) : Vec3d.ZERO;

		if (simulate && toVisualise != null && lastHoveredResult != null) {
			for (int i = 0; i < toVisualise.size() - 1; i++) {
				Vec3d vec1 = toVisualise.get(i).add(offset);
				Vec3d vec2 = toVisualise.get(i + 1).add(offset);
				CreateClient.OUTLINER.showLine(Pair.of(relocating, i), vec1.add(0, -.925f, 0), vec2.add(0, -.925f, 0))
					.colored(lastHoveredResult || i != toVisualise.size() - 2 ? 0x95CD41 : 0xEA5C2B)
					.disableLineNormals()
					.lineWidth(i % 2 == 1 ? 1 / 6f : 1 / 4f);
			}
		}

		BezierPointSelection bezierSelection = TrackBlockOutline.result;
		if (bezierSelection != null) {
			blockPos = bezierSelection.blockEntity()
				.getPos();
			hoveredBezier = bezierSelection.loc();
		}

		if (simulate) {
			if (lastHoveredPos != null && lastHoveredPos.equals(blockPos)
				&& Objects.equals(lastHoveredBezierSegment, hoveredBezier))
				return lastHoveredResult;
			lastHoveredPos = blockPos;
			lastHoveredBezierSegment = hoveredBezier;
			toVisualise = null;
		}

		BlockState blockState = mc.world.getBlockState(blockPos);
		if (!(blockState.getBlock()instanceof ITrackBlock track))
			return lastHoveredResult = null;

		Vec3d lookAngle = mc.player.getRotationVector();
		boolean direction = bezierSelection != null && lookAngle.dotProduct(bezierSelection.direction()) < 0;
		boolean result = relocate(relocating, mc.world, blockPos, hoveredBezier, direction, lookAngle, true);
		if (!simulate && result) {
			relocating.carriages.forEach(c -> c.forEachPresentEntity(e -> e.nonDamageTicks = 10));
			AllPackets.getChannel().sendToServer(new TrainRelocationPacket(relocatingTrain, blockPos, hoveredBezier,
				direction, lookAngle, relocatingEntityId));
		}

		return lastHoveredResult = result;
	}

	public static boolean relocate(Train train, World level, BlockPos pos, BezierTrackPointLocation bezier,
		boolean bezierDirection, Vec3d lookAngle, boolean simulate) {
		BlockState blockState = level.getBlockState(pos);
		if (!(blockState.getBlock()instanceof ITrackBlock track))
			return false;

		Pair<Vec3d, AxisDirection> nearestTrackAxis = track.getNearestTrackAxis(level, pos, blockState, lookAngle);
		TrackGraphLocation graphLocation = bezier != null
			? TrackGraphHelper.getBezierGraphLocationAt(level, pos,
				bezierDirection ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE, bezier)
			: TrackGraphHelper.getGraphLocationAt(level, pos, nearestTrackAxis.getSecond(),
				nearestTrackAxis.getFirst());

		if (graphLocation == null)
			return false;

		TrackGraph graph = graphLocation.graph;
		TrackNode node1 = graph.locateNode(graphLocation.edge.getFirst());
		TrackNode node2 = graph.locateNode(graphLocation.edge.getSecond());
		TrackEdge edge = graph.getConnectionsFrom(node1)
			.get(node2);
		if (edge == null)
			return false;

		TravellingPoint probe = new TravellingPoint(node1, node2, edge, graphLocation.position, false);
		IEdgePointListener ignoreSignals = probe.ignoreEdgePoints();
		ITurnListener ignoreTurns = probe.ignoreTurns();
		List<Pair<Couple<TrackNode>, Double>> recordedLocations = new ArrayList<>();
		List<Vec3d> recordedVecs = new ArrayList<>();
		Consumer<TravellingPoint> recorder = tp -> {
			recordedLocations.add(Pair.of(Couple.create(tp.node1, tp.node2), tp.position));
			recordedVecs.add(tp.getPosition(graph));
		};
		ITrackSelector steer = probe.steer(SteerDirection.NONE, track.getUpNormal(level, pos, blockState));
		MutableBoolean blocked = new MutableBoolean(false);
		MutableBoolean portal = new MutableBoolean(false);

		MutableInt blockingIndex = new MutableInt(0);
		train.forEachTravellingPointBackwards((tp, d) -> {
			if (blocked.booleanValue())
				return;
			probe.travel(graph, d, steer, ignoreSignals, ignoreTurns, $ -> {
				portal.setTrue();
				return true;
			});
			recorder.accept(probe);
			if (probe.blocked || portal.booleanValue()) {
				blocked.setTrue();
				return;
			}
			blockingIndex.increment();
		});

		if (level.isClient && simulate && !recordedVecs.isEmpty()) {
			toVisualise = new ArrayList<>();
			toVisualise.add(recordedVecs.get(0));
		}

		for (int i = 0; i < recordedVecs.size() - 1; i++) {
			Vec3d vec1 = recordedVecs.get(i);
			Vec3d vec2 = recordedVecs.get(i + 1);
			boolean blocking = i >= blockingIndex.intValue() - 1;
			boolean collided =
				!blocked.booleanValue() && train.findCollidingTrain(level, vec1, vec2, level.getRegistryKey()) != null;
			if (level.isClient && simulate)
				toVisualise.add(vec2);
			if (collided || blocking)
				return false;
		}

		if (blocked.booleanValue())
			return false;

		if (simulate)
			return true;

		train.leaveStation();
		train.derailed = false;
		train.navigation.waitingForSignal = null;
		train.occupiedSignalBlocks.clear();
		train.graph = graph;
		train.speed = 0;
		train.migratingPoints.clear();
		train.cancelStall();

		if (train.navigation.destination != null)
			train.navigation.cancelNavigation();

		train.forEachTravellingPoint(tp -> {
			Pair<Couple<TrackNode>, Double> last = recordedLocations.remove(recordedLocations.size() - 1);
			tp.node1 = last.getFirst()
				.getFirst();
			tp.node2 = last.getFirst()
				.getSecond();
			tp.position = last.getSecond();
			tp.edge = graph.getConnectionsFrom(tp.node1)
				.get(tp.node2);
		});

		for (Carriage carriage : train.carriages)
			carriage.updateContraptionAnchors();

		train.status.successfulMigration();
		train.collectInitiallyOccupiedSignalBlocks();
		return true;
	}

	@Environment(EnvType.CLIENT)
	public static void visualise(Train train, int i, Vec3d v1, Vec3d v2, boolean valid) {
		CreateClient.OUTLINER.showLine(Pair.of(train, i), v1.add(0, -.825f, 0), v2.add(0, -.825f, 0))
			.colored(valid ? 0x95CD41 : 0xEA5C2B)
			.disableLineNormals()
			.lineWidth(i % 2 == 1 ? 1 / 6f : 1 / 4f);
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;

		if (player == null)
			return;
		if (player.hasVehicle())
			return;
		if (mc.world == null)
			return;

		if (relocatingTrain != null) {
			Train relocating = getRelocating(mc.world);
			if (relocating == null) {
				relocatingTrain = null;
				return;
			}

			Entity entity = mc.world.getEntityById(relocatingEntityId);
			if (entity instanceof AbstractContraptionEntity ce && Math.abs(ce.getLerpedPos(0)
				.subtract(ce.getLerpedPos(1))
				.lengthSquared()) > 1 / 1024d) {
				player.sendMessage(Lang.translateDirect("train.cannot_relocate_moving")
					.formatted(Formatting.RED), true);
				relocatingTrain = null;
				return;
			}

			if (!AllItems.WRENCH.isIn(player.getMainHandStack())) {
				player.sendMessage(Lang.translateDirect("train.relocate.abort")
					.formatted(Formatting.RED), true);
				relocatingTrain = null;
				return;
			}

			if (!player.getPos()
				.isInRange(relocatingOrigin, 24)) {
				player.sendMessage(Lang.translateDirect("train.relocate.too_far")
					.formatted(Formatting.RED), true);
				return;
			}

			Boolean success = relocateClient(relocating, true);
			if (success == null)
				player.sendMessage(Lang.translateDirect("train.relocate", relocating.name), true);
			else if (success.booleanValue())
				player.sendMessage(Lang.translateDirect("train.relocate.valid")
					.formatted(Formatting.GREEN), true);
			else
				player.sendMessage(Lang.translateDirect("train.relocate.invalid")
					.formatted(Formatting.RED), true);
			return;
		}

		Couple<Vec3d> rayInputs = ContraptionHandlerClient.getRayInputs(player);
		Vec3d origin = rayInputs.getFirst();
		Vec3d target = rayInputs.getSecond();

		CarriageContraptionEntity currentEntity = hoveredEntity.get();
		if (currentEntity != null) {
			if (ContraptionHandlerClient.rayTraceContraption(origin, target, currentEntity) != null)
				return;
			hoveredEntity = new WeakReference<>(null);
		}

		Box aabb = new Box(origin, target);
		List<CarriageContraptionEntity> intersectingContraptions =
			mc.world.getNonSpectatingEntities(CarriageContraptionEntity.class, aabb);

		for (CarriageContraptionEntity contraptionEntity : intersectingContraptions) {
			if (ContraptionHandlerClient.rayTraceContraption(origin, target, contraptionEntity) == null)
				continue;
			hoveredEntity = new WeakReference<>(contraptionEntity);
		}
	}

	@Environment(EnvType.CLIENT)
	public static boolean carriageWrenched(Vec3d vec3, CarriageContraptionEntity entity) {
		Train train = getTrainFromEntity(entity);
		if (train == null)
			return false;
		relocatingOrigin = vec3;
		relocatingTrain = train.id;
		relocatingEntityId = entity.getId();
		return true;
	}

	@Environment(EnvType.CLIENT)
	public static boolean addToTooltip(List<Text> tooltip, boolean shiftKeyDown) {
		Train train = getTrainFromEntity(hoveredEntity.get());
		if (train != null && train.derailed) {
			TooltipHelper.addHint(tooltip, "hint.derailed_train");
			return true;
		}
		return false;
	}

	@Environment(EnvType.CLIENT)
	private static Train getRelocating(WorldAccess level) {
		return relocatingTrain == null ? null : Create.RAILWAYS.sided(level).trains.get(relocatingTrain);
	}

	private static Train getTrainFromEntity(CarriageContraptionEntity carriageContraptionEntity) {
		if (carriageContraptionEntity == null)
			return null;
		Carriage carriage = carriageContraptionEntity.getCarriage();
		if (carriage == null)
			return null;
		return carriage.train;
	}

}
