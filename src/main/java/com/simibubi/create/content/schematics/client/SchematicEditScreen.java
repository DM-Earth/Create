package com.simibubi.create.content.schematics.client;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class SchematicEditScreen extends AbstractSimiScreen {

	private final List<Text> rotationOptions =
		Lang.translatedOptions("schematic.rotation", "none", "cw90", "cw180", "cw270");
	private final List<Text> mirrorOptions =
		Lang.translatedOptions("schematic.mirror", "none", "leftRight", "frontBack");
	private final Text rotationLabel = Lang.translateDirect("schematic.rotation");
	private final Text mirrorLabel = Lang.translateDirect("schematic.mirror");

	private AllGuiTextures background;

	private TextFieldWidget xInput;
	private TextFieldWidget yInput;
	private TextFieldWidget zInput;
	private IconButton confirmButton;

	private ScrollInput rotationArea;
	private ScrollInput mirrorArea;
	private SchematicHandler handler;

	public SchematicEditScreen() {
		background = AllGuiTextures.SCHEMATIC;
		handler = CreateClient.SCHEMATIC_HANDLER;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height);
		setWindowOffset(-6, 0);
		super.init();

		int x = guiLeft;
		int y = guiTop;

		xInput = new TextFieldWidget(textRenderer, x + 50, y + 26, 34, 10, Components.immutableEmpty());
		yInput = new TextFieldWidget(textRenderer, x + 90, y + 26, 34, 10, Components.immutableEmpty());
		zInput = new TextFieldWidget(textRenderer, x + 130, y + 26, 34, 10, Components.immutableEmpty());

		BlockPos anchor = handler.getTransformation()
				.getAnchor();
		if (handler.isDeployed()) {
			xInput.setText("" + anchor.getX());
			yInput.setText("" + anchor.getY());
			zInput.setText("" + anchor.getZ());
		} else {
			BlockPos alt = client.player.getBlockPos();
			xInput.setText("" + alt.getX());
			yInput.setText("" + alt.getY());
			zInput.setText("" + alt.getZ());
		}

		for (TextFieldWidget widget : new TextFieldWidget[] { xInput, yInput, zInput }) {
			widget.setMaxLength(6);
			widget.setDrawsBackground(false);
			widget.setEditableColor(0xFFFFFF);
			widget.setFocused(false);
			widget.mouseClicked(0, 0, 0);
			widget.setTextPredicate(s -> {
				if (s.isEmpty() || s.equals("-"))
					return true;
				try {
					Integer.parseInt(s);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			});
		}

		StructurePlacementData settings = handler.getTransformation()
			.toSettings();
		Label labelR = new Label(x + 50, y + 48, Components.immutableEmpty()).withShadow();
		rotationArea = new SelectionScrollInput(x + 45, y + 43, 118, 18).forOptions(rotationOptions)
			.titled(rotationLabel.copyContentOnly())
			.setState(settings.getRotation()
				.ordinal())
			.writingTo(labelR);

		Label labelM = new Label(x + 50, y + 70, Components.immutableEmpty()).withShadow();
		mirrorArea = new SelectionScrollInput(x + 45, y + 65, 118, 18).forOptions(mirrorOptions)
			.titled(mirrorLabel.copyContentOnly())
			.setState(settings.getMirror()
				.ordinal())
			.writingTo(labelM);

		addRenderableWidgets(xInput, yInput, zInput);
		addRenderableWidgets(labelR, labelM, rotationArea, mirrorArea);

		confirmButton =
			new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			close();
		});
		addDrawableChild(confirmButton);
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (isPaste(code)) {
			String coords = client.keyboard.getClipboard();
			if (coords != null && !coords.isEmpty()) {
				coords.replaceAll(" ", "");
				String[] split = coords.split(",");
				if (split.length == 3) {
					boolean valid = true;
					for (String s : split) {
						try {
							Integer.parseInt(s);
						} catch (NumberFormatException e) {
							valid = false;
						}
					}
					if (valid) {
						xInput.setText(split[0]);
						yInput.setText(split[1]);
						zInput.setText(split[2]);
						return true;
					}
				}
			}
		}

		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		String title = handler.getCurrentSchematicName();
		graphics.drawCenteredTextWithShadow(textRenderer, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF);

		GuiGameElement.of(AllItems.SCHEMATIC.asStack())
				.<GuiGameElement.GuiRenderBuilder>at(x + background.width + 6, y + background.height - 40, -200)
				.scale(3)
				.render(graphics);
	}

	@Override
	public void removed() {
		boolean validCoords = true;
		BlockPos newLocation = null;
		try {
			newLocation = new BlockPos(Integer.parseInt(xInput.getText()), Integer.parseInt(yInput.getText()),
				Integer.parseInt(zInput.getText()));
		} catch (NumberFormatException e) {
			validCoords = false;
		}

		StructurePlacementData settings = new StructurePlacementData();
		settings.setRotation(BlockRotation.values()[rotationArea.getState()]);
		settings.setMirror(BlockMirror.values()[mirrorArea.getState()]);

		if (validCoords && newLocation != null) {
			ItemStack item = handler.getActiveSchematicItem();
			if (item != null) {
				item.getNbt()
					.putBoolean("Deployed", true);
				item.getNbt()
					.put("Anchor", NbtHelper.fromBlockPos(newLocation));
			}

			handler.getTransformation()
				.init(newLocation, settings, handler.getBounds());
			handler.markDirty();
			handler.deploy();
		}
	}

}
