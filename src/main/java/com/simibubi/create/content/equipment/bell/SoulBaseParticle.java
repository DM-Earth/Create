package com.simibubi.create.content.equipment.bell;

import org.joml.Quaternionf;
import com.simibubi.create.AllParticleTypes;
import io.github.fabricators_of_create.porting_lib.util.ParticleHelper;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;

public class SoulBaseParticle extends CustomRotationParticle {

	private final SpriteProvider animatedSprite;

	public SoulBaseParticle(ClientWorld worldIn, double x, double y, double z, double vx, double vy, double vz,
                            SpriteProvider spriteSet) {
		super(worldIn, x, y, z, spriteSet, 0);
		this.animatedSprite = spriteSet;
		this.scale = 0.5f;
		this.setBoundingBoxSpacing(this.scale, this.scale);
		this.loopLength = 16 + (int) (this.random.nextFloat() * 2f - 1f);
		this.maxAge = (int) (90.0F / (this.random.nextFloat() * 0.36F + 0.64F));
		this.selectSpriteLoopingWithAge(animatedSprite);
		ParticleHelper.setStoppedByCollision(this, true); // disable movement
	}

	@Override
	public void tick() {
		selectSpriteLoopingWithAge(animatedSprite);

		BlockPos pos = BlockPos.ofFloored(x, y, z);
		if (age++ >= maxAge || !SoulPulseEffect.isDark(world, pos))
			markDead();
	}

	@Override
	public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
		return RotationAxis.POSITIVE_X.rotationDegrees(90);
	}

	public static class Data extends BasicParticleData<SoulBaseParticle> {
		@Override
		public IBasicParticleFactory<SoulBaseParticle> getBasicFactory() {
			return SoulBaseParticle::new;
		}

		@Override
		public ParticleType<?> getType() {
			return AllParticleTypes.SOUL_BASE.get();
		}
	}
}
