package com.simibubi.create.foundation.ponder.element;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public abstract class AnimatedSceneElement extends PonderSceneElement {

	protected Vec3d fadeVec;
	protected LerpedFloat fade;

	public AnimatedSceneElement() {
		fade = LerpedFloat.linear()
			.startWithValue(0);
	}

	public void forceApplyFade(float fade) {
		this.fade.startWithValue(fade);
	}

	public void setFade(float fade) {
		this.fade.setValue(fade);
	}

	public void setFadeVec(Vec3d fadeVec) {
		this.fadeVec = fadeVec;
	}

	@Override
	public final void renderFirst(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float pt) {
		ms.push();
		float currentFade = applyFade(ms, pt);
		renderFirst(world, buffer, ms, currentFade, pt);
		ms.pop();
	}

	@Override
	public final void renderLayer(PonderWorld world, VertexConsumerProvider buffer, RenderLayer type, MatrixStack ms,
		float pt) {
		ms.push();
		float currentFade = applyFade(ms, pt);
		renderLayer(world, buffer, type, ms, currentFade, pt);
		ms.pop();
	}

	@Override
	public final void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float pt) {
		ms.push();
		float currentFade = applyFade(ms, pt);
		renderLast(world, buffer, ms, currentFade, pt);
		ms.pop();
	}

	protected float applyFade(MatrixStack ms, float pt) {
		float currentFade = fade.getValue(pt);
		if (fadeVec != null)
			TransformStack.cast(ms)
					.translate(fadeVec.multiply(-1 + currentFade));
		return currentFade;
	}

	protected void renderLayer(PonderWorld world, VertexConsumerProvider buffer, RenderLayer type, MatrixStack ms, float fade,
		float pt) {}

	protected void renderFirst(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {}

	protected void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {}

	protected int lightCoordsFromFade(float fade) {
		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		if (fade != 1) {
			light = (int) (MathHelper.lerp(fade, 5, 0xF));
			light = LightmapTextureManager.pack(light, light);
		}
		return light;
	}

}
