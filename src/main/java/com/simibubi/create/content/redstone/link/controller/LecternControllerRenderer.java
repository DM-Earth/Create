package com.simibubi.create.content.redstone.link.controller;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public class LecternControllerRenderer extends SafeBlockEntityRenderer<LecternControllerBlockEntity> {

	public LecternControllerRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	protected void renderSafe(LecternControllerBlockEntity be, float partialTicks, MatrixStack ms,
  		VertexConsumerProvider buffer, int light, int overlay) {

		ItemStack stack = AllItems.LINKED_CONTROLLER.asStack();
		ModelTransformationMode transformType = ModelTransformationMode.NONE;
		CustomRenderedItemModel mainModel = (CustomRenderedItemModel) MinecraftClient.getInstance()
			.getItemRenderer()
			.getModel(stack, be.getWorld(), null, 0);
		PartialItemModelRenderer renderer = PartialItemModelRenderer.of(stack, transformType, ms, buffer, overlay);
		boolean active = be.hasUser();
		boolean renderDepression = be.isUsedBy(MinecraftClient.getInstance().player);

		Direction facing = be.getCachedState().get(LecternControllerBlock.FACING);
		TransformStack msr = TransformStack.cast(ms);

		ms.push();
		msr.translate(0.5, 1.45, 0.5);
		msr.rotateY(AngleHelper.horizontalAngle(facing) - 90);
		msr.translate(0.28, 0, 0);
		msr.rotateZ(-22.0);
		LinkedControllerItemRenderer.renderInLectern(stack, mainModel, renderer, transformType, ms, light, active, renderDepression);
		ms.pop();
	}

}
