package com.simibubi.create.foundation.outliner;

import org.joml.Vector4f;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ChasingAABBOutline extends AABBOutline {

	Box targetBB;
	Box prevBB;

	public ChasingAABBOutline(Box bb) {
		super(bb);
		prevBB = bb.expand(0);
		targetBB = bb.expand(0);
	}

	public void target(Box target) {
		targetBB = target;
	}

	@Override
	public void tick() {
		prevBB = bb;
		setBounds(interpolateBBs(bb, targetBB, .5f));
	}

	@Override
	public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, float pt) {
		params.loadColor(colorTemp);
		Vector4f color = colorTemp;
		int lightmap = params.lightmap;
		boolean disableLineNormals = params.disableLineNormals;
		renderBox(ms, buffer, camera, interpolateBBs(prevBB, bb, pt), color, lightmap, disableLineNormals);
	}

	private static Box interpolateBBs(Box current, Box target, float pt) {
		return new Box(MathHelper.lerp(pt, current.minX, target.minX), MathHelper.lerp(pt, current.minY, target.minY),
			MathHelper.lerp(pt, current.minZ, target.minZ), MathHelper.lerp(pt, current.maxX, target.maxX),
			MathHelper.lerp(pt, current.maxY, target.maxY), MathHelper.lerp(pt, current.maxZ, target.maxZ));
	}

}