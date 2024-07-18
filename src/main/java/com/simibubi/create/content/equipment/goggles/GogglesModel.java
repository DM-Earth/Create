package com.simibubi.create.content.equipment.goggles;

import com.simibubi.create.AllPartialModels;

import io.github.fabricators_of_create.porting_lib.models.TransformTypeDependentItemBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;

public class GogglesModel extends ForwardingBakedModel implements TransformTypeDependentItemBakedModel {
	protected BakedModel itemModel;

	public GogglesModel(BakedModel template) {
		this.itemModel = wrapped = template;
	}

	@Override
	public BakedModel applyTransform(ModelTransformationMode cameraItemDisplayContext, MatrixStack mat, boolean leftHanded, DefaultTransform defaultTransform) {
		if (cameraItemDisplayContext == ModelTransformationMode.HEAD) {
			BakedModel headGoggles = AllPartialModels.GOGGLES.get();
			defaultTransform.apply(headGoggles);
			return headGoggles;
		}
		defaultTransform.apply(this);
		return this;
	}
}
