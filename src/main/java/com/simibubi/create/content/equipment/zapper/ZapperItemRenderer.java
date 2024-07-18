package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;

public abstract class ZapperItemRenderer extends CustomRenderedItemModelRenderer {

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ModelTransformationMode transformType,
		MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		// Block indicator
		if (transformType == ModelTransformationMode.GUI && stack.hasNbt() && stack.getNbt()
			.contains("BlockUsed"))
			renderBlockUsed(stack, ms, buffer, light, overlay);
	}

	@SuppressWarnings("deprecation")
	private void renderBlockUsed(ItemStack stack, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		BlockState state = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), stack.getNbt()
			.getCompound("BlockUsed"));

		ms.push();
		ms.translate(-0.3F, -0.45F, -0.0F);
		ms.scale(0.25F, 0.25F, 0.25F);
		BakedModel modelForState = MinecraftClient.getInstance()
			.getBlockRenderManager()
			.getModel(state);

		if (state.getBlock() instanceof HorizontalConnectingBlock)
			modelForState = MinecraftClient.getInstance()
				.getItemRenderer()
				.getModel(new ItemStack(state.getBlock()), null, null, 0);

		MinecraftClient.getInstance()
			.getItemRenderer()
			.renderItem(new ItemStack(state.getBlock()), ModelTransformationMode.NONE, false, ms, buffer, light, overlay,
				modelForState);
		ms.pop();
	}

	protected float getAnimationProgress(float pt, boolean leftHanded, boolean mainHand) {
		float animation = CreateClient.ZAPPER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded, pt);
		return MathHelper.clamp(animation * 5, 0, 1);
	}

}
