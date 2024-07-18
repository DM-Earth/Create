package com.simibubi.create.content.contraptions;

import static com.simibubi.create.foundation.utility.AngleHelper.angleLerp;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Ex: Pistons, bearings <br>
 * Controlled Contraption Entities can rotate around one axis and translate.
 * <br>
 * They are bound to an {@link IControlContraption}
 */
public class ControlledContraptionEntity extends AbstractContraptionEntity {

	protected BlockPos controllerPos;
	protected Axis rotationAxis;
	protected float prevAngle;
	protected float angle;
	protected float angleDelta;

	public ControlledContraptionEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	public static ControlledContraptionEntity create(World world, IControlContraption controller,
		Contraption contraption) {
		ControlledContraptionEntity entity =
			new ControlledContraptionEntity(AllEntityTypes.CONTROLLED_CONTRAPTION.get(), world);
		entity.controllerPos = controller.getBlockPosition();
		entity.setContraption(contraption);
		return entity;
	}

	@Override
	public void setPosition(double x, double y, double z) {
		super.setPosition(x, y, z);
		if (!getWorld().isClient())
			return;
		for (Entity entity : getPassengerList())
			updatePassengerPosition(entity);
	}

	@Override
	public Vec3d getContactPointMotion(Vec3d globalContactPoint) {
		if (contraption instanceof TranslatingContraption)
			return getVelocity();
		return super.getContactPointMotion(globalContactPoint);
	}

	@Override
	protected void setContraption(Contraption contraption) {
		super.setContraption(contraption);
		if (contraption instanceof BearingContraption)
			rotationAxis = ((BearingContraption) contraption).getFacing()
				.getAxis();
	}

	@Override
	protected void readAdditional(NbtCompound compound, boolean spawnPacket) {
		super.readAdditional(compound, spawnPacket);
		if (compound.contains("Controller")) // legacy
			controllerPos = NbtHelper.toBlockPos(compound.getCompound("Controller"));
		else
			controllerPos = NbtHelper.toBlockPos(compound.getCompound("ControllerRelative"))
				.add(getBlockPos());
		if (compound.contains("Axis"))
			rotationAxis = NBTHelper.readEnum(compound, "Axis", Axis.class);
		angle = compound.getFloat("Angle");
	}

	@Override
	protected void writeAdditional(NbtCompound compound, boolean spawnPacket) {
		super.writeAdditional(compound, spawnPacket);
		compound.put("ControllerRelative", NbtHelper.fromBlockPos(controllerPos.subtract(getBlockPos())));
		if (rotationAxis != null)
			NBTHelper.writeEnum(compound, "Axis", rotationAxis);
		compound.putFloat("Angle", angle);
	}

	@Override
	public ContraptionRotationState getRotationState() {
		ContraptionRotationState crs = new ContraptionRotationState();
		if (rotationAxis == Axis.X)
			crs.xRotation = angle;
		if (rotationAxis == Axis.Y)
			crs.yRotation = angle;
		if (rotationAxis == Axis.Z)
			crs.zRotation = angle;
		return crs;
	}

	@Override
	public Vec3d applyRotation(Vec3d localPos, float partialTicks) {
		localPos = VecHelper.rotate(localPos, getAngle(partialTicks), rotationAxis);
		return localPos;
	}

	@Override
	public Vec3d reverseRotation(Vec3d localPos, float partialTicks) {
		localPos = VecHelper.rotate(localPos, -getAngle(partialTicks), rotationAxis);
		return localPos;
	}

	public void setAngle(float angle) {
		this.angle = angle;

		if (!getWorld().isClient())
			return;
		for (Entity entity : getPassengerList())
			updatePassengerPosition(entity);
	}

	public float getAngle(float partialTicks) {
		return partialTicks == 1.0F ? angle : angleLerp(partialTicks, prevAngle, angle);
	}

	public void setRotationAxis(Axis rotationAxis) {
		this.rotationAxis = rotationAxis;
	}

	public Axis getRotationAxis() {
		return rotationAxis;
	}

	@Override
	public void requestTeleport(double p_70634_1_, double p_70634_3_, double p_70634_5_) {}

	@Override
	@Environment(EnvType.CLIENT)
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yw, float pt, int inc, boolean t) {}

	protected void tickContraption() {
		angleDelta = angle - prevAngle;
		prevAngle = angle;
		tickActors();

		if (controllerPos == null)
			return;
		if (!getWorld().canSetBlock(controllerPos))
			return;
		IControlContraption controller = getController();
		if (controller == null) {
			discard();
			return;
		}
		if (!controller.isAttachedTo(this)) {
			controller.attach(this);
			if (getWorld().isClient)
				setPosition(getX(), getY(), getZ());
		}
	}

	@Override
	protected boolean shouldActorTrigger(MovementContext context, StructureBlockInfo blockInfo, MovementBehaviour actor,
		Vec3d actorPosition, BlockPos gridPosition) {
		if (super.shouldActorTrigger(context, blockInfo, actor, actorPosition, gridPosition))
			return true;

		// Special activation timer for actors in the center of a bearing contraption
		if (!(contraption instanceof BearingContraption))
			return false;
		BearingContraption bc = (BearingContraption) contraption;
		Direction facing = bc.getFacing();
		Vec3d activeAreaOffset = actor.getActiveAreaOffset(context);
		if (!activeAreaOffset.multiply(VecHelper.axisAlingedPlaneOf(Vec3d.of(facing.getVector())))
			.equals(Vec3d.ZERO))
			return false;
		if (!VecHelper.onSameAxis(blockInfo.pos(), BlockPos.ORIGIN, facing.getAxis()))
			return false;
		context.motion = Vec3d.of(facing.getVector())
			.multiply(angleDelta / 360.0);
		context.relativeMotion = context.motion;
		int timer = context.data.getInt("StationaryTimer");
		if (timer > 0) {
			context.data.putInt("StationaryTimer", timer - 1);
			return false;
		}

		context.data.putInt("StationaryTimer", 20);
		return true;
	}

	protected IControlContraption getController() {
		if (controllerPos == null)
			return null;
		if (!getWorld().canSetBlock(controllerPos))
			return null;
		BlockEntity be = getWorld().getBlockEntity(controllerPos);
		if (!(be instanceof IControlContraption))
			return null;
		return (IControlContraption) be;
	}

	@Override
	protected StructureTransform makeStructureTransform() {
		BlockPos offset = BlockPos.ofFloored(getAnchorVec().add(.5, .5, .5));
		float xRot = rotationAxis == Axis.X ? angle : 0;
		float yRot = rotationAxis == Axis.Y ? angle : 0;
		float zRot = rotationAxis == Axis.Z ? angle : 0;
		return new StructureTransform(offset, xRot, yRot, zRot);
	}

	@Override
	protected void onContraptionStalled() {
		IControlContraption controller = getController();
		if (controller != null)
			controller.onStall();
		super.onContraptionStalled();
	}

	@Override
	protected float getStalledAngle() {
		return angle;
	}

	@Override
	protected void handleStallInformation(double x, double y, double z, float angle) {
		setPos(x, y, z);
		this.angle = this.prevAngle = angle;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void applyLocalTransforms(MatrixStack matrixStack, float partialTicks) {
		float angle = getAngle(partialTicks);
		Axis axis = getRotationAxis();

		TransformStack.cast(matrixStack)
			.nudge(getId())
			.centre()
			.rotate(angle, axis)
			.unCentre();
	}
}
