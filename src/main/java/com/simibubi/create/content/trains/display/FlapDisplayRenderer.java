package com.simibubi.create.content.trains.display;

import java.util.List;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.FontAccessor;

import org.joml.Matrix4f;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.EmptyGlyphRenderer;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.world.World;

public class FlapDisplayRenderer extends KineticBlockEntityRenderer<FlapDisplayBlockEntity> {

	public FlapDisplayRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FlapDisplayBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
		FontStorage fontSet = ((FontAccessor) fontRenderer).port_lib$getFontSet(Style.DEFAULT_FONT_ID);

		float scale = 1 / 32f;

		if (!be.isController)
			return;

		List<FlapDisplayLayout> lines = be.getLines();

		ms.push();
		TransformStack.cast(ms)
			.centre()
			.rotateY(AngleHelper.horizontalAngle(be.getCachedState()
				.get(FlapDisplayBlock.HORIZONTAL_FACING)))
			.unCentre()
			.translate(0, 0, -3 / 16f);

		ms.translate(0, 1, 1);
		ms.scale(scale, scale, scale);
		ms.scale(1, -1, 1);
		ms.translate(0, 0, 1 / 2f);

		for (int j = 0; j < lines.size(); j++) {
			List<FlapDisplaySection> line = lines.get(j)
				.getSections();
			int color = be.getLineColor(j);
			ms.push();

			float w = 0;
			for (FlapDisplaySection section : line)
				w += section.getSize() + (section.hasGap ? 8 : 1);
			ms.translate(be.xSize * 16 - w / 2 + 1, 4.5f, 0);

			Entry transform = ms.peek();
			FlapDisplayRenderOutput renderOutput = new FlapDisplayRenderOutput(buffer, color, transform.getPositionMatrix(), light,
				j, !be.isSpeedRequirementFulfilled(), be.getWorld(), be.isLineGlowing(j));

			for (int i = 0; i < line.size(); i++) {
				FlapDisplaySection section = line.get(i);
				renderOutput.nextSection(section);
				int ticks = AnimationTickHolder.getTicks(be.getWorld());
				String text = section.renderCharsIndividually() || !section.spinning[0] ? section.text
					: section.cyclingOptions[((ticks / 3) + i * 13) % section.cyclingOptions.length];
				TextVisitFactory.visitFormatted(text, Style.EMPTY, renderOutput);
				ms.translate(section.size + (section.hasGap ? 8 : 1), 0, 0);
			}

			if (buffer instanceof Immediate bs) {
				GlyphRenderer texturedglyph = fontSet.getRectangleRenderer();
				bs.draw(texturedglyph.getLayer(TextRenderer.TextLayerType.NORMAL));
			}

			ms.pop();
			ms.translate(0, 16, 0);
		}

		ms.pop();
	}

	@Environment(EnvType.CLIENT)
	static class FlapDisplayRenderOutput implements CharacterVisitor {

		final VertexConsumerProvider bufferSource;
		final float r, g, b, a;
		final Matrix4f pose;
		final int light;
		final boolean paused;

		FlapDisplaySection section;
		float x;
		private int lineIndex;
		private World level;

		public FlapDisplayRenderOutput(VertexConsumerProvider buffer, int color, Matrix4f pose, int light, int lineIndex,
			boolean paused, World level, boolean glowing) {
			this.bufferSource = buffer;
			this.lineIndex = lineIndex;
			this.level = level;
			this.a = glowing ? .975f : .85f;
			this.r = (color >> 16 & 255) / 255f;
			this.g = (color >> 8 & 255) / 255f;
			this.b = (color & 255) / 255f;
			this.pose = pose;
			this.light = glowing ? 0xf000f0 : light;
			this.paused = paused;
		}

		public void nextSection(FlapDisplaySection section) {
			this.section = section;
			x = 0;
		}

		public boolean accept(int charIndex, Style style, int glyph) {
			FontStorage fontset = getFontSet();
			int ticks = paused ? 0 : AnimationTickHolder.getTicks(level);
			float time = paused ? 0 : AnimationTickHolder.getRenderTime(level);
			float dim = 1;

			if (section.renderCharsIndividually() && section.spinning[Math.min(charIndex, section.spinning.length)]) {
				float speed = section.spinningTicks > 5 && section.spinningTicks < 20 ? 1.75f : 2.5f;
				float cycle = (time / speed) + charIndex * 16.83f + lineIndex * 0.75f;
				float partial = cycle % 1;
				char cyclingGlyph = section.cyclingOptions[((int) cycle) % section.cyclingOptions.length].charAt(0);
				glyph = paused ? cyclingGlyph : partial > 1 / 2f ? partial > 3 / 4f ? '_' : '-' : cyclingGlyph;
				dim = 0.75f;
			}

			Glyph glyphinfo = fontset.getGlyph(glyph, false);
			float glyphWidth = glyphinfo.getAdvance(false);

			if (!section.renderCharsIndividually() && section.spinning[0]) {
				glyph = ticks % 3 == 0 ? glyphWidth == 6 ? '-' : glyphWidth == 1 ? '\'' : glyph : glyph;
				glyph = ticks % 3 == 2 ? glyphWidth == 6 ? '_' : glyphWidth == 1 ? '.' : glyph : glyph;
				if (ticks % 3 != 1)
					dim = 0.75f;
			}

			GlyphRenderer bakedglyph =
				style.isObfuscated() && glyph != 32 ? fontset.getObfuscatedGlyphRenderer(glyphinfo) : fontset.getGlyphRenderer(glyph);
			TextColor textcolor = style.getColor();

			float red = this.r * dim;
			float green = this.g * dim;
			float blue = this.b * dim;

			if (textcolor != null) {
				int i = textcolor.getRgb();
				red = (i >> 16 & 255) / 255f;
				green = (i >> 8 & 255) / 255f;
				blue = (i & 255) / 255f;
			}

			float standardWidth = section.wideFlaps ? FlapDisplaySection.WIDE_MONOSPACE : FlapDisplaySection.MONOSPACE;

			if (section.renderCharsIndividually())
				x += (standardWidth - glyphWidth) / 2f;

			if (isNotEmpty(bakedglyph)) {
				VertexConsumer vertexconsumer = bufferSource.getBuffer(renderTypeOf(bakedglyph));
				bakedglyph.draw(style.isItalic(), x, 0, pose, vertexconsumer, red, green, blue, a, light);
			}

			if (section.renderCharsIndividually())
				x += standardWidth - (standardWidth - glyphWidth) / 2f;
			else
				x += glyphWidth;

			return true;
		}

		public float finish(int bgColor) {
			if (bgColor == 0)
				return x;

			float a = (bgColor >> 24 & 255) / 255f;
			float r = (bgColor >> 16 & 255) / 255f;
			float g = (bgColor >> 8 & 255) / 255f;
			float b = (bgColor & 255) / 255f;

			GlyphRenderer bakedglyph = getFontSet().getRectangleRenderer();
			VertexConsumer vertexconsumer = bufferSource.getBuffer(renderTypeOf(bakedglyph));
			bakedglyph.drawRectangle(new GlyphRenderer.Rectangle(-1f, 9f, section.size, -2f, 0.01f, r, g, b, a), this.pose,
				vertexconsumer, light);

			return x;
		}

		private FontStorage getFontSet() {
			return ((FontAccessor) MinecraftClient.getInstance().textRenderer).port_lib$getFontSet(Style.DEFAULT_FONT_ID);
		}

		private RenderLayer renderTypeOf(GlyphRenderer bakedglyph) {
			return bakedglyph.getLayer(TextRenderer.TextLayerType.NORMAL);
		}

		private boolean isNotEmpty(GlyphRenderer bakedglyph) {
			return !(bakedglyph instanceof EmptyGlyphRenderer);
		}

	}

	@Override
	protected SuperByteBuffer getRotatedModel(FlapDisplayBlockEntity be, BlockState state) {
		return CachedBufferer.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state,
			state.get(FlapDisplayBlock.HORIZONTAL_FACING));
	}

	@Override
	public boolean rendersOutsideBoundingBox(FlapDisplayBlockEntity be) {
		return be.isController;
	}

}
