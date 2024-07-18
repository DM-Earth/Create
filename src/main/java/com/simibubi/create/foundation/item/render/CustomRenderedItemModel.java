package com.simibubi.create.foundation.item.render;

import io.github.fabricators_of_create.porting_lib.models.TransformTypeDependentItemBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;

public class CustomRenderedItemModel extends ForwardingBakedModel implements TransformTypeDependentItemBakedModel {

	public CustomRenderedItemModel(BakedModel originalModel) {
		this.wrapped = originalModel;
	}

	@Override
	public boolean isBuiltin() {
		return true;
	}

	@Override
	public BakedModel applyTransform(ModelTransformationMode cameraItemDisplayContext, MatrixStack mat,
									 boolean leftHand, DefaultTransform defaultTransform) {
		// fabric: apply the wrapped model transforms, but render this model
		defaultTransform.apply(wrapped);
		return this;
	}

	public BakedModel getOriginalModel() {
		return wrapped;
	}

}
