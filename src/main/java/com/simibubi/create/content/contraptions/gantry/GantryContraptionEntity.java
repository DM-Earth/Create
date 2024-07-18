package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GantryContraptionEntity extends AbstractContraptionEntity {

	Direction movementAxis;
	double clientOffsetDiff;
	double axisMotion;

	public double sequencedOffsetLimit;

	public GantryContraptionEntity(EntityType<?> entityTypeIn, World worldIn) {
		super(entityTypeIn, worldIn);
		sequencedOffsetLimit = -1;
	}

	public static GantryContraptionEntity create(World world, Contraption contraption, Direction movementAxis) {
		GantryContraptionEntity entity = new GantryContraptionEntity(AllEntityTypes.GANTRY_CONTRAPTION.get(), world);
		entity.setContraption(contraption);
		entity.movementAxis = movementAxis;
		return entity;
	}

	public void limitMovement(double maxOffset) {
		sequencedOffsetLimit = maxOffset;
	}

	@Override
	protected void tickContraption() {
		if (!(contraption instanceof GantryContraption))
			return;

		double prevAxisMotion = axisMotion;
		if (getWorld().isClient) {
			clientOffsetDiff *= .75f;
			updateClientMotion();
		}

		checkPinionShaft();
		tickActors();
		Vec3d movementVec = getVelocity();

		if (ContraptionCollider.collideBlocks(this)) {
			if (!getWorld().isClient)
				disassemble();
			return;
		}

		if (!isStalled() && age > 2) {
			if (sequencedOffsetLimit >= 0)
				movementVec = VecHelper.clampComponentWise(movementVec, (float) sequencedOffsetLimit);
			move(movementVec.x, movementVec.y, movementVec.z);
			if (sequencedOffsetLimit > 0)
				sequencedOffsetLimit = Math.max(0, sequencedOffsetLimit - movementVec.length());
		}

		if (Math.signum(prevAxisMotion) != Math.signum(axisMotion) && prevAxisMotion != 0)
			contraption.stop(getWorld());
		if (!getWorld().isClient && (prevAxisMotion != axisMotion || age % 3 == 0))
			sendPacket();
	}

	@Override
	public void disassemble() {
		sequencedOffsetLimit = -1;
		super.disassemble();
	}

	protected void checkPinionShaft() {
		Vec3d movementVec;
		Direction facing = ((GantryContraption) contraption).getFacing();
		Vec3d currentPosition = getAnchorVec().add(.5, .5, .5);
		BlockPos gantryShaftPos = BlockPos.ofFloored(currentPosition).offset(facing.getOpposite());

		BlockEntity be = getWorld().getBlockEntity(gantryShaftPos);
		if (!(be instanceof GantryShaftBlockEntity) || !AllBlocks.GANTRY_SHAFT.has(be.getCachedState())) {
			if (!getWorld().isClient) {
				setContraptionMotion(Vec3d.ZERO);
				disassemble();
			}
			return;
		}

		BlockState blockState = be.getCachedState();
		Direction direction = blockState.get(GantryShaftBlock.FACING);
		GantryShaftBlockEntity gantryShaftBlockEntity = (GantryShaftBlockEntity) be;

		float pinionMovementSpeed = gantryShaftBlockEntity.getPinionMovementSpeed();
		if (blockState.get(GantryShaftBlock.POWERED) || pinionMovementSpeed == 0) {
			setContraptionMotion(Vec3d.ZERO);
			if (!getWorld().isClient)
				disassemble();
			return;
		}

		if (sequencedOffsetLimit >= 0)
			pinionMovementSpeed = (float) MathHelper.clamp(pinionMovementSpeed, -sequencedOffsetLimit, sequencedOffsetLimit);
		movementVec = Vec3d.of(direction.getVector())
			.multiply(pinionMovementSpeed);

		Vec3d nextPosition = currentPosition.add(movementVec);
		double currentCoord = direction.getAxis()
			.choose(currentPosition.x, currentPosition.y, currentPosition.z);
		double nextCoord = direction.getAxis()
			.choose(nextPosition.x, nextPosition.y, nextPosition.z);

		if ((MathHelper.floor(currentCoord) + .5f < nextCoord != (pinionMovementSpeed * direction.getDirection()
			.offset() < 0)))
			if (!gantryShaftBlockEntity.canAssembleOn()) {
				setContraptionMotion(Vec3d.ZERO);
				if (!getWorld().isClient)
					disassemble();
				return;
			}

		if (getWorld().isClient)
			return;

		axisMotion = pinionMovementSpeed;
		setContraptionMotion(movementVec);
	}

	@Override
	protected void writeAdditional(NbtCompound compound, boolean spawnPacket) {
		NBTHelper.writeEnum(compound, "GantryAxis", movementAxis);
		if (sequencedOffsetLimit >= 0)
			compound.putDouble("SequencedOffsetLimit", sequencedOffsetLimit);
		super.writeAdditional(compound, spawnPacket);
	}

	protected void readAdditional(NbtCompound compound, boolean spawnData) {
		movementAxis = NBTHelper.readEnum(compound, "GantryAxis", Direction.class);
		sequencedOffsetLimit =
			compound.contains("SequencedOffsetLimit") ? compound.getDouble("SequencedOffsetLimit") : -1;
		super.readAdditional(compound, spawnData);
	}

	@Override
	public Vec3d applyRotation(Vec3d localPos, float partialTicks) {
		return localPos;
	}

	@Override
	public Vec3d reverseRotation(Vec3d localPos, float partialTicks) {
		return localPos;
	}

	@Override
	protected StructureTransform makeStructureTransform() {
		return new StructureTransform(BlockPos.ofFloored(getAnchorVec().add(.5, .5, .5)), 0, 0, 0);
	}

	@Override
	protected float getStalledAngle() {
		return 0;
	}

	@Override
	public void requestTeleport(double p_70634_1_, double p_70634_3_, double p_70634_5_) {}

	@Override
	@Environment(EnvType.CLIENT)
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yw, float pt, int inc, boolean t) {}

	@Override
	protected void handleStallInformation(double x, double y, double z, float angle) {
		setPos(x, y, z);
		clientOffsetDiff = 0;
	}

	@Override
	public ContraptionRotationState getRotationState() {
		return ContraptionRotationState.NONE;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void applyLocalTransforms(MatrixStack matrixStack, float partialTicks) { }

	public void updateClientMotion() {
		float modifier = movementAxis.getDirection()
			.offset();
		Vec3d motion = Vec3d.of(movementAxis.getVector())
			.multiply((axisMotion + clientOffsetDiff * modifier / 2f) * ServerSpeedProvider.get());
		if (sequencedOffsetLimit >= 0)
			motion = VecHelper.clampComponentWise(motion, (float) sequencedOffsetLimit);
		setContraptionMotion(motion);
	}

	public double getAxisCoord() {
		Vec3d anchorVec = getAnchorVec();
		return movementAxis.getAxis()
			.choose(anchorVec.x, anchorVec.y, anchorVec.z);
	}

	public void sendPacket() {
		AllPackets.getChannel()
			.sendToClientsTracking(new GantryContraptionUpdatePacket(getId(), getAxisCoord(), axisMotion, sequencedOffsetLimit), this);
	}

	@Environment(EnvType.CLIENT)
	public static void handlePacket(GantryContraptionUpdatePacket packet) {
		Entity entity = MinecraftClient.getInstance().world.getEntityById(packet.entityID);
		if (!(entity instanceof GantryContraptionEntity))
			return;
		GantryContraptionEntity ce = (GantryContraptionEntity) entity;
		if (ce.movementAxis == null)
			return; // fabric: packet ordering makes this null for a short period
		ce.axisMotion = packet.motion;
		ce.clientOffsetDiff = packet.coord - ce.getAxisCoord();
		ce.sequencedOffsetLimit = packet.sequenceLimit;
	}

}
