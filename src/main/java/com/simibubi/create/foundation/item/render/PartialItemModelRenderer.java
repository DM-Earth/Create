package com.simibubi.create.foundation.item.render;

import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.util.ItemRendererHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class PartialItemModelRenderer {

	private static final PartialItemModelRenderer INSTANCE = new PartialItemModelRenderer();

	private final Random random = Random.create();

	private ItemStack stack;
	private ModelTransformationMode transformType;
	private MatrixStack ms;
	private VertexConsumerProvider buffer;
	private int overlay;

	public static PartialItemModelRenderer of(ItemStack stack, ModelTransformationMode transformType,
		MatrixStack ms, VertexConsumerProvider buffer, int overlay) {
		PartialItemModelRenderer instance = INSTANCE;
		instance.stack = stack;
		instance.transformType = transformType;
		instance.ms = ms;
		instance.buffer = buffer;
		instance.overlay = overlay;
		return instance;
	}

	public void render(BakedModel model, int light) {
		render(model, RenderTypes.getItemPartialTranslucent(), light);
	}

	public void renderSolid(BakedModel model, int light) {
		render(model, RenderTypes.getItemPartialSolid(), light);
	}

	public void renderSolidGlowing(BakedModel model, int light) {
		render(model, RenderTypes.getGlowingSolid(), light);
	}

	public void renderGlowing(BakedModel model, int light) {
		render(model, RenderTypes.getGlowingTranslucent(), light);
	}

	public void render(BakedModel model, RenderLayer type, int light) {
		if (stack.isEmpty())
			return;

		ms.push();
		ms.translate(-0.5D, -0.5D, -0.5D);

		if (!model.isBuiltin())
			// FIXME FRAPI COMPAT
			renderBakedItemModel(model, light, ms,
				ItemRenderer.getDirectItemGlintConsumer(buffer, type, true, stack.hasGlint()));
		else
			MinecraftClient.getInstance().getItemRenderer()
					.renderItem(stack, transformType, false, ms, buffer, light, overlay, model);

		ms.pop();
	}

	private void renderBakedItemModel(BakedModel model, int light, MatrixStack ms, VertexConsumer buffer) {
		ItemRenderer ir = MinecraftClient.getInstance()
				.getItemRenderer();
//		IModelData data = EmptyModelData.INSTANCE;

		for (Direction direction : Iterate.directions) {
			random.setSeed(42L);
			ItemRendererHelper.renderQuadList(ir, ms, buffer, model.getQuads(null, direction, random), stack, light, overlay);
		}

		random.setSeed(42L);
		ItemRendererHelper.renderQuadList(ir, ms, buffer, model.getQuads(null, null, random), stack, light, overlay);
	}

}
