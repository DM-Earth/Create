package com.simibubi.create.content.trains;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllSpecialTextures;
import io.github.fabricators_of_create.porting_lib.util.ParticleHelper;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class CubeParticle extends Particle {

	public static final Vec3d[] CUBE = {
		// TOP
		new Vec3d(1, 1, -1), new Vec3d(1, 1, 1), new Vec3d(-1, 1, 1), new Vec3d(-1, 1, -1),

		// BOTTOM
		new Vec3d(-1, -1, -1), new Vec3d(-1, -1, 1), new Vec3d(1, -1, 1), new Vec3d(1, -1, -1),

		// FRONT
		new Vec3d(-1, -1, 1), new Vec3d(-1, 1, 1), new Vec3d(1, 1, 1), new Vec3d(1, -1, 1),

		// BACK
		new Vec3d(1, -1, -1), new Vec3d(1, 1, -1), new Vec3d(-1, 1, -1), new Vec3d(-1, -1, -1),

		// LEFT
		new Vec3d(-1, -1, -1), new Vec3d(-1, 1, -1), new Vec3d(-1, 1, 1), new Vec3d(-1, -1, 1),

		// RIGHT
		new Vec3d(1, -1, 1), new Vec3d(1, 1, 1), new Vec3d(1, 1, -1), new Vec3d(1, -1, -1) };

	private static final ParticleTextureSheet RENDER_TYPE = new ParticleTextureSheet() {
		@Override
		public void begin(BufferBuilder builder, TextureManager textureManager) {
			AllSpecialTextures.BLANK.bind();

			// transparent, additive blending
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);

			// opaque
//			RenderSystem.depthMask(true);
//			RenderSystem.disableBlend();
//			RenderSystem.enableLighting();

			builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
		}

		@Override
		public void draw(Tessellator tessellator) {
			tessellator.draw();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
				GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		}
	};

	protected float scale;
	protected boolean hot;

	public CubeParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
		super(world, x, y, z);
		this.velocityX = motionX;
		this.velocityY = motionY;
		this.velocityZ = motionZ;

		setScale(0.2F);
	}

	public void setScale(float scale) {
		this.scale = scale;
		this.setBoundingBoxSpacing(scale * 0.5f, scale * 0.5f);
	}

	public void averageAge(int age) {
		this.maxAge = (int) (age + (random.nextDouble() * 2D - 1D) * 8);
	}

	public void setHot(boolean hot) {
		this.hot = hot;
	}

	private boolean billowing = false;

	@Override
	public void tick() {
		if (this.hot && this.age > 0) {
			if (this.prevPosY == this.y) {
				billowing = true;
				ParticleHelper.setStoppedByCollision(this, false); // Prevent motion being ignored due to vertical collision
				if (this.velocityX == 0 && this.velocityZ == 0) {
					Vec3d diff = Vec3d.of(BlockPos.ofFloored(x, y, z))
						.add(0.5, 0.5, 0.5)
						.subtract(x, y, z);
					this.velocityX = -diff.x * 0.1;
					this.velocityZ = -diff.z * 0.1;
				}
				this.velocityX *= 1.1;
				this.velocityY *= 0.9;
				this.velocityZ *= 1.1;
			} else if (billowing) {
				this.velocityY *= 1.2;
			}
		}
		super.tick();
	}

	@Override
	public void buildGeometry(VertexConsumer builder, Camera renderInfo, float p_225606_3_) {
		Vec3d projectedView = renderInfo.getPos();
		float lerpedX = (float) (MathHelper.lerp(p_225606_3_, this.prevPosX, this.x) - projectedView.getX());
		float lerpedY = (float) (MathHelper.lerp(p_225606_3_, this.prevPosY, this.y) - projectedView.getY());
		float lerpedZ = (float) (MathHelper.lerp(p_225606_3_, this.prevPosZ, this.z) - projectedView.getZ());

		// int light = getBrightnessForRender(p_225606_3_);
		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		double ageMultiplier = 1 - Math.pow(MathHelper.clamp(age + p_225606_3_, 0, maxAge), 3) / Math.pow(maxAge, 3);

		for (int i = 0; i < 6; i++) {
			// 6 faces to a cube
			for (int j = 0; j < 4; j++) {
				Vec3d vec = CUBE[i * 4 + j].multiply(-1);
				vec = vec
					/* .rotate(?) */
					.multiply(scale * ageMultiplier)
					.add(lerpedX, lerpedY, lerpedZ);

				builder.vertex(vec.x, vec.y, vec.z)
					.texture(j / 2, j % 2)
					.color(red, green, blue, alpha)
					.light(light)
					.next();
			}
		}
	}

	@Override
	public ParticleTextureSheet getType() {
		return RENDER_TYPE;
	}

	public static class Factory implements ParticleFactory<CubeParticleData> {

		@Override
		public Particle createParticle(CubeParticleData data, ClientWorld world, double x, double y, double z, double motionX,
			double motionY, double motionZ) {
			CubeParticle particle = new CubeParticle(world, x, y, z, motionX, motionY, motionZ);
			particle.setColor(data.r, data.g, data.b);
			particle.setScale(data.scale);
			particle.averageAge(data.avgAge);
			particle.setHot(data.hot);
			return particle;
		}
	}
}
