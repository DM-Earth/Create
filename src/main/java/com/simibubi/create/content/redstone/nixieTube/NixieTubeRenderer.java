package com.simibubi.create.content.redstone.nixieTube;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.redstone.nixieTube.DoubleFaceAttachedBlock.DoubleAttachFace;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.DyeHelper;

import com.simibubi.create.foundation.utility.Iterate;

import io.github.fabricators_of_create.porting_lib.util.FontRenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class NixieTubeRenderer extends SafeBlockEntityRenderer<NixieTubeBlockEntity> {

	private static Random r = Random.create();

	public NixieTubeRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(NixieTubeBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		ms.push();
		BlockState blockState = be.getCachedState();
		DoubleAttachFace face = blockState.get(NixieTubeBlock.FACE);
		float yRot = AngleHelper.horizontalAngle(blockState.get(NixieTubeBlock.FACING)) - 90
			+ (face == DoubleAttachFace.WALL_REVERSED ? 180 : 0);
		float xRot = face == DoubleAttachFace.WALL ? -90 : face == DoubleAttachFace.WALL_REVERSED ? 90 : 0;

		TransformStack msr = TransformStack.cast(ms);
		msr.centre()
			.rotateY(yRot)
			.rotateZ(xRot)
			.unCentre();

		if (be.signalState != null) {
			renderAsSignal(be, partialTicks, ms, buffer, light, overlay);
			ms.pop();
			return;
		}

		msr.centre();

		float height = face == DoubleAttachFace.CEILING ? 5 : 3;
		float scale = 1 / 20f;

		Couple<String> s = be.getDisplayedStrings();
		DyeColor color = NixieTubeBlock.colorOf(be.getCachedState());

		ms.push();
		ms.translate(-4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		drawTube(ms, buffer, s.getFirst(), height, color);
		ms.pop();

		ms.push();
		ms.translate(4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		drawTube(ms, buffer, s.getSecond(), height, color);
		ms.pop();

		ms.pop();
	}

	public static void drawTube(MatrixStack ms, VertexConsumerProvider buffer, String c, float height, DyeColor color) {
		TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
		float charWidth = fontRenderer.getWidth(c);
		float shadowOffset = .5f;
		float flicker = r.nextFloat();
		Couple<Integer> couple = DyeHelper.DYE_TABLE.get(color);
		int brightColor = couple.getFirst();
		int darkColor = couple.getSecond();
		int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4);

		ms.push();
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		drawInWorldString(ms, buffer, c, flickeringBrightColor);
		ms.push();
		ms.translate(shadowOffset, shadowOffset, -1 / 16f);
		drawInWorldString(ms, buffer, c, darkColor);
		ms.pop();
		ms.pop();

		ms.push();
		ms.scale(-1, 1, 1);
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		drawInWorldString(ms, buffer, c, darkColor);
		ms.push();
		ms.translate(-shadowOffset, shadowOffset, -1 / 16f);
		drawInWorldString(ms, buffer, c, Color.mixColors(darkColor, 0, .35f));
		ms.pop();
		ms.pop();
	}

	public static void drawInWorldString(MatrixStack ms, VertexConsumerProvider buffer, String c, int color) {
		TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
		fontRenderer.draw(c, 0, 0, color, false, ms.peek()
			.getPositionMatrix(), buffer, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
		if (buffer instanceof Immediate) {
			GlyphRenderer texturedglyph = FontRenderUtil.getFontStorage(fontRenderer, Style.DEFAULT_FONT_ID)
				.getRectangleRenderer();
			((Immediate) buffer).draw(texturedglyph.getLayer(TextRenderer.TextLayerType.NORMAL));
		}
	}

	private void renderAsSignal(NixieTubeBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		BlockState blockState = be.getCachedState();
		Direction facing = NixieTubeBlock.getFacing(blockState);
		Vec3d observerVec = MinecraftClient.getInstance().cameraEntity.getCameraPosVec(partialTicks);
		TransformStack msr = TransformStack.cast(ms);

		if (facing == Direction.DOWN)
			msr.centre()
				.rotateZ(180)
				.unCentre();

		boolean invertTubes =
			facing == Direction.DOWN || blockState.get(NixieTubeBlock.FACE) == DoubleAttachFace.WALL_REVERSED;

		CachedBufferer.partial(AllPartialModels.SIGNAL_PANEL, blockState)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));

		ms.push();
		ms.translate(1 / 2f, 7.5f / 16f, 1 / 2f);
		float renderTime = AnimationTickHolder.getRenderTime(be.getWorld());

		for (boolean first : Iterate.trueAndFalse) {
			Vec3d lampVec = Vec3d.ofCenter(be.getPos());
			Vec3d diff = lampVec.subtract(observerVec);

			if (first && !be.signalState.isRedLight(renderTime))
				continue;
			if (!first && !be.signalState.isGreenLight(renderTime) && !be.signalState.isYellowLight(renderTime))
				continue;

			boolean flip = first == invertTubes;
			boolean yellow = be.signalState.isYellowLight(renderTime);

			ms.push();
			ms.translate(flip ? 4 / 16f : -4 / 16f, 0, 0);

			if (diff.lengthSquared() < 96 * 96) {
				boolean vert = first ^ facing.getAxis()
					.isHorizontal();
				float longSide = yellow ? 1 : 4;
				float longSideGlow = yellow ? 2 : 5.125f;

				CachedBufferer.partial(AllPartialModels.SIGNAL_WHITE_CUBE, blockState)
					.light(0xf000f0)
					.disableDiffuse()
					.scale(vert ? longSide : 1, vert ? 1 : longSide, 1)
					.renderInto(ms, buffer.getBuffer(RenderLayer.getTranslucent()));

				CachedBufferer
					.partial(
						first ? AllPartialModels.SIGNAL_RED_GLOW
							: yellow ? AllPartialModels.SIGNAL_YELLOW_GLOW : AllPartialModels.SIGNAL_WHITE_GLOW,
						blockState)
					.light(0xf000f0)
					.disableDiffuse()
					.scale(vert ? longSideGlow : 2, vert ? 2 : longSideGlow, 2)
					.renderInto(ms, buffer.getBuffer(RenderTypes.getAdditive()));
			}

			CachedBufferer
				.partial(first ? AllPartialModels.SIGNAL_RED
					: yellow ? AllPartialModels.SIGNAL_YELLOW : AllPartialModels.SIGNAL_WHITE, blockState)
				.light(0xF000F0)
				.disableDiffuse()
				.scale(1 + 1 / 16f)
				.renderInto(ms, buffer.getBuffer(RenderTypes.getAdditive()));

			ms.pop();
		}
		ms.pop();

	}

	@Override
	public int getRenderDistance() {
		return 128;
	}

}
