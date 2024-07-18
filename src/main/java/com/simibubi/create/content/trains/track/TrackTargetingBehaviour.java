package com.simibubi.create.content.trains.track;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class TrackTargetingBehaviour<T extends TrackEdgePoint> extends BlockEntityBehaviour {

	public static final BehaviourType<TrackTargetingBehaviour<?>> TYPE = new BehaviourType<>();

	private BlockPos targetTrack;
	private BezierTrackPointLocation targetBezier;
	private AxisDirection targetDirection;
	private UUID id;

	private Vec3d prevDirection;
	private Vec3d rotatedDirection;

	private NbtCompound migrationData;
	private EdgePointType<T> edgePointType;
	private T edgePoint;
	private boolean orthogonal;

	public TrackTargetingBehaviour(SmartBlockEntity be, EdgePointType<T> edgePointType) {
		super(be);
		this.edgePointType = edgePointType;
		targetDirection = AxisDirection.POSITIVE;
		targetTrack = BlockPos.ORIGIN;
		id = UUID.randomUUID();
		migrationData = null;
		orthogonal = false;
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		nbt.putUuid("Id", id);
		nbt.put("TargetTrack", NbtHelper.fromBlockPos(targetTrack));
		nbt.putBoolean("Ortho", orthogonal);
		nbt.putBoolean("TargetDirection", targetDirection == AxisDirection.POSITIVE);
		if (rotatedDirection != null)
			nbt.put("RotatedAxis", VecHelper.writeNBT(rotatedDirection));
		if (prevDirection != null)
			nbt.put("PrevAxis", VecHelper.writeNBT(prevDirection));
		if (migrationData != null && !clientPacket)
			nbt.put("Migrate", migrationData);
		if (targetBezier != null) {
			NbtCompound bezierNbt = new NbtCompound();
			bezierNbt.putInt("Segment", targetBezier.segment());
			bezierNbt.put("Key", NbtHelper.fromBlockPos(targetBezier.curveTarget()
				.subtract(getPos())));
			nbt.put("Bezier", bezierNbt);
		}
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		id = nbt.contains("Id") ? nbt.getUuid("Id") : UUID.randomUUID();
		targetTrack = NbtHelper.toBlockPos(nbt.getCompound("TargetTrack"));
		targetDirection = nbt.getBoolean("TargetDirection") ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
		orthogonal = nbt.getBoolean("Ortho");
		if (nbt.contains("PrevAxis"))
			prevDirection = VecHelper.readNBT(nbt.getList("PrevAxis", NbtElement.DOUBLE_TYPE));
		if (nbt.contains("RotatedAxis"))
			rotatedDirection = VecHelper.readNBT(nbt.getList("RotatedAxis", NbtElement.DOUBLE_TYPE));
		if (nbt.contains("Migrate"))
			migrationData = nbt.getCompound("Migrate");
		if (clientPacket)
			edgePoint = null;
		if (nbt.contains("Bezier")) {
			NbtCompound bezierNbt = nbt.getCompound("Bezier");
			BlockPos key = NbtHelper.toBlockPos(bezierNbt.getCompound("Key"));
			targetBezier = new BezierTrackPointLocation(bezierNbt.contains("FromStack") ? key : key.add(getPos()),
				bezierNbt.getInt("Segment"));
		}
		super.read(nbt, clientPacket);
	}

	@Nullable
	public T getEdgePoint() {
		return edgePoint;
	}

	public void invalidateEdgePoint(NbtCompound migrationData) {
		this.migrationData = migrationData;
		edgePoint = null;
		blockEntity.sendData();
	}

	@Override
	public void tick() {
		super.tick();
		if (edgePoint == null)
			edgePoint = createEdgePoint();
	}

	@SuppressWarnings("unchecked")
	public T createEdgePoint() {
		World level = getWorld();
		boolean isClientSide = level.isClient;
		if (migrationData == null || isClientSide)
			for (TrackGraph trackGraph : Create.RAILWAYS.sided(level).trackNetworks.values()) {
				T point = trackGraph.getPoint(edgePointType, id);
				if (point == null)
					continue;
				return point;
			}

		if (isClientSide)
			return null;
		if (!hasValidTrack())
			return null;
		TrackGraphLocation loc = determineGraphLocation();
		if (loc == null)
			return null;

		TrackGraph graph = loc.graph;
		TrackNode node1 = graph.locateNode(loc.edge.getFirst());
		TrackNode node2 = graph.locateNode(loc.edge.getSecond());
		TrackEdge edge = graph.getConnectionsFrom(node1)
			.get(node2);
		if (edge == null)
			return null;

		T point = edgePointType.create();
		boolean front = getTargetDirection() == AxisDirection.POSITIVE;

		prevDirection = edge.getDirectionAt(loc.position)
			.multiply(front ? -1 : 1);

		if (rotatedDirection != null) {
			double dot = prevDirection.dotProduct(rotatedDirection);
			if (dot < -.85f) {
				rotatedDirection = null;
				targetDirection = targetDirection.getOpposite();
				return null;
			}

			rotatedDirection = null;
		}

		double length = edge.getLength();
		NbtCompound data = migrationData;
		migrationData = null;

		{
			orthogonal = targetBezier == null;
			Vec3d direction = edge.getDirection(true);
			int nonZeroComponents = 0;
			for (Axis axis : Iterate.axes)
				nonZeroComponents += direction.getComponentAlongAxis(axis) != 0 ? 1 : 0;
			orthogonal &= nonZeroComponents <= 1;
		}

		EdgeData signalData = edge.getEdgeData();
		if (signalData.hasPoints()) {
			for (EdgePointType<?> otherType : EdgePointType.TYPES.values()) {
				TrackEdgePoint otherPoint = signalData.get(otherType, loc.position);
				if (otherPoint == null)
					continue;
				if (otherType != edgePointType) {
					if (!otherPoint.canCoexistWith(edgePointType, front))
						return null;
					continue;
				}
				if (!otherPoint.canMerge())
					return null;
				otherPoint.blockEntityAdded(blockEntity, front);
				id = otherPoint.getId();
				blockEntity.notifyUpdate();
				return (T) otherPoint;
			}
		}

		if (data != null)
			point.read(data, true, DimensionPalette.read(data));

		point.setId(id);
		boolean reverseEdge = front || point instanceof SingleBlockEntityEdgePoint;
		point.setLocation(reverseEdge ? loc.edge : loc.edge.swap(), reverseEdge ? loc.position : length - loc.position);
		point.blockEntityAdded(blockEntity, front);
		loc.graph.addPoint(edgePointType, point);
		blockEntity.sendData();
		return point;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (edgePoint != null && !getWorld().isClient)
			edgePoint.blockEntityRemoved(getPos(), getTargetDirection() == AxisDirection.POSITIVE);
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	public boolean isOnCurve() {
		return targetBezier != null;
	}

	public boolean isOrthogonal() {
		return orthogonal;
	}

	public boolean hasValidTrack() {
		return getTrackBlockState().getBlock() instanceof ITrackBlock;
	}

	public ITrackBlock getTrack() {
		return (ITrackBlock) getTrackBlockState().getBlock();
	}

	public BlockState getTrackBlockState() {
		return getWorld().getBlockState(getGlobalPosition());
	}

	public BlockPos getGlobalPosition() {
		return targetTrack.add(blockEntity.getPos());
	}

	public BlockPos getPositionForMapMarker() {
		BlockPos target = targetTrack.add(blockEntity.getPos());
		if (targetBezier != null && getWorld().getBlockEntity(target) instanceof TrackBlockEntity tbe) {
			BezierConnection bc = tbe.getConnections()
				.get(targetBezier.curveTarget());
			if (bc == null)
				return target;
			double length = MathHelper.floor(bc.getLength() * 2);
			int seg = targetBezier.segment() + 1;
			double t = seg / length;
			return BlockPos.ofFloored(bc.getPosition(t));
		}
		return target;
	}

	public AxisDirection getTargetDirection() {
		return targetDirection;
	}

	public BezierTrackPointLocation getTargetBezier() {
		return targetBezier;
	}

	public TrackGraphLocation determineGraphLocation() {
		World level = getWorld();
		BlockPos pos = getGlobalPosition();
		BlockState state = getTrackBlockState();
		ITrackBlock track = getTrack();
		List<Vec3d> trackAxes = track.getTrackAxes(level, pos, state);
		AxisDirection targetDirection = getTargetDirection();

		return targetBezier != null
			? TrackGraphHelper.getBezierGraphLocationAt(level, pos, targetDirection, targetBezier)
			: TrackGraphHelper.getGraphLocationAt(level, pos, targetDirection, trackAxes.get(0));
	}

	public static enum RenderedTrackOverlayType {
		STATION, SIGNAL, DUAL_SIGNAL, OBSERVER;
	}

	@Environment(EnvType.CLIENT)
	public static void render(WorldAccess level, BlockPos pos, AxisDirection direction,
		BezierTrackPointLocation bezier, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay,
		RenderedTrackOverlayType type, float scale) {
		if (level instanceof SchematicWorld && !(level instanceof PonderWorld))
			return;

		BlockState trackState = level.getBlockState(pos);
		Block block = trackState.getBlock();
		if (!(block instanceof ITrackBlock))
			return;

		ms.push();
		ITrackBlock track = (ITrackBlock) block;
		PartialModel partial = track.prepareTrackOverlay(level, pos, trackState, bezier, direction, ms, type);
		if (partial != null)
			CachedBufferer.partial(partial, trackState)
				.translate(.5, 0, .5)
				.scale(scale)
				.translate(-.5, 0, -.5)
				.light(WorldRenderer.getLightmapCoordinates(level, pos))
				.renderInto(ms, buffer.getBuffer(RenderLayer.getCutoutMipped()));
		ms.pop();
	}

	public void transform(StructureTransform transform) {
		id = UUID.randomUUID();
		targetTrack = transform.applyWithoutOffset(targetTrack);
		if (prevDirection != null)
			rotatedDirection = transform.applyWithoutOffsetUncentered(prevDirection);
		if (targetBezier != null)
			targetBezier = new BezierTrackPointLocation(transform.applyWithoutOffset(targetBezier.curveTarget()
				.subtract(getPos()))
				.add(getPos()), targetBezier.segment());
		blockEntity.notifyUpdate();
	}

}
