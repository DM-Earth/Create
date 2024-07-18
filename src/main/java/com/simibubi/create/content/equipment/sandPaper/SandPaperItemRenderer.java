package com.simibubi.create.content.equipment.sandPaper;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SandPaperItemRenderer extends CustomRenderedItemModelRenderer {

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
		ModelTransformationMode transformType, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		float partialTicks = AnimationTickHolder.getPartialTicks();

		boolean leftHand = transformType == ModelTransformationMode.FIRST_PERSON_LEFT_HAND;
		boolean firstPerson = leftHand || transformType == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND;

		NbtCompound tag = stack.getOrCreateNbt();
		boolean jeiMode = tag.contains("JEI");

		ms.push();

		if (tag.contains("Polishing")) {
			ms.push();

			if (transformType == ModelTransformationMode.GUI) {
				ms.translate(0.0F, .2f, 1.0F);
				ms.scale(.75f, .75f, .75f);
			} else {
				int modifier = leftHand ? -1 : 1;
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(modifier * 40));
			}

			// Reverse bobbing
			float time = (float) (!jeiMode ? player.getItemUseTimeLeft()
					: (-AnimationTickHolder.getTicks()) % stack.getMaxUseTime()) - partialTicks + 1.0F;
			if (time / (float) stack.getMaxUseTime() < 0.8F) {
				float bobbing = -MathHelper.abs(MathHelper.cos(time / 4.0F * (float) Math.PI) * 0.1F);

				if (transformType == ModelTransformationMode.GUI)
					ms.translate(bobbing, bobbing, 0.0F);
				else
					ms.translate(0.0f, bobbing, 0.0F);
			}

			ItemStack toPolish = ItemStack.fromNbt(tag.getCompound("Polishing"));
			itemRenderer.renderItem(toPolish, ModelTransformationMode.NONE, light, overlay, ms, buffer, player.getWorld(), 0);

			ms.pop();
		}

		if (firstPerson) {
			int itemInUseCount = player.getItemUseTimeLeft();
			if (itemInUseCount > 0) {
				int modifier = leftHand ? -1 : 1;
				ms.translate(modifier * .5f, 0, -.25f);
				ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(modifier * 40));
				ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(modifier * 10));
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(modifier * 90));
			}
		}

		itemRenderer.renderItem(stack, ModelTransformationMode.NONE, false, ms, buffer, light, overlay, model.getOriginalModel());

		ms.pop();
	}

}
