package com.simibubi.create.foundation.model;

import static com.simibubi.create.foundation.block.render.SpriteShiftEntry.getUnInterpolatedU;
import static com.simibubi.create.foundation.block.render.SpriteShiftEntry.getUnInterpolatedV;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class BakedModelHelper {

	public static void cropAndMove(MutableQuadView quad, Sprite sprite, Box crop, Vec3d move) {
		Vec3d xyz0 = BakedQuadHelper.getXYZ(quad, 0);
		Vec3d xyz1 = BakedQuadHelper.getXYZ(quad, 1);
		Vec3d xyz2 = BakedQuadHelper.getXYZ(quad, 2);
		Vec3d xyz3 = BakedQuadHelper.getXYZ(quad, 3);

		Vec3d uAxis = xyz3.add(xyz2)
			.multiply(.5);
		Vec3d vAxis = xyz1.add(xyz2)
			.multiply(.5);
		Vec3d center = xyz3.add(xyz2)
			.add(xyz0)
			.add(xyz1)
			.multiply(.25);

		float u0 = quad.spriteU(0, 0);
		float u3 = quad.spriteU(3, 0);
		float v0 = quad.spriteV(0, 0);
		float v1 = quad.spriteV(1, 0);

		float uScale = (float) Math
			.round((getUnInterpolatedU(sprite, u3) - getUnInterpolatedU(sprite, u0)) / xyz3.distanceTo(xyz0));
		float vScale = (float) Math
			.round((getUnInterpolatedV(sprite, v1) - getUnInterpolatedV(sprite, v0)) / xyz1.distanceTo(xyz0));

		if (uScale == 0) {
			float v3 = quad.spriteV(3, 0);
			float u1 = quad.spriteU(1, 0);
			uAxis = xyz1.add(xyz2)
				.multiply(.5);
			vAxis = xyz3.add(xyz2)
				.multiply(.5);
			uScale = (float) Math
				.round((getUnInterpolatedU(sprite, u1) - getUnInterpolatedU(sprite, u0)) / xyz1.distanceTo(xyz0));
			vScale = (float) Math
				.round((getUnInterpolatedV(sprite, v3) - getUnInterpolatedV(sprite, v0)) / xyz3.distanceTo(xyz0));

		}

		uAxis = uAxis.subtract(center)
			.normalize();
		vAxis = vAxis.subtract(center)
			.normalize();

		Vec3d min = new Vec3d(crop.minX, crop.minY, crop.minZ);
		Vec3d max = new Vec3d(crop.maxX, crop.maxY, crop.maxZ);

		for (int vertex = 0; vertex < 4; vertex++) {
			Vec3d xyz = BakedQuadHelper.getXYZ(quad, vertex);
			Vec3d newXyz = VecHelper.componentMin(max, VecHelper.componentMax(xyz, min));
			Vec3d diff = newXyz.subtract(xyz);

			if (diff.lengthSquared() > 0) {
				float u = quad.spriteU(vertex, 0);
				float v = quad.spriteV(vertex, 0);
				float uDiff = (float) uAxis.dotProduct(diff) * uScale;
				float vDiff = (float) vAxis.dotProduct(diff) * vScale;
				quad.sprite(vertex, 0,
						sprite.getFrameU(getUnInterpolatedU(sprite, u) + uDiff),
						sprite.getFrameV(getUnInterpolatedV(sprite, v) + vDiff));
			}

			BakedQuadHelper.setXYZ(quad, vertex, newXyz.add(move));
		}
	}

	public static BakedModel generateModel(BakedModel template, UnaryOperator<Sprite> spriteSwapper) {
		Random random = Random.create();

		Map<Direction, List<BakedQuad>> culledFaces = new EnumMap<>(Direction.class);
		for (Direction cullFace : Iterate.directions) {
			random.setSeed(42L);
			List<BakedQuad> quads = template.getQuads(null, cullFace, random);
			culledFaces.put(cullFace, swapSprites(quads, spriteSwapper));
		}

		random.setSeed(42L);
		List<BakedQuad> quads = template.getQuads(null, null, random);
		List<BakedQuad> unculledFaces = swapSprites(quads, spriteSwapper);

		Sprite particleSprite = template.getParticleSprite();
		Sprite swappedParticleSprite = spriteSwapper.apply(particleSprite);
		if (swappedParticleSprite != null) {
			particleSprite = swappedParticleSprite;
		}
		return new BasicBakedModel(unculledFaces, culledFaces, template.useAmbientOcclusion(), template.isSideLit(), template.hasDepth(), particleSprite, template.getTransformation(), ModelOverrideList.EMPTY);
	}

	public static List<BakedQuad> swapSprites(List<BakedQuad> quads, UnaryOperator<Sprite> spriteSwapper) {
		List<BakedQuad> newQuads = new ArrayList<>(quads);
		int size = quads.size();
		for (int i = 0; i < size; i++) {
			BakedQuad quad = quads.get(i);
			Sprite sprite = quad.getSprite();
			Sprite newSprite = spriteSwapper.apply(sprite);
			if (newSprite == null || sprite == newSprite)
				continue;

			BakedQuad newQuad = BakedQuadHelper.clone(quad);
			int[] vertexData = newQuad.getVertexData();

			for (int vertex = 0; vertex < 4; vertex++) {
				float u = BakedQuadHelper.getU(vertexData, vertex);
				float v = BakedQuadHelper.getV(vertexData, vertex);
				BakedQuadHelper.setU(vertexData, vertex, newSprite.getFrameU(getUnInterpolatedU(sprite, u)));
				BakedQuadHelper.setV(vertexData, vertex, newSprite.getFrameV(getUnInterpolatedV(sprite, v)));
			}

			newQuads.set(i, newQuad);
		}
		return newQuads;
	}
}
