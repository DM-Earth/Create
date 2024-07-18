package com.simibubi.create.foundation.fluid;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class FluidRenderer {

	public static VertexConsumer getFluidBuilder(VertexConsumerProvider buffer) {
		return buffer.getBuffer(RenderTypes.getFluid());
	}

	public static void renderFluidStream(FluidStack fluidStack, Direction direction, float radius, float progress,
		boolean inbound, VertexConsumerProvider buffer, MatrixStack ms, int light) {
		renderFluidStream(fluidStack, direction, radius, progress, inbound, getFluidBuilder(buffer), ms, light);
	}

	public static void renderFluidStream(FluidStack fluidStack, Direction direction, float radius, float progress,
		boolean inbound, VertexConsumer builder, MatrixStack ms, int light) {
		FluidVariant fluidVariant = fluidStack.getType();
		Sprite[] sprites = FluidVariantRendering.getSprites(fluidVariant);
		if (sprites == null) {
			return;
		}
		Sprite flowTexture = sprites[1];
		Sprite stillTexture = sprites[0];

		int color = FluidVariantRendering.getColor(fluidVariant);
		int blockLightIn = (light >> 4) & 0xF;
		int luminosity = Math.max(blockLightIn, FluidVariantAttributes.getLuminance(fluidVariant));
		light = (light & 0xF00000) | luminosity << 4;

		if (inbound)
			direction = direction.getOpposite();

		TransformStack msr = TransformStack.cast(ms);
		ms.push();
		msr.centre()
			.rotateY(AngleHelper.horizontalAngle(direction))
			.rotateX(direction == Direction.UP ? 180 : direction == Direction.DOWN ? 0 : 270)
			.unCentre();
		ms.translate(.5, 0, .5);

		float h = radius;
		float hMin = -radius;
		float hMax = radius;
		float y = inbound ? 1 : .5f;
		float yMin = y - MathHelper.clamp(progress * .5f, 0, 1);
		float yMax = y;

		for (int i = 0; i < 4; i++) {
			ms.push();
			renderFlowingTiledFace(Direction.SOUTH, hMin, yMin, hMax, yMax, h, builder, ms, light, color, flowTexture);
			ms.pop();
			msr.rotateY(90);
		}

		if (progress != 1)
			renderStillTiledFace(Direction.DOWN, hMin, hMin, hMax, hMax, yMin, builder, ms, light, color, stillTexture);

		ms.pop();
	}

	public static void renderFluidBox(FluidStack fluidStack, float xMin, float yMin, float zMin, float xMax, float yMax,
		float zMax, VertexConsumerProvider buffer, MatrixStack ms, int light, boolean renderBottom) {
		renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, getFluidBuilder(buffer), ms, light,
			renderBottom);
	}

	public static void renderFluidBox(FluidStack fluidStack, float xMin, float yMin, float zMin, float xMax,
		float yMax, float zMax, VertexConsumer builder, MatrixStack ms, int light, boolean renderBottom) {
		FluidVariant fluidVariant = fluidStack.getType();
		Sprite[] sprites = FluidVariantRendering.getSprites(fluidVariant);
		Sprite fluidTexture = sprites != null ? sprites[0] : null;
		if (fluidTexture == null)
			return;

		int color = FluidVariantRendering.getColor(fluidVariant);
		int blockLightIn = (light >> 4) & 0xF;
		int luminosity = Math.max(blockLightIn, FluidVariantAttributes.getLuminance(fluidVariant));
		light = (light & 0xF00000) | luminosity << 4;

		Vec3d center = new Vec3d(xMin + (xMax - xMin) / 2, yMin + (yMax - yMin) / 2, zMin + (zMax - zMin) / 2);
		ms.push();
		if (FluidVariantAttributes.isLighterThanAir(fluidVariant))
			TransformStack.cast(ms)
				.translate(center)
				.rotateX(180)
				.translateBack(center);

		for (Direction side : Iterate.directions) {
			if (side == Direction.DOWN && !renderBottom)
				continue;

			boolean positive = side.getDirection() == AxisDirection.POSITIVE;
			if (side.getAxis()
				.isHorizontal()) {
				if (side.getAxis() == Axis.X) {
					renderStillTiledFace(side, zMin, yMin, zMax, yMax, positive ? xMax : xMin, builder, ms, light,
						color, fluidTexture);
				} else {
					renderStillTiledFace(side, xMin, yMin, xMax, yMax, positive ? zMax : zMin, builder, ms, light,
						color, fluidTexture);
				}
			} else {
				renderStillTiledFace(side, xMin, zMin, xMax, zMax, positive ? yMax : yMin, builder, ms, light, color,
					fluidTexture);
			}
		}

		ms.pop();
	}

	public static void renderStillTiledFace(Direction dir, float left, float down, float right, float up, float depth,
		VertexConsumer builder, MatrixStack ms, int light, int color, Sprite texture) {
		FluidRenderer.renderTiledFace(dir, left, down, right, up, depth, builder, ms, light, color, texture, 1);
	}

	public static void renderFlowingTiledFace(Direction dir, float left, float down, float right, float up, float depth,
		VertexConsumer builder, MatrixStack ms, int light, int color, Sprite texture) {
		FluidRenderer.renderTiledFace(dir, left, down, right, up, depth, builder, ms, light, color, texture, 0.5f);
	}

	public static void renderTiledFace(Direction dir, float left, float down, float right, float up, float depth,
		VertexConsumer builder, MatrixStack ms, int light, int color, Sprite texture, float textureScale) {
		boolean positive = dir.getDirection() == Direction.AxisDirection.POSITIVE;
		boolean horizontal = dir.getAxis()
			.isHorizontal();
		boolean x = dir.getAxis() == Axis.X;

		float shrink = texture.getAnimationFrameDelta() * 0.25f * textureScale;
		float centerU = texture.getMinU() + (texture.getMaxU() - texture.getMinU()) * 0.5f * textureScale;
		float centerV = texture.getMinV() + (texture.getMaxV() - texture.getMinV()) * 0.5f * textureScale;

		float f;
		float x2 = 0;
		float y2 = 0;
		float u1, u2;
		float v1, v2;
		for (float x1 = left; x1 < right; x1 = x2) {
			f = MathHelper.floor(x1);
			x2 = Math.min(f + 1, right);
			if (dir == Direction.NORTH || dir == Direction.EAST) {
				f = MathHelper.ceil(x2);
				u1 = texture.getFrameU((f - x2) * 16 * textureScale);
				u2 = texture.getFrameU((f - x1) * 16 * textureScale);
			} else {
				u1 = texture.getFrameU((x1 - f) * 16 * textureScale);
				u2 = texture.getFrameU((x2 - f) * 16 * textureScale);
			}
			u1 = MathHelper.lerp(shrink, u1, centerU);
			u2 = MathHelper.lerp(shrink, u2, centerU);
			for (float y1 = down; y1 < up; y1 = y2) {
				f = MathHelper.floor(y1);
				y2 = Math.min(f + 1, up);
				if (dir == Direction.UP) {
					v1 = texture.getFrameV((y1 - f) * 16 * textureScale);
					v2 = texture.getFrameV((y2 - f) * 16 * textureScale);
				} else {
					f = MathHelper.ceil(y2);
					v1 = texture.getFrameV((f - y2) * 16 * textureScale);
					v2 = texture.getFrameV((f - y1) * 16 * textureScale);
				}
				v1 = MathHelper.lerp(shrink, v1, centerV);
				v2 = MathHelper.lerp(shrink, v2, centerV);

				if (horizontal) {
					if (x) {
						putVertex(builder, ms, depth, y2, positive ? x2 : x1, color, u1, v1, dir, light);
						putVertex(builder, ms, depth, y1, positive ? x2 : x1, color, u1, v2, dir, light);
						putVertex(builder, ms, depth, y1, positive ? x1 : x2, color, u2, v2, dir, light);
						putVertex(builder, ms, depth, y2, positive ? x1 : x2, color, u2, v1, dir, light);
					} else {
						putVertex(builder, ms, positive ? x1 : x2, y2, depth, color, u1, v1, dir, light);
						putVertex(builder, ms, positive ? x1 : x2, y1, depth, color, u1, v2, dir, light);
						putVertex(builder, ms, positive ? x2 : x1, y1, depth, color, u2, v2, dir, light);
						putVertex(builder, ms, positive ? x2 : x1, y2, depth, color, u2, v1, dir, light);
					}
				} else {
					putVertex(builder, ms, x1, depth, positive ? y1 : y2, color, u1, v1, dir, light);
					putVertex(builder, ms, x1, depth, positive ? y2 : y1, color, u1, v2, dir, light);
					putVertex(builder, ms, x2, depth, positive ? y2 : y1, color, u2, v2, dir, light);
					putVertex(builder, ms, x2, depth, positive ? y1 : y2, color, u2, v1, dir, light);
				}
			}
		}
	}

	private static void putVertex(VertexConsumer builder, MatrixStack ms, float x, float y, float z, int color, float u,
		float v, Direction face, int light) {

		Vec3i normal = face.getVector();
		Entry peek = ms.peek();
		int a = color >> 24 & 0xff;
		int r = color >> 16 & 0xff;
		int g = color >> 8 & 0xff;
		int b = color & 0xff;

		builder.vertex(peek.getPositionMatrix(), x, y, z)
			.color(r, g, b, a)
			.texture(u, v)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(light)
			.normal(peek.getNormalMatrix(), normal.getX(), normal.getY(), normal.getZ())
			.next();
	}

}
