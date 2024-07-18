package com.simibubi.create.content.trains.track;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import com.simibubi.create.foundation.blockEntity.RemoveBlockEntityPacket;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TrackBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity, IMergeableBE, RenderAttachmentBlockEntity {

	Map<BlockPos, BezierConnection> connections;
	boolean cancelDrops;

	public Pair<RegistryKey<World>, BlockPos> boundLocation;
	public TrackBlockEntityTilt tilt;

	public TrackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		connections = new HashMap<>();
		setLazyTickRate(100);
		tilt = new TrackBlockEntityTilt(this);
	}

	public Map<BlockPos, BezierConnection> getConnections() {
		return connections;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!world.isClient && hasInteractableConnections())
			registerToCurveInteraction();
	}

	@Override
	public void tick() {
		super.tick();
		tilt.undoSmoothing();
	}

	@Override
	public void lazyTick() {
		for (BezierConnection connection : connections.values())
			if (connection.isPrimary())
				manageFakeTracksAlong(connection, false);
	}

	public void validateConnections() {
		Set<BlockPos> invalid = new HashSet<>();

		for (Entry<BlockPos, BezierConnection> entry : connections.entrySet()) {
			BlockPos key = entry.getKey();
			BezierConnection bc = entry.getValue();

			if (!key.equals(bc.getKey()) || !pos.equals(bc.tePositions.getFirst())) {
				invalid.add(key);
				continue;
			}

			BlockState blockState = world.getBlockState(key);
			if (blockState.getBlock()instanceof ITrackBlock trackBlock && !blockState.get(TrackBlock.HAS_BE))
				for (Vec3d v : trackBlock.getTrackAxes(world, key, blockState)) {
					Vec3d bcEndAxis = bc.axes.getSecond();
					if (v.distanceTo(bcEndAxis) < 1 / 1024f || v.distanceTo(bcEndAxis.multiply(-1)) < 1 / 1024f)
						world.setBlockState(key, blockState.with(TrackBlock.HAS_BE, true), 3);
				}

			BlockEntity blockEntity = world.getBlockEntity(key);
			if (!(blockEntity instanceof TrackBlockEntity trackBE) || blockEntity.isRemoved()) {
				invalid.add(key);
				continue;
			}

			if (!trackBE.connections.containsKey(pos)) {
				trackBE.addConnection(bc.secondary());
				trackBE.tilt.tryApplySmoothing();
			}
		}

		for (BlockPos blockPos : invalid)
			removeConnection(blockPos);
	}

	public void addConnection(BezierConnection connection) {
		// don't replace existing connections with different materials
		if (connections.containsKey(connection.getKey()) && connection.equalsSansMaterial(connections.get(connection.getKey())))
			return;
		connections.put(connection.getKey(), connection);
		world.scheduleBlockTick(pos, getCachedState().getBlock(), 1);
		notifyUpdate();

		if (connection.isPrimary())
			manageFakeTracksAlong(connection, false);
	}

	public void removeConnection(BlockPos target) {
		if (isTilted())
			tilt.captureSmoothingHandles();

		BezierConnection removed = connections.remove(target);
		notifyUpdate();

		if (removed != null)
			manageFakeTracksAlong(removed, true);

		if (!connections.isEmpty() || getCachedState().getOrEmpty(TrackBlock.SHAPE)
			.orElse(TrackShape.NONE)
			.isPortal())
			return;

		BlockState blockState = world.getBlockState(pos);
		if (blockState.contains(TrackBlock.HAS_BE))
			world.setBlockState(pos, blockState.with(TrackBlock.HAS_BE, false));
		if (world instanceof ServerWorld serverLevel)
			AllPackets.getChannel().sendToClientsTracking(new RemoveBlockEntityPacket(pos), serverLevel, pos);
	}

	public void removeInboundConnections(boolean dropAndDiscard) {
		for (BezierConnection bezierConnection : connections.values()) {
			if (!(world.getBlockEntity(bezierConnection.getKey())instanceof TrackBlockEntity tbe))
				return;
			tbe.removeConnection(bezierConnection.tePositions.getFirst());
			if (!dropAndDiscard)
				continue;
			if (!cancelDrops)
				bezierConnection.spawnItems(world);
			bezierConnection.spawnDestroyParticles(world);
		}
		if (dropAndDiscard)
			AllPackets.getChannel().sendToClientsTracking(new RemoveBlockEntityPacket(pos), this);
	}

	public void bind(RegistryKey<World> boundDimension, BlockPos boundLocation) {
		this.boundLocation = Pair.of(boundDimension, boundLocation);
		markDirty();
	}

	public boolean isTilted() {
		return tilt.smoothingAngle.isPresent();
	}

	@Override
	public void writeSafe(NbtCompound tag) {
		super.writeSafe(tag);
		writeTurns(tag, true);
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		writeTurns(tag, false);
		if (isTilted())
			tag.putDouble("Smoothing", tilt.smoothingAngle.get());
		if (boundLocation == null)
			return;
		tag.put("BoundLocation", NbtHelper.fromBlockPos(boundLocation.getSecond()));
		tag.putString("BoundDimension", boundLocation.getFirst()
			.getValue()
			.toString());
	}

	private void writeTurns(NbtCompound tag, boolean restored) {
		NbtList listTag = new NbtList();
		for (BezierConnection bezierConnection : connections.values())
			listTag.add((restored ? tilt.restoreToOriginalCurve(bezierConnection.clone()) : bezierConnection)
				.write(pos));
		tag.put("Connections", listTag);
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		connections.clear();
		for (NbtElement t : tag.getList("Connections", NbtElement.COMPOUND_TYPE)) {
			if (!(t instanceof NbtCompound))
				return;
			BezierConnection connection = new BezierConnection((NbtCompound) t, pos);
			connections.put(connection.getKey(), connection);
		}

		boolean smoothingPreviously = tilt.smoothingAngle.isPresent();
		tilt.smoothingAngle = Optional.ofNullable(tag.contains("Smoothing") ? tag.getDouble("Smoothing") : null);
		if (smoothingPreviously != tilt.smoothingAngle.isPresent() && clientPacket) {
			// fabric: no need for requestModelDataUpdate
			world.updateListeners(pos, getCachedState(), getCachedState(), 16);
		}

		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> InstancedRenderDispatcher.enqueueUpdate(this));

		if (hasInteractableConnections())
			registerToCurveInteraction();
		else
			removeFromCurveInteraction();

		if (tag.contains("BoundLocation"))
			boundLocation = Pair.of(
				RegistryKey.of(RegistryKeys.WORLD, new Identifier(tag.getString("BoundDimension"))),
				NbtHelper.toBlockPos(tag.getCompound("BoundLocation")));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Box getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void accept(BlockEntity other) {
		if (other instanceof TrackBlockEntity track)
			connections.putAll(track.connections);
		validateConnections();
		world.scheduleBlockTick(pos, getCachedState().getBlock(), 1);
	}

	public boolean hasInteractableConnections() {
		for (BezierConnection connection : connections.values())
			if (connection.isPrimary())
				return true;
		return false;
	}

	@Override
	public void transform(StructureTransform transform) {
		Map<BlockPos, BezierConnection> restoredConnections = new HashMap<>();
		for (Entry<BlockPos, BezierConnection> entry : connections.entrySet())
			restoredConnections.put(entry.getKey(),
				tilt.restoreToOriginalCurve(tilt.restoreToOriginalCurve(entry.getValue()
					.secondary())
					.secondary()));
		connections = restoredConnections;
		tilt.smoothingAngle = Optional.empty();

		if (transform.rotationAxis != Axis.Y)
			return;

		Map<BlockPos, BezierConnection> transformedConnections = new HashMap<>();
		for (Entry<BlockPos, BezierConnection> entry : connections.entrySet()) {
			BezierConnection newConnection = entry.getValue();
			newConnection.normals.replace(transform::applyWithoutOffsetUncentered);
			newConnection.axes.replace(transform::applyWithoutOffsetUncentered);

			BlockPos diff = newConnection.tePositions.getSecond()
				.subtract(newConnection.tePositions.getFirst());
			newConnection.tePositions
				.setSecond(BlockPos.ofFloored(Vec3d.ofCenter(newConnection.tePositions.getFirst())
					.add(transform.applyWithoutOffsetUncentered(Vec3d.of(diff)))));

			Vec3d beVec = Vec3d.of(pos);
			Vec3d teCenterVec = beVec.add(0.5, 0.5, 0.5);
			Vec3d start = newConnection.starts.getFirst();
			Vec3d startToBE = start.subtract(teCenterVec);
			Vec3d endToStart = newConnection.starts.getSecond()
				.subtract(start);
			startToBE = transform.applyWithoutOffsetUncentered(startToBE)
				.add(teCenterVec);
			endToStart = transform.applyWithoutOffsetUncentered(endToStart)
				.add(startToBE);

			newConnection.starts.setFirst(new TrackNodeLocation(startToBE).getLocation());
			newConnection.starts.setSecond(new TrackNodeLocation(endToStart).getLocation());

			BlockPos newTarget = newConnection.getKey();
			transformedConnections.put(newTarget, newConnection);
		}

		connections = transformedConnections;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (world.isClient)
			removeFromCurveInteraction();
	}

	@Override
	public void remove() {
		super.remove();

		for (BezierConnection connection : connections.values())
			manageFakeTracksAlong(connection, true);

		if (boundLocation != null && world instanceof ServerWorld) {
			ServerWorld otherLevel = world.getServer()
				.getWorld(boundLocation.getFirst());
			if (otherLevel == null)
				return;
			if (AllTags.AllBlockTags.TRACKS.matches(otherLevel.getBlockState(boundLocation.getSecond())))
				otherLevel.breakBlock(boundLocation.getSecond(), false);
		}
	}

	private void registerToCurveInteraction() {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerToCurveInteractionUnsafe);
	}

	private void removeFromCurveInteraction() {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::removeFromCurveInteractionUnsafe);
	}

	@Override
	@Nullable
	public Double getRenderAttachmentData() {
		if (!isTilted())
			return null;
		return tilt.smoothingAngle.get();
	}

	@Environment(EnvType.CLIENT)
	private void registerToCurveInteractionUnsafe() {
		TrackBlockOutline.TRACKS_WITH_TURNS.get(world)
			.put(pos, this);
	}

	@Environment(EnvType.CLIENT)
	private void removeFromCurveInteractionUnsafe() {
		TrackBlockOutline.TRACKS_WITH_TURNS.get(world)
			.remove(pos);
	}

	public void manageFakeTracksAlong(BezierConnection bc, boolean remove) {
		Map<Pair<Integer, Integer>, Double> yLevels = new HashMap<>();
		BlockPos tePosition = bc.tePositions.getFirst();
		Vec3d end1 = bc.starts.getFirst()
			.subtract(Vec3d.of(tePosition))
			.add(0, 3 / 16f, 0);
		Vec3d end2 = bc.starts.getSecond()
			.subtract(Vec3d.of(tePosition))
			.add(0, 3 / 16f, 0);
		Vec3d axis1 = bc.axes.getFirst();
		Vec3d axis2 = bc.axes.getSecond();

		double handleLength = bc.getHandleLength();

		Vec3d finish1 = axis1.multiply(handleLength)
			.add(end1);
		Vec3d finish2 = axis2.multiply(handleLength)
			.add(end2);

		Vec3d faceNormal1 = bc.normals.getFirst();
		Vec3d faceNormal2 = bc.normals.getSecond();

		int segCount = bc.getSegmentCount();
		float[] lut = bc.getStepLUT();

		for (int i = 0; i < segCount; i++) {
			float t = i == segCount ? 1 : i * lut[i] / segCount;
			t += 0.5f / segCount;

			Vec3d result = VecHelper.bezier(end1, end2, finish1, finish2, t);
			Vec3d derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
				.normalize();
			Vec3d faceNormal =
				faceNormal1.equals(faceNormal2) ? faceNormal1 : VecHelper.slerp(t, faceNormal1, faceNormal2);
			Vec3d normal = faceNormal.crossProduct(derivative)
				.normalize();
			Vec3d below = result.add(faceNormal.multiply(-.25f));
			Vec3d rail1 = below.add(normal.multiply(.05f));
			Vec3d rail2 = below.subtract(normal.multiply(.05f));
			Vec3d railMiddle = rail1.add(rail2)
				.multiply(.5);

			for (Vec3d vec : new Vec3d[] { railMiddle }) {
				BlockPos pos = BlockPos.ofFloored(vec);
				Pair<Integer, Integer> key = Pair.of(pos.getX(), pos.getZ());
				if (!yLevels.containsKey(key) || yLevels.get(key) > vec.y)
					yLevels.put(key, vec.y);
			}
		}

		for (Entry<Pair<Integer, Integer>, Double> entry : yLevels.entrySet()) {
			double yValue = entry.getValue();
			int floor = MathHelper.floor(yValue);
			BlockPos targetPos = new BlockPos(entry.getKey()
				.getFirst(), floor,
				entry.getKey()
					.getSecond());
			targetPos = targetPos.add(tePosition)
				.up(1);

			BlockState stateAtPos = world.getBlockState(targetPos);
			boolean present = AllBlocks.FAKE_TRACK.has(stateAtPos);

			if (remove) {
				if (present)
					world.removeBlock(targetPos, false);
				continue;
			}

			FluidState fluidState = stateAtPos.getFluidState();
			if (!fluidState.isEmpty() && !fluidState.isEqualAndStill(Fluids.WATER))
				continue;

			if (!present && stateAtPos.isReplaceable())
				world.setBlockState(targetPos,
					ProperWaterloggedBlock.withWater(world, AllBlocks.FAKE_TRACK.getDefaultState(), targetPos), 3);
			FakeTrackBlock.keepAlive(world, targetPos);
		}
	}

}
