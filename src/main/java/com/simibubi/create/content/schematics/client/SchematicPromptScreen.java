package com.simibubi.create.content.schematics.client;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SchematicPromptScreen extends AbstractSimiScreen {

	private AllGuiTextures background;

	private final Text convertLabel = Lang.translateDirect("schematicAndQuill.convert");
	private final Text abortLabel = Lang.translateDirect("action.discard");
	private final Text confirmLabel = Lang.translateDirect("action.saveToFile");

	private TextFieldWidget nameField;
	private IconButton confirm;
	private IconButton abort;
	private IconButton convert;

	public SchematicPromptScreen() {
		super(Lang.translateDirect("schematicAndQuill.title"));
		background = AllGuiTextures.SCHEMATIC_PROMPT;
	}

	@Override
	public void init() {
		setWindowSize(background.width, background.height);
		super.init();

		int x = guiLeft;
		int y = guiTop;

		nameField = new TextFieldWidget(textRenderer, x + 49, y + 26, 131, 10, Components.immutableEmpty());
		nameField.setEditableColor(-1);
		nameField.setUneditableColor(-1);
		nameField.setDrawsBackground(false);
		nameField.setMaxLength(35);
		nameField.setFocused(true);
		setFocused(nameField);
		addDrawableChild(nameField);

		abort = new IconButton(x + 7, y + 53, AllIcons.I_TRASH);
		abort.withCallback(() -> {
			CreateClient.SCHEMATIC_AND_QUILL_HANDLER.discard();
			close();
		});
		abort.setToolTip(abortLabel);
		addDrawableChild(abort);

		confirm = new IconButton(x + 158, y + 53, AllIcons.I_CONFIRM);
		confirm.withCallback(() -> {
			confirm(false);
		});
		confirm.setToolTip(confirmLabel);
		addDrawableChild(confirm);

		convert = new IconButton(x + 180, y + 53, AllIcons.I_SCHEMATIC);
		convert.withCallback(() -> {
			confirm(true);
		});
		convert.setToolTip(convertLabel);
		addDrawableChild(convert);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		graphics.drawCenteredTextWithShadow(textRenderer, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF);

		GuiGameElement.of(AllItems.SCHEMATIC.asStack())
				.at(x + 22, y + 23, 0)
				.render(graphics);

		GuiGameElement.of(AllItems.SCHEMATIC_AND_QUILL.asStack())
				.scale(3)
				.at(x + background.width + 6, y + background.height - 40, -200)
				.render(graphics);
	}

	@Override
	public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			confirm(false);
			return true;
		}
		if (keyCode == 256 && this.shouldCloseOnEsc()) {
			this.close();
			return true;
		}
		return nameField.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
	}

	private void confirm(boolean convertImmediately) {
		CreateClient.SCHEMATIC_AND_QUILL_HANDLER.saveSchematic(nameField.getText(), convertImmediately);
		close();
	}
}
