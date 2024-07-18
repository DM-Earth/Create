package com.simibubi.create.content.contraptions.actors.seat;

import com.simibubi.create.AllEntityTypes;

import io.github.fabricators_of_create.porting_lib.entity.IEntityAdditionalSpawnData;
import io.github.fabricators_of_create.porting_lib.entity.PortingLibEntity;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {

	public SeatEntity(EntityType<?> p_i48580_1_, World p_i48580_2_) {
		super(p_i48580_1_, p_i48580_2_);
	}

	public SeatEntity(World world, BlockPos pos) {
		this(AllEntityTypes.SEAT.get(), world);
		noClip = true;
	}

	public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
//		@SuppressWarnings("unchecked")
//		EntityType.Builder<SeatEntity> entityBuilder = (EntityType.Builder<SeatEntity>) builder;
		return builder.dimensions(EntityDimensions.fixed(0.25f, 0.35f));
	}

	@Override
	public void setPosition(double x, double y, double z) {
		super.setPosition(x, y, z);
		Box bb = getBoundingBox();
		Vec3d diff = new Vec3d(x, y, z).subtract(bb.getCenter());
		setBoundingBox(bb.offset(diff));
	}

	@Override
	protected void updatePassengerPosition(Entity pEntity, Entity.PositionUpdater pCallback) {
		if (!this.hasPassenger(pEntity))
			return;
		double d0 = this.getY() + this.getMountedHeightOffset() + pEntity.getHeightOffset();
		pCallback.accept(pEntity, this.getX(), d0 + getCustomEntitySeatOffset(pEntity), this.getZ());
	}

	public static double getCustomEntitySeatOffset(Entity entity) {
		if (entity instanceof SlimeEntity)
			return 0.25f;
		if (entity instanceof ParrotEntity)
			return 1 / 16f;
		if (entity instanceof SkeletonEntity)
			return 1 / 8f;
		if (entity instanceof CreeperEntity)
			return 1 / 8f;
		if (entity instanceof CatEntity)
			return 1 / 8f;
		if (entity instanceof WolfEntity)
			return 1 / 16f;
		if (entity instanceof FrogEntity)
			return 1 / 8f + 1 / 64f;
		return 0;
	}

	@Override
	public void setVelocity(Vec3d p_213317_1_) {}

	@Override
	public void tick() {
		if (getWorld().isClient)
			return;
		boolean blockPresent = getWorld().getBlockState(getBlockPos())
			.getBlock() instanceof SeatBlock;
		if (hasPassengers() && blockPresent)
			return;
		this.discard();
	}

	@Override
	protected boolean canStartRiding(Entity entity) {
		// Fake Players (tested with deployers) have a BUNCH of weird issues, don't let
		// them ride seats
		return !(entity instanceof PlayerEntity player && player instanceof FakePlayer);
	}

	@Override
	protected void removePassenger(Entity entity) {
		super.removePassenger(entity);
		if (entity instanceof TameableEntity ta)
			ta.setInSittingPose(false);
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity pLivingEntity) {
		return super.updatePassengerForDismount(pLivingEntity).add(0, 0.5f, 0);
	}

	@Override
	protected void initDataTracker() {}

	@Override
	protected void readCustomDataFromNbt(NbtCompound p_70037_1_) {}

	@Override
	protected void writeCustomDataToNbt(NbtCompound p_213281_1_) {}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return PortingLibEntity.getEntitySpawningPacket(this);
	}

	public static class Render extends EntityRenderer<SeatEntity> {

		public Render(EntityRendererFactory.Context context) {
			super(context);
		}

		@Override
		public boolean shouldRender(SeatEntity p_225626_1_, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_,
			double p_225626_7_) {
			return false;
		}

		@Override
		public Identifier getTexture(SeatEntity p_110775_1_) {
			return null;
		}
	}

	@Override
	public void writeSpawnData(PacketByteBuf buffer) {}

	@Override
	public void readSpawnData(PacketByteBuf additionalData) {}
}
