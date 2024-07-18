package com.simibubi.create.foundation.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fabricators_of_create.porting_lib.util.client.ScreenUtils;

import org.joml.Matrix4f;

public class RemovedGuiUtils {
	@Nonnull
	private static ItemStack cachedTooltipStack = ItemStack.EMPTY;

	public static void preItemToolTip(@Nonnull ItemStack stack) {
		cachedTooltipStack = stack;
	}

	public static void postItemToolTip() {
		cachedTooltipStack = ItemStack.EMPTY;
	}

	public static void drawHoveringText(DrawContext graphics, List<? extends StringVisitable> textLines, int mouseX,
										int mouseY, int screenWidth, int screenHeight, int maxTextWidth, TextRenderer font) {
		drawHoveringText(graphics, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth,
				ScreenUtils.DEFAULT_BACKGROUND_COLOR, ScreenUtils.DEFAULT_BORDER_COLOR_START, ScreenUtils.DEFAULT_BORDER_COLOR_END,
				font);
	}

	public static void drawHoveringText(DrawContext graphics, List<? extends StringVisitable> textLines, int mouseX,
										int mouseY, int screenWidth, int screenHeight, int maxTextWidth, int backgroundColor, int borderColorStart,
										int borderColorEnd, TextRenderer font) {
		drawHoveringText(cachedTooltipStack, graphics, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth,
				backgroundColor, borderColorStart, borderColorEnd, font);
	}

	public static void drawHoveringText(@Nonnull final ItemStack stack, DrawContext graphics,
										List<? extends StringVisitable> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
										int maxTextWidth, TextRenderer font) {
		drawHoveringText(stack, graphics, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth,
				ScreenUtils.DEFAULT_BACKGROUND_COLOR, ScreenUtils.DEFAULT_BORDER_COLOR_START, ScreenUtils.DEFAULT_BORDER_COLOR_END,
				font);
	}

	public static void drawHoveringText(@Nonnull final ItemStack stack, DrawContext graphics,
										List<? extends StringVisitable> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
										int maxTextWidth, int backgroundColor, int borderColorStart, int borderColorEnd, TextRenderer font) {
		if (textLines.isEmpty())
			return;

		List<TooltipComponent> list = new ArrayList<>();
		for (StringVisitable textLine : textLines) {
			OrderedText charSequence = textLine instanceof Text component
					? component.asOrderedText()
					: Language.getInstance().reorder(textLine);
			list.add(TooltipComponent.of(charSequence));
		}

		MatrixStack pStack = graphics.getMatrices();

		// RenderSystem.disableRescaleNormal();
		RenderSystem.disableDepthTest();
		int tooltipTextWidth = 0;

		for (StringVisitable textLine : textLines) {
			int textLineWidth = font.getWidth(textLine);
			if (textLineWidth > tooltipTextWidth)
				tooltipTextWidth = textLineWidth;
		}

		boolean needsWrap = false;

		int titleLinesCount = 1;
		int tooltipX = mouseX + 12;
		if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
			tooltipX = mouseX - 16 - tooltipTextWidth;
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
			{
				if (mouseX > screenWidth / 2)
					tooltipTextWidth = mouseX - 12 - 8;
				else
					tooltipTextWidth = screenWidth - 16 - mouseX;
				needsWrap = true;
			}
		}

		if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
			tooltipTextWidth = maxTextWidth;
			needsWrap = true;
		}

		if (needsWrap) {
			int wrappedTooltipWidth = 0;
			List<StringVisitable> wrappedTextLines = new ArrayList<>();
			for (int i = 0; i < textLines.size(); i++) {
				StringVisitable textLine = textLines.get(i);
				List<StringVisitable> wrappedLine = font.getTextHandler()
						.wrapLines(textLine, tooltipTextWidth, Style.EMPTY);
				if (i == 0)
					titleLinesCount = wrappedLine.size();

				for (StringVisitable line : wrappedLine) {
					int lineWidth = font.getWidth(line);
					if (lineWidth > wrappedTooltipWidth)
						wrappedTooltipWidth = lineWidth;
					wrappedTextLines.add(line);
				}
			}
			tooltipTextWidth = wrappedTooltipWidth;
			textLines = wrappedTextLines;

			if (mouseX > screenWidth / 2)
				tooltipX = mouseX - 16 - tooltipTextWidth;
			else
				tooltipX = mouseX + 12;
		}

		int tooltipY = mouseY - 12;
		int tooltipHeight = 8;

		if (textLines.size() > 1) {
			tooltipHeight += (textLines.size() - 1) * 10;
			if (textLines.size() > titleLinesCount)
				tooltipHeight += 2; // gap between title lines and next lines
		}

		if (tooltipY < 4)
			tooltipY = 4;
		else if (tooltipY + tooltipHeight + 4 > screenHeight)
			tooltipY = screenHeight - tooltipHeight - 4;

		final int zLevel = 400;

		pStack.push();
		Matrix4f mat = pStack.peek()
				.getPositionMatrix();
		graphics.fillGradient(tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3,
				tooltipY - 3, zLevel, backgroundColor, backgroundColor);
		graphics.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 3,
				tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, zLevel, backgroundColor, backgroundColor);
		graphics.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3,
				tooltipY + tooltipHeight + 3, zLevel, backgroundColor, backgroundColor);
		graphics.fillGradient(tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3,
				zLevel, backgroundColor, backgroundColor);
		graphics.fillGradient(tooltipX + tooltipTextWidth + 3, tooltipY - 3,
				tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, zLevel, backgroundColor, backgroundColor);
		graphics.fillGradient(tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1,
				tooltipY + tooltipHeight + 3 - 1, zLevel, borderColorStart, borderColorEnd);
		graphics.fillGradient(tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1,
				tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, zLevel, borderColorStart, borderColorEnd);
		graphics.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3,
				tooltipY - 3 + 1, zLevel, borderColorStart, borderColorStart);
		graphics.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 2,
				tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, zLevel, borderColorEnd, borderColorEnd);

		VertexConsumerProvider.Immediate renderType = VertexConsumerProvider.immediate(Tessellator.getInstance()
				.getBuffer());
		pStack.translate(0.0D, 0.0D, zLevel);

		for (int lineNumber = 0; lineNumber < list.size(); ++lineNumber) {
			TooltipComponent line = list.get(lineNumber);

			if (line != null)
				line.drawText(font, tooltipX, tooltipY, mat, renderType);

			if (lineNumber + 1 == titleLinesCount)
				tooltipY += 2;

			tooltipY += 10;
		}

		renderType.draw();
		pStack.pop();

		RenderSystem.enableDepthTest();
	}
}
