package com.simibubi.create.foundation.gui.menu;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.TickableGuiEventListener;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ScreenAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
public abstract class AbstractSimiContainerScreen<T extends ScreenHandler> extends HandledScreen<T> {

	protected int windowXOffset, windowYOffset;

	public AbstractSimiContainerScreen(T container, PlayerInventory inv, Text title) {
		super(container, inv, title);
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		backgroundWidth = width;
		backgroundHeight = height;
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
		super.init();
		x += windowXOffset;
		y += windowYOffset;
	}

	@Override
	protected void handledScreenTick() {
		for (Element listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
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

		renderBackground(graphics);

		super.render(graphics, mouseX, mouseY, partialTicks);

		renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void drawForeground(DrawContext graphics, int mouseX, int mouseY) {
		// no-op to prevent screen- and inventory-title from being rendered at incorrect
		// location
		// could also set this.titleX/Y and this.playerInventoryTitleX/Y to the proper
		// values instead
	}

	protected void renderForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		drawMouseoverTooltip(graphics, mouseX, mouseY);
		for (Drawable widget : ((ScreenAccessor) this).port_lib$getRenderables()) {
			if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isMouseOver(mouseX, mouseY)) {
				List<Text> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					continue;
				int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.drawTooltip(textRenderer, tooltip, ttx, tty);
			}
		}
	}

	public int getLeftOfCentered(int textureWidth) {
		return x - windowXOffset + (backgroundWidth - textureWidth) / 2;
	}

	public void renderPlayerInventory(DrawContext graphics, int x, int y) {
		AllGuiTextures.PLAYER_INVENTORY.render(graphics, x, y);
		graphics.drawText(textRenderer, playerInventoryTitle, x + 8, y + 6, 0x404040, false);
	}

	@Override
	public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
		InputUtil.Key mouseKey = InputUtil.fromKeyCode(pKeyCode, pScanCode);
		if (getFocused() instanceof TextFieldWidget && KeyBindingHelper.getBoundKeyOf(MinecraftClient.getInstance().options.inventoryKey) == mouseKey)
			return false;
		return super.keyPressed(pKeyCode, pScanCode, pModifiers);
	}
@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		if (getFocused() != null && !getFocused().isMouseOver(pMouseX, pMouseY))
			setFocused(null);
		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}

	@Override
	public Element getFocused() {
		Element focused = super.getFocused();
		if (focused instanceof ClickableWidget && !((ClickableWidget) focused).isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

	/**
	 * Used for moving JEI out of the way of extra things like block renders.
	 *
	 * @return the space that the GUI takes up outside the normal rectangle defined
	 *         by {@link GenericContainerScreen}.
	 */
	public List<Rect2i> getExtraAreas() {
		return Collections.emptyList();
	}

	@Deprecated
	protected void debugWindowArea(DrawContext graphics) {
		graphics.fill(x + backgroundWidth, y + backgroundHeight, x, y, 0xD3D3D3D3);
	}

	@Deprecated
	protected void debugExtraAreas(DrawContext graphics) {
		for (Rect2i area : getExtraAreas()) {
			graphics.fill(area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(),
				0xD3D3D3D3);
		}
	}

}
