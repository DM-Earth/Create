package com.simibubi.create.foundation.ponder.element;

import java.util.function.Supplier;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.ParrotEntity.Variant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import com.simibubi.create.foundation.utility.AngleHelper;

public class ParrotElement extends AnimatedSceneElement {

	private Vec3d location;
	private ParrotEntity entity;
	private ParrotPose pose;
	private boolean deferConductor = false;
	private Supplier<? extends ParrotPose> initialPose;

	public static ParrotElement create(Vec3d location, Supplier<? extends ParrotPose> pose) {
		return new ParrotElement(location, pose);
	}

	protected ParrotElement(Vec3d location, Supplier<? extends ParrotPose> pose) {
		this.location = location;
		initialPose = pose;
		setPose(initialPose.get());
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		setPose(initialPose.get());
		entity.setPos(0, 0, 0);
		entity.prevX = 0;
		entity.prevY = 0;
		entity.prevZ = 0;
		entity.lastRenderX = 0;
		entity.lastRenderY = 0;
		entity.lastRenderZ = 0;
		entity.setPitch(entity.prevPitch = 0);
		entity.setYaw(entity.prevYaw = 180);
		entity.getCustomData()
			.remove("TrainHat");
		deferConductor = false;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		if (entity == null) {
			entity = pose.create(scene.getWorld());
			entity.setYaw(entity.prevYaw = 180);
			if (deferConductor)
				setConductor(deferConductor);
			deferConductor = false;
		}

		entity.age++;
		entity.prevHeadYaw = entity.headYaw;
		entity.prevMaxWingDeviation = entity.maxWingDeviation;
		entity.prevFlapProgress = entity.flapProgress;
		entity.setOnGround(true);

		entity.prevX = entity.getX();
		entity.prevY = entity.getY();
		entity.prevZ = entity.getZ();
		entity.prevYaw = entity.getYaw();
		entity.prevPitch = entity.getPitch();

		pose.tick(scene, entity, location);

		entity.lastRenderX = entity.getX();
		entity.lastRenderY = entity.getY();
		entity.lastRenderZ = entity.getZ();
	}

	public void setPositionOffset(Vec3d position, boolean immediate) {
		if (entity == null)
			return;
		entity.setPosition(position.x, position.y, position.z);
		if (!immediate)
			return;
		entity.prevX = position.x;
		entity.prevY = position.y;
		entity.prevZ = position.z;
	}

	public void setRotation(Vec3d eulers, boolean immediate) {
		if (entity == null)
			return;
		entity.setPitch((float) eulers.x);
		entity.setYaw((float) eulers.y);
		if (!immediate)
			return;
		entity.prevPitch = entity.getPitch();
		entity.prevYaw = entity.getYaw();
	}

	public void setConductor(boolean isConductor) {
		if (entity == null) {
			deferConductor = isConductor;
			return;
		}
		NbtCompound data = entity.getCustomData();
		if (isConductor)
			data.putBoolean("TrainHat", true);
		else
			data.remove("TrainHat");
	}

	public Vec3d getPositionOffset() {
		return entity != null ? entity.getPos() : Vec3d.ZERO;
	}

	public Vec3d getRotation() {
		return entity != null ? new Vec3d(entity.getPitch(), entity.getYaw(), 0) : Vec3d.ZERO;
	}

	@Override
	protected void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {
		EntityRenderDispatcher entityrenderermanager = MinecraftClient.getInstance()
			.getEntityRenderDispatcher();

		if (entity == null) {
			entity = pose.create(world);
			entity.setYaw(entity.prevYaw = 180);
		}

		ms.push();
		ms.translate(location.x, location.y, location.z);
		ms.translate(MathHelper.lerp(pt, entity.prevX, entity.getX()), MathHelper.lerp(pt, entity.prevY, entity.getY()),
			MathHelper.lerp(pt, entity.prevZ, entity.getZ()));

		TransformStack.cast(ms)
			.rotateY(AngleHelper.angleLerp(pt, entity.prevYaw, entity.getYaw()));

		entityrenderermanager.render(entity, 0, 0, 0, 0, pt, ms, buffer, lightCoordsFromFade(fade));
		ms.pop();
	}

	public void setPose(ParrotPose pose) {
		this.pose = pose;
	}

	public static abstract class ParrotPose {

		abstract void tick(PonderScene scene, ParrotEntity entity, Vec3d location);

		ParrotEntity create(PonderWorld world) {
			ParrotEntity entity = new ParrotEntity(EntityType.PARROT, world);
			Variant[] variants = ParrotEntity.Variant.values();
			ParrotEntity.Variant variant = variants[Create.RANDOM.nextInt(variants.length)];
			entity.setVariant(variant == Variant.BLUE ? Variant.RED_BLUE : variant); // blue parrots are difficult to see
			return entity;
		}

	}

	public static class DancePose extends ParrotPose {

		@Override
		ParrotEntity create(PonderWorld world) {
			ParrotEntity entity = super.create(world);
			entity.setNearbySongPlaying(BlockPos.ORIGIN, true);
			return entity;
		}

		@Override
		void tick(PonderScene scene, ParrotEntity entity, Vec3d location) {
			entity.prevYaw = entity.getYaw();
			entity.setYaw(entity.getYaw() - 2);
		}

	}

	public static class FlappyPose extends ParrotPose {

		@Override
		void tick(PonderScene scene, ParrotEntity entity, Vec3d location) {
			double length = entity.getPos()
				.subtract(entity.lastRenderX, entity.lastRenderY, entity.lastRenderZ)
				.length();
			entity.setOnGround(false);
			double phase = Math.min(length * 15, 8);
			float f = (float) ((PonderUI.ponderTicks % 100) * phase);
			entity.maxWingDeviation = MathHelper.sin(f) + 1;
			if (length == 0)
				entity.maxWingDeviation = 0;
		}

	}

	public static class SpinOnComponentPose extends ParrotPose {

		private BlockPos componentPos;

		public SpinOnComponentPose(BlockPos componentPos) {
			this.componentPos = componentPos;
		}

		@Override
		void tick(PonderScene scene, ParrotEntity entity, Vec3d location) {
			BlockEntity blockEntity = scene.getWorld()
				.getBlockEntity(componentPos);
			if (!(blockEntity instanceof KineticBlockEntity))
				return;
			float rpm = ((KineticBlockEntity) blockEntity).getSpeed();
			entity.prevYaw = entity.getYaw();
			entity.setYaw(entity.getYaw() + (rpm * .3f));
		}

	}

	public static abstract class FaceVecPose extends ParrotPose {

		@Override
		void tick(PonderScene scene, ParrotEntity entity, Vec3d location) {
			Vec3d p_200602_2_ = getFacedVec(scene);
			Vec3d Vector3d = location.add(entity.getCameraPosVec(0));
			double d0 = p_200602_2_.x - Vector3d.x;
			double d1 = p_200602_2_.y - Vector3d.y;
			double d2 = p_200602_2_.z - Vector3d.z;
			double d3 = MathHelper.sqrt((float) (d0 * d0 + d2 * d2));
			float targetPitch = MathHelper.wrapDegrees((float) -(MathHelper.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
			float targetYaw = MathHelper.wrapDegrees((float) -(MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) + 90);

			entity.setPitch(AngleHelper.angleLerp(.4f, entity.getPitch(), targetPitch));
			entity.setYaw(AngleHelper.angleLerp(.4f, entity.getYaw(), targetYaw));
		}

		protected abstract Vec3d getFacedVec(PonderScene scene);

	}

	public static class FacePointOfInterestPose extends FaceVecPose {

		@Override
		protected Vec3d getFacedVec(PonderScene scene) {
			return scene.getPointOfInterest();
		}

	}

	public static class FaceCursorPose extends FaceVecPose {

		@Override
		protected Vec3d getFacedVec(PonderScene scene) {
			MinecraftClient minecraft = MinecraftClient.getInstance();
			Window w = minecraft.getWindow();
			double mouseX = minecraft.mouse.getX() * w.getScaledWidth() / w.getWidth();
			double mouseY = minecraft.mouse.getY() * w.getScaledHeight() / w.getHeight();
			return scene.getTransform()
				.screenToScene(mouseX, mouseY, 300, 0);
		}

	}

}
