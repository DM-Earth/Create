package com.simibubi.create.content.equipment.wrench;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class WrenchItemRenderer extends CustomRenderedItemModelRenderer {

	protected static final PartialModel GEAR = new PartialModel(Create.asResource("item/wrench/gear"));

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ModelTransformationMode transformType,
		MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		renderer.render(model.getOriginalModel(), light);

		float xOffset = -1/16f;
		ms.translate(-xOffset, 0, 0);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ScrollValueHandler.getScroll(AnimationTickHolder.getPartialTicks())));
		ms.translate(xOffset, 0, 0);

		renderer.render(GEAR.get(), light);
	}

}
