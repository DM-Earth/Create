package com.simibubi.create.content.equipment.potatoCannon;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class PotatoCannonItemRenderer extends CustomRenderedItemModelRenderer {

	protected static final PartialModel COG = new PartialModel(Create.asResource("item/potato_cannon/cog"));

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
		ModelTransformationMode transformType, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ItemRenderer itemRenderer = mc.getItemRenderer();
		renderer.render(model.getOriginalModel(), light);
		ClientPlayerEntity player = mc.player;
		boolean mainHand = player.getMainHandStack() == stack;
		boolean offHand = player.getOffHandStack() == stack;
		boolean leftHanded = player.getMainArm() == Arm.LEFT;

		float offset = .5f / 16;
		float worldTime = AnimationTickHolder.getRenderTime() / 10;
		float angle = worldTime * -25;
		float speed = CreateClient.POTATO_CANNON_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded,
			AnimationTickHolder.getPartialTicks());

		if (mainHand || offHand)
			angle += 360 * MathHelper.clamp(speed * 5, 0, 1);
		angle %= 360;

		ms.push();
		ms.translate(0, offset, 0);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
		ms.translate(0, -offset, 0);
		renderer.render(COG.get(), light);
		ms.pop();

		if (transformType == ModelTransformationMode.GUI) {
			PotatoCannonItem.getAmmoforPreview(stack)
				.ifPresent(ammo -> {
					MatrixStack localMs = new MatrixStack();
					localMs.translate(-1 / 4f, -1 / 4f, 1);
					localMs.scale(.5f, .5f, .5f);
					TransformStack.cast(localMs)
						.rotateY(-34);
					itemRenderer.renderItem(ammo, ModelTransformationMode.GUI, light, OverlayTexture.DEFAULT_UV, localMs,
						buffer, mc.world, 0);
				});
		}

	}

}
