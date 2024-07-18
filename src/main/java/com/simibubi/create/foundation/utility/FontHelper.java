package com.simibubi.create.foundation.utility;

import java.text.BreakIterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import io.github.fabricators_of_create.porting_lib.common.util.MinecraftClientUtil;

import org.joml.Matrix4f;

public final class FontHelper {

	private FontHelper() {}

	public static List<String> cutString(TextRenderer font, String text, int maxWidthPerLine) {
		// Split words
		List<String> words = new LinkedList<>();
		BreakIterator iterator = BreakIterator.getLineInstance(MinecraftClientUtil.getLocale());
		iterator.setText(text);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
			String word = text.substring(start, end);
			words.add(word);
		}
		// Apply hard wrap
		List<String> lines = new LinkedList<>();
		StringBuilder currentLine = new StringBuilder();
		int width = 0;
		for (String word : words) {
			int newWidth = font.getWidth(word);
			if (width + newWidth > maxWidthPerLine) {
				if (width > 0) {
					String line = currentLine.toString();
					lines.add(line);
					currentLine = new StringBuilder();
					width = 0;
				} else {
					lines.add(word);
					continue;
				}
			}
			currentLine.append(word);
			width += newWidth;
		}
		if (width > 0) {
			lines.add(currentLine.toString());
		}
		return lines;
	}

	public static void drawSplitString(MatrixStack ms, TextRenderer font, String text, int x, int y, int width,
		int color) {
		List<String> list = cutString(font, text, width);
		Matrix4f matrix4f = ms.peek()
			.getPositionMatrix();

		for (String s : list) {
			float f = (float) x;
			if (font.isRightToLeft()) {
				int i = font.getWidth(font.mirror(s));
				f += (float) (width - i);
			}

			draw(font, s, f, (float) y, color, matrix4f, false);
			y += 9;
		}
	}

	private static int draw(TextRenderer font, String p_228078_1_, float p_228078_2_, float p_228078_3_,
		int p_228078_4_, Matrix4f p_228078_5_, boolean p_228078_6_) {
		if (p_228078_1_ == null) {
			return 0;
		} else {
			VertexConsumerProvider.Immediate irendertypebuffer$impl = VertexConsumerProvider.immediate(Tessellator.getInstance()
				.getBuffer());
			int i = font.draw(p_228078_1_, p_228078_2_, p_228078_3_, p_228078_4_, p_228078_6_, p_228078_5_,
				irendertypebuffer$impl, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
			irendertypebuffer$impl.draw();
			return i;
		}
	}

}
