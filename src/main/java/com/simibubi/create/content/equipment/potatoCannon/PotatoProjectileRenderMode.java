package com.simibubi.create.content.equipment.potatoCannon;

import static com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileRenderMode.entityRandom;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.utility.AngleHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public interface PotatoProjectileRenderMode {

	@Environment(EnvType.CLIENT)
	void transform(MatrixStack ms, PotatoProjectileEntity entity, float pt);

	public static class Billboard implements PotatoProjectileRenderMode {

		public static final Billboard INSTANCE = new Billboard();

		@Override
		@Environment(EnvType.CLIENT)
		public void transform(MatrixStack ms, PotatoProjectileEntity entity, float pt) {
			MinecraftClient mc = MinecraftClient.getInstance();
			Vec3d p1 = mc.getCameraEntity()
				.getCameraPosVec(pt);
			Vec3d diff = entity.getBoundingBox()
				.getCenter()
				.subtract(p1);

			TransformStack.cast(ms)
				.rotateY(AngleHelper.deg(MathHelper.atan2(diff.x, diff.z)) + 180)
				.rotateX(AngleHelper.deg(MathHelper.atan2(diff.y, MathHelper.sqrt((float) (diff.x * diff.x + diff.z * diff.z)))));
		}

	}

	public static class Tumble extends Billboard {

		public static final Tumble INSTANCE = new Tumble();

		@Override
		@Environment(EnvType.CLIENT)
		public void transform(MatrixStack ms, PotatoProjectileEntity entity, float pt) {
			super.transform(ms, entity, pt);
			TransformStack.cast(ms)
				.rotateZ((entity.age + pt) * 2 * entityRandom(entity, 16))
				.rotateX((entity.age + pt) * entityRandom(entity, 32));
		}

	}

	public static class TowardMotion implements PotatoProjectileRenderMode {

		private int spriteAngleOffset;
		private float spin;

		public TowardMotion(int spriteAngleOffset, float spin) {
			this.spriteAngleOffset = spriteAngleOffset;
			this.spin = spin;
		}

		@Override
		@Environment(EnvType.CLIENT)
		public void transform(MatrixStack ms, PotatoProjectileEntity entity, float pt) {
			Vec3d diff = entity.getVelocity();
			TransformStack.cast(ms)
				.rotateY(AngleHelper.deg(MathHelper.atan2(diff.x, diff.z)))
				.rotateX(270
					+ AngleHelper.deg(MathHelper.atan2(diff.y, -MathHelper.sqrt((float) (diff.x * diff.x + diff.z * diff.z)))));
			TransformStack.cast(ms)
				.rotateY((entity.age + pt) * 20 * spin + entityRandom(entity, 360))
				.rotateZ(-spriteAngleOffset);
		}

	}

	public static class StuckToEntity implements PotatoProjectileRenderMode {

		private Vec3d offset;

		public StuckToEntity(Vec3d offset) {
			this.offset = offset;
		}

		@Override
		@Environment(EnvType.CLIENT)
		public void transform(MatrixStack ms, PotatoProjectileEntity entity, float pt) {
			TransformStack.cast(ms).rotateY(AngleHelper.deg(MathHelper.atan2(offset.x, offset.z)));
		}

	}

	public static int entityRandom(Entity entity, int maxValue) {
		return (System.identityHashCode(entity) * 31) % maxValue;
	}

}
