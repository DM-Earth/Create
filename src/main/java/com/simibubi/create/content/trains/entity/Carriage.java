package com.simibubi.create.content.trains.entity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableDouble;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;
import com.simibubi.create.content.trains.entity.TravellingPoint.ITrackSelector;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Carriage {

	public static final AtomicInteger netIdGenerator = new AtomicInteger();

	public Train train;
	public int id;
	public boolean blocked;
	public boolean stalled;
	public Couple<Boolean> presentConductors;

	public int bogeySpacing;
	public Couple<CarriageBogey> bogeys;
	public TrainCargoManager storage;

	NbtCompound serialisedEntity;
	Map<Integer, NbtCompound> serialisedPassengers;

	private Map<RegistryKey<World>, DimensionalCarriageEntity> entities;

	static final int FIRST = 0, MIDDLE = 1, LAST = 2, BOTH = 3;

	public Carriage(CarriageBogey bogey1, @Nullable CarriageBogey bogey2, int bogeySpacing) {
		this.bogeySpacing = bogeySpacing;
		this.bogeys = Couple.create(bogey1, bogey2);
		this.id = netIdGenerator.incrementAndGet();
		this.serialisedEntity = new NbtCompound();
		this.presentConductors = Couple.create(false, false);
		this.serialisedPassengers = new HashMap<>();
		this.entities = new HashMap<>();
		this.storage = new TrainCargoManager();

		bogey1.setLeading();
		bogey1.carriage = this;
		if (bogey2 != null)
			bogey2.carriage = this;
	}

	public boolean isOnIncompatibleTrack() {
		return leadingBogey().type.isOnIncompatibleTrack(this, true)
				|| trailingBogey().type.isOnIncompatibleTrack(this, false);
	}

	public void setTrain(Train train) {
		this.train = train;
	}

	public boolean presentInMultipleDimensions() {
		return entities.size() > 1;
	}

	public void setContraption(World level, CarriageContraption contraption) {
		this.storage = null;
		CarriageContraptionEntity entity = CarriageContraptionEntity.create(level, contraption);
		entity.setCarriage(this);
		contraption.startMoving(level);
		contraption.onEntityInitialize(level, entity);
		updateContraptionAnchors();

		DimensionalCarriageEntity dimensional = getDimensional(level);
		dimensional.alignEntity(entity);
		dimensional.removeAndSaveEntity(entity, true);
	}

	public DimensionalCarriageEntity getDimensional(World level) {
		return getDimensional(level.getRegistryKey());
	}

	public DimensionalCarriageEntity getDimensional(RegistryKey<World> dimension) {
		return entities.computeIfAbsent(dimension, $ -> new DimensionalCarriageEntity());
	}

	@Nullable
	public DimensionalCarriageEntity getDimensionalIfPresent(RegistryKey<World> dimension) {
		return entities.get(dimension);
	}

	public double travel(World level, TrackGraph graph, double distance, TravellingPoint toFollowForward,
		TravellingPoint toFollowBackward, int type) {

		Function<TravellingPoint, ITrackSelector> forwardControl =
			toFollowForward == null ? train.navigation::control : mp -> mp.follow(toFollowForward);
		Function<TravellingPoint, ITrackSelector> backwardControl =
			toFollowBackward == null ? train.navigation::control : mp -> mp.follow(toFollowBackward);

		boolean onTwoBogeys = isOnTwoBogeys();
		double stress = train.derailed ? 0 : onTwoBogeys ? bogeySpacing - getAnchorDiff() : 0;
		blocked = false;

		MutableDouble distanceMoved = new MutableDouble(distance);
		boolean iterateFromBack = distance < 0;

		for (boolean firstBogey : Iterate.trueAndFalse) {
			if (!firstBogey && !onTwoBogeys)
				continue;

			boolean actuallyFirstBogey = !onTwoBogeys || (firstBogey ^ iterateFromBack);
			CarriageBogey bogey = bogeys.get(actuallyFirstBogey);
			double bogeyCorrection = stress * (actuallyFirstBogey ? 0.5d : -0.5d);
			double bogeyStress = bogey.getStress();

			for (boolean firstWheel : Iterate.trueAndFalse) {
				boolean actuallyFirstWheel = firstWheel ^ iterateFromBack;
				TravellingPoint point = bogey.points.get(actuallyFirstWheel);
				TravellingPoint prevPoint = !actuallyFirstWheel ? bogey.points.getFirst()
					: !actuallyFirstBogey && onTwoBogeys ? bogeys.getFirst().points.getSecond() : null;
				TravellingPoint nextPoint = actuallyFirstWheel ? bogey.points.getSecond()
					: actuallyFirstBogey && onTwoBogeys ? bogeys.getSecond().points.getFirst() : null;

				double correction = bogeyStress * (actuallyFirstWheel ? 0.5d : -0.5d);
				double toMove = distanceMoved.getValue();

				ITrackSelector frontTrackSelector =
					prevPoint == null ? forwardControl.apply(point) : point.follow(prevPoint);
				ITrackSelector backTrackSelector =
					nextPoint == null ? backwardControl.apply(point) : point.follow(nextPoint);

				boolean atFront = (type == FIRST || type == BOTH) && actuallyFirstWheel && actuallyFirstBogey;
				boolean atBack =
					(type == LAST || type == BOTH) && !actuallyFirstWheel && (!actuallyFirstBogey || !onTwoBogeys);

				IEdgePointListener frontListener = train.frontSignalListener();
				IEdgePointListener backListener = train.backSignalListener();
				IEdgePointListener passiveListener = point.ignoreEdgePoints();

				toMove += correction + bogeyCorrection;

				ITrackSelector trackSelector = toMove > 0 ? frontTrackSelector : backTrackSelector;
				IEdgePointListener signalListener =
					toMove > 0 ? atFront ? frontListener : atBack ? backListener : passiveListener
						: atFront ? backListener : atBack ? frontListener : passiveListener;

				double moved = point.travel(graph, toMove, trackSelector, signalListener, point.ignoreTurns(), c -> {
					for (DimensionalCarriageEntity dce : entities.values())
						if (c.either(tnl -> tnl.equalsIgnoreDim(dce.pivot)))
							return false;
					if (entities.size() > 1) {
						train.status.doublePortal();
						return true;
					}
					return false;
				});

				blocked |= point.blocked;

				distanceMoved.setValue(moved);
			}
		}

		updateContraptionAnchors();
		manageEntities(level);
		return distanceMoved.getValue();
	}

	public double getAnchorDiff() {
		double diff = 0;
		int entries = 0;

		TravellingPoint leadingPoint = getLeadingPoint();
		TravellingPoint trailingPoint = getTrailingPoint();
		if (leadingPoint.node1 != null && trailingPoint.node1 != null)
			if (!leadingPoint.node1.getLocation().dimension.equals(trailingPoint.node1.getLocation().dimension))
				return bogeySpacing;

		for (DimensionalCarriageEntity dce : entities.values())
			if (dce.leadingAnchor() != null && dce.trailingAnchor() != null) {
				entries++;
				diff += dce.leadingAnchor()
					.distanceTo(dce.trailingAnchor());
			}

		if (entries == 0)
			return bogeySpacing;
		return diff / entries;
	}

	public void updateConductors() {
		if (anyAvailableEntity() == null || entities.size() > 1 || serialisedPassengers.size() > 0)
			return;
		presentConductors.replace($ -> false);
		for (DimensionalCarriageEntity dimensionalCarriageEntity : entities.values()) {
			CarriageContraptionEntity entity = dimensionalCarriageEntity.entity.get();
			if (entity != null && entity.isAlive())
				presentConductors.replaceWithParams((current, checked) -> current || checked, entity.checkConductors());
		}
	}

	private Set<RegistryKey<World>> currentlyTraversedDimensions = new HashSet<>();

	public void manageEntities(World level) {
		currentlyTraversedDimensions.clear();

		bogeys.forEach(cb -> {
			if (cb == null)
				return;
			cb.points.forEach(tp -> {
				if (tp.node1 == null)
					return;
				currentlyTraversedDimensions.add(tp.node1.getLocation().dimension);
			});
		});

		for (Iterator<Entry<RegistryKey<World>, DimensionalCarriageEntity>> iterator = entities.entrySet()
			.iterator(); iterator.hasNext();) {
			Entry<RegistryKey<World>, DimensionalCarriageEntity> entry = iterator.next();

			boolean discard =
				!currentlyTraversedDimensions.isEmpty() && !currentlyTraversedDimensions.contains(entry.getKey());

			MinecraftServer server = level.getServer();
			if (server == null)
				continue;
			ServerWorld currentLevel = server.getWorld(entry.getKey());
			if (currentLevel == null)
				continue;

			DimensionalCarriageEntity dimensionalCarriageEntity = entry.getValue();
			CarriageContraptionEntity entity = dimensionalCarriageEntity.entity.get();

			if (entity == null) {
				if (discard)
					iterator.remove();
				else if (dimensionalCarriageEntity.positionAnchor != null && CarriageEntityHandler
					.isActiveChunk(currentLevel, BlockPos.ofFloored(dimensionalCarriageEntity.positionAnchor)))
					dimensionalCarriageEntity.createEntity(currentLevel, anyAvailableEntity() == null);

			} else {
				if (discard) {
					discard = dimensionalCarriageEntity.discardTicks > 3;
					dimensionalCarriageEntity.discardTicks++;
				} else
					dimensionalCarriageEntity.discardTicks = 0;

				CarriageEntityHandler.validateCarriageEntity(entity);
				if (!entity.isAlive() || entity.leftTickingChunks || discard) {
					dimensionalCarriageEntity.removeAndSaveEntity(entity, discard);
					if (discard)
						iterator.remove();
					continue;
				}
			}

			entity = dimensionalCarriageEntity.entity.get();
			if (entity != null && dimensionalCarriageEntity.positionAnchor != null) {
				dimensionalCarriageEntity.alignEntity(entity);
				entity.syncCarriage();
			}
		}

	}

	public void updateContraptionAnchors() {
		CarriageBogey leadingBogey = leadingBogey();
		if (leadingBogey.points.either(t -> t.edge == null))
			return;
		CarriageBogey trailingBogey = trailingBogey();
		if (trailingBogey.points.either(t -> t.edge == null))
			return;

		RegistryKey<World> leadingBogeyDim = leadingBogey.getDimension();
		RegistryKey<World> trailingBogeyDim = trailingBogey.getDimension();
		double leadingWheelSpacing = leadingBogey.type.getWheelPointSpacing();
		double trailingWheelSpacing = trailingBogey.type.getWheelPointSpacing();

		boolean leadingUpsideDown = leadingBogey.isUpsideDown();
		boolean trailingUpsideDown = trailingBogey.isUpsideDown();

		for (boolean leading : Iterate.trueAndFalse) {
			TravellingPoint point = leading ? getLeadingPoint() : getTrailingPoint();
			TravellingPoint otherPoint = !leading ? getLeadingPoint() : getTrailingPoint();
			RegistryKey<World> dimension = point.node1.getLocation().dimension;
			RegistryKey<World> otherDimension = otherPoint.node1.getLocation().dimension;

			if (dimension.equals(otherDimension) && leading) {
				getDimensional(dimension).discardPivot();
				continue;
			}

			DimensionalCarriageEntity dce = getDimensional(dimension);

			dce.positionAnchor = dimension.equals(leadingBogeyDim) ? leadingBogey.getAnchorPosition()
				: pivoted(dce, dimension, point,
					leading ? leadingWheelSpacing / 2 : bogeySpacing + trailingWheelSpacing / 2,
					leadingUpsideDown, trailingUpsideDown);

			boolean backAnchorFlip = trailingBogey.isUpsideDown() ^ leadingBogey.isUpsideDown();

			if (isOnTwoBogeys()) {
				dce.rotationAnchors.setFirst(dimension.equals(leadingBogeyDim) ? leadingBogey.getAnchorPosition()
					: pivoted(dce, dimension, point,
						leading ? leadingWheelSpacing / 2 : bogeySpacing + trailingWheelSpacing / 2,
						leadingUpsideDown, trailingUpsideDown));
				dce.rotationAnchors.setSecond(dimension.equals(trailingBogeyDim) ? trailingBogey.getAnchorPosition(backAnchorFlip)
					: pivoted(dce, dimension, point,
						leading ? leadingWheelSpacing / 2 + bogeySpacing : trailingWheelSpacing / 2,
						leadingUpsideDown, trailingUpsideDown));

			} else {
				if (dimension.equals(otherDimension)) {
					dce.rotationAnchors = leadingBogey.points.map(tp -> tp.getPosition(train.graph));
				} else {
					dce.rotationAnchors.setFirst(leadingBogey.points.getFirst() == point
						? point.getPosition(train.graph)
						: pivoted(dce, dimension, point, leadingWheelSpacing, leadingUpsideDown, trailingUpsideDown));
					dce.rotationAnchors.setSecond(leadingBogey.points.getSecond() == point
						? point.getPosition(train.graph)
						: pivoted(dce, dimension, point, leadingWheelSpacing, leadingUpsideDown, trailingUpsideDown));
				}
			}

			int prevmin = dce.minAllowedLocalCoord();
			int prevmax = dce.maxAllowedLocalCoord();

			dce.updateCutoff(leading);

			if (prevmin != dce.minAllowedLocalCoord() || prevmax != dce.maxAllowedLocalCoord()) {
				dce.updateRenderedCutoff();
				dce.updatePassengerLoadout();
			}
		}

	}

	private Vec3d pivoted(DimensionalCarriageEntity dce, RegistryKey<World> dimension, TravellingPoint start,
		double offset, boolean leadingUpsideDown, boolean trailingUpsideDown) {
		if (train.graph == null)
			return dce.pivot == null ? null : dce.pivot.getLocation();
		TrackNodeLocation pivot = dce.findPivot(dimension, start == getLeadingPoint());
		if (pivot == null)
			return null;
		boolean flipped = start != getLeadingPoint() && (leadingUpsideDown != trailingUpsideDown);
		Vec3d startVec = start.getPosition(train.graph, flipped);
		Vec3d portalVec = pivot.getLocation()
			.add(0, leadingUpsideDown ? -1.0 : 1.0, 0);
		return VecHelper.lerp((float) (offset / startVec.distanceTo(portalVec)), startVec, portalVec);
	}

	public void alignEntity(World level) {
		DimensionalCarriageEntity dimensionalCarriageEntity = entities.get(level.getRegistryKey());
		if (dimensionalCarriageEntity != null) {
			CarriageContraptionEntity entity = dimensionalCarriageEntity.entity.get();
			if (entity != null)
				dimensionalCarriageEntity.alignEntity(entity);
		}
	}

	public TravellingPoint getLeadingPoint() {
		return leadingBogey().leading();
	}

	public TravellingPoint getTrailingPoint() {
		return trailingBogey().trailing();
	}

	public CarriageBogey leadingBogey() {
		return bogeys.getFirst();
	}

	public CarriageBogey trailingBogey() {
		return isOnTwoBogeys() ? bogeys.getSecond() : leadingBogey();
	}

	public boolean isOnTwoBogeys() {
		return bogeys.getSecond() != null;
	}

	public CarriageContraptionEntity anyAvailableEntity() {
		for (DimensionalCarriageEntity dimensionalCarriageEntity : entities.values()) {
			CarriageContraptionEntity entity = dimensionalCarriageEntity.entity.get();
			if (entity != null)
				return entity;
		}
		return null;
	}

	public void forEachPresentEntity(Consumer<CarriageContraptionEntity> callback) {
		for (DimensionalCarriageEntity dimensionalCarriageEntity : entities.values()) {
			CarriageContraptionEntity entity = dimensionalCarriageEntity.entity.get();
			if (entity != null)
				callback.accept(entity);
		}
	}

	public NbtCompound write(DimensionPalette dimensions) {
		NbtCompound tag = new NbtCompound();
		tag.put("FirstBogey", bogeys.getFirst()
			.write(dimensions));
		if (isOnTwoBogeys())
			tag.put("SecondBogey", bogeys.getSecond()
				.write(dimensions));
		tag.putInt("Spacing", bogeySpacing);
		tag.putBoolean("FrontConductor", presentConductors.getFirst());
		tag.putBoolean("BackConductor", presentConductors.getSecond());
		tag.putBoolean("Stalled", stalled);

		Map<Integer, NbtCompound> passengerMap = new HashMap<>();

		for (DimensionalCarriageEntity dimensionalCarriageEntity : entities.values()) {
			CarriageContraptionEntity entity = dimensionalCarriageEntity.entity.get();
			if (entity == null)
				continue;
			serialize(entity);
			Contraption contraption = entity.getContraption();
			if (contraption == null)
				continue;
			Map<UUID, Integer> mapping = contraption.getSeatMapping();
			for (Entity passenger : entity.getPassengerList())
				if (mapping.containsKey(passenger.getUuid()))
					passengerMap.put(mapping.get(passenger.getUuid()), NBTSerializer.serializeNBTCompound(passenger));
		}

		tag.put("Entity", serialisedEntity.copy());

		NbtCompound passengerTag = new NbtCompound();
		passengerMap.putAll(serialisedPassengers);
		passengerMap.forEach((seat, nbt) -> passengerTag.put("Seat" + seat, nbt.copy()));
		tag.put("Passengers", passengerTag);

		tag.put("EntityPositioning", NBTHelper.writeCompoundList(entities.entrySet(), e -> {
			NbtCompound c = e.getValue()
				.write();
			c.putInt("Dim", dimensions.encode(e.getKey()));
			return c;
		}));

		return tag;
	}

	private void serialize(Entity entity) {
		serialisedEntity = NBTSerializer.serializeNBTCompound(entity);
		serialisedEntity.remove("Passengers");
		serialisedEntity.getCompound("Contraption")
			.remove("Passengers");
	}

	public static Carriage read(NbtCompound tag, TrackGraph graph, DimensionPalette dimensions) {
		CarriageBogey bogey1 = CarriageBogey.read(tag.getCompound("FirstBogey"), graph, dimensions);
		CarriageBogey bogey2 =
			tag.contains("SecondBogey") ? CarriageBogey.read(tag.getCompound("SecondBogey"), graph, dimensions) : null;

		Carriage carriage = new Carriage(bogey1, bogey2, tag.getInt("Spacing"));

		carriage.stalled = tag.getBoolean("Stalled");
		carriage.presentConductors = Couple.create(tag.getBoolean("FrontConductor"), tag.getBoolean("BackConductor"));
		carriage.serialisedEntity = tag.getCompound("Entity")
			.copy();

		NBTHelper.iterateCompoundList(tag.getList("EntityPositioning", NbtElement.COMPOUND_TYPE),
			c -> carriage.getDimensional(dimensions.decode(c.getInt("Dim")))
				.read(c));

		NbtCompound passengersTag = tag.getCompound("Passengers");
		passengersTag.getKeys()
			.forEach(key -> carriage.serialisedPassengers.put(Integer.valueOf(key.substring(4)),
				passengersTag.getCompound(key)));

		return carriage;
	}

	private TravellingPoint portalScout = new TravellingPoint();

	public class DimensionalCarriageEntity {
		public Vec3d positionAnchor;
		public Couple<Vec3d> rotationAnchors;
		public WeakReference<CarriageContraptionEntity> entity;

		public TrackNodeLocation pivot;
		int discardTicks;

		// 0 == whole, 0..1 = fading out, -1..0 = fading in
		public float cutoff;

		// client
		public boolean pointsInitialised;

		public DimensionalCarriageEntity() {
			this.entity = new WeakReference<>(null);
			this.rotationAnchors = Couple.create(null, null);
			this.pointsInitialised = false;
		}

		public void discardPivot() {
			int prevmin = minAllowedLocalCoord();
			int prevmax = maxAllowedLocalCoord();

			cutoff = 0;
			pivot = null;

			if ((!serialisedPassengers.isEmpty() && entity.get() != null) || prevmin != minAllowedLocalCoord()
				|| prevmax != maxAllowedLocalCoord()) {
				updatePassengerLoadout();
				updateRenderedCutoff();
			}
		}

		public void updateCutoff(boolean leadingIsCurrent) {
			Vec3d leadingAnchor = rotationAnchors.getFirst();
			Vec3d trailingAnchor = rotationAnchors.getSecond();

			if (leadingAnchor == null || trailingAnchor == null)
				return;
			if (pivot == null) {
				cutoff = 0;
				return;
			}

			Vec3d pivotLoc = pivot.getLocation()
				.add(0, 1, 0);

			double leadingSpacing = leadingBogey().type.getWheelPointSpacing() / 2;
			double trailingSpacing = trailingBogey().type.getWheelPointSpacing() / 2;
			double anchorSpacing = leadingSpacing + bogeySpacing + trailingSpacing;

			if (isOnTwoBogeys()) {
				Vec3d diff = trailingAnchor.subtract(leadingAnchor)
					.normalize();
				trailingAnchor = trailingAnchor.add(diff.multiply(trailingSpacing));
				leadingAnchor = leadingAnchor.add(diff.multiply(-leadingSpacing));
			}

			double leadingDiff = leadingAnchor.distanceTo(pivotLoc);
			double trailingDiff = trailingAnchor.distanceTo(pivotLoc);

			leadingDiff /= anchorSpacing;
			trailingDiff /= anchorSpacing;

			if (leadingIsCurrent && leadingDiff > trailingDiff && leadingDiff > 1)
				cutoff = 0;
			else if (leadingIsCurrent && leadingDiff < trailingDiff && trailingDiff > 1)
				cutoff = 1;
			else if (!leadingIsCurrent && leadingDiff > trailingDiff && leadingDiff > 1)
				cutoff = -1;
			else if (!leadingIsCurrent && leadingDiff < trailingDiff && trailingDiff > 1)
				cutoff = 0;
			else
				cutoff = (float) MathHelper.clamp(1 - (leadingIsCurrent ? leadingDiff : trailingDiff), 0, 1)
					* (leadingIsCurrent ? 1 : -1);
		}

		public TrackNodeLocation findPivot(RegistryKey<World> dimension, boolean leading) {
			if (pivot != null)
				return pivot;

			TravellingPoint start = leading ? getLeadingPoint() : getTrailingPoint();
			TravellingPoint end = !leading ? getLeadingPoint() : getTrailingPoint();

			portalScout.node1 = start.node1;
			portalScout.node2 = start.node2;
			portalScout.edge = start.edge;
			portalScout.position = start.position;

			ITrackSelector trackSelector = portalScout.follow(end);
			int distance = bogeySpacing + 10;
			int direction = leading ? -1 : 1;

			portalScout.travel(train.graph, direction * distance, trackSelector, portalScout.ignoreEdgePoints(),
				portalScout.ignoreTurns(), nodes -> {
					for (boolean b : Iterate.trueAndFalse)
						if (nodes.get(b).dimension.equals(dimension))
							pivot = nodes.get(b);
					return true;
				});

			return pivot;
		}

		public NbtCompound write() {
			NbtCompound tag = new NbtCompound();
			tag.putFloat("Cutoff", cutoff);
			tag.putInt("DiscardTicks", discardTicks);
			storage.write(tag, false);
			if (pivot != null)
				tag.put("Pivot", pivot.write(null));
			if (positionAnchor != null)
				tag.put("PositionAnchor", VecHelper.writeNBT(positionAnchor));
			if (rotationAnchors.both(Objects::nonNull))
				tag.put("RotationAnchors", rotationAnchors.serializeEach(VecHelper::writeNBTCompound));
			return tag;
		}

		public void read(NbtCompound tag) {
			cutoff = tag.getFloat("Cutoff");
			discardTicks = tag.getInt("DiscardTicks");
			storage.read(tag, null, false);
			if (tag.contains("Pivot"))
				pivot = TrackNodeLocation.read(tag.getCompound("Pivot"), null);
			if (positionAnchor != null)
				return;
			if (tag.contains("PositionAnchor"))
				positionAnchor = VecHelper.readNBT(tag.getList("PositionAnchor", NbtElement.DOUBLE_TYPE));
			if (tag.contains("RotationAnchors"))
				rotationAnchors = Couple.deserializeEach(tag.getList("RotationAnchors", NbtElement.COMPOUND_TYPE),
					VecHelper::readNBTCompound);
		}

		public Vec3d leadingAnchor() {
			return isOnTwoBogeys() ? rotationAnchors.getFirst() : positionAnchor;
		}

		public Vec3d trailingAnchor() {
			return isOnTwoBogeys() ? rotationAnchors.getSecond() : positionAnchor;
		}

		public int minAllowedLocalCoord() {
			if (cutoff <= 0)
				return Integer.MIN_VALUE;
			if (cutoff >= 1)
				return Integer.MAX_VALUE;
			return MathHelper.floor(-bogeySpacing + -1 + (2 + bogeySpacing) * cutoff);
		}

		public int maxAllowedLocalCoord() {
			if (cutoff >= 0)
				return Integer.MAX_VALUE;
			if (cutoff <= -1)
				return Integer.MIN_VALUE;
			return MathHelper.ceil(-bogeySpacing + -1 + (2 + bogeySpacing) * (cutoff + 1));
		}

		public void updatePassengerLoadout() {
			Entity entity = this.entity.get();
			if (!(entity instanceof CarriageContraptionEntity cce))
				return;
			if (!(entity.getWorld() instanceof ServerWorld sLevel))
				return;

			Set<Integer> loadedPassengers = new HashSet<>();
			int min = minAllowedLocalCoord();
			int max = maxAllowedLocalCoord();

			for (Entry<Integer, NbtCompound> entry : serialisedPassengers.entrySet()) {
				Integer seatId = entry.getKey();
				List<BlockPos> seats = cce.getContraption()
					.getSeats();
				if (seatId >= seats.size())
					continue;

				BlockPos localPos = seats.get(seatId);
				if (!cce.isLocalCoordWithin(localPos, min, max))
					continue;

				NbtCompound tag = entry.getValue();
				Entity passenger = null;

				if (tag.contains("PlayerPassenger")) {
					passenger = sLevel.getServer()
						.getPlayerManager()
						.getPlayer(tag.getUuid("PlayerPassenger"));

				} else {
					passenger = EntityType.loadEntityWithPassengers(tag, entity.getWorld(), e -> {
						e.refreshPositionAfterTeleport(positionAnchor);
						return e;
					});
					if (passenger != null)
						sLevel.spawnNewEntityAndPassengers(passenger);
				}

				if (passenger != null) {
					RegistryKey<World> passengerDimension = passenger.getWorld().getRegistryKey();
					if (!passengerDimension.equals(sLevel.getRegistryKey()) && passenger instanceof ServerPlayerEntity sp)
						continue;
					cce.addSittingPassenger(passenger, seatId);
				}

				loadedPassengers.add(seatId);
			}

			loadedPassengers.forEach(serialisedPassengers::remove);

			Map<UUID, Integer> mapping = cce.getContraption()
				.getSeatMapping();
			for (Entity passenger : entity.getPassengerList()) {
				BlockPos localPos = cce.getContraption()
					.getSeatOf(passenger.getUuid());
				if (cce.isLocalCoordWithin(localPos, min, max))
					continue;
				if (!mapping.containsKey(passenger.getUuid()))
					continue;

				Integer seat = mapping.get(passenger.getUuid());
				if ((passenger instanceof ServerPlayerEntity sp)) {
					dismountPlayer(sLevel, sp, seat, true);
					continue;
				}

				serialisedPassengers.put(seat, NBTSerializer.serializeNBTCompound(passenger));
				passenger.discard();
			}

		}

		private void dismountPlayer(ServerWorld sLevel, ServerPlayerEntity sp, Integer seat, boolean capture) {
			if (!capture) {
				sp.stopRiding();
				return;
			}

			NbtCompound tag = new NbtCompound();
			tag.putUuid("PlayerPassenger", sp.getUuid());
			serialisedPassengers.put(seat, tag);
			sp.stopRiding();
			sp.getCustomData()
				.remove("ContraptionDismountLocation");

			for (Entry<RegistryKey<World>, DimensionalCarriageEntity> other : entities.entrySet()) {
				DimensionalCarriageEntity otherDce = other.getValue();
				if (otherDce == this)
					continue;
				if (sp.getWorld().getRegistryKey()
					.equals(other.getKey()))
					continue;
				Vec3d loc = otherDce.pivot == null ? otherDce.positionAnchor : otherDce.pivot.getLocation();
				if (loc == null)
					continue;
				ServerWorld level = sLevel.getServer()
					.getWorld(other.getKey());
				sp.teleport(level, loc.x, loc.y, loc.z, sp.getYaw(), sp.getPitch());
				sp.resetPortalCooldown();
				AllAdvancements.TRAIN_PORTAL.awardTo(sp);
			}
		}

		public void updateRenderedCutoff() {
			Entity entity = this.entity.get();
			if (!(entity instanceof CarriageContraptionEntity cce))
				return;
			Contraption contraption = cce.getContraption();
			if (!(contraption instanceof CarriageContraption cc))
				return;
			cc.portalCutoffMin = minAllowedLocalCoord();
			cc.portalCutoffMax = maxAllowedLocalCoord();
			if (!entity.getWorld().isClient())
				return;
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> invalidate(cce));
		}

		@Environment(EnvType.CLIENT)
		private void invalidate(CarriageContraptionEntity entity) {
			entity.getContraption().deferInvalidate = true;
			entity.updateRenderedPortalCutoff();
		}

		private void createEntity(World level, boolean loadPassengers) {
			Entity entity = EntityType.getEntityFromNbt(serialisedEntity, level)
				.orElse(null);

			if (!(entity instanceof CarriageContraptionEntity cce)) {
				train.invalid = true;
				return;
			}

			entity.refreshPositionAfterTeleport(positionAnchor);
			this.entity = new WeakReference<>(cce);

			cce.setCarriage(Carriage.this);
			cce.syncCarriage();

			if (level instanceof ServerWorld sl)
				sl.spawnEntity(entity);

			updatePassengerLoadout();
		}

		private void removeAndSaveEntity(CarriageContraptionEntity entity, boolean portal) {
			Contraption contraption = entity.getContraption();
			if (contraption != null) {
				Map<UUID, Integer> mapping = contraption.getSeatMapping();
				for (Entity passenger : entity.getPassengerList()) {
					if (!mapping.containsKey(passenger.getUuid()))
						continue;

					Integer seat = mapping.get(passenger.getUuid());

					if (passenger instanceof ServerPlayerEntity sp) {
						dismountPlayer(sp.getServerWorld(), sp, seat, portal);
						continue;
					}

					serialisedPassengers.put(seat, NBTSerializer.serializeNBTCompound(passenger));
				}
			}

			for (Entity passenger : entity.getPassengerList())
				if (!(passenger instanceof PlayerEntity))
					passenger.discard();

			serialize(entity);
			entity.discard();
			this.entity.clear();
		}

		public void alignEntity(CarriageContraptionEntity entity) {
			if (rotationAnchors.either(Objects::isNull))
				return;

			Vec3d positionVec = rotationAnchors.getFirst();
			Vec3d coupledVec = rotationAnchors.getSecond();

			double diffX = positionVec.x - coupledVec.x;
			double diffY = positionVec.y - coupledVec.y;
			double diffZ = positionVec.z - coupledVec.z;

			entity.prevYaw = entity.yaw;
			entity.prevPitch = entity.pitch;

			if (!entity.getWorld().isClient()) {
				Vec3d lookahead = positionAnchor.add(positionAnchor.subtract(entity.getPos())
					.normalize()
					.multiply(16));

				for (Entity e : entity.getPassengerList()) {
					if (!(e instanceof PlayerEntity))
						continue;
					if (e.squaredDistanceTo(entity) > 32 * 32)
						continue;
					if (CarriageEntityHandler.isActiveChunk(entity.getWorld(), BlockPos.ofFloored(lookahead)))
						break;
					train.carriageWaitingForChunks = id;
					return;
				}

				if (entity.getPassengerList()
					.stream()
					.anyMatch(p -> p instanceof PlayerEntity)
					) {
				}

				if (train.carriageWaitingForChunks == id)
					train.carriageWaitingForChunks = -1;

				entity.setServerSidePrevPosition();
			}

			entity.setPosition(positionAnchor);
			entity.yaw = (float) (MathHelper.atan2(diffZ, diffX) * 180 / Math.PI) + 180;
			entity.pitch = (float) (Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)) * 180 / Math.PI) * -1;

			if (!entity.firstPositionUpdate)
				return;

			entity.prevX = entity.getX();
			entity.prevY = entity.getY();
			entity.prevZ = entity.getZ();
			entity.prevYaw = entity.yaw;
			entity.prevPitch = entity.pitch;
		}
	}

}
