package com.simibubi.create.content.logistics.chute;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.logistics.chute.ChuteBlock.Shape;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class ChuteRenderer extends SafeBlockEntityRenderer<ChuteBlockEntity> {

	public ChuteRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(ChuteBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light,
		int overlay) {
		if (be.item.isEmpty())
			return;
		BlockState blockState = be.getCachedState();
		if (blockState.get(ChuteBlock.FACING) != Direction.DOWN)
			return;
		if (blockState.get(ChuteBlock.SHAPE) != Shape.WINDOW
			&& (be.bottomPullDistance == 0 || be.itemPosition.getValue(partialTicks) > .5f))
			return;

		renderItem(be, partialTicks, ms, buffer, light, overlay);
	}

	public static void renderItem(ChuteBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		ItemRenderer itemRenderer = MinecraftClient.getInstance()
			.getItemRenderer();
		TransformStack msr = TransformStack.cast(ms);
		ms.push();
		msr.centre();
		float itemScale = .5f;
		float itemPosition = be.itemPosition.getValue(partialTicks);
		ms.translate(0, -.5 + itemPosition, 0);
		ms.scale(itemScale, itemScale, itemScale);
		msr.rotateX(itemPosition * 180);
		msr.rotateY(itemPosition * 180);
		itemRenderer.renderItem(be.item, ModelTransformationMode.FIXED, light, overlay, ms, buffer, be.getWorld(), 0);
		ms.pop();
	}

}
