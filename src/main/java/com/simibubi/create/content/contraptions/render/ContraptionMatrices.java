package com.simibubi.create.content.contraptions.render;

import org.joml.Matrix4f;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * <p>
 * ContraptionMatrices must be cleared and setup per-contraption per-frame
 * </p>
 */
public class ContraptionMatrices {

	private final MatrixStack modelViewProjection = new MatrixStack();
	private final MatrixStack viewProjection = new MatrixStack();
	private final MatrixStack model = new MatrixStack();
	private final Matrix4f world = new Matrix4f();
	private final Matrix4f light = new Matrix4f();

	private boolean ready;

	public void setup(MatrixStack viewProjection, AbstractContraptionEntity entity) {
		float partialTicks = AnimationTickHolder.getPartialTicks();

		this.viewProjection.push();
		transform(this.viewProjection, viewProjection);
		model.push();
		entity.applyLocalTransforms(model, partialTicks);

		modelViewProjection.push();
		transform(modelViewProjection, viewProjection);
		transform(modelViewProjection, model);

		translateToEntity(world, entity, partialTicks);

		light.set(world);
		light.mul(model.peek()
			.getPositionMatrix());

		ready = true;
	}

	public void clear() {
		clearStack(modelViewProjection);
		clearStack(viewProjection);
		clearStack(model);
		world.identity();
		light.identity();
		ready = false;
	}

	public MatrixStack getModelViewProjection() {
		return modelViewProjection;
	}

	public MatrixStack getViewProjection() {
		return viewProjection;
	}

	public MatrixStack getModel() {
		return model;
	}

	public Matrix4f getWorld() {
		return world;
	}

	public Matrix4f getLight() {
		return light;
	}

	public boolean isReady() {
		return ready;
	}

	public static void transform(MatrixStack ms, MatrixStack transform) {
		ms.peek()
			.getPositionMatrix()
			.mul(transform.peek()
				.getPositionMatrix());
		ms.peek()
			.getNormalMatrix()
			.mul(transform.peek()
				.getNormalMatrix());
	}

	public static void translateToEntity(Matrix4f matrix, Entity entity, float partialTicks) {
		double x = MathHelper.lerp(partialTicks, entity.lastRenderX, entity.getX());
		double y = MathHelper.lerp(partialTicks, entity.lastRenderY, entity.getY());
		double z = MathHelper.lerp(partialTicks, entity.lastRenderZ, entity.getZ());
		matrix.setTranslation((float) x, (float) y, (float) z);
	}

	public static void clearStack(MatrixStack ms) {
		while (!ms.isEmpty()) {
			ms.pop();
		}
	}

}
