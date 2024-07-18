package com.simibubi.create.content.kinetics.fan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.simibubi.create.foundation.mixin.fabric.ServerGamePacketListenerImplAccessor;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class AirCurrent {

	public final IAirCurrentSource source;
	public Box bounds = new Box(0, 0, 0, 0, 0, 0);
	public List<AirCurrentSegment> segments = new ArrayList<>();
	public Direction direction;
	public boolean pushing;
	public float maxDistance;

	protected List<Pair<TransportedItemStackHandlerBehaviour, FanProcessingType>> affectedItemHandlers =
		new ArrayList<>();
	protected List<Entity> caughtEntities = new ArrayList<>();

	public AirCurrent(IAirCurrentSource source) {
		this.source = source;
	}

	public void tick() {
		if (direction == null)
			rebuild();
		World world = source.getAirCurrentWorld();
		if (world != null && world.isClient) {
			float offset = pushing ? 0.5f : maxDistance + .5f;
			Vec3d pos = VecHelper.getCenterOf(source.getAirCurrentPos())
				.add(Vec3d.of(direction.getVector())
					.multiply(offset));
			if (world.random.nextFloat() < AllConfigs.client().fanParticleDensity.get())
				world.addParticle(new AirFlowParticleData(source.getAirCurrentPos()), pos.x, pos.y, pos.z, 0, 0, 0);
		}

		tickAffectedEntities(world);
		tickAffectedHandlers();
	}

	protected void tickAffectedEntities(World world) {
		for (Iterator<Entity> iterator = caughtEntities.iterator(); iterator.hasNext();) {
			Entity entity = iterator.next();
			if (!entity.isAlive() || !entity.getBoundingBox()
				.intersects(bounds) || isPlayerCreativeFlying(entity)) {
				iterator.remove();
				continue;
			}

			Vec3i flow = (pushing ? direction : direction.getOpposite()).getVector();
			float speed = Math.abs(source.getSpeed());
			float sneakModifier = entity.isSneaking() ? 4096f : 512f;
			double entityDistance = VecHelper.alignedDistanceToFace(entity.getPos(), source.getAirCurrentPos(), direction);
			// entityDistanceOld should be removed eventually. Remember that entityDistanceOld cannot be 0 while entityDistance can,
			// so division by 0 must be avoided.
			double entityDistanceOld = entity.getPos().distanceTo(VecHelper.getCenterOf(source.getAirCurrentPos()));
			float acceleration = (float) (speed / sneakModifier / (entityDistanceOld / maxDistance));
			Vec3d previousMotion = entity.getVelocity();
			float maxAcceleration = 5;

			double xIn = MathHelper.clamp(flow.getX() * acceleration - previousMotion.x, -maxAcceleration, maxAcceleration);
			double yIn = MathHelper.clamp(flow.getY() * acceleration - previousMotion.y, -maxAcceleration, maxAcceleration);
			double zIn = MathHelper.clamp(flow.getZ() * acceleration - previousMotion.z, -maxAcceleration, maxAcceleration);

			entity.setVelocity(previousMotion.add(new Vec3d(xIn, yIn, zIn).multiply(1 / 8f)));
			entity.fallDistance = 0;
			EnvExecutor.runWhenOn(EnvType.CLIENT,
				() -> () -> enableClientPlayerSound(entity, MathHelper.clamp(speed / 128f * .4f, 0.01f, .4f)));

			if (entity instanceof ServerPlayerEntity)
				((ServerGamePacketListenerImplAccessor) ((ServerPlayerEntity) entity).networkHandler).create$setVehicleFloatingTicks(0);

			FanProcessingType processingType = getTypeAt((float) entityDistance);

			if (processingType == AllFanProcessingTypes.NONE)
				continue;

			if (entity instanceof ItemEntity itemEntity) {
				if (world != null && world.isClient) {
					processingType.spawnProcessingParticles(world, entity.getPos());
					continue;
				}
				if (FanProcessing.canProcess(itemEntity, processingType))
					if (FanProcessing.applyProcessing(itemEntity, processingType)
						&& source instanceof EncasedFanBlockEntity fan)
						fan.award(AllAdvancements.FAN_PROCESSING);
				continue;
			}

			if (world != null)
				processingType.affectEntity(entity, world);
		}
	}

	public static boolean isPlayerCreativeFlying(Entity entity) {
		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entity;
			return player.isCreative() && player.getAbilities().flying;
		}
		return false;
	}

	public void tickAffectedHandlers() {
		for (Pair<TransportedItemStackHandlerBehaviour, FanProcessingType> pair : affectedItemHandlers) {
			TransportedItemStackHandlerBehaviour handler = pair.getKey();
			World world = handler.getWorld();
			FanProcessingType processingType = pair.getRight();

			handler.handleProcessingOnAllItems(transported -> {
				if (world.isClient) {
					processingType.spawnProcessingParticles(world, handler.getWorldPositionOf(transported));
					return TransportedResult.doNothing();
				}
				TransportedResult applyProcessing = FanProcessing.applyProcessing(transported, world, processingType);
				if (!applyProcessing.doesNothing() && source instanceof EncasedFanBlockEntity fan)
					fan.award(AllAdvancements.FAN_PROCESSING);
				return applyProcessing;
			});
		}
	}

	public void rebuild() {
		if (source.getSpeed() == 0) {
			maxDistance = 0;
			segments.clear();
			bounds = new Box(0, 0, 0, 0, 0, 0);
			return;
		}

		direction = source.getAirflowOriginSide();
		pushing = source.getAirFlowDirection() == direction;
		maxDistance = source.getMaxDistance();

		World world = source.getAirCurrentWorld();
		BlockPos start = source.getAirCurrentPos();
		float max = this.maxDistance;
		Direction facing = direction;
		Vec3d directionVec = Vec3d.of(facing.getVector());
		maxDistance = getFlowLimit(world, start, max, facing);

		// Determine segments with transported fluids/gases
		segments.clear();
		AirCurrentSegment currentSegment = null;
		FanProcessingType type = AllFanProcessingTypes.NONE;

		int limit = getLimit();
		int searchStart = pushing ? 1 : limit;
		int searchEnd = pushing ? limit : 1;
		int searchStep = pushing ? 1 : -1;
		int toOffset = pushing ? -1 : 0;

		for (int i = searchStart; i * searchStep <= searchEnd * searchStep; i += searchStep) {
			BlockPos currentPos = start.offset(direction, i);
			FanProcessingType newType = FanProcessingType.getAt(world, currentPos);
			if (newType != AllFanProcessingTypes.NONE) {
				type = newType;
			}
			if (currentSegment == null) {
				currentSegment = new AirCurrentSegment();
				currentSegment.startOffset = i + toOffset;
				currentSegment.type = type;
			} else if (currentSegment.type != type) {
				currentSegment.endOffset = i + toOffset;
				segments.add(currentSegment);
				currentSegment = new AirCurrentSegment();
				currentSegment.startOffset = i + toOffset;
				currentSegment.type = type;
			}
		}
		if (currentSegment != null) {
			currentSegment.endOffset = searchEnd + searchStep + toOffset;
			segments.add(currentSegment);
		}

		// Build Bounding Box
		if (maxDistance < 0.25f)
			bounds = new Box(0, 0, 0, 0, 0, 0);
		else {
			float factor = maxDistance - 1;
			Vec3d scale = directionVec.multiply(factor);
			if (factor > 0)
				bounds = new Box(start.offset(direction)).stretch(scale);
			else {
				bounds = new Box(start.offset(direction)).shrink(scale.x, scale.y, scale.z)
					.offset(scale);
			}
		}

		findAffectedHandlers();
	}

	public static float getFlowLimit(World world, BlockPos start, float max, Direction facing) {
		for (int i = 0; i < max; i++) {
			BlockPos currentPos = start.offset(facing, i + 1);
			if (!world.canSetBlock(currentPos)) {
				return i;
			}

			BlockState state = world.getBlockState(currentPos);
			BlockState copycatState = CopycatBlock.getMaterial(world, currentPos);
			if (shouldAlwaysPass(copycatState.isAir() ? state : copycatState)) {
				continue;
			}

			VoxelShape shape = state.getCollisionShape(world, currentPos);
			if (shape.isEmpty()) {
				continue;
			}
			if (shape == VoxelShapes.fullCube()) {
				return i;
			}
			double shapeDepth = findMaxDepth(shape, facing);
			if (shapeDepth == Double.POSITIVE_INFINITY) {
				continue;
			}
			return Math.min((float) (i + shapeDepth + 1/32d), max);
		}

		return max;
	}

	private static final double[][] DEPTH_TEST_COORDINATES = {
			{ 0.25, 0.25 },
			{ 0.25, 0.75 },
			{ 0.5, 0.5 },
			{ 0.75, 0.25 },
			{ 0.75, 0.75 }
	};

	// Finds the maximum depth of the shape when traveling in the given direction.
	// The result is always positive.
	// If there is a hole, the result will be Double.POSITIVE_INFINITY.
	private static double findMaxDepth(VoxelShape shape, Direction direction) {
		Direction.Axis axis = direction.getAxis();
		Direction.AxisDirection axisDirection = direction.getDirection();
		double maxDepth = 0;

		for (double[] coordinates : DEPTH_TEST_COORDINATES) {
			double depth;
			if (axisDirection == Direction.AxisDirection.POSITIVE) {
				double min = shape.getStartingCoord(axis, coordinates[0], coordinates[1]);
				if (min == Double.POSITIVE_INFINITY) {
					return Double.POSITIVE_INFINITY;
				}
				depth = min;
			} else {
				double max = shape.getEndingCoord(axis, coordinates[0], coordinates[1]);
				if (max == Double.NEGATIVE_INFINITY) {
					return Double.POSITIVE_INFINITY;
				}
				depth = 1 - max;
			}

			if (depth > maxDepth) {
				maxDepth = depth;
			}
		}

		return maxDepth;
	}

	private static boolean shouldAlwaysPass(BlockState state) {
		return AllTags.AllBlockTags.FAN_TRANSPARENT.matches(state);
	}

	private int getLimit() {
		if ((float) (int) maxDistance == maxDistance) {
			return (int) maxDistance;
		} else {
			return (int) maxDistance + 1;
		}
	}

	public void findAffectedHandlers() {
		World world = source.getAirCurrentWorld();
		BlockPos start = source.getAirCurrentPos();
		affectedItemHandlers.clear();
		int limit = getLimit();
		for (int i = 1; i <= limit; i++) {
			FanProcessingType segmentType = getTypeAt(i - 1);
			for (int offset : Iterate.zeroAndOne) {
				BlockPos pos = start.offset(direction, i)
					.down(offset);
				TransportedItemStackHandlerBehaviour behaviour =
					BlockEntityBehaviour.get(world, pos, TransportedItemStackHandlerBehaviour.TYPE);
				if (behaviour != null) {
					FanProcessingType type = FanProcessingType.getAt(world, pos);
					if (type == AllFanProcessingTypes.NONE)
						type = segmentType;
					affectedItemHandlers.add(Pair.of(behaviour, type));
				}
				if (direction.getAxis()
					.isVertical())
					break;
			}
		}
	}

	public void findEntities() {
		caughtEntities.clear();
		caughtEntities = source.getAirCurrentWorld()
			.getOtherEntities(null, bounds);
	}

	public FanProcessingType getTypeAt(float offset) {
		if (offset >= 0 && offset <= maxDistance) {
			if (pushing) {
				for (AirCurrentSegment airCurrentSegment : segments) {
					if (offset <= airCurrentSegment.endOffset) {
						return airCurrentSegment.type;
					}
				}
			} else {
				for (AirCurrentSegment airCurrentSegment : segments) {
					if (offset >= airCurrentSegment.endOffset) {
						return airCurrentSegment.type;
					}
				}
			}
		}
		return AllFanProcessingTypes.NONE;
	}

	private static class AirCurrentSegment {
		private FanProcessingType type;
		private int startOffset;
		private int endOffset;
	}

	private static boolean isClientPlayerInAirCurrent;

	@Environment(EnvType.CLIENT)
	private static AirCurrentSound flyingSound;

	@Environment(EnvType.CLIENT)
	private static void enableClientPlayerSound(Entity e, float maxVolume) {
		if (e != MinecraftClient.getInstance()
			.getCameraEntity())
			return;

		isClientPlayerInAirCurrent = true;

		float pitch = (float) MathHelper.clamp(e.getVelocity()
			.length() * .5f, .5f, 2f);

		if (flyingSound == null || flyingSound.isDone()) {
			flyingSound = new AirCurrentSound(SoundEvents.ITEM_ELYTRA_FLYING, pitch);
			MinecraftClient.getInstance()
				.getSoundManager()
				.play(flyingSound);
		}
		flyingSound.setPitch(pitch);
		flyingSound.fadeIn(maxVolume);
	}

	@Environment(EnvType.CLIENT)
	public static void tickClientPlayerSounds() {
		if (!isClientPlayerInAirCurrent && flyingSound != null)
			if (flyingSound.isFaded())
				flyingSound.stopSound();
			else
				flyingSound.fadeOut();
		isClientPlayerInAirCurrent = false;
	}

}
