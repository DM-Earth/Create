package com.simibubi.create.content.decoration.steamWhistle;

import static com.simibubi.create.AllSoundEvents.WHISTLE_HIGH;
import static com.simibubi.create.AllSoundEvents.WHISTLE_LOW;
import static com.simibubi.create.AllSoundEvents.WHISTLE_MEDIUM;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WhistleSoundInstance extends MovingSoundInstance {

	private boolean active;
	private int keepAlive;
	private WhistleSize size;

	public WhistleSoundInstance(WhistleSize size, BlockPos worldPosition) {
		super((size == WhistleSize.SMALL ? WHISTLE_HIGH : size == WhistleSize.MEDIUM ? WHISTLE_MEDIUM : WHISTLE_LOW)
			.getMainEvent(), SoundCategory.RECORDS, SoundInstance.createRandom());
		this.size = size;
		repeat = true;
		active = true;
		volume = 0.05f;
		repeatDelay = 0;
		keepAlive();
		Vec3d v = Vec3d.ofCenter(worldPosition);
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public WhistleSize getOctave() {
		return size;
	}

	public void fadeOut() {
		this.active = false;
	}

	public void keepAlive() {
		keepAlive = 2;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	@Override
	public void tick() {
		if (active) {
			volume = Math.min(1, volume + .25f);
			keepAlive--;
			if (keepAlive == 0)
				fadeOut();
			return;

		}
		volume = Math.max(0, volume - .25f);
		if (volume == 0)
			setDone();
	}

}
