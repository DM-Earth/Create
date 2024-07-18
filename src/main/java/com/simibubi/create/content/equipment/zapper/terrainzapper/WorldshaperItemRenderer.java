package com.simibubi.create.content.equipment.zapper.terrainzapper;

import static java.lang.Math.max;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.zapper.ZapperItemRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class WorldshaperItemRenderer extends ZapperItemRenderer {

	protected static final PartialModel CORE = new PartialModel(Create.asResource("item/handheld_worldshaper/core"));
	protected static final PartialModel CORE_GLOW = new PartialModel(Create.asResource("item/handheld_worldshaper/core_glow"));
	protected static final PartialModel ACCELERATOR = new PartialModel(Create.asResource("item/handheld_worldshaper/accelerator"));

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ModelTransformationMode transformType,
		MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		super.render(stack, model, renderer, transformType, ms, buffer, light, overlay);

		float pt = AnimationTickHolder.getPartialTicks();
		float worldTime = AnimationTickHolder.getRenderTime() / 20;

		renderer.renderSolid(model.getOriginalModel(), light);

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		boolean leftHanded = player.getMainArm() == Arm.LEFT;
		boolean mainHand = player.getMainHandStack() == stack;
		boolean offHand = player.getOffHandStack() == stack;
		float animation = getAnimationProgress(pt, leftHanded, mainHand);

		// Core glows
		float multiplier;
		if (mainHand || offHand) 
			multiplier = animation;
		else
			multiplier = MathHelper.sin(worldTime * 5);

		int lightItensity = (int) (15 * MathHelper.clamp(multiplier, 0, 1));
		int glowLight = LightmapTextureManager.pack(lightItensity, max(lightItensity, 4));
		renderer.renderSolidGlowing(CORE.get(), glowLight);
		renderer.renderGlowing(CORE_GLOW.get(), glowLight);

		// Accelerator spins
		float angle = worldTime * -25;
		if (mainHand || offHand)
			angle += 360 * animation;

		angle %= 360;
		float offset = -.155f;
		ms.translate(0, offset, 0);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
		ms.translate(0, -offset, 0);
		renderer.render(ACCELERATOR.get(), light);
	}

}
