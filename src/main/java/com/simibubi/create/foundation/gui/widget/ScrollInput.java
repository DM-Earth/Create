package com.simibubi.create.foundation.gui.widget;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour.StepContext;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class ScrollInput extends AbstractSimiWidget {

	protected Consumer<Integer> onScroll;
	protected int state;
	protected Text title = Lang.translateDirect("gui.scrollInput.defaultTitle");
	protected final Text scrollToModify = Lang.translateDirect("gui.scrollInput.scrollToModify");
	protected final Text shiftScrollsFaster = Lang.translateDirect("gui.scrollInput.shiftScrollsFaster");
	protected Text hint = null;
	protected Label displayLabel;
	protected boolean inverted;
	protected boolean soundPlayed;
	protected Function<Integer, Text> formatter;

	protected int min, max;
	protected int shiftStep;
	Function<StepContext, Integer> step;

	public ScrollInput(int xIn, int yIn, int widthIn, int heightIn) {
		super(xIn, yIn, widthIn, heightIn);
		state = 0;
		min = 0;
		max = 1;
		shiftStep = 5;
		step = standardStep();
		formatter = i -> Components.literal(String.valueOf(i));
		soundPlayed = false;
	}

	public Function<StepContext, Integer> standardStep() {
		return c -> c.shift ? shiftStep : 1;
	}

	public ScrollInput inverted() {
		inverted = true;
		return this;
	}

	public ScrollInput withRange(int min, int max) {
		this.min = min;
		this.max = max;
		return this;
	}

	public ScrollInput calling(Consumer<Integer> onScroll) {
		this.onScroll = onScroll;
		return this;
	}

	public ScrollInput format(Function<Integer, Text> formatter) {
		this.formatter = formatter;
		return this;
	}

	public ScrollInput removeCallback() {
		this.onScroll = null;
		return this;
	}

	public ScrollInput titled(MutableText title) {
		this.title = title;
		applyTooltip();
		return this;
	}

	public ScrollInput addHint(MutableText hint) {
		this.hint = hint;
		applyTooltip();
		return this;
	}

	public ScrollInput withStepFunction(Function<StepContext, Integer> step) {
		this.step = step;
		return this;
	}

	public ScrollInput writingTo(Label label) {
		this.displayLabel = label;
		if (label != null)
			writeToLabel();
		return this;
	}
	
	@Override
	public void tick() {
		super.tick();
		soundPlayed = false;
	}

	public int getState() {
		return state;
	}

	public ScrollInput setState(int state) {
		this.state = state;
		clampState();
		applyTooltip();
		if (displayLabel != null)
			writeToLabel();
		return this;
	}

	public ScrollInput withShiftStep(int step) {
		shiftStep = step;
		return this;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (inverted)
			delta *= -1;

		StepContext context = new StepContext();
		context.control = AllKeys.ctrlDown();
		context.shift = AllKeys.shiftDown();
		context.currentValue = state;
		context.forward = delta > 0;

		int priorState = state;
		boolean shifted = AllKeys.shiftDown();
		int step = (int) Math.signum(delta) * this.step.apply(context);

		state += step;
		if (shifted)
			state -= state % shiftStep;

		clampState();

		if (priorState != state) {
			if (!soundPlayed)
				MinecraftClient.getInstance()
					.getSoundManager()
					.play(PositionedSoundInstance.master(AllSoundEvents.SCROLL_VALUE.getMainEvent(),
						1.5f + 0.1f * (state - min) / (max - min)));
			soundPlayed = true;
			onChanged();
		}

		return priorState != state;
	}

	protected void clampState() {
		if (state >= max)
			state = max - 1;
		if (state < min)
			state = min;
	}

	public void onChanged() {
		if (displayLabel != null)
			writeToLabel();
		if (onScroll != null)
			onScroll.accept(state);
		applyTooltip();
	}

	protected void writeToLabel() {
		displayLabel.text = formatter.apply(state);
	}

	protected void applyTooltip() {
		toolTip.clear();
		if (title == null)
			return;
		toolTip.add(title.copyContentOnly()
			.styled(s -> s.withColor(HEADER_RGB)));
		if (hint != null)
			toolTip.add(hint.copyContentOnly()
				.styled(s -> s.withColor(HINT_RGB)));
		toolTip.add(scrollToModify.copyContentOnly()
			.formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
		toolTip.add(shiftScrollsFaster.copyContentOnly()
			.formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
	}

}
