package com.simibubi.create.content.trains.entity;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Strings;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.ContraptionBlockChangedPacket;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.CubeParticleData;
import com.simibubi.create.content.trains.TrainHUDUpdatePacket;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.TravellingPoint.SteerDirection;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CarriageContraptionEntity extends OrientedContraptionEntity {

	// fabric: cannot use custom entity data serializers
//	private static final EntityDataAccessor<CarriageSyncData> CARRIAGE_DATA =
//		SynchedEntityData.defineId(CarriageContraptionEntity.class, AllEntityDataSerializers.CARRIAGE_DATA);
	private static final TrackedData<Optional<UUID>> TRACK_GRAPH =
		DataTracker.registerData(CarriageContraptionEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private static final TrackedData<Boolean> SCHEDULED =
		DataTracker.registerData(CarriageContraptionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public UUID trainId;
	public int carriageIndex;

	private Carriage carriage;
	public boolean validForRender;
	public boolean movingBackwards;

	public boolean leftTickingChunks;
	public boolean firstPositionUpdate;

	private boolean arrivalSoundPlaying;
	private boolean arrivalSoundReversed;
	private int arrivalSoundTicks;

	private Vec3d serverPrevPos;

	@Environment(EnvType.CLIENT)
	public CarriageSounds sounds;
	@Environment(EnvType.CLIENT)
	public CarriageParticles particles;

	// fabric: cannot use custom entity data serializers
	public CarriageSyncData carriageData = new CarriageSyncData();

	public CarriageContraptionEntity(EntityType<?> type, World world) {
		super(type, world);
		validForRender = false;
		firstPositionUpdate = true;
		arrivalSoundTicks = Integer.MIN_VALUE;
		derailParticleOffset = VecHelper.offsetRandomly(Vec3d.ZERO, world.random, 1.5f)
				.multiply(1, .25f, 1);
	}

	@Override
	public boolean isLogicalSideForUpdatingMovement() {
		return true;
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
//		entityData.define(CARRIAGE_DATA, new CarriageSyncData());
		dataTracker.startTracking(TRACK_GRAPH, Optional.empty());
		dataTracker.startTracking(SCHEDULED, false);
	}

	public void syncCarriage() {
		CarriageSyncData carriageData = getCarriageData();
		if (carriageData == null)
			return;
		if (carriage == null)
			return;
		carriageData.update(this, carriage);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> key) {
		super.onTrackedDataSet(key);

		if (!getWorld().isClient)
			return;

		bindCarriage();

		if (TRACK_GRAPH.equals(key))
			updateTrackGraph();

		// fabric: carriage data update listening moved
	}

	public void onCarriageDataUpdate(CarriageSyncData newData) {
		this.carriageData = newData;
		if (carriage == null)
			return;
		carriageData.apply(this, carriage);
	}

	public void sendCarriageDataUpdate() {
		AllPackets.getChannel().sendToClientsTracking(new CarriageDataUpdatePacket(this), this);
	}

	// fabric: initial carriageData sync since that's not handled by tracked data anymore

	@Override
	public void writeSpawnData(PacketByteBuf buffer) {
		super.writeSpawnData(buffer);
		carriageData.write(buffer);
	}

	@Override
	public void readSpawnData(PacketByteBuf additionalData) {
		super.readSpawnData(additionalData);
		carriageData.read(additionalData);
	}

	public CarriageSyncData getCarriageData() {
		return carriageData;
	}

	public boolean hasSchedule() {
		return dataTracker.get(SCHEDULED);
	}

	public void setServerSidePrevPosition() {
		serverPrevPos = getPos();
	}

	@Override
	public Vec3d getPrevPositionVec() {
		if (!getWorld().isClient() && serverPrevPos != null)
			return serverPrevPos;
		return super.getPrevPositionVec();
	}

	public boolean isLocalCoordWithin(BlockPos localPos, int min, int max) {
		if (!(getContraption()instanceof CarriageContraption cc))
			return false;
		Direction facing = cc.getAssemblyDirection();
		Axis axis = facing.rotateYClockwise()
			.getAxis();
		int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getDirection()
			.offset();
		return coord >= min && coord <= max;
	}

	public static CarriageContraptionEntity create(World world, CarriageContraption contraption) {
		CarriageContraptionEntity entity =
			new CarriageContraptionEntity(AllEntityTypes.CARRIAGE_CONTRAPTION.get(), world);
		entity.setContraption(contraption);
		entity.setInitialOrientation(contraption.getAssemblyDirection()
			.rotateYClockwise());
		entity.startAtInitialYaw();
		return entity;
	}

	@Override
	public void tick() {
		super.tick();

		if (contraption instanceof CarriageContraption cc)
			for (Entity entity : getPassengerList()) {
				if (entity instanceof PlayerEntity)
					continue;
				BlockPos seatOf = cc.getSeatOf(entity.getUuid());
				if (seatOf == null)
					continue;
				if (cc.conductorSeats.get(seatOf) == null)
					continue;
				alignPassenger(entity);
			}
	}

	@Override
	public void setBlock(BlockPos localPos, StructureBlockInfo newInfo) {
		if (carriage == null)
			return;
		carriage.forEachPresentEntity(cce -> {
			cce.contraption.getBlocks()
				.put(localPos, newInfo);
			AllPackets.getChannel().sendToClientsTracking(
				new ContraptionBlockChangedPacket(cce.getId(), localPos, newInfo.state()), cce);
		});
	}

	@Override
	protected void tickContraption() {
		if (nonDamageTicks > 0)
			nonDamageTicks--;
		if (!(contraption instanceof CarriageContraption cc))
			return;

		if (carriage == null) {
			if (getWorld().isClient)
				bindCarriage();
			else
				discard();
			return;
		}

		if (!Create.RAILWAYS.sided(getWorld()).trains.containsKey(carriage.train.id)) {
			discard();
			return;
		}

		tickActors();
		boolean isStalled = isStalled();
		carriage.stalled = isStalled;

		CarriageSyncData carriageData = getCarriageData();

		if (!getWorld().isClient) {

			dataTracker.set(SCHEDULED, carriage.train.runtime.getSchedule() != null);

			boolean shouldCarriageSyncThisTick =
				carriage.train.shouldCarriageSyncThisTick(getWorld().getTime(), getType().getTrackTickInterval());
			if (shouldCarriageSyncThisTick && carriageData.isDirty()) {
				sendCarriageDataUpdate();
				carriageData.setDirty(false);
			}

			Navigation navigation = carriage.train.navigation;
			if (navigation.announceArrival && Math.abs(navigation.distanceToDestination) < 60
				&& carriageIndex == (carriage.train.speed < 0 ? carriage.train.carriages.size() - 1 : 0)) {
				navigation.announceArrival = false;
				arrivalSoundPlaying = true;
				arrivalSoundReversed = carriage.train.speed < 0;
				arrivalSoundTicks = Integer.MIN_VALUE;
			}

			if (arrivalSoundPlaying)
				tickArrivalSound(cc);

			dataTracker.set(TRACK_GRAPH, Optional.ofNullable(carriage.train.graph)
				.map(g -> g.id));

			return;
		}

		DimensionalCarriageEntity dce = carriage.getDimensional(getWorld());
		if (age % 10 == 0)
			updateTrackGraph();

		if (!dce.pointsInitialised)
			return;

		carriageData.approach(this, carriage, 1f / getType().getTrackTickInterval());

		if (!carriage.train.derailed)
			carriage.updateContraptionAnchors();

		prevX = getX();
		prevY = getY();
		prevZ = getZ();

		dce.alignEntity(this);

		if (sounds == null)
			sounds = new CarriageSounds(this);
		sounds.tick(dce);

		if (particles == null)
			particles = new CarriageParticles(this);
		particles.tick(dce);

		double distanceTo = 0;
		if (!firstPositionUpdate) {
			Vec3d diff = getPos().subtract(prevX, prevY, prevZ);
			Vec3d relativeDiff = VecHelper.rotate(diff, yaw, Axis.Y);
			double signum = Math.signum(-relativeDiff.x);
			distanceTo = diff.length() * signum;
			movingBackwards = signum < 0;
		}

		carriage.bogeys.getFirst()
			.updateAngles(this, distanceTo);
		if (carriage.isOnTwoBogeys())
			carriage.bogeys.getSecond()
				.updateAngles(this, distanceTo);

		if (carriage.train.derailed)
			spawnDerailParticles(carriage);
		if (dce.pivot != null)
			spawnPortalParticles(dce);

		firstPositionUpdate = false;
		validForRender = true;
	}

	private void bindCarriage() {
		if (carriage != null)
			return;
		Train train = Create.RAILWAYS.sided(getWorld()).trains.get(trainId);
		if (train == null || train.carriages.size() <= carriageIndex)
			return;
		carriage = train.carriages.get(carriageIndex);
		if (carriage != null) {
			DimensionalCarriageEntity dimensional = carriage.getDimensional(getWorld());
			dimensional.entity = new WeakReference<>(this);
			dimensional.pivot = null;
			carriage.updateContraptionAnchors();
			dimensional.updateRenderedCutoff();
		}
		updateTrackGraph();
	}

	private void tickArrivalSound(CarriageContraption cc) {
		List<Carriage> carriages = carriage.train.carriages;

		if (arrivalSoundTicks == Integer.MIN_VALUE) {
			int carriageCount = carriages.size();
			Integer tick = null;

			for (int index = 0; index < carriageCount; index++) {
				int i = arrivalSoundReversed ? carriageCount - 1 - index : index;
				Carriage carriage = carriages.get(i);
				CarriageContraptionEntity entity = carriage.getDimensional(getWorld()).entity.get();
				if (entity == null || !(entity.contraption instanceof CarriageContraption otherCC))
					break;
				tick = arrivalSoundReversed ? otherCC.soundQueue.lastTick() : otherCC.soundQueue.firstTick();
				if (tick != null)
					break;
			}

			if (tick == null) {
				arrivalSoundPlaying = false;
				return;
			}

			arrivalSoundTicks = tick;
		}

		if (age % 2 == 0)
			return;

		boolean keepTicking = false;
		for (Carriage c : carriages) {
			CarriageContraptionEntity entity = c.getDimensional(getWorld()).entity.get();
			if (entity == null || !(entity.contraption instanceof CarriageContraption otherCC))
				continue;
			keepTicking |= otherCC.soundQueue.tick(entity, arrivalSoundTicks, arrivalSoundReversed);
		}

		if (!keepTicking) {
			arrivalSoundPlaying = false;
			return;
		}

		arrivalSoundTicks += arrivalSoundReversed ? -1 : 1;
	}

	@Override
	public void tickActors() {
		super.tickActors();
	}

	@Override
	protected boolean isActorActive(MovementContext context, MovementBehaviour actor) {
		if (!(contraption instanceof CarriageContraption cc))
			return false;
		if (!super.isActorActive(context, actor))
			return false;
		return cc.notInPortal() || getWorld().isClient();
	}

	@Override
	protected void handleStallInformation(double x, double y, double z, float angle) {}

	Vec3d derailParticleOffset;

	private void spawnDerailParticles(Carriage carriage) {
		if (random.nextFloat() < 1 / 20f) {
			Vec3d v = getPos().add(derailParticleOffset);
			getWorld().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, v.x, v.y, v.z, 0, .04, 0);
		}
	}

	@Override
	protected void addPassenger(Entity pPassenger) {
		super.addPassenger(pPassenger);
		if (!(pPassenger instanceof PlayerEntity player))
			return;
		player.getCustomData()
			.put("ContraptionMountLocation", VecHelper.writeNBT(player.getPos()));
	}

	private Set<BlockPos> particleSlice = new HashSet<>();
	private float particleAvgY = 0;

	private void spawnPortalParticles(DimensionalCarriageEntity dce) {
		Vec3d pivot = dce.pivot.getLocation()
			.add(0, 1.5f, 0);
		if (particleSlice.isEmpty())
			return;

		boolean alongX = MathHelper.approximatelyEquals(pivot.x, Math.round(pivot.x));
		int extraFlip = Direction.fromRotation(yaw)
			.getDirection()
			.offset();

		Vec3d emitter = pivot.add(0, particleAvgY, 0);
		double speed = getPos().distanceTo(getPrevPositionVec());
		int size = (int) (particleSlice.size() * MathHelper.clamp(4 - speed * 4, 0, 4));

		for (BlockPos pos : particleSlice) {
			if (size != 0 && random.nextInt(size) != 0)
				continue;
			if (alongX)
				pos = new BlockPos(0, pos.getY(), pos.getX());
			Vec3d v = pivot.add(pos.getX() * extraFlip, pos.getY(), pos.getZ() * extraFlip);
			CubeParticleData data =
				new CubeParticleData(.25f, 0, .5f, .65f + (random.nextFloat() - .5f) * .25f, 4, false);
			Vec3d m = v.subtract(emitter)
				.normalize()
				.multiply(.325f);
			m = VecHelper.rotate(m, random.nextFloat() * 360, alongX ? Axis.X : Axis.Z);
			m = m.add(VecHelper.offsetRandomly(Vec3d.ZERO, random, 0.25f));
			getWorld().addParticle(data, v.x, v.y, v.z, m.x, m.y, m.z);
		}

	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		carriageData = new CarriageSyncData();
		if (carriage != null) {
			DimensionalCarriageEntity dce = carriage.getDimensional(getWorld());
			dce.pointsInitialised = false;
			carriage.leadingBogey().couplingAnchors = Couple.create(null, null);
			carriage.trailingBogey().couplingAnchors = Couple.create(null, null);
		}
		firstPositionUpdate = true;
		if (sounds != null)
			sounds.stop();
	}

	@Override
	protected void writeAdditional(NbtCompound compound, boolean spawnPacket) {
		super.writeAdditional(compound, spawnPacket);
		compound.putUuid("TrainId", trainId);
		compound.putInt("CarriageIndex", carriageIndex);
	}

	@Override
	protected void readAdditional(NbtCompound compound, boolean spawnPacket) {
		super.readAdditional(compound, spawnPacket);
		trainId = compound.getUuid("TrainId");
		carriageIndex = compound.getInt("CarriageIndex");
		if (spawnPacket) {
			lastRenderX = getX();
			lastRenderY = getY();
			lastRenderZ = getZ();
		}
	}

	@Override
	public Text getContraptionName() {
		if (carriage != null)
			return carriage.train.name;
		Text contraptionName = super.getContraptionName();
		return contraptionName;
	}

	public Couple<Boolean> checkConductors() {
		Couple<Boolean> sides = Couple.create(false, false);
		if (!(contraption instanceof CarriageContraption cc))
			return sides;

		sides.setFirst(cc.blazeBurnerConductors.getFirst());
		sides.setSecond(cc.blazeBurnerConductors.getSecond());

		for (Entity entity : getPassengerList()) {
			if (entity instanceof PlayerEntity)
				continue;
			BlockPos seatOf = cc.getSeatOf(entity.getUuid());
			if (seatOf == null)
				continue;
			Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
			if (validSides == null)
				continue;
			sides.setFirst(sides.getFirst() || validSides.getFirst());
			sides.setSecond(sides.getSecond() || validSides.getSecond());
		}

		return sides;
	}

	@Override
	public boolean startControlling(BlockPos controlsLocalPos, PlayerEntity player) {
		if (player == null || player.isSpectator())
			return false;
		if (carriage == null)
			return false;
		if (carriage.train.derailed)
			return false;

		Train train = carriage.train;
		if (train.runtime.getSchedule() != null && !train.runtime.paused)
			train.status.manualControls();
		train.navigation.cancelNavigation();
		train.runtime.paused = true;
		train.navigation.waitingForSignal = null;
		return true;
	}

	@Override
	public Text getDisplayName() {
		if (carriage == null)
			return Lang.translateDirect("train");
		return carriage.train.name;
	}

	double navDistanceTotal = 0;
	int hudPacketCooldown = 0;

	@Override
	public boolean control(BlockPos controlsLocalPos, Collection<Integer> heldControls, PlayerEntity player) {
		if (carriage == null)
			return false;
		if (carriage.train.derailed)
			return false;
		if (getWorld().isClient)
			return true;
		if (player.isSpectator())
			return false;
		if (!toGlobalVector(VecHelper.getCenterOf(controlsLocalPos), 1).isInRange(player.getPos(), 8))
			return false;
		if (heldControls.contains(5))
			return false;

		StructureBlockInfo info = contraption.getBlocks()
			.get(controlsLocalPos);
		Direction initialOrientation = getInitialOrientation().rotateYCounterclockwise();
		boolean inverted = false;
		if (info != null && info.state().contains(ControlsBlock.FACING))
			inverted = !info.state().get(ControlsBlock.FACING)
				.equals(initialOrientation);

		if (hudPacketCooldown-- <= 0 && player instanceof ServerPlayerEntity sp) {
			AllPackets.getChannel().sendToClient(new TrainHUDUpdatePacket(carriage.train), sp);
			hudPacketCooldown = 5;
		}

		int targetSpeed = 0;
		if (heldControls.contains(0))
			targetSpeed++;
		if (heldControls.contains(1))
			targetSpeed--;

		int targetSteer = 0;
		if (heldControls.contains(2))
			targetSteer++;
		if (heldControls.contains(3))
			targetSteer--;

		if (inverted) {
			targetSpeed *= -1;
			targetSteer *= -1;
		}

		if (targetSpeed != 0)
			carriage.train.burnFuel();

		boolean slow = inverted ^ targetSpeed < 0;
		boolean spaceDown = heldControls.contains(4);
		GlobalStation currentStation = carriage.train.getCurrentStation();
		if (currentStation != null && spaceDown) {
			sendPrompt(player, Lang.translateDirect("train.arrived_at",
				Components.literal(currentStation.name).styled(s -> s.withColor(0x704630))), false);
			return true;
		}

		if (carriage.train.speedBeforeStall != null && targetSpeed != 0
			&& Math.signum(carriage.train.speedBeforeStall) != Math.signum(targetSpeed)) {
			carriage.train.cancelStall();
		}

		if (currentStation != null && targetSpeed != 0) {
			stationMessage = false;
			sendPrompt(player, Lang.translateDirect("train.departing_from",
				Components.literal(currentStation.name).styled(s -> s.withColor(0x704630))), false);
		}

		if (currentStation == null) {

			Navigation nav = carriage.train.navigation;
			if (nav.destination != null) {
				if (!spaceDown)
					nav.cancelNavigation();
				if (spaceDown) {
					double f = (nav.distanceToDestination / navDistanceTotal);
					int progress = (int) (MathHelper.clamp(1 - ((1 - f) * (1 - f)), 0, 1) * 30);
					boolean arrived = progress == 0;
					MutableText whiteComponent = Components.literal(Strings.repeat("|", progress));
					MutableText greenComponent = Components.literal(Strings.repeat("|", 30 - progress));

					int fromColor = 0x00_FFC244;
					int toColor = 0x00_529915;

					int mixedColor = Color.mixColors(toColor, fromColor, progress / 30f);
					int targetColor = arrived ? toColor : 0x00_544D45;

					MutableText component = greenComponent.styled(st -> st.withColor(mixedColor))
						.append(whiteComponent.styled(st -> st.withColor(targetColor)));
					sendPrompt(player, component, true);
					carriage.train.manualTick = true;
					return true;
				}
			}

			double directedSpeed = targetSpeed != 0 ? targetSpeed : carriage.train.speed;
			GlobalStation lookAhead = nav.findNearestApproachable(
				!carriage.train.doubleEnded || (directedSpeed != 0 ? directedSpeed > 0 : !inverted));

			if (lookAhead != null) {
				if (spaceDown) {
					carriage.train.manualTick = true;
					nav.startNavigation(nav.findPathTo(lookAhead, -1));
					carriage.train.manualTick = false;
					navDistanceTotal = nav.distanceToDestination;
					return true;
				}
				displayApproachStationMessage(player, lookAhead);
			} else
				cleanUpApproachStationMessage(player);
		}

		carriage.train.manualSteer =
			targetSteer < 0 ? SteerDirection.RIGHT : targetSteer > 0 ? SteerDirection.LEFT : SteerDirection.NONE;

		double topSpeed = carriage.train.maxSpeed() * AllConfigs.server().trains.manualTrainSpeedModifier.getF();
		double cappedTopSpeed = topSpeed * carriage.train.throttle;

		if (carriage.getLeadingPoint().edge != null && carriage.getLeadingPoint().edge.isTurn()
			|| carriage.getTrailingPoint().edge != null && carriage.getTrailingPoint().edge.isTurn())
			topSpeed = carriage.train.maxTurnSpeed();

		if (slow)
			topSpeed /= 4;
		carriage.train.targetSpeed = Math.min(topSpeed, cappedTopSpeed) * targetSpeed;

		boolean counteringAcceleration = Math.abs(Math.signum(targetSpeed) - Math.signum(carriage.train.speed)) > 1.5f;

		if (slow && !counteringAcceleration)
			carriage.train.backwardsDriver = player;

		carriage.train.manualTick = true;
		carriage.train.approachTargetSpeed(counteringAcceleration ? 2 : 1);
		return true;
	}

	private void sendPrompt(PlayerEntity player, MutableText component, boolean shadow) {
		if (player instanceof ServerPlayerEntity sp)
			AllPackets.getChannel().sendToClient(new TrainPromptPacket(component, shadow), sp);
	}

	boolean stationMessage = false;

	private void displayApproachStationMessage(PlayerEntity player, GlobalStation station) {
		sendPrompt(player, Lang.translateDirect("contraption.controls.approach_station",
			Components.keybind("key.jump"), station.name), false);
		stationMessage = true;
	}

	private void cleanUpApproachStationMessage(PlayerEntity player) {
		if (!stationMessage)
			return;
		player.sendMessage(Components.immutableEmpty(), true);
		stationMessage = false;
	}

	private void updateTrackGraph() {
		if (carriage == null)
			return;
		Optional<UUID> optional = dataTracker.get(TRACK_GRAPH);
		if (optional.isEmpty()) {
			carriage.train.graph = null;
			carriage.train.derailed = true;
			return;
		}

		TrackGraph graph = CreateClient.RAILWAYS.sided(getWorld()).trackNetworks.get(optional.get());
		if (graph == null)
			return;
		carriage.train.graph = graph;
		carriage.train.derailed = false;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	public Carriage getCarriage() {
		return carriage;
	}

	public void setCarriage(Carriage carriage) {
		this.carriage = carriage;
		this.trainId = carriage.train.id;
		this.carriageIndex = carriage.train.carriages.indexOf(carriage);
		if (contraption instanceof CarriageContraption cc)
			cc.swapStorageAfterAssembly(this);
		if (carriage.train.graph != null)
			dataTracker.set(TRACK_GRAPH, Optional.of(carriage.train.graph.id));

		DimensionalCarriageEntity dimensional = carriage.getDimensional(getWorld());
		dimensional.pivot = null;
		carriage.updateContraptionAnchors();
		dimensional.updateRenderedCutoff();
	}

	@Environment(EnvType.CLIENT)
	private WeakReference<CarriageContraptionInstance> instanceHolder;

	@Environment(EnvType.CLIENT)
	public void bindInstance(CarriageContraptionInstance instance) {
		this.instanceHolder = new WeakReference<>(instance);
		updateRenderedPortalCutoff();
	}

	@Environment(EnvType.CLIENT)
	public void updateRenderedPortalCutoff() {
		if (carriage == null)
			return;

		// update portal slice
		particleSlice.clear();
		particleAvgY = 0;

		if (contraption instanceof CarriageContraption cc) {
			Direction forward = cc.getAssemblyDirection()
				.rotateYClockwise();
			Axis axis = forward.getAxis();
			boolean x = axis == Axis.X;
			boolean flip = true;

			for (BlockPos pos : contraption.getBlocks()
				.keySet()) {
				if (!cc.atSeam(pos))
					continue;
				int pX = x ? pos.getX() : pos.getZ();
				pX *= forward.getDirection()
					.offset() * (flip ? 1 : -1);
				pos = new BlockPos(pX, pos.getY(), 0);
				particleSlice.add(pos);
				particleAvgY += pos.getY();
			}

		}
		if (particleSlice.size() > 0)
			particleAvgY /= particleSlice.size();

		// update hidden bogeys (if instanced)
		if (instanceHolder == null)
			return;
		CarriageContraptionInstance instance = instanceHolder.get();
		if (instance == null)
			return;

		int bogeySpacing = carriage.bogeySpacing;

		// fabric: do not pass instance to lambda, class loading issues
		Couple<Boolean> bogeyVisibility = carriage.bogeys.map(bogey -> {
			if (bogey == null)
				return null;

			BlockPos bogeyPos = bogey.isLeading ? BlockPos.ORIGIN
					: BlockPos.ORIGIN.offset(getInitialOrientation().rotateYCounterclockwise(), bogeySpacing);
			return !contraption.isHiddenInPortal(bogeyPos);
		});
		for (boolean first : Iterate.trueAndFalse) {
			Boolean visible = bogeyVisibility.get(first);
			if (visible != null)
				instance.setBogeyVisibility(first, visible);
		}
	}

}
