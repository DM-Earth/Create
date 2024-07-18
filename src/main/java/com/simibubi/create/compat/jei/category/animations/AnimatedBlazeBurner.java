package com.simibubi.create.compat.jei.category.animations;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class AnimatedBlazeBurner extends AnimatedKinetics {

	private HeatLevel heatLevel;

	public AnimatedBlazeBurner withHeat(HeatLevel heatLevel) {
		this.heatLevel = heatLevel;
		return this;
	}

	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 200);
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 23;

		float offset = (MathHelper.sin(AnimationTickHolder.getRenderTime() / 16f) + 0.5f) / 16f;

		blockElement(AllBlocks.BLAZE_BURNER.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);

		PartialModel blaze =
			heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_SUPER : AllPartialModels.BLAZE_ACTIVE;
		PartialModel rods2 = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
			: AllPartialModels.BLAZE_BURNER_RODS_2;

		blockElement(blaze).atLocal(1, 1.8, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.render(graphics);
		blockElement(rods2).atLocal(1, 1.7 + offset, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.scale(scale, -scale, scale);
		matrixStack.translate(0, -1.8, 0);

		SpriteShiftEntry spriteShift =
			heatLevel == HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME : AllSpriteShifts.BURNER_FLAME;

		float spriteWidth = spriteShift.getTarget()
			.getMaxU()
			- spriteShift.getTarget()
				.getMinU();

		float spriteHeight = spriteShift.getTarget()
			.getMaxV()
			- spriteShift.getTarget()
				.getMinV();

		float time = AnimationTickHolder.getRenderTime(MinecraftClient.getInstance().world);
		float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

		double vScroll = speed * time;
		vScroll = vScroll - Math.floor(vScroll);
		vScroll = vScroll * spriteHeight / 2;

		double uScroll = speed * time / 2;
		uScroll = uScroll - Math.floor(uScroll);
		uScroll = uScroll * spriteWidth / 2;

		MinecraftClient mc = MinecraftClient.getInstance();
		VertexConsumerProvider.Immediate buffer = mc.getBufferBuilders()
			.getEntityVertexConsumers();
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getCutoutMipped());
		CachedBufferer.partial(AllPartialModels.BLAZE_BURNER_FLAME, Blocks.AIR.getDefaultState())
			.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.renderInto(matrixStack, vb);
		matrixStack.pop();
	}

}
