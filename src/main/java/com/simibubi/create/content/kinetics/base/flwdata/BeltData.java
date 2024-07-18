package com.simibubi.create.content.kinetics.base.flwdata;

import org.joml.Quaternionf;

import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import net.minecraft.client.texture.Sprite;

public class BeltData extends KineticData {
    float qX;
    float qY;
    float qZ;
    float qW;
    float sourceU;
    float sourceV;
    float minU;
    float minV;
    float maxU;
    float maxV;
    byte scrollMult;

    public BeltData setRotation(Quaternionf q) {
        this.qX = q.x();
        this.qY = q.y();
        this.qZ = q.z();
        this.qW = q.w();
        markDirty();
        return this;
    }

    public BeltData setScrollTexture(SpriteShiftEntry spriteShift) {
        Sprite source = spriteShift.getOriginal();
        Sprite target = spriteShift.getTarget();

        this.sourceU = source.getMinU();
        this.sourceV = source.getMinV();
        this.minU = target.getMinU();
        this.minV = target.getMinV();
        this.maxU = target.getMaxU();
        this.maxV = target.getMaxV();
        markDirty();

		return this;
	}

	public BeltData setScrollMult(float scrollMult) {
		this.scrollMult = (byte) (scrollMult * 127);
		markDirty();
		return this;
	}

}
