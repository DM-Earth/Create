package com.simibubi.create.foundation.gui.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class SelectionScrollInput extends ScrollInput {

	private final MutableText scrollToSelect = Lang.translateDirect("gui.scrollInput.scrollToSelect");
	protected List<? extends Text> options;

	public SelectionScrollInput(int xIn, int yIn, int widthIn, int heightIn) {
		super(xIn, yIn, widthIn, heightIn);
		options = new ArrayList<>();
		inverted();
	}

	public ScrollInput forOptions(List<? extends Text> options) {
		this.options = options;
		this.max = options.size();
		format(options::get);
		applyTooltip();
		return this;
	}

	@Override
	protected void applyTooltip() {
		toolTip.clear();
		if (title == null)
			return;
		toolTip.add(title.copyContentOnly()
			.styled(s -> s.withColor(HEADER_RGB)));
		int min = Math.min(this.max - 16, state - 7);
		int max = Math.max(this.min + 16, state + 8);
		min = Math.max(min, this.min);
		max = Math.min(max, this.max);
		if (this.min + 1 == min)
			min--;
		if (min > this.min)
			toolTip.add(Components.literal("> ...")
				.formatted(Formatting.GRAY));
		if (this.max - 1 == max)
			max++;
		for (int i = min; i < max; i++) {
			if (i == state)
				toolTip.add(Components.empty()
					.append("-> ")
					.append(options.get(i))
					.formatted(Formatting.WHITE));
			else
				toolTip.add(Components.empty()
					.append("> ")
					.append(options.get(i))
					.formatted(Formatting.GRAY));
		}
		if (max < this.max)
			toolTip.add(Components.literal("> ...")
				.formatted(Formatting.GRAY));

		if (hint != null)
			toolTip.add(hint.copyContentOnly()
				.styled(s -> s.withColor(HINT_RGB)));
		toolTip.add(scrollToSelect.copyContentOnly()
			.formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
	}

}
