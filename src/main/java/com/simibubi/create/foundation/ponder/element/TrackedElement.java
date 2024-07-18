package com.simibubi.create.foundation.ponder.element;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import com.simibubi.create.foundation.ponder.PonderWorld;

public abstract class TrackedElement<T> extends PonderSceneElement {

	private WeakReference<T> reference;

	public TrackedElement(T wrapped) {
		this.reference = new WeakReference<>(wrapped);
	}

	public void ifPresent(Consumer<T> func) {
		if (reference == null)
			return;
		T resolved = reference.get();
		if (resolved == null)
			return;
		func.accept(resolved);
	}
	
	protected boolean isStillValid(T element) { 
		return true;
	}
	
	@Override
	public void renderFirst(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float pt) {}

	@Override
	public void renderLayer(PonderWorld world, VertexConsumerProvider buffer, RenderLayer type, MatrixStack ms, float pt) {}

	@Override
	public void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float pt) {}

}
