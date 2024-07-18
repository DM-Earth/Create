package com.simibubi.create.content.trains.schedule;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.simibubi.create.foundation.utility.IntAttached;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.CommandSuggestions$SuggestionsListAccessor;

public class DestinationSuggestions extends ChatInputSuggestor {

	private TextFieldWidget textBox;
	private List<IntAttached<String>> viableStations;
	private String previous = "<>";
	private TextRenderer font;
	private boolean active;

	List<Suggestion> currentSuggestions;
	private int yOffset;

	public DestinationSuggestions(MinecraftClient pMinecraft, Screen pScreen, TextFieldWidget pInput, TextRenderer pFont,
		List<IntAttached<String>> viableStations, int yOffset) {
		super(pMinecraft, pScreen, pInput, pFont, true, true, 0, 7, false, 0xee_303030);
		this.textBox = pInput;
		this.font = pFont;
		this.viableStations = viableStations;
		this.yOffset = yOffset;
		currentSuggestions = new ArrayList<>();
		active = false;
	}

	public void tick() {
		if (window == null)
			textBox.setSuggestion("");
		if (active == textBox.isFocused())
			return;
		active = textBox.isFocused();
		refresh();
	}

	@Override
	public void refresh() {
		String value = this.textBox.getText();
		if (value.equals(previous))
			return;
		if (!active) {
			window = null;
			return;
		}

		previous = value;
		currentSuggestions = viableStations.stream()
			.filter(ia -> !ia.getValue()
				.equals(value) && ia.getValue()
					.toLowerCase()
					.startsWith(value.toLowerCase()))
			.sorted((ia1, ia2) -> Long.compare(ia1.getFirst(), ia2.getFirst()))
			.map(IntAttached::getValue)
			.map(s -> new Suggestion(new StringRange(0, s.length()), s))
			.toList();

		show(false);
	}

	public void show(boolean pNarrateFirstSuggestion) {
		if (currentSuggestions.isEmpty()) {
			window = null;
			return;
		}

		int width = 0;
		for (Suggestion suggestion : currentSuggestions)
			width = Math.max(width, this.font.getWidth(suggestion.getText()));
		int x = MathHelper.clamp(textBox.getCharacterX(0), 0, textBox.getCharacterX(0) + textBox.getInnerWidth() - width);
		window = CommandSuggestions$SuggestionsListAccessor.port_lib$create(this, x, 72 + yOffset, width, currentSuggestions, false);
	}

}
