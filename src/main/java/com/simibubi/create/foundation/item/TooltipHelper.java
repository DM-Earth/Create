package com.simibubi.create.foundation.item;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.google.common.base.Strings;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.common.util.MinecraftClientUtil;

public class TooltipHelper {

	public static final int MAX_WIDTH_PER_LINE = 200;

	public static MutableText holdShift(Palette palette, boolean highlighted) {
		return Lang.translateDirect("tooltip.holdForDescription", Lang.translateDirect("tooltip.keyShift")
			.formatted(Formatting.GRAY))
			.formatted(Formatting.DARK_GRAY);
	}

	public static void addHint(List<Text> tooltip, String hintKey, Object... messageParams) {
		Text spacing = IHaveGoggleInformation.componentSpacing;
		tooltip.add(spacing.copyContentOnly()
			.append(Lang.translateDirect(hintKey + ".title"))
			.formatted(Formatting.GOLD));
		Text hint = Lang.translateDirect(hintKey);
		List<Text> cutComponent = cutTextComponent(hint, Palette.GRAY_AND_WHITE);
		for (Text component : cutComponent)
			tooltip.add(spacing.copyContentOnly()
				.append(component));
	}

	public static String makeProgressBar(int length, int filledLength) {
		String bar = " ";
		int emptySpaces = length - filledLength;
		for (int i = 0; i < filledLength; i++)
			bar += "\u2588";
		for (int i = 0; i < emptySpaces; i++)
			bar += "\u2592";
		return bar + " ";
	}

	public static Style styleFromColor(Formatting color) {
		return Style.EMPTY.withFormatting(color);
	}

	public static Style styleFromColor(int hex) {
		return Style.EMPTY.withColor(hex);
	}

	public static List<Text> cutStringTextComponent(String s, Palette palette) {
		return cutTextComponent(Components.literal(s), palette);
	}

	public static List<Text> cutTextComponent(Text c, Palette palette) {
		return cutTextComponent(c, palette.primary(), palette.highlight());
	}

	public static List<Text> cutStringTextComponent(String s, Style primaryStyle,
		Style highlightStyle) {
		return cutTextComponent(Components.literal(s), primaryStyle, highlightStyle);
	}

	public static List<Text> cutTextComponent(Text c, Style primaryStyle,
		Style highlightStyle) {
		return cutTextComponent(c, primaryStyle, highlightStyle, 0);
	}

	public static List<Text> cutStringTextComponent(String c, Style primaryStyle,
		Style highlightStyle, int indent) {
		return cutTextComponent(Components.literal(c), primaryStyle, highlightStyle, indent);
	}

	public static List<Text> cutTextComponent(Text c, Style primaryStyle,
		Style highlightStyle, int indent) {
		String s = c.getString();

		// Split words
		List<String> words = new LinkedList<>();
		BreakIterator iterator = BreakIterator.getLineInstance(MinecraftClientUtil.getLocale());
		iterator.setText(s);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
			String word = s.substring(start, end);
			words.add(word);
		}

		// Apply hard wrap
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		List<String> lines = new LinkedList<>();
		StringBuilder currentLine = new StringBuilder();
		int width = 0;
		for (String word : words) {
			int newWidth = font.getWidth(word.replaceAll("_", ""));
			if (width + newWidth > MAX_WIDTH_PER_LINE) {
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

		// Format
		MutableText lineStart = Components.literal(Strings.repeat(" ", indent));
		lineStart.fillStyle(primaryStyle);
		List<Text> formattedLines = new ArrayList<>(lines.size());
		Couple<Style> styles = Couple.create(highlightStyle, primaryStyle);

		boolean currentlyHighlighted = false;
		for (String string : lines) {
			MutableText currentComponent = lineStart.copyContentOnly();
			String[] split = string.split("_");
			for (String part : split) {
				currentComponent.append(Components.literal(part).fillStyle(styles.get(currentlyHighlighted)));
				currentlyHighlighted = !currentlyHighlighted;
			}

			formattedLines.add(currentComponent);
			currentlyHighlighted = !currentlyHighlighted;
		}

		return formattedLines;
	}

	public record Palette(Style primary, Style highlight) {
		public static final Palette STANDARD_CREATE = new Palette(styleFromColor(0xC9974C), styleFromColor(0xF1DD79));

		public static final Palette BLUE = ofColors(Formatting.BLUE, Formatting.AQUA);
		public static final Palette GREEN = ofColors(Formatting.DARK_GREEN, Formatting.GREEN);
		public static final Palette YELLOW = ofColors(Formatting.GOLD, Formatting.YELLOW);
		public static final Palette RED = ofColors(Formatting.DARK_RED, Formatting.RED);
		public static final Palette PURPLE = ofColors(Formatting.DARK_PURPLE, Formatting.LIGHT_PURPLE);
		public static final Palette GRAY = ofColors(Formatting.DARK_GRAY, Formatting.GRAY);

		public static final Palette ALL_GRAY = ofColors(Formatting.GRAY, Formatting.GRAY);
		public static final Palette GRAY_AND_BLUE = ofColors(Formatting.GRAY, Formatting.BLUE);
		public static final Palette GRAY_AND_WHITE = ofColors(Formatting.GRAY, Formatting.WHITE);
		public static final Palette GRAY_AND_GOLD = ofColors(Formatting.GRAY, Formatting.GOLD);
		public static final Palette GRAY_AND_RED = ofColors(Formatting.GRAY, Formatting.RED);

		public static Palette ofColors(Formatting primary, Formatting highlight) {
			return new Palette(styleFromColor(primary), styleFromColor(highlight));
		}
	}

}
