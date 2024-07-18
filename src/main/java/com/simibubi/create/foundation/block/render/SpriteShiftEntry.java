package com.simibubi.create.foundation.block.render;

import com.jozufozu.flywheel.core.StitchedSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

public class SpriteShiftEntry {
	protected StitchedSprite original;
	protected StitchedSprite target;

	public void set(Identifier originalTextureLocation, Identifier targetTextureLocation) {
		original = new StitchedSprite(originalTextureLocation);
		target = new StitchedSprite(targetTextureLocation);
	}

	public Identifier getOriginalResourceLocation() {
		return original.getLocation();
	}

	public Identifier getTargetResourceLocation() {
		return target.getLocation();
	}

	public Sprite getOriginal() {
		return original.get();
	}

	public Sprite getTarget() {
		return target.get();
	}

	public float getTargetU(float localU) {
		return getTarget().getFrameU(getUnInterpolatedU(getOriginal(), localU));
	}

	public float getTargetV(float localV) {
		return getTarget().getFrameV(getUnInterpolatedV(getOriginal(), localV));
	}

	public static float getUnInterpolatedU(Sprite sprite, float u) {
		float f = sprite.getMaxU() - sprite.getMinU();
		return (u - sprite.getMinU()) / f * 16.0F;
	}

	public static float getUnInterpolatedV(Sprite sprite, float v) {
		float f = sprite.getMaxV() - sprite.getMinV();
		return (v - sprite.getMinV()) / f * 16.0F;
	}
}
