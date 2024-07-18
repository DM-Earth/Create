package com.simibubi.create.foundation.outliner;

import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Color;

public abstract class Outline {

	protected final OutlineParams params;

	protected final Vector4f colorTemp = new Vector4f();
	protected final Vector3f diffPosTemp = new Vector3f();
	protected final Vector3f minPosTemp = new Vector3f();
	protected final Vector3f maxPosTemp = new Vector3f();
	protected final Vector4f posTransformTemp = new Vector4f();
	protected final Vector3f normalTransformTemp = new Vector3f();

	public Outline() {
		params = new OutlineParams();
	}

	public OutlineParams getParams() {
		return params;
	}

	public abstract void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera, float pt);

	public void tick() {}

	public void bufferCuboidLine(MatrixStack poseStack, VertexConsumer consumer, Vec3d camera, Vector3d start, Vector3d end,
		float width, Vector4f color, int lightmap, boolean disableNormals) {
		Vector3f diff = this.diffPosTemp;
		diff.set((float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z));

		float length = MathHelper.sqrt(diff.x() * diff.x() + diff.y() * diff.y() + diff.z() * diff.z());
		float hAngle = AngleHelper.deg(MathHelper.atan2(diff.x(), diff.z()));
		float hDistance = MathHelper.sqrt(diff.x() * diff.x() + diff.z() * diff.z());
		float vAngle = AngleHelper.deg(MathHelper.atan2(hDistance, diff.y())) - 90;

		poseStack.push();
		TransformStack.cast(poseStack)
			.translate(start.x - camera.x, start.y - camera.y, start.z - camera.z)
			.rotateY(hAngle)
			.rotateX(vAngle);
		bufferCuboidLine(poseStack.peek(), consumer, new Vector3f(), Direction.SOUTH, length, width, color, lightmap,
			disableNormals);
		poseStack.pop();
	}

	public void bufferCuboidLine(MatrixStack.Entry pose, VertexConsumer consumer, Vector3f origin, Direction direction,
		float length, float width, Vector4f color, int lightmap, boolean disableNormals) {
		Vector3f minPos = minPosTemp;
		Vector3f maxPos = maxPosTemp;

		float halfWidth = width / 2;
		minPos.set(origin.x() - halfWidth, origin.y() - halfWidth, origin.z() - halfWidth);
		maxPos.set(origin.x() + halfWidth, origin.y() + halfWidth, origin.z() + halfWidth);

		switch (direction) {
		case DOWN -> {
			minPos.add(0, -length, 0);
		}
		case UP -> {
			maxPos.add(0, length, 0);
		}
		case NORTH -> {
			minPos.add(0, 0, -length);
		}
		case SOUTH -> {
			maxPos.add(0, 0, length);
		}
		case WEST -> {
			minPos.add(-length, 0, 0);
		}
		case EAST -> {
			maxPos.add(length, 0, 0);
		}
		}

		bufferCuboid(pose, consumer, minPos, maxPos, color, lightmap, disableNormals);
	}

	public void bufferCuboid(MatrixStack.Entry pose, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos,
		Vector4f color, int lightmap, boolean disableNormals) {
		Vector4f posTransformTemp = this.posTransformTemp;
		Vector3f normalTransformTemp = this.normalTransformTemp;

		float minX = minPos.x();
		float minY = minPos.y();
		float minZ = minPos.z();
		float maxX = maxPos.x();
		float maxY = maxPos.y();
		float maxZ = maxPos.z();

		Matrix4f posMatrix = pose.getPositionMatrix();

		posTransformTemp.set(minX, minY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x0 = posTransformTemp.x();
		double y0 = posTransformTemp.y();
		double z0 = posTransformTemp.z();

		posTransformTemp.set(minX, minY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x1 = posTransformTemp.x();
		double y1 = posTransformTemp.y();
		double z1 = posTransformTemp.z();

		posTransformTemp.set(maxX, minY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x2 = posTransformTemp.x();
		double y2 = posTransformTemp.y();
		double z2 = posTransformTemp.z();

		posTransformTemp.set(maxX, minY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x3 = posTransformTemp.x();
		double y3 = posTransformTemp.y();
		double z3 = posTransformTemp.z();

		posTransformTemp.set(minX, maxY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x4 = posTransformTemp.x();
		double y4 = posTransformTemp.y();
		double z4 = posTransformTemp.z();

		posTransformTemp.set(minX, maxY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x5 = posTransformTemp.x();
		double y5 = posTransformTemp.y();
		double z5 = posTransformTemp.z();

		posTransformTemp.set(maxX, maxY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x6 = posTransformTemp.x();
		double y6 = posTransformTemp.y();
		double z6 = posTransformTemp.z();

		posTransformTemp.set(maxX, maxY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x7 = posTransformTemp.x();
		double y7 = posTransformTemp.y();
		double z7 = posTransformTemp.z();

		float r = color.x();
		float g = color.y();
		float b = color.z();
		float a = color.w();

		Matrix3f normalMatrix = pose.getNormalMatrix();

		// down

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(0, -1, 0);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx0 = normalTransformTemp.x();
		float ny0 = normalTransformTemp.y();
		float nz0 = normalTransformTemp.z();

		consumer.vertex(x0, y0, z0)
			.color(r, g, b, a)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx0, ny0, nz0)
			.next();

		consumer.vertex(x1, y1, z1)
			.color(r, g, b, a)
			.texture(0, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx0, ny0, nz0)
			.next();

		consumer.vertex(x2, y2, z2)
			.color(r, g, b, a)
			.texture(1, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx0, ny0, nz0)
			.next();

		consumer.vertex(x3, y3, z3)
			.color(r, g, b, a)
			.texture(1, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx0, ny0, nz0)
			.next();

		// up

		normalTransformTemp.set(0, 1, 0);
		normalTransformTemp.mul(normalMatrix);
		float nx1 = normalTransformTemp.x();
		float ny1 = normalTransformTemp.y();
		float nz1 = normalTransformTemp.z();

		consumer.vertex(x4, y4, z4)
			.color(r, g, b, a)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx1, ny1, nz1)
			.next();

		consumer.vertex(x5, y5, z5)
			.color(r, g, b, a)
			.texture(0, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx1, ny1, nz1)
			.next();

		consumer.vertex(x6, y6, z6)
			.color(r, g, b, a)
			.texture(1, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx1, ny1, nz1)
			.next();

		consumer.vertex(x7, y7, z7)
			.color(r, g, b, a)
			.texture(1, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx1, ny1, nz1)
			.next();

		// north

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(0, 0, -1);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx2 = normalTransformTemp.x();
		float ny2 = normalTransformTemp.y();
		float nz2 = normalTransformTemp.z();

		consumer.vertex(x7, y7, z7)
			.color(r, g, b, a)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx2, ny2, nz2)
			.next();

		consumer.vertex(x2, y2, z2)
			.color(r, g, b, a)
			.texture(0, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx2, ny2, nz2)
			.next();

		consumer.vertex(x1, y1, z1)
			.color(r, g, b, a)
			.texture(1, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx2, ny2, nz2)
			.next();

		consumer.vertex(x4, y4, z4)
			.color(r, g, b, a)
			.texture(1, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx2, ny2, nz2)
			.next();

		// south

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(0, 0, 1);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx3 = normalTransformTemp.x();
		float ny3 = normalTransformTemp.y();
		float nz3 = normalTransformTemp.z();

		consumer.vertex(x5, y5, z5)
			.color(r, g, b, a)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx3, ny3, nz3)
			.next();

		consumer.vertex(x0, y0, z0)
			.color(r, g, b, a)
			.texture(0, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx3, ny3, nz3)
			.next();

		consumer.vertex(x3, y3, z3)
			.color(r, g, b, a)
			.texture(1, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx3, ny3, nz3)
			.next();

		consumer.vertex(x6, y6, z6)
			.color(r, g, b, a)
			.texture(1, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx3, ny3, nz3)
			.next();

		// west

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(-1, 0, 0);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx4 = normalTransformTemp.x();
		float ny4 = normalTransformTemp.y();
		float nz4 = normalTransformTemp.z();

		consumer.vertex(x4, y4, z4)
			.color(r, g, b, a)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx4, ny4, nz4)
			.next();

		consumer.vertex(x1, y1, z1)
			.color(r, g, b, a)
			.texture(0, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx4, ny4, nz4)
			.next();

		consumer.vertex(x0, y0, z0)
			.color(r, g, b, a)
			.texture(1, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx4, ny4, nz4)
			.next();

		consumer.vertex(x5, y5, z5)
			.color(r, g, b, a)
			.texture(1, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx4, ny4, nz4)
			.next();

		// east

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(1, 0, 0);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx5 = normalTransformTemp.x();
		float ny5 = normalTransformTemp.y();
		float nz5 = normalTransformTemp.z();

		consumer.vertex(x6, y6, z6)
			.color(r, g, b, a)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx5, ny5, nz5)
			.next();

		consumer.vertex(x3, y3, z3)
			.color(r, g, b, a)
			.texture(0, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx5, ny5, nz5)
			.next();

		consumer.vertex(x2, y2, z2)
			.color(r, g, b, a)
			.texture(1, 1)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx5, ny5, nz5)
			.next();

		consumer.vertex(x7, y7, z7)
			.color(r, g, b, a)
			.texture(1, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx5, ny5, nz5)
			.next();
	}

	public void bufferQuad(MatrixStack.Entry pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2,
		Vector3f pos3, Vector4f color, int lightmap, Vector3f normal) {
		bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, 0, 0, 1, 1, lightmap, normal);
	}

	public void bufferQuad(MatrixStack.Entry pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2,
		Vector3f pos3, Vector4f color, float minU, float minV, float maxU, float maxV, int lightmap, Vector3f normal) {
		Vector4f posTransformTemp = this.posTransformTemp;
		Vector3f normalTransformTemp = this.normalTransformTemp;

		Matrix4f posMatrix = pose.getPositionMatrix();

		posTransformTemp.set(pos0.x(), pos0.y(), pos0.z(), 1);
		posTransformTemp.mul(posMatrix);
		double x0 = posTransformTemp.x();
		double y0 = posTransformTemp.y();
		double z0 = posTransformTemp.z();

		posTransformTemp.set(pos1.x(), pos1.y(), pos1.z(), 1);
		posTransformTemp.mul(posMatrix);
		double x1 = posTransformTemp.x();
		double y1 = posTransformTemp.y();
		double z1 = posTransformTemp.z();

		posTransformTemp.set(pos2.x(), pos2.y(), pos2.z(), 1);
		posTransformTemp.mul(posMatrix);
		double x2 = posTransformTemp.x();
		double y2 = posTransformTemp.y();
		double z2 = posTransformTemp.z();

		posTransformTemp.set(pos3.x(), pos3.y(), pos3.z(), 1);
		posTransformTemp.mul(posMatrix);
		double x3 = posTransformTemp.x();
		double y3 = posTransformTemp.y();
		double z3 = posTransformTemp.z();

		float r = color.x();
		float g = color.y();
		float b = color.z();
		float a = color.w();

		normalTransformTemp.set(normal);
		normalTransformTemp.mul(pose.getNormalMatrix());
		float nx = normalTransformTemp.x();
		float ny = normalTransformTemp.y();
		float nz = normalTransformTemp.z();

		consumer.vertex(x0, y0, z0)
			.color(r, g, b, a)
			.texture(minU, minV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx, ny, nz)
			.next();

		consumer.vertex(x1, y1, z1)
			.color(r, g, b, a)
			.texture(minU, maxV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx, ny, nz)
			.next();

		consumer.vertex(x2, y2, z2)
			.color(r, g, b, a)
			.texture(maxU, maxV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx, ny, nz)
			.next();

		consumer.vertex(x3, y3, z3)
			.color(r, g, b, a)
			.texture(maxU, minV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(lightmap)
			.normal(nx, ny, nz)
			.next();
	}

	public static class OutlineParams {
		protected Optional<AllSpecialTextures> faceTexture;
		protected Optional<AllSpecialTextures> hightlightedFaceTexture;
		protected Direction highlightedFace;
		protected boolean fadeLineWidth;
		protected boolean disableCull;
		protected boolean disableLineNormals;
		protected float alpha;
		protected int lightmap;
		protected Color rgb;
		private float lineWidth;

		public OutlineParams() {
			faceTexture = hightlightedFaceTexture = Optional.empty();
			alpha = 1;
			lineWidth = 1 / 32f;
			fadeLineWidth = true;
			rgb = Color.WHITE;
			lightmap = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		}

		// builder

		public OutlineParams colored(int color) {
			rgb = new Color(color, false);
			return this;
		}

		public OutlineParams colored(Color c) {
			rgb = c.copy();
			return this;
		}

		public OutlineParams lightmap(int light) {
			lightmap = light;
			return this;
		}

		public OutlineParams lineWidth(float width) {
			this.lineWidth = width;
			return this;
		}

		public OutlineParams withFaceTexture(AllSpecialTextures texture) {
			this.faceTexture = Optional.ofNullable(texture);
			return this;
		}

		public OutlineParams clearTextures() {
			return this.withFaceTextures(null, null);
		}

		public OutlineParams withFaceTextures(AllSpecialTextures texture, AllSpecialTextures highlightTexture) {
			this.faceTexture = Optional.ofNullable(texture);
			this.hightlightedFaceTexture = Optional.ofNullable(highlightTexture);
			return this;
		}

		public OutlineParams highlightFace(@Nullable Direction face) {
			highlightedFace = face;
			return this;
		}

		public OutlineParams disableLineNormals() {
			disableLineNormals = true;
			return this;
		}

		public OutlineParams disableCull() {
			disableCull = true;
			return this;
		}

		// getter

		public float getLineWidth() {
			return fadeLineWidth ? alpha * lineWidth : lineWidth;
		}

		public Direction getHighlightedFace() {
			return highlightedFace;
		}

		public void loadColor(Vector4f vec) {
			vec.set(rgb.getRedAsFloat(), rgb.getGreenAsFloat(), rgb.getBlueAsFloat(), rgb.getAlphaAsFloat() * alpha);
		}
	}

}
