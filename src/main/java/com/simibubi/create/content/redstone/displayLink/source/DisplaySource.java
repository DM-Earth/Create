package com.simibubi.create.content.redstone.displayLink.source;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.redstone.displayLink.DisplayBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayBoardTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public abstract class DisplaySource extends DisplayBehaviour {

	public static final List<MutableText> EMPTY = ImmutableList.of(Components.empty());
	public static final MutableText EMPTY_LINE = Components.empty();
	public static final MutableText WHITESPACE = Components.literal(" ");

	public abstract List<MutableText> provideText(DisplayLinkContext context, DisplayTargetStats stats);

	public void transferData(DisplayLinkContext context, DisplayTarget activeTarget, int line) {
		DisplayTargetStats stats = activeTarget.provideStats(context);

		if (activeTarget instanceof DisplayBoardTarget fddt) {
			List<List<MutableText>> flapDisplayText = provideFlapDisplayText(context, stats);
			fddt.acceptFlapText(line, flapDisplayText, context);
		}

		List<MutableText> text = provideText(context, stats);
		if (text.isEmpty())
			text = EMPTY;
		activeTarget.acceptText(line, text, context);
	}

	public void onSignalReset(DisplayLinkContext context) {};

	public void populateData(DisplayLinkContext context) {};

	public int getPassiveRefreshTicks() {
		return 100;
	};

	public boolean shouldPassiveReset() {
		return true;
	}

	protected String getTranslationKey() {
		return id.getPath();
	}

	public Text getName() {
		return Components.translatable(id.getNamespace() + ".display_source." + getTranslationKey());
	}

	public void loadFlapDisplayLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout, int lineIndex) {
		loadFlapDisplayLayout(context, flapDisplay, layout);
	}

	public void loadFlapDisplayLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay,
		FlapDisplayLayout layout) {
		if (!layout.isLayout("Default"))
			layout.loadDefault(flapDisplay.getMaxCharCount());
	}

	public List<List<MutableText>> provideFlapDisplayText(DisplayLinkContext context, DisplayTargetStats stats) {
		return provideText(context, stats).stream()
			.map(Arrays::asList)
			.toList();
	}

	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder,
		boolean isFirstLine) {}

}
