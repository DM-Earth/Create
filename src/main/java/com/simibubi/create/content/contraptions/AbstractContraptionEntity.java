package com.simibubi.create.content.contraptions;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.foundation.utility.AdventureUtil;

import io.github.fabricators_of_create.porting_lib.entity.IEntityAdditionalSpawnData;
import io.github.fabricators_of_create.porting_lib.entity.PortingLibEntity;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.EntityAccessor;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.MutablePair;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsStopControllingPacket;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.sync.ContraptionSeatMappingPacket;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.mixin.accessor.ServerLevelAccessor;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractContraptionEntity extends Entity implements IEntityAdditionalSpawnData {

	private static final TrackedData<Boolean> STALLED =
		DataTracker.registerData(AbstractContraptionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Optional<UUID>> CONTROLLED_BY =
		DataTracker.registerData(AbstractContraptionEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

	public final Map<Entity, MutableInt> collidingEntities;

	protected Contraption contraption;
	protected boolean initialized;
	protected boolean prevPosInvalid;
	private boolean skipActorStop;

	/*
	 * staleTicks are a band-aid to prevent a frame or two of missing blocks between
	 * contraption discard and off-thread block placement on disassembly
	 *
	 * FIXME this timeout should be longer but then also cancelled early based on a
	 * chunk rebuild listener
	 */
	public int staleTicks = 3;

	public AbstractContraptionEntity(EntityType<?> entityTypeIn, World worldIn) {
		super(entityTypeIn, worldIn);
		prevPosInvalid = true;
		collidingEntities = new IdentityHashMap<>();
	}

	protected void setContraption(Contraption contraption) {
		this.contraption = contraption;
		if (contraption == null)
			return;
		if (getWorld().isClient)
			return;
		contraption.onEntityCreated(this);
	}

	@Override
	public void move(MovementType pType, Vec3d pPos) {
		if (pType == MovementType.SHULKER)
			return;
		if (pType == MovementType.SHULKER_BOX)
			return;
		if (pType == MovementType.PISTON)
			return;
		super.move(pType, pPos);
	}

	public boolean supportsTerrainCollision() {
		return contraption instanceof TranslatingContraption && !(contraption instanceof ElevatorContraption);
	}

	protected void contraptionInitialize() {
		contraption.onEntityInitialize(getWorld(), this);
		initialized = true;
	}

	public boolean collisionEnabled() {
		return true;
	}

	public void registerColliding(Entity collidingEntity) {
		collidingEntities.put(collidingEntity, new MutableInt());
	}

	public void addSittingPassenger(Entity passenger, int seatIndex) {
		for (Entity entity : getPassengerList()) {
			BlockPos seatOf = contraption.getSeatOf(entity.getUuid());
			if (seatOf != null && seatOf.equals(contraption.getSeats()
				.get(seatIndex))) {
				if (entity instanceof PlayerEntity)
					return;
				if (!(passenger instanceof PlayerEntity))
					return;
				entity.stopRiding();
			}
		}
		passenger.startRiding(this, true);
		if (passenger instanceof TameableEntity ta)
			ta.setInSittingPose(true);
		if (getWorld().isClient)
			return;
		contraption.getSeatMapping()
			.put(passenger.getUuid(), seatIndex);
		AllPackets.getChannel().sendToClientsTracking(
			new ContraptionSeatMappingPacket(getId(), contraption.getSeatMapping()), this);
	}

	@Override
	protected void removePassenger(Entity passenger) {
		// fabric: when a passenger is present, passengers are synced too early. Everything seems to behave fine anyway if this is ignored.
		// This is not an issue on forge because extra data is directly part of the spawn packet.
		// there's some ordering weirdness that I spent a couple hours debugging and gave up on.
		if (this.contraption == null)
			return;
		Vec3d transformedVector = getPassengerPosition(passenger, 1);
		super.removePassenger(passenger);
		if (passenger instanceof TameableEntity ta)
			ta.setInSittingPose(false);
		if (getWorld().isClient)
			return;
		if (transformedVector != null)
			passenger.getCustomData()
				.put("ContraptionDismountLocation", VecHelper.writeNBT(transformedVector));
		contraption.getSeatMapping()
			.remove(passenger.getUuid());
		AllPackets.getChannel().sendToClientsTracking(
			new ContraptionSeatMappingPacket(getId(), contraption.getSeatMapping(), passenger.getId()), this);
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity entityLiving) {
		Vec3d position = super.updatePassengerForDismount(entityLiving);
		NbtCompound data = entityLiving.getCustomData();
		if (!data.contains("ContraptionDismountLocation"))
			return position;

		position = VecHelper.readNBT(data.getList("ContraptionDismountLocation", NbtElement.DOUBLE_TYPE));
		data.remove("ContraptionDismountLocation");
		entityLiving.setOnGround(false);

		if (!data.contains("ContraptionMountLocation"))
			return position;

		Vec3d prevPosition = VecHelper.readNBT(data.getList("ContraptionMountLocation", NbtElement.DOUBLE_TYPE));
		data.remove("ContraptionMountLocation");
		if (entityLiving instanceof PlayerEntity player && !prevPosition.isInRange(position, 5000))
			AllAdvancements.LONG_TRAVEL.awardTo(player);
		return position;
	}

	@Override
	public void updatePassengerPosition(Entity passenger, PositionUpdater callback) {
		if (!hasPassenger(passenger))
			return;
		Vec3d transformedVector = getPassengerPosition(passenger, 1);
		if (transformedVector == null)
			return;
		callback.accept(passenger, transformedVector.x,
			transformedVector.y + SeatEntity.getCustomEntitySeatOffset(passenger) - 1 / 8f, transformedVector.z);
	}

	public Vec3d getPassengerPosition(Entity passenger, float partialTicks) {
		if (contraption == null)
			return null;

		UUID id = passenger.getUuid();
		if (passenger instanceof OrientedContraptionEntity) {
			BlockPos localPos = contraption.getBearingPosOf(id);
			if (localPos != null)
				return toGlobalVector(VecHelper.getCenterOf(localPos), partialTicks)
					.add(VecHelper.getCenterOf(BlockPos.ORIGIN))
					.subtract(.5f, 1, .5f);
		}

		Box bb = passenger.getBoundingBox();
		double ySize = bb.getYLength();
		BlockPos seat = contraption.getSeatOf(id);
		if (seat == null)
			return null;

		Vec3d transformedVector = toGlobalVector(Vec3d.of(seat)
			.add(.5, passenger.getHeightOffset() + ySize - .15f, .5), partialTicks)
				.add(VecHelper.getCenterOf(BlockPos.ORIGIN))
				.subtract(0.5, ySize, 0.5);
		return transformedVector;
	}

	@Override
	protected boolean canAddPassenger(Entity p_184219_1_) {
		if (p_184219_1_ instanceof OrientedContraptionEntity)
			return true;
		return contraption.getSeatMapping()
			.size() < contraption.getSeats()
				.size();
	}

	public Text getContraptionName() {
		return getName();
	}

	public Optional<UUID> getControllingPlayer() {
		return dataTracker.get(CONTROLLED_BY);
	}

	public void setControllingPlayer(@Nullable UUID playerId) {
		dataTracker.set(CONTROLLED_BY, Optional.ofNullable(playerId));
	}

	public boolean startControlling(BlockPos controlsLocalPos, PlayerEntity player) {
		return false;
	}

	public boolean control(BlockPos controlsLocalPos, Collection<Integer> heldControls, PlayerEntity player) {
		return true;
	}

	public void stopControlling(BlockPos controlsLocalPos) {
		getControllingPlayer().map(getWorld()::getPlayerByUuid)
			.map(p -> (p instanceof ServerPlayerEntity) ? ((ServerPlayerEntity) p) : null)
			.ifPresent(p -> AllPackets.getChannel().sendToClient(new ControlsStopControllingPacket(),
				p));
		setControllingPlayer(null);
	}

	public boolean handlePlayerInteraction(PlayerEntity player, BlockPos localPos, Direction side,
		Hand interactionHand) {
		int indexOfSeat = contraption.getSeats()
			.indexOf(localPos);
		if (indexOfSeat == -1 || AllItems.WRENCH.isIn(player.getStackInHand(interactionHand))) {
			if (contraption.interactors.containsKey(localPos))
				return contraption.interactors.get(localPos)
					.handlePlayerInteraction(player, interactionHand, localPos, this);
			return contraption.storage.handlePlayerStorageInteraction(contraption, player, localPos);
		}
		if (player.hasVehicle())
			return false;

		// Eject potential existing passenger
		Entity toDismount = null;
		for (Entry<UUID, Integer> entry : contraption.getSeatMapping()
			.entrySet()) {
			if (entry.getValue() != indexOfSeat)
				continue;
			for (Entity entity : getPassengerList()) {
				if (!entry.getKey()
					.equals(entity.getUuid()))
					continue;
				if (entity instanceof PlayerEntity)
					return false;
				toDismount = entity;
			}
		}

		if (toDismount != null && AdventureUtil.isAdventure(player))
			return false;

		if (toDismount != null && !getWorld().isClient) {
			Vec3d transformedVector = getPassengerPosition(toDismount, 1);
			toDismount.stopRiding();
			if (transformedVector != null)
				toDismount.requestTeleport(transformedVector.x, transformedVector.y, transformedVector.z);
		}

		if (getWorld().isClient)
			return true;
		addSittingPassenger(SeatBlock.getLeashed(getWorld(), player)
			.or(player), indexOfSeat);
		return true;
	}

	public Vec3d toGlobalVector(Vec3d localVec, float partialTicks) {
		return toGlobalVector(localVec, partialTicks, false);
	}

	public Vec3d toGlobalVector(Vec3d localVec, float partialTicks, boolean prevAnchor) {
		Vec3d anchor = prevAnchor ? getPrevAnchorVec() : getAnchorVec();
		Vec3d rotationOffset = VecHelper.getCenterOf(BlockPos.ORIGIN);
		localVec = localVec.subtract(rotationOffset);
		localVec = applyRotation(localVec, partialTicks);
		localVec = localVec.add(rotationOffset)
			.add(anchor);
		return localVec;
	}

	public Vec3d toLocalVector(Vec3d localVec, float partialTicks) {
		return toLocalVector(localVec, partialTicks, false);
	}

	public Vec3d toLocalVector(Vec3d globalVec, float partialTicks, boolean prevAnchor) {
		Vec3d anchor = prevAnchor ? getPrevAnchorVec() : getAnchorVec();
		Vec3d rotationOffset = VecHelper.getCenterOf(BlockPos.ORIGIN);
		globalVec = globalVec.subtract(anchor)
			.subtract(rotationOffset);
		globalVec = reverseRotation(globalVec, partialTicks);
		globalVec = globalVec.add(rotationOffset);
		return globalVec;
	}

	@Override
	public void tick() {
		if (contraption == null) {
			discard();
			return;
		}

		collidingEntities.entrySet()
			.removeIf(e -> e.getValue()
				.incrementAndGet() > 3);

		prevX = getX();
		prevY = getY();
		prevZ = getZ();
		prevPosInvalid = false;

		if (!initialized)
			contraptionInitialize();

		contraption.tickStorage(this);
		tickContraption();
		super.tick();

		if (getWorld().isClient())
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
				if (!contraption.deferInvalidate)
					return;
				contraption.deferInvalidate = false;
				ContraptionRenderDispatcher.invalidate(contraption);
			});

		if (!(getWorld() instanceof ServerLevelAccessor sl))
			return;

		for (Entity entity : getPassengerList()) {
			if (entity instanceof PlayerEntity)
				continue;
			if (entity.isPlayer())
				continue;
			if (sl.create$getEntityList()
				.has(entity))
				continue;
			updatePassengerPosition(entity);
		}
	}

	public void alignPassenger(Entity passenger) {
		Vec3d motion = getContactPointMotion(passenger.getEyePos());
		if (MathHelper.approximatelyEquals(motion.length(), 0))
			return;
		if (passenger instanceof ArmorStandEntity)
			return;
		if (!(passenger instanceof LivingEntity living))
			return;
		float prevAngle = living.getYaw();
		float angle = AngleHelper.deg(-MathHelper.atan2(motion.x, motion.z));
		angle = AngleHelper.angleLerp(0.4f, prevAngle, angle);
		if (getWorld().isClient) {
			living.updateTrackedPositionAndAngles(0, 0, 0, 0, 0, 0, false);
			living.updateTrackedHeadRotation(0, 0);
			living.setYaw(angle);
			living.setPitch(0);
			living.bodyYaw = angle;
			living.headYaw = angle;
		} else
			living.setYaw(angle);
	}

	public void setBlock(BlockPos localPos, StructureBlockInfo newInfo) {
		contraption.blocks.put(localPos, newInfo);
		AllPackets.getChannel().sendToClientsTracking(new ContraptionBlockChangedPacket(getId(), localPos, newInfo.state()), this);
	}

	protected abstract void tickContraption();

	public abstract Vec3d applyRotation(Vec3d localPos, float partialTicks);

	public abstract Vec3d reverseRotation(Vec3d localPos, float partialTicks);

	public void tickActors() {
		boolean stalledPreviously = contraption.stalled;

		if (!getWorld().isClient)
			contraption.stalled = false;

		skipActorStop = true;
		for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
			MovementContext context = pair.right;
			StructureBlockInfo blockInfo = pair.left;
			MovementBehaviour actor = AllMovementBehaviours.getBehaviour(blockInfo.state());

			if (actor == null)
				continue;

			Vec3d oldMotion = context.motion;
			Vec3d actorPosition = toGlobalVector(VecHelper.getCenterOf(blockInfo.pos())
				.add(actor.getActiveAreaOffset(context)), 1);
			BlockPos gridPosition = BlockPos.ofFloored(actorPosition);
			boolean newPosVisited =
				!context.stall && shouldActorTrigger(context, blockInfo, actor, actorPosition, gridPosition);

			context.rotation = v -> applyRotation(v, 1);
			context.position = actorPosition;
			if (!isActorActive(context, actor) && !actor.mustTickWhileDisabled())
				continue;
			if (newPosVisited && !context.stall) {
				actor.visitNewPosition(context, gridPosition);
				if (!isAlive())
					break;
				context.firstMovement = false;
			}
			if (!oldMotion.equals(context.motion)) {
				actor.onSpeedChanged(context, oldMotion, context.motion);
				if (!isAlive())
					break;
			}
			actor.tick(context);
			if (!isAlive())
				break;
			contraption.stalled |= context.stall;
		}
		if (!isAlive()) {
			contraption.stop(getWorld());
			return;
		}
		skipActorStop = false;

		for (Entity entity : getPassengerList()) {
			if (!(entity instanceof OrientedContraptionEntity))
				continue;
			if (!contraption.stabilizedSubContraptions.containsKey(entity.getUuid()))
				continue;
			OrientedContraptionEntity orientedCE = (OrientedContraptionEntity) entity;
			if (orientedCE.contraption != null && orientedCE.contraption.stalled) {
				contraption.stalled = true;
				break;
			}
		}

		if (!getWorld().isClient) {
			if (!stalledPreviously && contraption.stalled)
				onContraptionStalled();
			dataTracker.set(STALLED, contraption.stalled);
			return;
		}

		contraption.stalled = isStalled();
	}

	public void refreshPSIs() {
		for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
			MovementContext context = pair.right;
			StructureBlockInfo blockInfo = pair.left;
			MovementBehaviour actor = AllMovementBehaviours.getBehaviour(blockInfo.state());
			if (actor instanceof PortableStorageInterfaceMovement && isActorActive(context, actor))
				if (context.position != null)
					actor.visitNewPosition(context, BlockPos.ofFloored(context.position));
		}
	}

	protected boolean isActorActive(MovementContext context, MovementBehaviour actor) {
		return actor.isActive(context);
	}

	protected void onContraptionStalled() {
		AllPackets.getChannel().sendToClientsTracking(
			new ContraptionStallPacket(getId(), getX(), getY(), getZ(), getStalledAngle()), this);
	}

	protected boolean shouldActorTrigger(MovementContext context, StructureBlockInfo blockInfo, MovementBehaviour actor,
		Vec3d actorPosition, BlockPos gridPosition) {
		Vec3d previousPosition = context.position;
		if (previousPosition == null)
			return false;

		context.motion = actorPosition.subtract(previousPosition);

		if (!getWorld().isClient() && context.contraption.entity instanceof CarriageContraptionEntity cce
			&& cce.getCarriage() != null) {
			Train train = cce.getCarriage().train;
			double actualSpeed = train.speedBeforeStall != null ? train.speedBeforeStall : train.speed;
			context.motion = context.motion.normalize()
				.multiply(Math.abs(actualSpeed));
		}

		Vec3d relativeMotion = context.motion;
		relativeMotion = reverseRotation(relativeMotion, 1);
		context.relativeMotion = relativeMotion;

		return !BlockPos.ofFloored(previousPosition).equals(gridPosition)
			|| (context.relativeMotion.length() > 0 || context.contraption instanceof CarriageContraption)
				&& context.firstMovement;
	}

	public void move(double x, double y, double z) {
		setPosition(getX() + x, getY() + y, getZ() + z);
	}

	public Vec3d getAnchorVec() {
		return getPos();
	}

	public Vec3d getPrevAnchorVec() {
		return getPrevPositionVec();
	}

	public float getYawOffset() {
		return 0;
	}

	@Override
	public void setPosition(double x, double y, double z) {
		super.setPosition(x, y, z);
		if (contraption == null)
			return;
		Box cbox = contraption.bounds;
		if (cbox == null)
			return;
		Vec3d actualVec = getAnchorVec();
		setBoundingBox(cbox.offset(actualVec));
	}

	public static float yawFromVector(Vec3d vec) {
		return (float) ((3 * Math.PI / 2 + Math.atan2(vec.z, vec.x)) / Math.PI * 180);
	}

	public static float pitchFromVector(Vec3d vec) {
		return (float) ((Math.acos(vec.y)) / Math.PI * 180);
	}

	public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
//		@SuppressWarnings("unchecked")
//		EntityType.Builder<AbstractContraptionEntity> entityBuilder =
//			(EntityType.Builder<AbstractContraptionEntity>) builder;
		return builder.dimensions(EntityDimensions.fixed(1, 1));
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(STALLED, false);
		this.dataTracker.startTracking(CONTROLLED_BY, Optional.empty());
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return PortingLibEntity.getEntitySpawningPacket(this);
	}

	@Override
	public void writeSpawnData(PacketByteBuf buffer) {
		NbtCompound compound = new NbtCompound();
		writeAdditional(compound, true);

		if (ContraptionData.isTooLargeForSync(compound)) {
			String info = getContraption().getType().id + " @" + getPos() + " (" + getUuidAsString() + ")";
			Create.LOGGER.warn("Could not send Contraption Spawn Data (Packet too big): " + info);
			compound = null;
		}

		buffer.writeNbt(compound);
	}

	@Override
	protected final void writeCustomDataToNbt(NbtCompound compound) {
		writeAdditional(compound, false);
	}

	protected void writeAdditional(NbtCompound compound, boolean spawnPacket) {
		if (contraption != null)
			compound.put("Contraption", contraption.writeNBT(spawnPacket));
		compound.putBoolean("Stalled", isStalled());
		compound.putBoolean("Initialized", initialized);
	}

	@Override
	public void readSpawnData(PacketByteBuf additionalData) {
		NbtCompound nbt = additionalData.readUnlimitedNbt();
		if (nbt != null) {
			readAdditional(nbt, true);
		}
	}

	@Override
	protected final void readCustomDataFromNbt(NbtCompound compound) {
		readAdditional(compound, false);
	}

	protected void readAdditional(NbtCompound compound, boolean spawnData) {
		if (compound.isEmpty())
			return;

		initialized = compound.getBoolean("Initialized");
		contraption = Contraption.fromNBT(getWorld(), compound.getCompound("Contraption"), spawnData);
		contraption.entity = this;
		dataTracker.set(STALLED, compound.getBoolean("Stalled"));
	}

	public void disassemble() {
		if (!isAlive())
			return;
		if (contraption == null)
			return;

		StructureTransform transform = makeStructureTransform();

		contraption.stop(getWorld());
		AllPackets.getChannel().sendToClientsTracking(
			new ContraptionDisassemblyPacket(this.getId(), transform), this);

		contraption.addBlocksToWorld(getWorld(), transform);
		contraption.addPassengersToWorld(getWorld(), transform, getPassengerList());

		for (Entity entity : getPassengerList()) {
			if (!(entity instanceof OrientedContraptionEntity))
				continue;
			UUID id = entity.getUuid();
			if (!contraption.stabilizedSubContraptions.containsKey(id))
				continue;
			BlockPos transformed = transform.apply(contraption.stabilizedSubContraptions.get(id)
				.getConnectedPos());
			entity.setPosition(transformed.getX(), transformed.getY(), transformed.getZ());
			((AbstractContraptionEntity) entity).disassemble();
		}

		skipActorStop = true;
		discard();

		removeAllPassengers();
		moveCollidedEntitiesOnDisassembly(transform);
		AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(getWorld(), getBlockPos());
	}

	private void moveCollidedEntitiesOnDisassembly(StructureTransform transform) {
		for (Entity entity : collidingEntities.keySet()) {
			Vec3d localVec = toLocalVector(entity.getPos(), 0);
			Vec3d transformed = transform.apply(localVec);
			if (getWorld().isClient)
				entity.setPosition(transformed.x, transformed.y + 1 / 16f, transformed.z);
			else
				entity.requestTeleport(transformed.x, transformed.y + 1 / 16f, transformed.z);
		}
	}

	@Override
	public void remove(RemovalReason p_146834_) {
		if (!getWorld().isClient && !isRemoved() && contraption != null && !skipActorStop)
			contraption.stop(getWorld());
		if (contraption != null)
			contraption.onEntityRemoved(this);
		super.remove(p_146834_);
	}

	protected abstract StructureTransform makeStructureTransform();

	@Override
	public void kill() {
		removeAllPassengers();
		super.kill();
	}

	@Override
	protected void tickInVoid() {
		removeAllPassengers();
		super.tickInVoid();
	}

	@Override
	protected void onSwimmingStart() {}

	public Contraption getContraption() {
		return contraption;
	}

	public boolean isStalled() {
		return dataTracker.get(STALLED);
	}

	@Environment(EnvType.CLIENT)
	static void handleStallPacket(ContraptionStallPacket packet) {
		if (MinecraftClient.getInstance().world.getEntityById(packet.entityID) instanceof AbstractContraptionEntity ce)
			ce.handleStallInformation(packet.x, packet.y, packet.z, packet.angle);
	}

	@Environment(EnvType.CLIENT)
	static void handleBlockChangedPacket(ContraptionBlockChangedPacket packet) {
		if (MinecraftClient.getInstance().world.getEntityById(packet.entityID) instanceof AbstractContraptionEntity ce)
			ce.handleBlockChange(packet.localPos, packet.newState);
	}

	@Environment(EnvType.CLIENT)
	static void handleDisassemblyPacket(ContraptionDisassemblyPacket packet) {
		if (MinecraftClient.getInstance().world.getEntityById(packet.entityID) instanceof AbstractContraptionEntity ce)
			ce.moveCollidedEntitiesOnDisassembly(packet.transform);
	}

	protected abstract float getStalledAngle();

	protected abstract void handleStallInformation(double x, double y, double z, float angle);

	@Environment(EnvType.CLIENT)
	protected void handleBlockChange(BlockPos localPos, BlockState newState) {
		if (contraption == null || !contraption.blocks.containsKey(localPos))
			return;
		StructureBlockInfo info = contraption.blocks.get(localPos);
		contraption.blocks.put(localPos, new StructureBlockInfo(info.pos(), newState, info.nbt()));
		if (info.state() != newState && !(newState.getBlock() instanceof SlidingDoorBlock))
			contraption.deferInvalidate = true;
		contraption.invalidateColliders();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		Vec3d vec = getPos();
		List<Entity> passengers = getPassengerList();

		for (Entity entity : passengers) {
			// setPos has world accessing side-effects when removed == null
			((EntityAccessor) entity).port_lib$setRemovalReason(RemovalReason.UNLOADED_TO_CHUNK);

			// Gather passengers into same chunk when saving
			Vec3d prevVec = entity.getPos();
			entity.setPos(vec.x, prevVec.y, vec.z);

			// Super requires all passengers to not be removed in order to write them to the
			// tag
			((EntityAccessor) entity).port_lib$setRemovalReason(null);
		}

		NbtCompound tag = super.writeNbt(nbt);
		return tag;
	}

	@Override
	// Make sure nothing can move contraptions out of the way
	public void setVelocity(Vec3d motionIn) {}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	public void setContraptionMotion(Vec3d vec) {
		super.setVelocity(vec);
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	public Vec3d getPrevPositionVec() {
		return prevPosInvalid ? getPos() : new Vec3d(prevX, prevY, prevZ);
	}

	public abstract ContraptionRotationState getRotationState();

	public Vec3d getContactPointMotion(Vec3d globalContactPoint) {
		if (prevPosInvalid)
			return Vec3d.ZERO;

		Vec3d contactPoint = toGlobalVector(toLocalVector(globalContactPoint, 0, true), 1, true);
		Vec3d contraptionLocalMovement = contactPoint.subtract(globalContactPoint);
		Vec3d contraptionAnchorMovement = getPos().subtract(getPrevPositionVec());
		return contraptionLocalMovement.add(contraptionAnchorMovement);
	}

	public boolean collidesWith(Entity e) {
		if (e instanceof PlayerEntity && e.isSpectator())
			return false;
		if (e.noClip)
			return false;
		if (e instanceof AbstractDecorationEntity)
			return false;
		if (e instanceof AbstractMinecartEntity)
			return !(contraption instanceof MountedContraption);
		if (e instanceof SuperGlueEntity)
			return false;
		if (e instanceof SeatEntity)
			return false;
		if (e instanceof ProjectileEntity)
			return false;
		if (e.getVehicle() != null)
			return false;

		Entity riding = this.getVehicle();
		while (riding != null) {
			if (riding == e)
				return false;
			riding = riding.getVehicle();
		}

		return e.getPistonBehavior() == PistonBehavior.NORMAL;
	}

	@Override
	public boolean hasPlayerRider() {
		return false;
	}

	@Environment(EnvType.CLIENT)
	public abstract void applyLocalTransforms(MatrixStack matrixStack, float partialTicks);

	public static class ContraptionRotationState {
		public static final ContraptionRotationState NONE = new ContraptionRotationState();

		float xRotation = 0;
		float yRotation = 0;
		float zRotation = 0;
		float secondYRotation = 0;
		Matrix3d matrix;

		public Matrix3d asMatrix() {
			if (matrix != null)
				return matrix;

			matrix = new Matrix3d().asIdentity();
			if (xRotation != 0)
				matrix.multiply(new Matrix3d().asXRotation(AngleHelper.rad(-xRotation)));
			if (yRotation != 0)
				matrix.multiply(new Matrix3d().asYRotation(AngleHelper.rad(-yRotation)));
			if (zRotation != 0)
				matrix.multiply(new Matrix3d().asZRotation(AngleHelper.rad(-zRotation)));
			return matrix;
		}

		public boolean hasVerticalRotation() {
			return xRotation != 0 || zRotation != 0;
		}

		public float getYawOffset() {
			return secondYRotation;
		}

	}

	@Override
	protected boolean updateWaterState() {
		/*
		 * Override this with an empty method to reduce enormous calculation time when
		 * contraptions are in water WARNING: THIS HAS A BUNCH OF SIDE EFFECTS! - Fluids
		 * will not try to change contraption movement direction - this.inWater and
		 * this.isInWater() will return unreliable data - entities riding a contraption
		 * will not cause water splashes (seats are their own entity so this should be
		 * fine) - fall distance is not reset when the contraption is in water -
		 * this.eyesInWater and this.canSwim() will always be false - swimming state
		 * will never be updated
		 */
		return false;
	}

	@Override
	public void setOnFireFor(int p_70015_1_) {
		// Contraptions no longer catch fire
	}

	public boolean isReadyForRender() {
		return initialized;
	}

	public boolean isAliveOrStale() {
		return isAlive() || getWorld().isClient() ? staleTicks > 0 : false;
	}

}
