package com.simibubi.create.foundation.config.ui;

import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.utility.Components;

public class HintableTextFieldWidget extends TextFieldWidget {

	protected TextRenderer font;
	protected String hint;

	public HintableTextFieldWidget(TextRenderer font, int x, int y, int width, int height) {
		super(font, x, y, width, height, Components.immutableEmpty());
		this.font = font;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	@Override
	public void renderButton(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderButton(graphics, mouseX, mouseY, partialTicks);

		if (hint == null || hint.isEmpty())
			return;

		if (!getText().isEmpty())
			return;

		graphics.drawText(font, hint, getX() + 5, this.getY() + (this.height - 8) / 2, Theme.c(Theme.Key.TEXT).scaleAlpha(.75f).getRGB(), false);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (!isMouseOver(x, y))
			return false;

		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			setText("");
			return true;
		} else
			return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		InputUtil.Key mouseKey = InputUtil.fromKeyCode(code, p_keyPressed_2_);
		if (KeyBindingHelper.isActiveAndMatches(MinecraftClient.getInstance().options.inventoryKey, mouseKey)) {
			return true;
		}

		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}
}
