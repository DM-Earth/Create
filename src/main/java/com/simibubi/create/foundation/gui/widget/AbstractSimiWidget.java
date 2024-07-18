package com.simibubi.create.foundation.gui.widget;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import com.simibubi.create.foundation.gui.TickableGuiEventListener;
import com.simibubi.create.foundation.utility.Components;

public abstract class AbstractSimiWidget extends ClickableWidget implements TickableGuiEventListener {

	public static final int HEADER_RGB = 0x5391E1;
	public static final int HINT_RGB = 0x96B7E0;

	protected float z;
	protected boolean wasHovered = false;
	protected List<Text> toolTip = new LinkedList<>();
	protected BiConsumer<Integer, Integer> onClick = (_$, _$$) -> {};

	public int lockedTooltipX = -1;
	public int lockedTooltipY = -1;

	protected AbstractSimiWidget(int x, int y) {
		this(x, y, 16, 16);
	}

	protected AbstractSimiWidget(int x, int y, int width, int height) {
		this(x, y, width, height, Components.immutableEmpty());
	}

	protected AbstractSimiWidget(int x, int y, int width, int height, Text message) {
		super(x, y, width, height, message);
	}

	@Override
	protected TooltipPositioner getTooltipPositioner() {
		return HoveredTooltipPositioner.INSTANCE;
	}

	public <T extends AbstractSimiWidget> T withCallback(BiConsumer<Integer, Integer> cb) {
		this.onClick = cb;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends AbstractSimiWidget> T withCallback(Runnable cb) {
		return withCallback((_$, _$$) -> cb.run());
	}

	public <T extends AbstractSimiWidget> T atZLevel(float z) {
		this.z = z;
		//noinspection unchecked
		return (T) this;
	}

	public List<Text> getToolTip() {
		return toolTip;
	}

	@Override
	public void tick() {}

	@Override
	public void renderButton(@Nonnull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		beforeRender(graphics, mouseX, mouseY, partialTicks);
		doRender(graphics, mouseX, mouseY, partialTicks);
		afterRender(graphics, mouseX, mouseY, partialTicks);
		wasHovered = isSelected();
	}

	protected void beforeRender(@Nonnull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.getMatrices().push();
	}

	protected void doRender(@Nonnull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
	}

	protected void afterRender(@Nonnull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.getMatrices().pop();
	}

	public void runCallback(double mouseX, double mouseY) {
		onClick.accept((int) mouseX, (int) mouseY);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		runCallback(mouseX, mouseY);
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder pNarrationElementOutput) {
		appendDefaultNarrations(pNarrationElementOutput);
	}
}
