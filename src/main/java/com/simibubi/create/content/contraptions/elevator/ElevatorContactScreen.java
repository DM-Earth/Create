package com.simibubi.create.content.contraptions.elevator;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class ElevatorContactScreen extends AbstractSimiScreen {

	private AllGuiTextures background;

	private TextFieldWidget shortNameInput;
	private TextFieldWidget longNameInput;
	private IconButton confirm;

	private String shortName;
	private String longName;
	private DoorControl doorControl;

	private BlockPos pos;

	public ElevatorContactScreen(BlockPos pos, String prevShortName, String prevLongName, DoorControl prevDoorControl) {
		super(Lang.translateDirect("elevator_contact.title"));
		this.pos = pos;
		this.doorControl = prevDoorControl;
		background = AllGuiTextures.ELEVATOR_CONTACT;
		this.shortName = prevShortName;
		this.longName = prevLongName;
	}

	@Override
	public void init() {
		setWindowSize(background.width + 30, background.height);
		super.init();

		int x = guiLeft;
		int y = guiTop;

		confirm = new IconButton(x + 200, y + 58, AllIcons.I_CONFIRM);
		confirm.withCallback(this::confirm);
		addDrawableChild(confirm);

		shortNameInput = editBox(33, 30, 4);
		shortNameInput.setText(shortName);
		centerInput(x);
		shortNameInput.setChangedListener(s -> {
			shortName = s;
			centerInput(x);
		});
		shortNameInput.setFocused(true);
		setFocused(shortNameInput);
		shortNameInput.setSelectionEnd(0);

		longNameInput = editBox(63, 140, 30);
		longNameInput.setText(longName);
		longNameInput.setChangedListener(s -> longName = s);

		MutableText rmbToEdit = Lang.translate("gui.schedule.lmb_edit")
			.style(Formatting.DARK_GRAY)
			.style(Formatting.ITALIC)
			.component();

		addDrawable(new TooltipArea(x + 21, y + 23, 30, 18)
			.withTooltip(ImmutableList.of(Lang.translate("elevator_contact.floor_identifier")
				.color(0x5391E1)
				.component(), rmbToEdit)));

		addDrawable(new TooltipArea(x + 57, y + 23, 147, 18).withTooltip(ImmutableList.of(
			Lang.translate("elevator_contact.floor_description")
				.color(0x5391E1)
				.component(),
			Lang.translate("crafting_blueprint.optional")
				.style(Formatting.GRAY)
				.component(),
			rmbToEdit)));

		Pair<ScrollInput, Label> doorControlWidgets =
			DoorControl.createWidget(x + 58, y + 57, mode -> doorControl = mode, doorControl);
		addDrawableChild(doorControlWidgets.getFirst());
		addDrawableChild(doorControlWidgets.getSecond());
	}

	private int centerInput(int x) {
		int centeredX = x + (shortName.isEmpty() ? 34 : 36 - textRenderer.getWidth(shortName) / 2);
		shortNameInput.setX(centeredX);
		return centeredX;
	}

	private TextFieldWidget editBox(int x, int width, int chars) {
		TextFieldWidget editBox = new TextFieldWidget(textRenderer, guiLeft + x, guiTop + 30, width, 10, Components.immutableEmpty());
		editBox.setEditableColor(-1);
		editBox.setUneditableColor(-1);
		editBox.setDrawsBackground(false);
		editBox.setMaxLength(chars);
		editBox.setFocused(false);
		editBox.mouseClicked(0, 0, 0);
		addDrawableChild(editBox);
		return editBox;
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);

		OrderedText formattedcharsequence = title.asOrderedText();
		graphics.drawText(textRenderer, formattedcharsequence,
			x + (background.width - 8) / 2 - textRenderer.getWidth(formattedcharsequence) / 2, y + 6, 0x2F3738, false);

		GuiGameElement.of(AllBlocks.ELEVATOR_CONTACT.asStack()).<GuiGameElement
			.GuiRenderBuilder>at(x + background.width + 6, y + background.height - 56, -200)
			.scale(5)
			.render(graphics);

		graphics.drawItem(AllBlocks.TRAIN_DOOR.asStack(), x + 37, y + 58);
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		boolean consumed = super.mouseClicked(pMouseX, pMouseY, pButton);

		if (!shortNameInput.isFocused()) {
			int length = shortNameInput.getText()
				.length();
			shortNameInput.setSelectionEnd(length);
			shortNameInput.setSelectionStart(length);
		}

		if (shortNameInput.isSelected())
			longNameInput.mouseClicked(0, 0, 0);

		if (!consumed && pMouseX > guiLeft + 22 && pMouseY > guiTop + 24 && pMouseX < guiLeft + 50
			&& pMouseY < guiTop + 40) {
			setFocused(shortNameInput);
			shortNameInput.setFocused(true);
			return true;
		}

		return consumed;
	}

	@Override
	public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (super.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_))
			return true;
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			confirm();
			return true;
		}
		if (keyCode == 256 && this.shouldCloseOnEsc()) {
			this.close();
			return true;
		}
		return false;
	}

	private void confirm() {
		AllPackets.getChannel()
			.sendToServer(new ElevatorContactEditPacket(pos, shortName, longName, doorControl));
		close();
	}

}
