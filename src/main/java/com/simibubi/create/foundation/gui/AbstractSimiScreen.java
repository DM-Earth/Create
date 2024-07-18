package com.simibubi.create.foundation.gui;

import java.util.Collection;
import java.util.List;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.utility.Components;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ScreenAccessor;
import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public abstract class AbstractSimiScreen extends Screen {

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	protected AbstractSimiScreen(Text title) {
		super(title);
	}

	protected AbstractSimiScreen() {
		this(Components.immutableEmpty());
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	protected void init() {
		guiLeft = (width - windowWidth) / 2;
		guiTop = (height - windowHeight) / 2;
		guiLeft += windowXOffset;
		guiTop += windowYOffset;
	}

	@Override
	public void tick() {
		for (Element listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		if (getFocused() != null && !getFocused().isMouseOver(pMouseX, pMouseY))
			setFocused(null);
		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@SuppressWarnings("unchecked")
	protected <W extends Element & Drawable & Selectable> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addDrawableChild(widget);
		}
	}

	protected <W extends Element & Drawable & Selectable> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addDrawableChild(widget);
		}
	}

	protected void removeWidgets(Element... widgets) {
		for (Element widget : widgets) {
			remove(widget);
		}
	}

	protected void removeWidgets(Collection<? extends Element> widgets) {
		for (Element widget : widgets) {
			remove(widget);
		}
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		partialTicks = client.getTickDelta();
		MatrixStack ms = graphics.getMatrices();

		ms.push();

		prepareFrame();

		renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
		renderWindow(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderWindowForeground(graphics, mouseX, mouseY, partialTicks);

		endFrame();

		ms.pop();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
		if (keyPressed || getFocused() instanceof TextFieldWidget)
			return keyPressed;

		InputUtil.Key mouseKey = InputUtil.fromKeyCode(keyCode, scanCode);
		if (KeyBindingHelper.isActiveAndMatches(this.client.options.inventoryKey, mouseKey)) {
			this.close();
			return true;
		}

		return false;
	}

	protected void prepareFrame() {
	}

	protected void renderWindowBackground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
	}

	protected abstract void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		for (Drawable widget : ((ScreenAccessor) this).port_lib$getRenderables()) {
			if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isMouseOver(mouseX, mouseY)
					&& simiWidget.visible) {
				List<Text> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					continue;
				int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.drawTooltip(textRenderer, tooltip, ttx, tty);
			}
		}
	}

	protected void endFrame() {
	}

	@Deprecated
	protected void debugWindowArea(DrawContext graphics) {
		graphics.fill(guiLeft + windowWidth, guiTop + windowHeight, guiLeft, guiTop, 0xD3D3D3D3);
	}

	@Override
	public Element getFocused() {
		Element focused = super.getFocused();
		if (focused instanceof ClickableWidget && !((ClickableWidget) focused).isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

}
