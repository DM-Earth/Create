package com.simibubi.create.content.equipment.bell;

import org.joml.Quaternionf;
import com.simibubi.create.AllParticleTypes;
import io.github.fabricators_of_create.porting_lib.util.ParticleHelper;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SoulParticle extends CustomRotationParticle {

	private final SpriteProvider animatedSprite;

	protected int startTicks;
	protected int endTicks;
	protected int numLoops;

	protected int firstStartFrame = 0;
	protected int startFrames = 17;

	protected int firstLoopFrame = 17;
	protected int loopFrames = 16;

	protected int firstEndFrame = 33;
	protected int endFrames = 20;

	protected AnimationStage animationStage;

	protected int totalFrames = 53;
	protected int ticksPerFrame = 2;

	protected boolean isPerimeter = false;
	protected boolean isExpandingPerimeter = false;
	protected boolean isVisible = true;
	protected int perimeterFrames = 8;

	public SoulParticle(ClientWorld worldIn, double x, double y, double z, double vx, double vy, double vz,
		SpriteProvider spriteSet, ParticleEffect data) {
		super(worldIn, x, y, z, spriteSet, 0);
		this.animatedSprite = spriteSet;
		this.scale = 0.5f;
		this.setBoundingBoxSpacing(this.scale, this.scale);

		this.loopLength = loopFrames + (int) (this.random.nextFloat() * 5f - 4f);
		this.startTicks = startFrames + (int) (this.random.nextFloat() * 5f - 4f);
		this.endTicks = endFrames + (int) (this.random.nextFloat() * 5f - 4f);
		this.numLoops = (int) (1f + this.random.nextFloat() * 2f);

		this.setFrame(0);
		ParticleHelper.setStoppedByCollision(this, true); // disable movement
		this.mirror = this.random.nextBoolean();

		this.isPerimeter = data instanceof PerimeterData;
		this.isExpandingPerimeter = data instanceof ExpandingPerimeterData;
		this.animationStage = !isPerimeter ? new StartAnimation(this) : new PerimeterAnimation(this);
		if (isPerimeter) {
			prevPosY = y -= .5f - 1 / 128f;
			totalFrames = perimeterFrames;
			isVisible = false;
		}
	}

	@Override
	public void tick() {
		animationStage.tick();
		animationStage = animationStage.getNext();

		BlockPos pos = BlockPos.ofFloored(x, y, z);
		if (animationStage == null)
			markDead();
		if (!SoulPulseEffect.isDark(world, pos)) {
			isVisible = true;
			if (!isPerimeter)
				markDead();
		} else if (isPerimeter)
			isVisible = false;
	}

	@Override
	public void buildGeometry(VertexConsumer builder, Camera camera, float partialTicks) {
		if (!isVisible)
			return;
		super.buildGeometry(builder, camera, partialTicks);
	}

	public void setFrame(int frame) {
		if (frame >= 0 && frame < totalFrames)
			setSprite(animatedSprite.getSprite(frame, totalFrames));
	}

	@Override
	public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
		if (isPerimeter)
			return RotationAxis.POSITIVE_X.rotationDegrees(90);
		return new Quaternionf().rotationXYZ(0, -camera.getYaw() * MathHelper.RADIANS_PER_DEGREE, 0);
	}

	public static class Data extends BasicParticleData<SoulParticle> {
		@Override
		public IBasicParticleFactory<SoulParticle> getBasicFactory() {
			return (worldIn, x, y, z, vx, vy, vz, spriteSet) -> new SoulParticle(worldIn, x, y, z, vx, vy, vz,
				spriteSet, this);
		}

		@Override
		public ParticleType<?> getType() {
			return AllParticleTypes.SOUL.get();
		}
	}

	public static class PerimeterData extends BasicParticleData<SoulParticle> {
		@Override
		public IBasicParticleFactory<SoulParticle> getBasicFactory() {
			return (worldIn, x, y, z, vx, vy, vz, spriteSet) -> new SoulParticle(worldIn, x, y, z, vx, vy, vz,
				spriteSet, this);
		}

		@Override
		public ParticleType<?> getType() {
			return AllParticleTypes.SOUL_PERIMETER.get();
		}
	}

	public static class ExpandingPerimeterData extends PerimeterData {
		@Override
		public ParticleType<?> getType() {
			return AllParticleTypes.SOUL_EXPANDING_PERIMETER.get();
		}
	}

	public static abstract class AnimationStage {

		protected final SoulParticle particle;

		protected int ticks;
		protected int animAge;

		public AnimationStage(SoulParticle particle) {
			this.particle = particle;
		}

		public void tick() {
			ticks++;

			if (ticks % particle.ticksPerFrame == 0)
				animAge++;
		}

		public float getAnimAge() {
			return (float) animAge;
		}

		public abstract AnimationStage getNext();
	}

	public static class StartAnimation extends AnimationStage {

		public StartAnimation(SoulParticle particle) {
			super(particle);
		}

		@Override
		public void tick() {
			super.tick();

			particle.setFrame(
				particle.firstStartFrame + (int) (getAnimAge() / (float) particle.startTicks * particle.startFrames));
		}

		@Override
		public AnimationStage getNext() {
			if (animAge < particle.startTicks)
				return this;
			else
				return new LoopAnimation(particle);
		}
	}

	public static class LoopAnimation extends AnimationStage {

		int loops;

		public LoopAnimation(SoulParticle particle) {
			super(particle);
		}

		@Override
		public void tick() {
			super.tick();

			int loopTick = getLoopTick();

			if (loopTick == 0)
				loops++;

			particle.setFrame(particle.firstLoopFrame + loopTick);// (int) (((float) loopTick / (float)
																	// particle.loopLength) * particle.loopFrames));

		}

		private int getLoopTick() {
			return animAge % particle.loopFrames;
		}

		@Override
		public AnimationStage getNext() {
			if (loops <= particle.numLoops)
				return this;
			else
				return new EndAnimation(particle);
		}
	}

	public static class EndAnimation extends AnimationStage {

		public EndAnimation(SoulParticle particle) {
			super(particle);
		}

		@Override
		public void tick() {
			super.tick();

			particle.setFrame(
				particle.firstEndFrame + (int) ((getAnimAge() / (float) particle.endTicks) * particle.endFrames));

		}

		@Override
		public AnimationStage getNext() {
			if (animAge < particle.endTicks)
				return this;
			else
				return null;
		}
	}

	public static class PerimeterAnimation extends AnimationStage {

		public PerimeterAnimation(SoulParticle particle) {
			super(particle);
		}

		@Override
		public void tick() {
			super.tick();
			particle.setFrame((int) getAnimAge() % particle.perimeterFrames);
		}

		@Override
		public AnimationStage getNext() {
			if (animAge < (particle.isExpandingPerimeter ? 8
				: particle.startTicks + particle.endTicks + particle.numLoops * particle.loopLength))
				return this;
			else
				return null;
		}
	}
}
