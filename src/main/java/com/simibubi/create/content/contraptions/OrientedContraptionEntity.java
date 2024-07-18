package com.simibubi.create.content.contraptions;

import static com.simibubi.create.foundation.utility.AngleHelper.angleLerp;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.bearing.StabilizedContraption;
import com.simibubi.create.content.contraptions.minecart.MinecartSim2020;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity.CartMovementMode;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Ex: Minecarts, Couplings <br>
 * Oriented Contraption Entities can rotate freely around two axes
 * simultaneously.
 */
public class OrientedContraptionEntity extends AbstractContraptionEntity {

	private static final Ingredient FUEL_ITEMS = Ingredient.ofItems(Items.COAL, Items.CHARCOAL);

	private static final TrackedData<Optional<UUID>> COUPLING =
		DataTracker.registerData(OrientedContraptionEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private static final TrackedData<Direction> INITIAL_ORIENTATION =
		DataTracker.registerData(OrientedContraptionEntity.class, TrackedDataHandlerRegistry.FACING);

	protected Vec3d motionBeforeStall;
	protected boolean forceAngle;
	private boolean isSerializingFurnaceCart;
	private boolean attachedExtraInventories;
	private boolean manuallyPlaced;

	public float prevYaw;
	public float yaw;
	public float targetYaw;

	public float prevPitch;
	public float pitch;

	public int nonDamageTicks;

	public OrientedContraptionEntity(EntityType<?> type, World world) {
		super(type, world);
		motionBeforeStall = Vec3d.ZERO;
		attachedExtraInventories = false;
		isSerializingFurnaceCart = false;
		nonDamageTicks = 10;
	}

	public static OrientedContraptionEntity create(World world, Contraption contraption, Direction initialOrientation) {
		OrientedContraptionEntity entity =
			new OrientedContraptionEntity(AllEntityTypes.ORIENTED_CONTRAPTION.get(), world);
		entity.setContraption(contraption);
		entity.setInitialOrientation(initialOrientation);
		entity.startAtInitialYaw();
		return entity;
	}

	public static OrientedContraptionEntity createAtYaw(World world, Contraption contraption,
		Direction initialOrientation, float initialYaw) {
		OrientedContraptionEntity entity = create(world, contraption, initialOrientation);
		entity.startAtYaw(initialYaw);
		entity.manuallyPlaced = true;
		return entity;
	}

	public void setInitialOrientation(Direction direction) {
		dataTracker.set(INITIAL_ORIENTATION, direction);
	}

	public Direction getInitialOrientation() {
		return dataTracker.get(INITIAL_ORIENTATION);
	}

	@Override
	public float getYawOffset() {
		return getInitialYaw();
	}

	public float getInitialYaw() {
		return (isInitialOrientationPresent() ? dataTracker.get(INITIAL_ORIENTATION) : Direction.SOUTH).asRotation();
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(COUPLING, Optional.empty());
		dataTracker.startTracking(INITIAL_ORIENTATION, Direction.UP);
	}

	@Override
	public ContraptionRotationState getRotationState() {
		ContraptionRotationState crs = new ContraptionRotationState();

		float yawOffset = getYawOffset();
		crs.zRotation = pitch;
		crs.yRotation = -yaw + yawOffset;

		if (pitch != 0 && yaw != 0) {
			crs.secondYRotation = -yaw;
			crs.yRotation = yawOffset;
		}

		return crs;
	}

	@Override
	public void remove(RemovalReason p_146834_) {
		super.remove(p_146834_);
	}

	@Override
	public void stopRiding() {
		if (!getWorld().isClient && isAlive())
			disassemble();
		super.stopRiding();
	}

	@Override
	protected void readAdditional(NbtCompound compound, boolean spawnPacket) {
		super.readAdditional(compound, spawnPacket);

		if (compound.contains("InitialOrientation"))
			setInitialOrientation(NBTHelper.readEnum(compound, "InitialOrientation", Direction.class));

		yaw = compound.getFloat("Yaw");
		pitch = compound.getFloat("Pitch");
		manuallyPlaced = compound.getBoolean("Placed");

		if (compound.contains("ForceYaw"))
			startAtYaw(compound.getFloat("ForceYaw"));

		NbtList vecNBT = compound.getList("CachedMotion", 6);
		if (!vecNBT.isEmpty()) {
			motionBeforeStall = new Vec3d(vecNBT.getDouble(0), vecNBT.getDouble(1), vecNBT.getDouble(2));
			if (!motionBeforeStall.equals(Vec3d.ZERO))
				targetYaw = prevYaw = yaw += yawFromVector(motionBeforeStall);
			setVelocity(Vec3d.ZERO);
		}

		setCouplingId(compound.contains("OnCoupling") ? compound.getUuid("OnCoupling") : null);
	}

	@Override
	protected void writeAdditional(NbtCompound compound, boolean spawnPacket) {
		super.writeAdditional(compound, spawnPacket);

		if (motionBeforeStall != null)
			compound.put("CachedMotion", toNbtList(motionBeforeStall.x, motionBeforeStall.y, motionBeforeStall.z));

		Direction optional = dataTracker.get(INITIAL_ORIENTATION);
		if (optional.getAxis()
			.isHorizontal())
			NBTHelper.writeEnum(compound, "InitialOrientation", optional);
		if (forceAngle) {
			compound.putFloat("ForceYaw", yaw);
			forceAngle = false;
		}

		compound.putBoolean("Placed", manuallyPlaced);
		compound.putFloat("Yaw", yaw);
		compound.putFloat("Pitch", pitch);

		if (getCouplingId() != null)
			compound.putUuid("OnCoupling", getCouplingId());
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> key) {
		super.onTrackedDataSet(key);
		if (INITIAL_ORIENTATION.equals(key) && isInitialOrientationPresent() && !manuallyPlaced)
			startAtInitialYaw();
	}

	public boolean isInitialOrientationPresent() {
		return dataTracker.get(INITIAL_ORIENTATION)
			.getAxis()
			.isHorizontal();
	}

	public void startAtInitialYaw() {
		startAtYaw(getInitialYaw());
	}

	public void startAtYaw(float yaw) {
		targetYaw = this.yaw = prevYaw = yaw;
		forceAngle = true;
	}

	@Override
	public Vec3d applyRotation(Vec3d localPos, float partialTicks) {
		localPos = VecHelper.rotate(localPos, getInitialYaw(), Axis.Y);
		localPos = VecHelper.rotate(localPos, getPitch(partialTicks), Axis.Z);
		localPos = VecHelper.rotate(localPos, getYaw(partialTicks), Axis.Y);
		return localPos;
	}

	@Override
	public Vec3d reverseRotation(Vec3d localPos, float partialTicks) {
		localPos = VecHelper.rotate(localPos, -getYaw(partialTicks), Axis.Y);
		localPos = VecHelper.rotate(localPos, -getPitch(partialTicks), Axis.Z);
		localPos = VecHelper.rotate(localPos, -getInitialYaw(), Axis.Y);
		return localPos;
	}

	public float getYaw(float partialTicks) {
		return -(partialTicks == 1.0F ? yaw : angleLerp(partialTicks, prevYaw, yaw));
	}

	public float getPitch(float partialTicks) {
		return partialTicks == 1.0F ? pitch : angleLerp(partialTicks, prevPitch, pitch);
	}

	@Override
	protected void tickContraption() {
		if (nonDamageTicks > 0)
			nonDamageTicks--;
		Entity e = getVehicle();
		if (e == null)
			return;

		boolean rotationLock = false;
		boolean pauseWhileRotating = false;
		boolean wasStalled = isStalled();
		if (contraption instanceof MountedContraption) {
			MountedContraption mountedContraption = (MountedContraption) contraption;
			rotationLock = mountedContraption.rotationMode == CartMovementMode.ROTATION_LOCKED;
			pauseWhileRotating = mountedContraption.rotationMode == CartMovementMode.ROTATE_PAUSED;
		}

		Entity riding = e;
		while (riding.getVehicle() != null && !(contraption instanceof StabilizedContraption))
			riding = riding.getVehicle();

		boolean isOnCoupling = false;
		UUID couplingId = getCouplingId();
		isOnCoupling = couplingId != null && riding instanceof AbstractMinecartEntity;

		if (!attachedExtraInventories) {
			attachInventoriesFromRidingCarts(riding, isOnCoupling, couplingId);
			attachedExtraInventories = true;
		}

		boolean rotating = updateOrientation(rotationLock, wasStalled, riding, isOnCoupling);
		if (!rotating || !pauseWhileRotating)
			tickActors();
		boolean isStalled = isStalled();

		MinecartController capability = null;
		if(riding instanceof AbstractMinecartEntity minecart)
			capability = minecart.create$getController();

		if (capability != null) {
			if (!getWorld().isClient())
				capability
					.setStalledExternally(isStalled);
		} else {
			if (isStalled) {
				if (!wasStalled)
					motionBeforeStall = riding.getVelocity();
				riding.setVelocity(0, 0, 0);
			}
			if (wasStalled && !isStalled) {
				riding.setVelocity(motionBeforeStall);
				motionBeforeStall = Vec3d.ZERO;
			}
		}

		if (getWorld().isClient)
			return;

		if (!isStalled()) {
			if (isOnCoupling) {
				Couple<MinecartController> coupledCarts = getCoupledCartsIfPresent();
				if (coupledCarts == null)
					return;
				coupledCarts.map(MinecartController::cart)
					.forEach(this::powerFurnaceCartWithFuelFromStorage);
				return;
			}
			powerFurnaceCartWithFuelFromStorage(riding);
		}
	}

	protected boolean updateOrientation(boolean rotationLock, boolean wasStalled, Entity riding, boolean isOnCoupling) {
		if (isOnCoupling) {
			Couple<MinecartController> coupledCarts = getCoupledCartsIfPresent();
			if (coupledCarts == null)
				return false;

			Vec3d positionVec = coupledCarts.getFirst()
				.cart()
				.getPos();
			Vec3d coupledVec = coupledCarts.getSecond()
				.cart()
				.getPos();

			double diffX = positionVec.x - coupledVec.x;
			double diffY = positionVec.y - coupledVec.y;
			double diffZ = positionVec.z - coupledVec.z;

			prevYaw = yaw;
			prevPitch = pitch;
			yaw = (float) (MathHelper.atan2(diffZ, diffX) * 180 / Math.PI);
			pitch = (float) (Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)) * 180 / Math.PI);

			if (getCouplingId().equals(riding.getUuid())) {
				pitch *= -1;
				yaw += 180;
			}
			return false;
		}

		if (contraption instanceof StabilizedContraption) {
			if (!(riding instanceof OrientedContraptionEntity))
				return false;
			StabilizedContraption stabilized = (StabilizedContraption) contraption;
			Direction facing = stabilized.getFacing();
			if (facing.getAxis()
				.isVertical())
				return false;
			OrientedContraptionEntity parent = (OrientedContraptionEntity) riding;
			prevYaw = yaw;
			yaw = -parent.getYaw(1);
			return false;
		}

		prevYaw = yaw;
		if (wasStalled)
			return false;

		boolean rotating = false;
		Vec3d movementVector = riding.getVelocity();
		Vec3d locationDiff = riding.getPos()
			.subtract(riding.prevX, riding.prevY, riding.prevZ);
		if (!(riding instanceof AbstractMinecartEntity))
			movementVector = locationDiff;
		Vec3d motion = movementVector.normalize();

		if (!rotationLock) {
			if (riding instanceof AbstractMinecartEntity) {
				AbstractMinecartEntity minecartEntity = (AbstractMinecartEntity) riding;
				BlockPos railPosition = minecartEntity.getCurrentRailPos();
				BlockState blockState = getWorld().getBlockState(railPosition);
				if (blockState.getBlock() instanceof AbstractRailBlock) {
					AbstractRailBlock abstractRailBlock = (AbstractRailBlock) blockState.getBlock();
					RailShape railDirection =
						MinecartAndRailUtil.getDirectionOfRail(blockState, getWorld(), railPosition, abstractRailBlock);
					motion = VecHelper.project(motion, MinecartSim2020.getRailVec(railDirection));
				}
			}

			if (motion.length() > 0) {
				targetYaw = yawFromVector(motion);
				if (targetYaw < 0)
					targetYaw += 360;
				if (yaw < 0)
					yaw += 360;
			}

			prevYaw = yaw;
			float maxApproachSpeed = (float) (motion.length() * 12f / (Math.max(1, getBoundingBox().getXLength() / 6f)));
			float yawHint = AngleHelper.getShortestAngleDiff(yaw, yawFromVector(locationDiff));
			float approach = AngleHelper.getShortestAngleDiff(yaw, targetYaw, yawHint);
			approach = MathHelper.clamp(approach, -maxApproachSpeed, maxApproachSpeed);
			yaw += approach;
			if (Math.abs(AngleHelper.getShortestAngleDiff(yaw, targetYaw)) < 1f)
				yaw = targetYaw;
			else
				rotating = true;
		}
		return rotating;
	}

	protected void powerFurnaceCartWithFuelFromStorage(Entity riding) {
		if (!(riding instanceof FurnaceMinecartEntity))
			return;
		FurnaceMinecartEntity furnaceCart = (FurnaceMinecartEntity) riding;

		// Notify to not trigger serialization side-effects
		isSerializingFurnaceCart = true;
		NbtCompound nbt = NBTSerializer.serializeNBTCompound(furnaceCart);
		isSerializingFurnaceCart = false;

		int fuel = nbt.getInt("Fuel");
		int fuelBefore = fuel;
		double pushX = nbt.getDouble("PushX");
		double pushZ = nbt.getDouble("PushZ");

		int i = MathHelper.floor(furnaceCart.getX());
		int j = MathHelper.floor(furnaceCart.getY());
		int k = MathHelper.floor(furnaceCart.getZ());
		if (furnaceCart.getWorld().getBlockState(new BlockPos(i, j - 1, k))
			.isIn(BlockTags.RAILS))
			--j;

		BlockPos blockpos = new BlockPos(i, j, k);
		BlockState blockstate = this.getWorld().getBlockState(blockpos);
		if (blockstate.isIn(BlockTags.RAILS))
			if (fuel > 1)
				riding.setVelocity(riding.getVelocity()
					.normalize()
					.multiply(1));
		if (fuel < 5 && contraption != null) {
			ItemStack coal = ItemHelper.extract(contraption.getSharedInventory(), FUEL_ITEMS, 1, false);
			if (!coal.isEmpty())
				fuel += 3600;
		}

		if (fuel != fuelBefore || pushX != 0 || pushZ != 0) {
			nbt.putInt("Fuel", fuel);
			nbt.putDouble("PushX", 0);
			nbt.putDouble("PushZ", 0);
			NBTSerializer.deserializeNBT(furnaceCart, nbt);
		}
	}

	@Nullable
	public Couple<MinecartController> getCoupledCartsIfPresent() {
		UUID couplingId = getCouplingId();
		if (couplingId == null)
			return null;
		MinecartController controller = CapabilityMinecartController.getIfPresent(getWorld(), couplingId);
		if (controller == null || !controller.isPresent())
			return null;
		UUID coupledCart = controller.getCoupledCart(true);
		MinecartController coupledController = CapabilityMinecartController.getIfPresent(getWorld(), coupledCart);
		if (coupledController == null || !coupledController.isPresent())
			return null;
		return Couple.create(controller, coupledController);
	}

	protected void attachInventoriesFromRidingCarts(Entity riding, boolean isOnCoupling, UUID couplingId) {
		if (!(contraption instanceof MountedContraption mc))
			return;
		if (!isOnCoupling) {
			mc.addExtraInventories(riding);
			return;
		}
		Couple<MinecartController> coupledCarts = getCoupledCartsIfPresent();
		if (coupledCarts == null)
			return;
		coupledCarts.map(MinecartController::cart)
			.forEach(mc::addExtraInventories);
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		return isSerializingFurnaceCart ? nbt : super.writeNbt(nbt);
	}

	@Nullable
	public UUID getCouplingId() {
		Optional<UUID> uuid = dataTracker.get(COUPLING);
		return uuid == null ? null : uuid.isPresent() ? uuid.get() : null;
	}

	public void setCouplingId(UUID id) {
		dataTracker.set(COUPLING, Optional.ofNullable(id));
	}

	@Override
	public Vec3d getAnchorVec() {
		Vec3d anchorVec = super.getAnchorVec();
		return anchorVec.subtract(.5, 0, .5);
	}

	@Override
	public Vec3d getPrevAnchorVec() {
		Vec3d prevAnchorVec = super.getPrevAnchorVec();
		return prevAnchorVec.subtract(.5, 0, .5);
	}

	@Override
	protected StructureTransform makeStructureTransform() {
		BlockPos offset = BlockPos.ofFloored(getAnchorVec().add(.5, .5, .5));
		return new StructureTransform(offset, 0, -yaw + getInitialYaw(), 0);
	}

	@Override
	protected float getStalledAngle() {
		return yaw;
	}

	@Override
	protected void handleStallInformation(double x, double y, double z, float angle) {
		yaw = angle;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void applyLocalTransforms(MatrixStack matrixStack, float partialTicks) {
		float angleInitialYaw = getInitialYaw();
		float angleYaw = getYaw(partialTicks);
		float anglePitch = getPitch(partialTicks);

		matrixStack.translate(-.5f, 0, -.5f);

		Entity ridingEntity = getVehicle();
		if (ridingEntity instanceof AbstractMinecartEntity)
			repositionOnCart(matrixStack, partialTicks, ridingEntity);
		else if (ridingEntity instanceof AbstractContraptionEntity) {
			if (ridingEntity.getVehicle() instanceof AbstractMinecartEntity)
				repositionOnCart(matrixStack, partialTicks, ridingEntity.getVehicle());
			else
				repositionOnContraption(matrixStack, partialTicks, ridingEntity);
		}

		TransformStack.cast(matrixStack)
			.nudge(getId())
			.centre()
			.rotateY(angleYaw)
			.rotateZ(anglePitch)
			.rotateY(angleInitialYaw)
			.unCentre();
	}

	@Environment(EnvType.CLIENT)
	private void repositionOnContraption(MatrixStack matrixStack, float partialTicks, Entity ridingEntity) {
		Vec3d pos = getContraptionOffset(partialTicks, ridingEntity);
		matrixStack.translate(pos.x, pos.y, pos.z);
	}

	// Minecarts do not always render at their exact location, so the contraption
	// has to adjust aswell
	@Environment(EnvType.CLIENT)
	private void repositionOnCart(MatrixStack matrixStack, float partialTicks, Entity ridingEntity) {
		Vec3d cartPos = getCartOffset(partialTicks, ridingEntity);

		if (cartPos == Vec3d.ZERO)
			return;

		matrixStack.translate(cartPos.x, cartPos.y, cartPos.z);
	}

	@Environment(EnvType.CLIENT)
	private Vec3d getContraptionOffset(float partialTicks, Entity ridingEntity) {
		AbstractContraptionEntity parent = (AbstractContraptionEntity) ridingEntity;
		Vec3d passengerPosition = parent.getPassengerPosition(this, partialTicks);
		if (passengerPosition == null)
			return Vec3d.ZERO;

		double x = passengerPosition.x - MathHelper.lerp(partialTicks, this.lastRenderX, this.getX());
		double y = passengerPosition.y - MathHelper.lerp(partialTicks, this.lastRenderY, this.getY());
		double z = passengerPosition.z - MathHelper.lerp(partialTicks, this.lastRenderZ, this.getZ());

		return new Vec3d(x, y, z);
	}

	@Environment(EnvType.CLIENT)
	private Vec3d getCartOffset(float partialTicks, Entity ridingEntity) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) ridingEntity;
		double cartX = MathHelper.lerp(partialTicks, cart.lastRenderX, cart.getX());
		double cartY = MathHelper.lerp(partialTicks, cart.lastRenderY, cart.getY());
		double cartZ = MathHelper.lerp(partialTicks, cart.lastRenderZ, cart.getZ());
		Vec3d cartPos = cart.snapPositionToRail(cartX, cartY, cartZ);

		if (cartPos != null) {
			Vec3d cartPosFront = cart.snapPositionToRailWithOffset(cartX, cartY, cartZ, (double) 0.3F);
			Vec3d cartPosBack = cart.snapPositionToRailWithOffset(cartX, cartY, cartZ, (double) -0.3F);
			if (cartPosFront == null)
				cartPosFront = cartPos;
			if (cartPosBack == null)
				cartPosBack = cartPos;

			cartX = cartPos.x - cartX;
			cartY = (cartPosFront.y + cartPosBack.y) / 2.0D - cartY;
			cartZ = cartPos.z - cartZ;

			return new Vec3d(cartX, cartY, cartZ);
		}

		return Vec3d.ZERO;
	}

	@Environment(EnvType.CLIENT)
	public static void handleRelocationPacket(ContraptionRelocationPacket packet) {
		if (MinecraftClient.getInstance().world.getEntityById(packet.entityID) instanceof OrientedContraptionEntity oce)
			oce.nonDamageTicks = 10;
	}
}
