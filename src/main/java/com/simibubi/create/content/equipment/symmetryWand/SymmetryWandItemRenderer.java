package com.simibubi.create.content.equipment.symmetryWand;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SymmetryWandItemRenderer extends CustomRenderedItemModelRenderer {

	protected static final PartialModel BITS = new PartialModel(Create.asResource("item/wand_of_symmetry/bits"));
	protected static final PartialModel CORE = new PartialModel(Create.asResource("item/wand_of_symmetry/core"));
	protected static final PartialModel CORE_GLOW = new PartialModel(Create.asResource("item/wand_of_symmetry/core_glow"));

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ModelTransformationMode transformType,
		MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		float worldTime = AnimationTickHolder.getRenderTime() / 20;
		int maxLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

		renderer.render(model.getOriginalModel(), light);
		renderer.renderSolidGlowing(CORE.get(), maxLight);
		renderer.renderGlowing(CORE_GLOW.get(), maxLight);

		float floating = MathHelper.sin(worldTime) * .05f;
		float angle = worldTime * -10 % 360;

		ms.translate(0, floating, 0);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

		renderer.renderGlowing(BITS.get(), maxLight);
	}

}
