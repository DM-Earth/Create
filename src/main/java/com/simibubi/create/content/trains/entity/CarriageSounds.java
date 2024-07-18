package com.simibubi.create.content.trains.entity;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSoundEvents.SoundEntry;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class CarriageSounds {

	CarriageContraptionEntity entity;

	LerpedFloat distanceFactor;
	LerpedFloat speedFactor;
	LerpedFloat approachFactor;
	LerpedFloat seatCrossfade;

	LoopingSound minecartEsqueSound;
	LoopingSound sharedWheelSound;
	LoopingSound sharedWheelSoundSeated;
	LoopingSound sharedHonkSound;

	Couple<SoundEvent> bogeySounds;
	SoundEvent closestBogeySound;

	boolean arrived;

	int tick;
	int prevSharedTick;

	public CarriageSounds(CarriageContraptionEntity entity) {
		this.entity = entity;
		bogeySounds = entity.getCarriage().bogeys.map(bogey ->
				bogey != null && bogey.getStyle() != null ? bogey.getStyle().getSoundType()
					: AllSoundEvents.TRAIN2.getMainEvent());
		closestBogeySound = bogeySounds.getFirst();
		distanceFactor = LerpedFloat.linear();
		speedFactor = LerpedFloat.linear();
		approachFactor = LerpedFloat.linear();
		seatCrossfade = LerpedFloat.linear();
		arrived = true;
	}

	public void tick(DimensionalCarriageEntity dce) {
		MinecraftClient mc = MinecraftClient.getInstance();
		Entity camEntity = mc.cameraEntity;
		if (camEntity == null)
			return;

		Vec3d leadingAnchor = dce.leadingAnchor();
		Vec3d trailingAnchor = dce.trailingAnchor();
		if (leadingAnchor == null || trailingAnchor == null)
			return;

		tick++;

		Vec3d cam = camEntity.getEyePos();
		Vec3d contraptionMotion = entity.getPos()
			.subtract(entity.getPrevPositionVec());
		Vec3d combinedMotion = contraptionMotion.subtract(camEntity.getVelocity());

		Train train = entity.getCarriage().train;

		if (arrived && contraptionMotion.length() > 0.01f)
			arrived = false;
		if (arrived && entity.carriageIndex == 0)
			train.accumulatedSteamRelease /= 2;

		arrived |= entity.isStalled();

		if (entity.carriageIndex == 0)
			train.accumulatedSteamRelease = (float) Math
				.min(train.accumulatedSteamRelease + Math.min(0.5f, Math.abs(contraptionMotion.length() / 10f)), 10);

		Vec3d toBogey1 = leadingAnchor.subtract(cam);
		Vec3d toBogey2 = trailingAnchor.subtract(cam);
		double distance1 = toBogey1.length();
		double distance2 = toBogey2.length();

		Couple<CarriageBogey> bogeys = entity.getCarriage().bogeys;
		CarriageBogey relevantBogey = bogeys.get(distance1 > distance2);
		if (relevantBogey == null) {
			relevantBogey = bogeys.getFirst();
		}
		if (relevantBogey != null) {
			closestBogeySound = relevantBogey.getStyle().getSoundType();
		}

		Vec3d toCarriage = distance1 > distance2 ? toBogey2 : toBogey1;
		double distance = Math.min(distance1, distance2);
		Vec3d soundLocation = cam.add(toCarriage);

		double dot = toCarriage.normalize()
			.dotProduct(combinedMotion.normalize());

		speedFactor.chase(contraptionMotion.length(), .25f, Chaser.exp(.05f));
		distanceFactor.chase(MathHelper.clampedLerp(100, 0, (distance - 3) / 64d), .25f, Chaser.exp(50f));
		approachFactor.chase(MathHelper.clampedLerp(50, 200, .5f * (dot + 1)), .25f, Chaser.exp(10f));
		seatCrossfade.chase(camEntity.getVehicle() instanceof CarriageContraptionEntity ? 1 : 0, .1f, Chaser.EXP);

		speedFactor.tickChaser();
		distanceFactor.tickChaser();
		approachFactor.tickChaser();
		seatCrossfade.tickChaser();

		minecartEsqueSound = playIfMissing(mc, minecartEsqueSound, AllSoundEvents.TRAIN.getMainEvent());
		sharedWheelSound = playIfMissing(mc, sharedWheelSound, closestBogeySound);
		sharedWheelSoundSeated = playIfMissing(mc, sharedWheelSoundSeated, AllSoundEvents.TRAIN3.getMainEvent());

		float volume = Math.min(Math.min(speedFactor.getValue(), distanceFactor.getValue() / 100),
			approachFactor.getValue() / 300 + .0125f);

		if (entity.carriageIndex == 0) {
			float v = volume * (1 - seatCrossfade.getValue() * .35f) * .75f;
			if ((3 + tick) % 4 == 0)
				AllSoundEvents.STEAM.playAt(entity.getWorld(), soundLocation, v * ((tick + 7) % 8 == 0 ? 0.75f : .45f),
					1.17f, false);
			if (tick % 16 == 0)
				AllSoundEvents.STEAM.playAt(entity.getWorld(), soundLocation, v * 1.5f, .8f, false);
		}

		if (!arrived && speedFactor.getValue() < .002f && train.accumulatedSteamRelease > 1) {
			arrived = true;
			float releaseVolume = train.accumulatedSteamRelease / 10f;
			entity.getWorld().playSound(soundLocation.x, soundLocation.y, soundLocation.z, SoundEvents.BLOCK_LAVA_EXTINGUISH,
				SoundCategory.NEUTRAL, .25f * releaseVolume, .78f, false);
			entity.getWorld().playSound(soundLocation.x, soundLocation.y, soundLocation.z,
				SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.NEUTRAL, .2f * releaseVolume, 1.5f, false);
			AllSoundEvents.STEAM.playAt(entity.getWorld(), soundLocation, .75f * releaseVolume, .5f, false);
		}

		float pitchModifier = ((entity.getId() * 10) % 13) / 36f;

		volume = Math.min(volume, distanceFactor.getValue() / 800);

		float pitch = MathHelper.clamp(speedFactor.getValue() * 2 + .25f, .75f, 1.95f) - pitchModifier;
//		float pitch2 = Mth.clamp(speedFactor.getValue() * 2, 0.75f, 1.25f) - pitchModifier;

		minecartEsqueSound.setPitch(pitch * 1.5f);

		volume = Math.min(volume, distanceFactor.getValue() / 1000);

		for (Carriage carriage : train.carriages) {
			DimensionalCarriageEntity mainDCE = carriage.getDimensionalIfPresent(entity.getWorld().getRegistryKey());
			if (mainDCE == null)
				continue;
			CarriageContraptionEntity mainEntity = mainDCE.entity.get();
			if (mainEntity == null)
				continue;
			if (mainEntity.sounds == null)
				mainEntity.sounds = new CarriageSounds(mainEntity);
			mainEntity.sounds.submitSharedSoundVolume(soundLocation, volume);
			if (carriage != entity.getCarriage()) {
				finalizeSharedVolume(0);
				return;
			}
			break;
		}

//		finalizeSharedVolume(volume);
//		minecartEsqueSound.setLocation(soundLocation);
//		sharedWheelSound.setPitch(pitch2);
//		sharedWheelSound.setLocation(soundLocation);
//		sharedWheelSoundSeated.setPitch(pitch2);
//		sharedWheelSoundSeated.setLocation(soundLocation);

		if (train.honkTicks == 0) {
			if (sharedHonkSound != null) {
				sharedHonkSound.stopSound();
				sharedHonkSound = null;
			}
			return;
		}

		train.honkTicks--;
		train.determineHonk(entity.getWorld());

		if (train.lowHonk == null)
			return;

		boolean low = train.lowHonk;
		float honkPitch = (float) Math.pow(2, train.honkPitch / 12.0);

		SoundEntry endSound =
			!low ? AllSoundEvents.WHISTLE_TRAIN_MANUAL_END : AllSoundEvents.WHISTLE_TRAIN_MANUAL_LOW_END;
		SoundEntry continuousSound =
			!low ? AllSoundEvents.WHISTLE_TRAIN_MANUAL : AllSoundEvents.WHISTLE_TRAIN_MANUAL_LOW;

		if (train.honkTicks == 5)
			endSound.playAt(mc.world, soundLocation, 1, honkPitch, false);
		if (train.honkTicks == 19)
			endSound.playAt(mc.world, soundLocation, .5f, honkPitch, false);

		sharedHonkSound = playIfMissing(mc, sharedHonkSound, continuousSound.getMainEvent());
		sharedHonkSound.setLocation(soundLocation);
		float fadeout = MathHelper.clamp((3 - train.honkTicks) / 3f, 0, 1);
		float fadein = MathHelper.clamp((train.honkTicks - 17) / 3f, 0, 1);
		sharedHonkSound.setVolume(1 - fadeout - fadein);
		sharedHonkSound.setPitch(honkPitch);

	}

	private LoopingSound playIfMissing(MinecraftClient mc, LoopingSound loopingSound, SoundEvent sound) {
		if (loopingSound == null) {
			loopingSound = new LoopingSound(sound, SoundCategory.NEUTRAL);
			mc.getSoundManager()
				.play(loopingSound);
		}
		return loopingSound;
	}

	public void submitSharedSoundVolume(Vec3d location, float volume) {
		MinecraftClient mc = MinecraftClient.getInstance();
		minecartEsqueSound = playIfMissing(mc, minecartEsqueSound, AllSoundEvents.TRAIN.getMainEvent());
		sharedWheelSound = playIfMissing(mc, sharedWheelSound, closestBogeySound);
		sharedWheelSoundSeated = playIfMissing(mc, sharedWheelSoundSeated, AllSoundEvents.TRAIN3.getMainEvent());

		boolean approach = true;

		if (tick != prevSharedTick) {
			prevSharedTick = tick;
			approach = false;
		} else if (sharedWheelSound.getVolume() > volume)
			return;

		Vec3d currentLoc = new Vec3d(minecartEsqueSound.getX(), minecartEsqueSound.getY(), minecartEsqueSound.getZ());
		Vec3d newLoc = approach ? currentLoc.add(location.subtract(currentLoc)
			.multiply(.125f)) : location;

		minecartEsqueSound.setLocation(newLoc);
		sharedWheelSound.setLocation(newLoc);
		sharedWheelSoundSeated.setLocation(newLoc);
		finalizeSharedVolume(volume);
	}

	public void finalizeSharedVolume(float volume) {
		float crossfade = seatCrossfade.getValue();
		minecartEsqueSound.setVolume((1 - crossfade * .65f) * volume / 2);
		volume = Math.min(volume, Math.max((speedFactor.getValue() - .25f) / 4 + 0.01f, 0));
		sharedWheelSoundSeated.setVolume(volume * crossfade);
		sharedWheelSound.setVolume(volume * (1 - crossfade) * 1.5f);
	}

	public void stop() {
		if (minecartEsqueSound != null)
			minecartEsqueSound.stopSound();
		if (sharedWheelSound != null)
			sharedWheelSound.stopSound();
		if (sharedWheelSoundSeated != null)
			sharedWheelSoundSeated.stopSound();
		if (sharedHonkSound != null)
			sharedHonkSound.stopSound();
	}

	class LoopingSound extends MovingSoundInstance {

		protected LoopingSound(SoundEvent p_119606_, SoundCategory p_119607_) {
			super(p_119606_, p_119607_, SoundInstance.createRandom());
			attenuationType = AttenuationType.LINEAR;
			repeat = true;
			repeatDelay = 0;
			volume = 0.0001f;
		}

		@Override
		public void tick() {}

		public void setVolume(float volume) {
			this.volume = volume;
		}

		@Override
		public float getVolume() {
			return volume;
		}

		public void setPitch(float pitch) {
			this.pitch = pitch;
		}

		@Override
		public float getPitch() {
			return pitch;
		}

		public void setLocation(Vec3d location) {
			x = location.x;
			y = location.y;
			z = location.z;
		}

		public void stopSound() {
			MinecraftClient.getInstance()
				.getSoundManager()
				.stop(this);
		}

	}

}
