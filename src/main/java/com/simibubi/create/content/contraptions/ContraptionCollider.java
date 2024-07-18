package com.simibubi.create.content.contraptions;

import static net.minecraft.entity.Entity.adjustMovementForCollisions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.simibubi.create.AllDamageTypes;

import com.simibubi.create.foundation.mixin.fabric.ServerGamePacketListenerImplAccessor;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.MutablePair;

import com.google.common.base.Predicates;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity.ContraptionRotationState;
import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.sync.ClientMotionPacket;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider.ContinuousSeparationManifold;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class ContraptionCollider {

	enum PlayerType {
		NONE, CLIENT, REMOTE, SERVER
	}

	private static MutablePair<WeakReference<AbstractContraptionEntity>, Double> safetyLock = new MutablePair<>();
	private static Map<AbstractContraptionEntity, Map<PlayerEntity, Double>> remoteSafetyLocks = new WeakHashMap<>();

	static void collideEntities(AbstractContraptionEntity contraptionEntity) {
		World world = contraptionEntity.getEntityWorld();
		Contraption contraption = contraptionEntity.getContraption();
		Box bounds = contraptionEntity.getBoundingBox();

		if (contraption == null)
			return;
		if (bounds == null)
			return;

		Vec3d contraptionPosition = contraptionEntity.getPos();
		Vec3d contraptionMotion = contraptionPosition.subtract(contraptionEntity.getPrevPositionVec());
		Vec3d anchorVec = contraptionEntity.getAnchorVec();
		ContraptionRotationState rotation = null;

		if (world.isClient() && safetyLock.left != null && safetyLock.left.get() == contraptionEntity)
			EnvExecutor.runWhenOn(EnvType.CLIENT,
				() -> () -> saveClientPlayerFromClipping(contraptionEntity, contraptionMotion));

		// After death, multiple refs to the client player may show up in the area
		boolean skipClientPlayer = false;

		List<Entity> entitiesWithinAABB = world.getEntitiesByClass(Entity.class, bounds.expand(2)
			.stretch(0, 32, 0), contraptionEntity::collidesWith);
		for (Entity entity : entitiesWithinAABB) {
			if (!entity.isAlive())
				continue;

			PlayerType playerType = getPlayerType(entity);
			if (playerType == PlayerType.REMOTE) {
				if (!(contraption instanceof TranslatingContraption))
					continue;
				EnvExecutor.runWhenOn(EnvType.CLIENT,
					() -> () -> saveRemotePlayerFromClipping((PlayerEntity) entity, contraptionEntity, contraptionMotion));
				continue;
			}

			entity.streamSelfAndPassengers()
				.forEach(e -> {
					if (e instanceof ServerPlayerEntity)
						((ServerGamePacketListenerImplAccessor) ((ServerPlayerEntity) e).networkHandler).create$setFloatingTicks(0);
				});
			if (playerType == PlayerType.SERVER)
				continue;

			if (playerType == PlayerType.CLIENT) {
				if (skipClientPlayer)
					continue;
				else
					skipClientPlayer = true;
			}

			// Init matrix
			if (rotation == null)
				rotation = contraptionEntity.getRotationState();
			Matrix3d rotationMatrix = rotation.asMatrix();

			// Transform entity position and motion to local space
			Vec3d entityPosition = entity.getPos();
			Box entityBounds = entity.getBoundingBox();
			Vec3d motion = entity.getVelocity();
			float yawOffset = rotation.getYawOffset();
			Vec3d position = getWorldToLocalTranslation(entity, anchorVec, rotationMatrix, yawOffset);

			motion = motion.subtract(contraptionMotion);
			motion = rotationMatrix.transform(motion);

			// Prepare entity bounds
			Box localBB = entityBounds.offset(position)
				.expand(1.0E-7D);

			OrientedBB obb = new OrientedBB(localBB);
			obb.setRotation(rotationMatrix);

			// Use simplified bbs when present
			final Vec3d motionCopy = motion;
			List<Box> collidableBBs = contraption.getSimplifiedEntityColliders()
				.orElseGet(() -> {

					// Else find 'nearby' individual block shapes to collide with
					List<Box> bbs = new ArrayList<>();
					List<VoxelShape> potentialHits =
						getPotentiallyCollidedShapes(world, contraption, localBB.stretch(motionCopy));
					potentialHits.forEach(shape -> shape.getBoundingBoxes()
						.forEach(bbs::add));
					return bbs;

				});

			MutableObject<Vec3d> collisionResponse = new MutableObject<>(Vec3d.ZERO);
			MutableObject<Vec3d> normal = new MutableObject<>(Vec3d.ZERO);
			MutableObject<Vec3d> location = new MutableObject<>(Vec3d.ZERO);
			MutableBoolean surfaceCollision = new MutableBoolean(false);
			MutableFloat temporalResponse = new MutableFloat(1);
			Vec3d obbCenter = obb.getCenter();

			// Apply separation maths
			boolean doHorizontalPass = !rotation.hasVerticalRotation();
			for (boolean horizontalPass : Iterate.trueAndFalse) {
				boolean verticalPass = !horizontalPass || !doHorizontalPass;

				for (Box bb : collidableBBs) {
					Vec3d currentResponse = collisionResponse.getValue();
					Vec3d currentCenter = obbCenter.add(currentResponse);

					if (Math.abs(currentCenter.x - bb.getCenter().x) - entityBounds.getXLength() - 1 > bb.getXLength() / 2)
						continue;
					if (Math.abs((currentCenter.y + motion.y) - bb.getCenter().y) - entityBounds.getYLength()
						- 1 > bb.getYLength() / 2)
						continue;
					if (Math.abs(currentCenter.z - bb.getCenter().z) - entityBounds.getZLength() - 1 > bb.getZLength() / 2)
						continue;

					obb.setCenter(currentCenter);
					ContinuousSeparationManifold intersect = obb.intersect(bb, motion);

					if (intersect == null)
						continue;
					if (verticalPass && surfaceCollision.isFalse())
						surfaceCollision.setValue(intersect.isSurfaceCollision());

					double timeOfImpact = intersect.getTimeOfImpact();
					boolean isTemporal = timeOfImpact > 0 && timeOfImpact < 1;
					Vec3d collidingNormal = intersect.getCollisionNormal();
					Vec3d collisionPosition = intersect.getCollisionPosition();

					if (!isTemporal) {
						Vec3d separation = intersect.asSeparationVec(entity.getStepHeight());
						if (separation != null && !separation.equals(Vec3d.ZERO)) {
							collisionResponse.setValue(currentResponse.add(separation));
							timeOfImpact = 0;
						}
					}

					boolean nearest = timeOfImpact >= 0 && temporalResponse.getValue() > timeOfImpact;
					if (collidingNormal != null && nearest)
						normal.setValue(collidingNormal);
					if (collisionPosition != null && nearest)
						location.setValue(collisionPosition);

					if (isTemporal) {
						if (temporalResponse.getValue() > timeOfImpact)
							temporalResponse.setValue(timeOfImpact);
					}
				}

				if (verticalPass)
					break;

				boolean noVerticalMotionResponse = temporalResponse.getValue() == 1;
				boolean noVerticalCollision = collisionResponse.getValue().y == 0;
				if (noVerticalCollision && noVerticalMotionResponse)
					break;

				// Re-run collisions with horizontal offset
				collisionResponse.setValue(collisionResponse.getValue()
					.multiply(129 / 128f, 0, 129 / 128f));
				continue;
			}

			// Resolve collision
			Vec3d entityMotion = entity.getVelocity();
			Vec3d entityMotionNoTemporal = entityMotion;
			Vec3d collisionNormal = normal.getValue();
			Vec3d collisionLocation = location.getValue();
			Vec3d totalResponse = collisionResponse.getValue();
			boolean hardCollision = !totalResponse.equals(Vec3d.ZERO);
			boolean temporalCollision = temporalResponse.getValue() != 1;
			Vec3d motionResponse = !temporalCollision ? motion
				: motion.normalize()
					.multiply(motion.length() * temporalResponse.getValue());

			rotationMatrix.transpose();
			motionResponse = rotationMatrix.transform(motionResponse)
				.add(contraptionMotion);
			totalResponse = rotationMatrix.transform(totalResponse);
			totalResponse = VecHelper.rotate(totalResponse, yawOffset, Axis.Y);
			collisionNormal = rotationMatrix.transform(collisionNormal);
			collisionNormal = VecHelper.rotate(collisionNormal, yawOffset, Axis.Y);
			collisionNormal = collisionNormal.normalize();
			collisionLocation = rotationMatrix.transform(collisionLocation);
			collisionLocation = VecHelper.rotate(collisionLocation, yawOffset, Axis.Y);
			rotationMatrix.transpose();

			double bounce = 0;
			double slide = 0;

			if (!collisionLocation.equals(Vec3d.ZERO)) {
				collisionLocation = collisionLocation.add(entity.getPos()
					.add(entity.getBoundingBox()
						.getCenter())
					.multiply(.5f));
				if (temporalCollision)
					collisionLocation = collisionLocation.add(0, motionResponse.y, 0);

				BlockPos pos = BlockPos.ofFloored(contraptionEntity.toLocalVector(entity.getPos(), 0));
				if (contraption.getBlocks()
					.containsKey(pos)) {
					BlockState blockState = contraption.getBlocks()
						.get(pos).state();
					if (blockState.isIn(BlockTags.CLIMBABLE)) {
						surfaceCollision.setTrue();
						totalResponse = totalResponse.add(0, .1f, 0);
					}
				}

				pos = BlockPos.ofFloored(contraptionEntity.toLocalVector(collisionLocation, 0));
				if (contraption.getBlocks()
					.containsKey(pos)) {
					BlockState blockState = contraption.getBlocks()
						.get(pos).state();

					MovingInteractionBehaviour movingInteractionBehaviour = contraption.interactors.get(pos);
					if (movingInteractionBehaviour != null)
						movingInteractionBehaviour.handleEntityCollision(entity, pos, contraptionEntity);

					bounce = BlockHelper.getBounceMultiplier(blockState.getBlock());
					slide = Math.max(0, blockState.getBlock().getSlipperiness()) - .6f;
				}
			}

			boolean hasNormal = !collisionNormal.equals(Vec3d.ZERO);
			boolean anyCollision = hardCollision || temporalCollision;

			if (bounce > 0 && hasNormal && anyCollision
				&& bounceEntity(entity, collisionNormal, contraptionEntity, bounce)) {
				entity.getWorld().playSound(playerType == PlayerType.CLIENT ? (PlayerEntity) entity : null, entity.getX(),
					entity.getY(), entity.getZ(), SoundEvents.BLOCK_SLIME_BLOCK_FALL, SoundCategory.BLOCKS, .5f, 1);
				continue;
			}

			if (temporalCollision) {
				double idealVerticalMotion = motionResponse.y;
				if (idealVerticalMotion != entityMotion.y) {
					entity.setVelocity(entityMotion.multiply(1, 0, 1)
						.add(0, idealVerticalMotion, 0));
					entityMotion = entity.getVelocity();
				}
			}

			if (hardCollision) {
				double motionX = entityMotion.getX();
				double motionY = entityMotion.getY();
				double motionZ = entityMotion.getZ();
				double intersectX = totalResponse.getX();
				double intersectY = totalResponse.getY();
				double intersectZ = totalResponse.getZ();

				double horizonalEpsilon = 1 / 128f;
				if (motionX != 0 && Math.abs(intersectX) > horizonalEpsilon && motionX > 0 == intersectX < 0)
					entityMotion = entityMotion.multiply(0, 1, 1);
				if (motionY != 0 && intersectY != 0 && motionY > 0 == intersectY < 0)
					entityMotion = entityMotion.multiply(1, 0, 1)
						.add(0, contraptionMotion.y, 0);
				if (motionZ != 0 && Math.abs(intersectZ) > horizonalEpsilon && motionZ > 0 == intersectZ < 0)
					entityMotion = entityMotion.multiply(1, 1, 0);

			}

			if (bounce == 0 && slide > 0 && hasNormal && anyCollision && rotation.hasVerticalRotation()) {
				double slideFactor = collisionNormal.multiply(1, 0, 1)
					.length() * 1.25f;
				Vec3d motionIn = entityMotionNoTemporal.multiply(0, .9, 0)
					.add(0, -.01f, 0);
				Vec3d slideNormal = collisionNormal.crossProduct(motionIn.crossProduct(collisionNormal))
					.normalize();
				Vec3d newMotion = entityMotion.multiply(.85, 0, .85)
					.add(slideNormal.multiply((.2f + slide) * motionIn.length() * slideFactor)
						.add(0, -.1f - collisionNormal.y * .125f, 0));
				entity.setVelocity(newMotion);
				entityMotion = entity.getVelocity();
			}

			if (!hardCollision && surfaceCollision.isFalse())
				continue;

			Vec3d allowedMovement = collide(totalResponse, entity);
			entity.setPosition(entityPosition.x + allowedMovement.x, entityPosition.y + allowedMovement.y,
				entityPosition.z + allowedMovement.z);
			entityPosition = entity.getPos();

			entityMotion =
				handleDamageFromTrain(world, contraptionEntity, contraptionMotion, entity, entityMotion, playerType);

			entity.velocityModified = true;
			Vec3d contactPointMotion = Vec3d.ZERO;

			if (surfaceCollision.isTrue()) {
				contraptionEntity.registerColliding(entity);
				entity.fallDistance = 0;
				for (Entity rider : entity.getPassengersDeep())
					if (getPlayerType(rider) == PlayerType.CLIENT)
						AllPackets.getChannel()
							.sendToServer(new ClientMotionPacket(rider.getVelocity(), true, 0));
				boolean canWalk = bounce != 0 || slide == 0;
				if (canWalk || !rotation.hasVerticalRotation()) {
					if (canWalk)
						entity.setOnGround(true);
					if (entity instanceof ItemEntity)
						entityMotion = entityMotion.multiply(.5f, 1, .5f);
				}
				contactPointMotion = contraptionEntity.getContactPointMotion(entityPosition);
				allowedMovement = collide(contactPointMotion, entity);
				entity.setPosition(entityPosition.x + allowedMovement.x, entityPosition.y,
					entityPosition.z + allowedMovement.z);
			}

			entity.setVelocity(entityMotion);

			if (playerType != PlayerType.CLIENT)
				continue;

			double d0 = entity.getX() - entity.prevX - contactPointMotion.x;
			double d1 = entity.getZ() - entity.prevZ - contactPointMotion.z;
			float limbSwing = MathHelper.sqrt((float) (d0 * d0 + d1 * d1)) * 4.0F;
			if (limbSwing > 1.0F)
				limbSwing = 1.0F;
			AllPackets.getChannel()
				.sendToServer(new ClientMotionPacket(entityMotion, true, limbSwing));

			if (entity.isOnGround() && contraption instanceof TranslatingContraption) {
				safetyLock.setLeft(new WeakReference<>(contraptionEntity));
				safetyLock.setRight(entity.getY() - contraptionEntity.getY());
			}
		}

	}

	private static int packetCooldown = 0;

	@Environment(EnvType.CLIENT)
	private static void saveClientPlayerFromClipping(AbstractContraptionEntity contraptionEntity,
		Vec3d contraptionMotion) {
		ClientPlayerEntity entity = MinecraftClient.getInstance().player;
		if (entity.hasVehicle())
			return;

		double prevDiff = safetyLock.right;
		double currentDiff = entity.getY() - contraptionEntity.getY();
		double motion = contraptionMotion.subtract(entity.getVelocity()).y;
		double trend = Math.signum(currentDiff - prevDiff);

		ClientPlayNetworkHandler handler = entity.networkHandler;
		if (handler.getPlayerList()
			.size() > 1) {
			if (packetCooldown > 0)
				packetCooldown--;
			if (packetCooldown == 0) {
				AllPackets.getChannel()
					.sendToServer(new ContraptionColliderLockPacketRequest(contraptionEntity.getId(), currentDiff));
				packetCooldown = 3;
			}
		}

		if (trend == 0)
			return;
		if (trend == Math.signum(motion))
			return;

		double speed = contraptionMotion.multiply(0, 1, 0)
			.lengthSquared();
		if (trend > 0 && speed < 0.1)
			return;
		if (speed < 0.05)
			return;

		if (!savePlayerFromClipping(entity, contraptionEntity, contraptionMotion, prevDiff))
			safetyLock.setLeft(null);
	}

	@Environment(EnvType.CLIENT)
	public static void lockPacketReceived(int contraptionId, int remotePlayerId, double suggestedOffset) {
		ClientWorld level = MinecraftClient.getInstance().world;
		if (!(level.getEntityById(contraptionId) instanceof ControlledContraptionEntity contraptionEntity))
			return;
		if (!(level.getEntityById(remotePlayerId) instanceof OtherClientPlayerEntity player))
			return;
		remoteSafetyLocks.computeIfAbsent(contraptionEntity, $ -> new WeakHashMap<>())
			.put(player, suggestedOffset);
	}

	@Environment(EnvType.CLIENT)
	private static void saveRemotePlayerFromClipping(PlayerEntity entity, AbstractContraptionEntity contraptionEntity,
		Vec3d contraptionMotion) {
		if (entity.hasVehicle())
			return;

		Map<PlayerEntity, Double> locksOnThisContraption =
			remoteSafetyLocks.getOrDefault(contraptionEntity, Collections.emptyMap());
		double prevDiff = locksOnThisContraption.getOrDefault(entity, entity.getY() - contraptionEntity.getY());
		if (!savePlayerFromClipping(entity, contraptionEntity, contraptionMotion, prevDiff))
			if (locksOnThisContraption.containsKey(entity))
				locksOnThisContraption.remove(entity);
	}

	@Environment(EnvType.CLIENT)
	private static boolean savePlayerFromClipping(PlayerEntity entity, AbstractContraptionEntity contraptionEntity,
		Vec3d contraptionMotion, double yStartOffset) {
		Box bb = entity.getBoundingBox()
			.contract(1 / 4f, 0, 1 / 4f);
		double shortestDistance = Double.MAX_VALUE;
		double yStart = entity.getStepHeight() + contraptionEntity.getY() + yStartOffset;
		double rayLength = Math.max(5, Math.abs(entity.getY() - yStart));

		for (int rayIndex = 0; rayIndex < 4; rayIndex++) {
			Vec3d start = new Vec3d(rayIndex / 2 == 0 ? bb.minX : bb.maxX, yStart, rayIndex % 2 == 0 ? bb.minZ : bb.maxZ);
			Vec3d end = start.add(0, -rayLength, 0);

			BlockHitResult hitResult = ContraptionHandlerClient.rayTraceContraption(start, end, contraptionEntity);
			if (hitResult == null)
				continue;

			Vec3d hit = contraptionEntity.toGlobalVector(hitResult.getPos(), 1);
			double hitDiff = start.y - hit.y;
			if (shortestDistance > hitDiff)
				shortestDistance = hitDiff;
		}

		if (shortestDistance > rayLength)
			return false;
		entity.setPosition(entity.getX(), yStart - shortestDistance, entity.getZ());
		return true;
	}

	private static Vec3d handleDamageFromTrain(World world, AbstractContraptionEntity contraptionEntity,
		Vec3d contraptionMotion, Entity entity, Vec3d entityMotion, PlayerType playerType) {

		if (!(contraptionEntity instanceof CarriageContraptionEntity cce))
			return entityMotion;
		if (!entity.isOnGround())
			return entityMotion;

		NbtCompound persistentData = entity.getCustomData();
		if (persistentData.contains("ContraptionGrounded")) {
			persistentData.remove("ContraptionGrounded");
			return entityMotion;
		}

		if (cce.collidingEntities.containsKey(entity))
			return entityMotion;
		if (entity instanceof ItemEntity)
			return entityMotion;
		if (cce.nonDamageTicks != 0)
			return entityMotion;
		if (!AllConfigs.server().trains.trainsCauseDamage.get())
			return entityMotion;

		Vec3d diffMotion = contraptionMotion.subtract(entity.getVelocity());

		if (diffMotion.length() <= 0.35f || contraptionMotion.length() <= 0.35f)
			return entityMotion;

		DamageSource source = CreateDamageSources.runOver(world, contraptionEntity);
		double damage = diffMotion.length();
		if (entity.getType().getSpawnGroup() == SpawnGroup.MONSTER)
			damage *= 2;

		if (entity instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()))
			return entityMotion;

		if (playerType == PlayerType.CLIENT) {
			AllPackets.getChannel()
				.sendToServer(new TrainCollisionPacket((int) (damage * 16), contraptionEntity.getId()));
			world.playSound((PlayerEntity) entity, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
				SoundCategory.NEUTRAL, 1, .75f);
		} else {
			entity.damage(source, (int) (damage * 16));
			world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1, .75f);
			if (!entity.isAlive())
				contraptionEntity.getControllingPlayer()
					.map(world::getPlayerByUuid)
					.ifPresent(AllAdvancements.TRAIN_ROADKILL::awardTo);
		}

		Vec3d added = entityMotion.add(contraptionMotion.multiply(1, 0, 1)
			.normalize()
			.add(0, .25, 0)
			.multiply(damage * 4))
			.add(diffMotion);

		return VecHelper.clamp(added, 3);
	}

	static boolean bounceEntity(Entity entity, Vec3d normal, AbstractContraptionEntity contraption, double factor) {
		if (factor == 0)
			return false;
		if (entity.bypassesLandingEffects())
			return false;

		Vec3d contactPointMotion = contraption.getContactPointMotion(entity.getPos());
		Vec3d motion = entity.getVelocity()
			.subtract(contactPointMotion);
		Vec3d deltav = normal.multiply(factor * 2 * motion.dotProduct(normal));
		if (deltav.dotProduct(deltav) < 0.1f)
			return false;
		entity.setVelocity(entity.getVelocity()
			.subtract(deltav));
		return true;
	}

	public static Vec3d getWorldToLocalTranslation(Entity entity, Vec3d anchorVec, Matrix3d rotationMatrix,
		float yawOffset) {
		Vec3d entityPosition = entity.getPos();
		Vec3d centerY = new Vec3d(0, entity.getBoundingBox()
			.getYLength() / 2, 0);
		Vec3d position = entityPosition;
		position = position.add(centerY);
		position = worldToLocalPos(position, anchorVec, rotationMatrix, yawOffset);
		position = position.subtract(centerY);
		position = position.subtract(entityPosition);
		return position;
	}

	public static Vec3d worldToLocalPos(Vec3d worldPos, AbstractContraptionEntity contraptionEntity) {
		return worldToLocalPos(worldPos, contraptionEntity.getAnchorVec(),
			contraptionEntity.getRotationState());
	}

	public static Vec3d worldToLocalPos(Vec3d worldPos, Vec3d anchorVec, ContraptionRotationState rotation) {
		return worldToLocalPos(worldPos, anchorVec, rotation.asMatrix(), rotation.getYawOffset());
	}

	public static Vec3d worldToLocalPos(Vec3d worldPos, Vec3d anchorVec, Matrix3d rotationMatrix, float yawOffset) {
		Vec3d localPos = worldPos;
		localPos = localPos.subtract(anchorVec);
		localPos = localPos.subtract(VecHelper.CENTER_OF_ORIGIN);
		localPos = VecHelper.rotate(localPos, -yawOffset, Axis.Y);
		localPos = rotationMatrix.transform(localPos);
		localPos = localPos.add(VecHelper.CENTER_OF_ORIGIN);
		return localPos;
	}

	/** From Entity#collide **/
	static Vec3d collide(Vec3d p_20273_, Entity e) {
		Box aabb = e.getBoundingBox();
		List<VoxelShape> list = e.getWorld().getEntityCollisions(e, aabb.stretch(p_20273_));
		Vec3d vec3 = p_20273_.lengthSquared() == 0.0D ? p_20273_ : adjustMovementForCollisions(e, p_20273_, aabb, e.getWorld(), list);
		boolean flag = p_20273_.x != vec3.x;
		boolean flag1 = p_20273_.y != vec3.y;
		boolean flag2 = p_20273_.z != vec3.z;
		boolean flag3 = flag1 && p_20273_.y < 0.0D;
		if (e.getStepHeight() > 0.0F && flag3 && (flag || flag2)) {
			Vec3d vec31 = adjustMovementForCollisions(e, new Vec3d(p_20273_.x, e.getStepHeight(), p_20273_.z), aabb,
				e.getWorld(), list);
			Vec3d vec32 = adjustMovementForCollisions(e, new Vec3d(0.0D, e.getStepHeight(), 0.0D),
				aabb.stretch(p_20273_.x, 0.0D, p_20273_.z), e.getWorld(), list);
			if (vec32.y < (double) e.getStepHeight()) {
				Vec3d vec33 =
					adjustMovementForCollisions(e, new Vec3d(p_20273_.x, 0.0D, p_20273_.z), aabb.offset(vec32), e.getWorld(), list)
						.add(vec32);
				if (vec33.horizontalLengthSquared() > vec31.horizontalLengthSquared()) {
					vec31 = vec33;
				}
			}

			if (vec31.horizontalLengthSquared() > vec3.horizontalLengthSquared()) {
				return vec31.add(adjustMovementForCollisions(e, new Vec3d(0.0D, -vec31.y + p_20273_.y, 0.0D), aabb.offset(vec31),
					e.getWorld(), list));
			}
		}

		return vec3;
	}

	private static PlayerType getPlayerType(Entity entity) {
		if (!(entity instanceof PlayerEntity))
			return PlayerType.NONE;
		if (!entity.getWorld().isClient)
			return PlayerType.SERVER;
		MutableBoolean isClient = new MutableBoolean(false);
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> isClient.setValue(isClientPlayerEntity(entity)));
		return isClient.booleanValue() ? PlayerType.CLIENT : PlayerType.REMOTE;
	}

	@Environment(EnvType.CLIENT)
	private static boolean isClientPlayerEntity(Entity entity) {
		return entity instanceof ClientPlayerEntity;
	}

	private static List<VoxelShape> getPotentiallyCollidedShapes(World world, Contraption contraption, Box localBB) {

		double height = localBB.getYLength();
		double width = localBB.getXLength();
		double horizontalFactor = (height > width && width != 0) ? height / width : 1;
		double verticalFactor = (width > height && height != 0) ? width / height : 1;
		Box blockScanBB = localBB.expand(0.5f);
		blockScanBB = blockScanBB.expand(horizontalFactor, verticalFactor, horizontalFactor);

		BlockPos min = BlockPos.ofFloored(blockScanBB.minX, blockScanBB.minY, blockScanBB.minZ);
		BlockPos max = BlockPos.ofFloored(blockScanBB.maxX, blockScanBB.maxY, blockScanBB.maxZ);

		List<VoxelShape> potentialHits = BlockPos.stream(min, max)
			.filter(contraption.getBlocks()::containsKey)
			.filter(Predicates.not(contraption::isHiddenInPortal))
			.map(p -> {
				BlockState blockState = contraption.getBlocks()
					.get(p).state();
				BlockPos pos = contraption.getBlocks()
					.get(p).pos();
				VoxelShape collisionShape = blockState.getCollisionShape(world, p);
				return collisionShape.offset(pos.getX(), pos.getY(), pos.getZ());
			})
			.filter(Predicates.not(VoxelShape::isEmpty))
			.toList();

		return potentialHits;
	}

	public static boolean collideBlocks(AbstractContraptionEntity contraptionEntity) {
		if (!contraptionEntity.supportsTerrainCollision())
			return false;

		World world = contraptionEntity.getEntityWorld();
		Vec3d motion = contraptionEntity.getVelocity();
		TranslatingContraption contraption = (TranslatingContraption) contraptionEntity.getContraption();
		Box bounds = contraptionEntity.getBoundingBox();
		Vec3d position = contraptionEntity.getPos();
		BlockPos gridPos = BlockPos.ofFloored(position);

		if (contraption == null)
			return false;
		if (bounds == null)
			return false;
		if (motion.equals(Vec3d.ZERO))
			return false;

		Direction movementDirection = Direction.getFacing(motion.x, motion.y, motion.z);

		// Blocks in the world
		if (movementDirection.getDirection() == AxisDirection.POSITIVE)
			gridPos = gridPos.offset(movementDirection);
		if (isCollidingWithWorld(world, contraption, gridPos, movementDirection))
			return true;

		// Other moving Contraptions
		for (ControlledContraptionEntity otherContraptionEntity : world.getEntitiesByClass(
			ControlledContraptionEntity.class, bounds.expand(1), e -> !e.equals(contraptionEntity))) {

			if (!otherContraptionEntity.supportsTerrainCollision())
				continue;

			Vec3d otherMotion = otherContraptionEntity.getVelocity();
			TranslatingContraption otherContraption = (TranslatingContraption) otherContraptionEntity.getContraption();
			Box otherBounds = otherContraptionEntity.getBoundingBox();
			Vec3d otherPosition = otherContraptionEntity.getPos();

			if (otherContraption == null)
				return false;
			if (otherBounds == null)
				return false;

			if (!bounds.offset(motion)
				.intersects(otherBounds.offset(otherMotion)))
				continue;

			for (BlockPos colliderPos : contraption.getOrCreateColliders(world, movementDirection)) {
				colliderPos = colliderPos.add(gridPos)
					.subtract(BlockPos.ofFloored(otherPosition));
				if (!otherContraption.getBlocks()
					.containsKey(colliderPos))
					continue;
				return true;
			}
		}

		return false;
	}

	public static boolean isCollidingWithWorld(World world, TranslatingContraption contraption, BlockPos anchor,
		Direction movementDirection) {
		for (BlockPos pos : contraption.getOrCreateColliders(world, movementDirection)) {
			BlockPos colliderPos = pos.add(anchor);

			if (!world.canSetBlock(colliderPos))
				return true;

			BlockState collidedState = world.getBlockState(colliderPos);
			StructureBlockInfo blockInfo = contraption.getBlocks()
				.get(pos);
			boolean emptyCollider = collidedState.getCollisionShape(world, pos)
				.isEmpty();

			if (collidedState.getBlock() instanceof CocoaBlock)
				continue;

			MovementBehaviour movementBehaviour = AllMovementBehaviours.getBehaviour(blockInfo.state());
			if (movementBehaviour != null) {
				if (movementBehaviour instanceof BlockBreakingMovementBehaviour) {
					BlockBreakingMovementBehaviour behaviour = (BlockBreakingMovementBehaviour) movementBehaviour;
					if (!behaviour.canBreak(world, colliderPos, collidedState) && !emptyCollider)
						return true;
					continue;
				}
				if (movementBehaviour instanceof HarvesterMovementBehaviour) {
					HarvesterMovementBehaviour harvesterMovementBehaviour =
						(HarvesterMovementBehaviour) movementBehaviour;
					if (!harvesterMovementBehaviour.isValidCrop(world, colliderPos, collidedState)
						&& !harvesterMovementBehaviour.isValidOther(world, colliderPos, collidedState)
						&& !emptyCollider)
						return true;
					continue;
				}
			}

			if (AllBlocks.PULLEY_MAGNET.has(collidedState) && pos.equals(BlockPos.ORIGIN)
				&& movementDirection == Direction.UP)
				continue;
			if (!collidedState.isReplaceable() && !emptyCollider) {
				return true;
			}

		}
		return false;
	}

}
