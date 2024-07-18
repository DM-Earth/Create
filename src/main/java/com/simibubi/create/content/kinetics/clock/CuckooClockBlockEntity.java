package com.simibubi.create.content.kinetics.clock;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World.ExplosionSourceType;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

public class CuckooClockBlockEntity extends KineticBlockEntity {

	public LerpedFloat hourHand = LerpedFloat.angular();
	public LerpedFloat minuteHand = LerpedFloat.angular();
	public LerpedFloat animationProgress = LerpedFloat.linear();
	public Animation animationType;
	private boolean sendAnimationUpdate;

	enum Animation {
		PIG, CREEPER, SURPRISE, NONE;
	}

	public CuckooClockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		animationType = Animation.NONE;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.CUCKOO_CLOCK);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (clientPacket && compound.contains("Animation")) {
			animationType = NBTHelper.readEnum(compound, "Animation", Animation.class);
			animationProgress.startWithValue(0);
		}
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		if (clientPacket && sendAnimationUpdate)
			NBTHelper.writeEnum(compound, "Animation", animationType);
		sendAnimationUpdate = false;
		super.write(compound, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();
		if (getSpeed() == 0)
			return;


		boolean isNatural = world.getDimension().natural();
		int dayTime = (int) ((world.getTimeOfDay() * (isNatural ? 1 : 24)) % 24000);
		int hours = (dayTime / 1000 + 6) % 24;
		int minutes = (dayTime % 1000) * 60 / 1000;

		if (!isNatural) {
			if (world.isClient) {
				moveHands(hours, minutes);

				if (AnimationTickHolder.getTicks() % 6 == 0)
					playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1 / 16f, 2f);
				else if (AnimationTickHolder.getTicks() % 3 == 0)
					playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1 / 16f, 1.5f);
			}
			return;
		}

		if (!world.isClient) {
			if (animationType == Animation.NONE) {
				if (hours == 12 && minutes < 5)
					startAnimation(Animation.PIG);
				if (hours == 18 && minutes < 36 && minutes > 31)
					startAnimation(Animation.CREEPER);
			} else {
				float value = animationProgress.getValue();
				animationProgress.setValue(value + 1);
				if (value > 100)
					animationType = Animation.NONE;

				if (animationType == Animation.SURPRISE && MathHelper.approximatelyEquals(animationProgress.getValue(), 50)) {
					Vec3d center = VecHelper.getCenterOf(pos);
					world.breakBlock(pos, false);
					DamageSource damageSource = CreateDamageSources.cuckooSurprise(world);
					world.createExplosion(null, damageSource, null, center.x, center.y, center.z, 3, false,
						ExplosionSourceType.BLOCK);
				}

			}
		}

		if (world.isClient) {
			moveHands(hours, minutes);

			if (animationType == Animation.NONE) {
				if (AnimationTickHolder.getTicks() % 32 == 0)
					playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1 / 16f, 2f);
				else if (AnimationTickHolder.getTicks() % 16 == 0)
					playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1 / 16f, 1.5f);
			} else {

				boolean isSurprise = animationType == Animation.SURPRISE;
				float value = animationProgress.getValue();
				animationProgress.setValue(value + 1);
				if (value > 100)
					animationType = null;

				// sounds

				if (value == 1)
					playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 2, .5f);
				if (value == 21)
					playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 2, 0.793701f);

				if (value > 30 && isSurprise) {
					Vec3d pos = VecHelper.offsetRandomly(VecHelper.getCenterOf(this.pos), world.random, .5f);
					world.addParticle(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 0, 0, 0);
				}
				if (value == 40 && isSurprise)
					playSound(SoundEvents.ENTITY_TNT_PRIMED, 1f, 1f);

				int step = isSurprise ? 3 : 15;
				for (int phase = 30; phase <= 60; phase += step) {
					if (value == phase - step / 3)
						playSound(SoundEvents.BLOCK_CHEST_OPEN, 1 / 16f, 2f);
					if (value == phase) {
						if (animationType == Animation.PIG)
							playSound(SoundEvents.ENTITY_PIG_AMBIENT, 1 / 4f, 1f);
						else
							playSound(SoundEvents.ENTITY_CREEPER_HURT, 1 / 4f, 3f);
					}
					if (value == phase + step / 3)
						playSound(SoundEvents.BLOCK_CHEST_CLOSE, 1 / 16f, 2f);

				}

			}

			return;
		}
	}

	public void startAnimation(Animation animation) {
		animationType = animation;
		if (animation != null && CuckooClockBlock.containsSurprise(getCachedState()))
			animationType = Animation.SURPRISE;
		animationProgress.startWithValue(0);
		sendAnimationUpdate = true;

		if (animation == Animation.CREEPER)
			awardIfNear(AllAdvancements.CUCKOO_CLOCK, 32);

		sendData();
	}

	public void moveHands(int hours, int minutes) {
		float hourTarget = (float) (360 / 12 * (hours % 12));
		float minuteTarget = (float) (360 / 60 * minutes);

		hourHand.chase(hourTarget, .2f, Chaser.EXP);
		minuteHand.chase(minuteTarget, .2f, Chaser.EXP);

		hourHand.tickChaser();
		minuteHand.tickChaser();
	}

	private void playSound(SoundEvent sound, float volume, float pitch) {
		Vec3d vec = VecHelper.getCenterOf(pos);
		world.playSound(vec.x, vec.y, vec.z, sound, SoundCategory.BLOCKS, volume, pitch, false);
	}
}
