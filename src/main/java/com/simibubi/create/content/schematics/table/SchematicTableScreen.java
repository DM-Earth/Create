package com.simibubi.create.content.schematics.table;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;
import static com.simibubi.create.foundation.gui.AllGuiTextures.SCHEMATIC_TABLE_PROGRESS;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.client.ClientSchematicLoader;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class SchematicTableScreen extends AbstractSimiContainerScreen<SchematicTableMenu> {

	private final Text uploading = Lang.translateDirect("gui.schematicTable.uploading");
	private final Text finished = Lang.translateDirect("gui.schematicTable.finished");
	private final Text refresh = Lang.translateDirect("gui.schematicTable.refresh");
	private final Text folder = Lang.translateDirect("gui.schematicTable.open_folder");
	private final Text noSchematics = Lang.translateDirect("gui.schematicTable.noSchematics");
	private final Text availableSchematicsTitle = Lang.translateDirect("gui.schematicTable.availableSchematics");

	protected AllGuiTextures background;

	private ScrollInput schematicsArea;
	private IconButton confirmButton;
	private IconButton folderButton;
	private IconButton refreshButton;
	private Label schematicsLabel;

	private float progress;
	private float chasingProgress;
	private float lastChasingProgress;

	private final ItemStack renderedItem = AllBlocks.SCHEMATIC_TABLE.asStack();

	private List<Rect2i> extraAreas = Collections.emptyList();

	public SchematicTableScreen(SchematicTableMenu menu, PlayerInventory playerInventory,
		Text title) {
		super(menu, playerInventory, title);
		background = AllGuiTextures.SCHEMATIC_TABLE;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height + 4 + AllGuiTextures.PLAYER_INVENTORY.height);
		setWindowOffset(-11, 8);
		super.init();

		CreateClient.SCHEMATIC_SENDER.refresh();
		List<Text> availableSchematics = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();

//		int x = x;
//		int y = y;

		schematicsLabel = new Label(x + 49, y + 26, Components.immutableEmpty()).withShadow();
		schematicsLabel.text = Components.immutableEmpty();
		if (!availableSchematics.isEmpty()) {
			schematicsArea =
				new SelectionScrollInput(x + 45, y + 21, 139, 18).forOptions(availableSchematics)
					.titled(availableSchematicsTitle.copyContentOnly())
					.writingTo(schematicsLabel);
			addDrawableChild(schematicsArea);
			addDrawableChild(schematicsLabel);
		}

		confirmButton = new IconButton(x + 44, y + 56, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			if (handler.canWrite() && schematicsArea != null) {
				ClientSchematicLoader schematicSender = CreateClient.SCHEMATIC_SENDER;
				lastChasingProgress = chasingProgress = progress = 0;
				List<Text> availableSchematics1 = schematicSender.getAvailableSchematics();
				Text schematic = availableSchematics1.get(schematicsArea.getState());
				schematicSender.startNewUpload(schematic.getString());
			}
		});

		folderButton = new IconButton(x + 21, y + 21, AllIcons.I_OPEN_FOLDER);
		folderButton.withCallback(() -> {
			Util.getOperatingSystem()
				.open(Paths.get("schematics/")
					.toFile());
		});
		folderButton.setToolTip(folder);
		refreshButton = new IconButton(x + 207, y + 21, AllIcons.I_REFRESH);
		refreshButton.withCallback(() -> {
			ClientSchematicLoader schematicSender = CreateClient.SCHEMATIC_SENDER;
			schematicSender.refresh();
			List<Text> availableSchematics1 = schematicSender.getAvailableSchematics();
			remove(schematicsArea);

			if (!availableSchematics1.isEmpty()) {
				schematicsArea = new SelectionScrollInput(x + 45, y + 21, 139, 18)
					.forOptions(availableSchematics1)
					.titled(availableSchematicsTitle.copyContentOnly())
					.writingTo(schematicsLabel);
				schematicsArea.onChanged();
				addDrawableChild(schematicsArea);
			} else {
				schematicsArea = null;
				schematicsLabel.text = Components.immutableEmpty();
			}
		});
		refreshButton.setToolTip(refresh);

		addDrawableChild(confirmButton);
		addDrawableChild(folderButton);
		addDrawableChild(refreshButton);

		extraAreas = ImmutableList.of(
			new Rect2i(x + background.width, y + background.height - 40, 48, 48),
			new Rect2i(refreshButton.getX(), refreshButton.getY(), refreshButton.getWidth(), refreshButton.getHeight())
		);
	}

	@Override
	protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.width);
		int invY = y + background.height + 4;
		renderPlayerInventory(graphics, invX, invY);

		int sx = x;
		int sy = y;

		background.render(graphics, sx, sy);

		Text titleText;
		if (handler.contentHolder.isUploading)
			titleText = uploading;
		else if (handler.getSlot(1)
			.hasStack())
			titleText = finished;
		else
			titleText = title;
		graphics.drawCenteredTextWithShadow(textRenderer, titleText, sx + (background.width - 8) / 2, sy + 3, 0xFFFFFF);

		if (schematicsArea == null)
			graphics.drawTextWithShadow(textRenderer, noSchematics, sx + 54, sy + 26, 0xD3D3D3);

		GuiGameElement.of(renderedItem)
			.<GuiGameElement.GuiRenderBuilder>at(sx + background.width, sy + background.height - 40, -200)
			.scale(3)
			.render(graphics);

		int width = (int) (SCHEMATIC_TABLE_PROGRESS.width
			* MathHelper.lerp(partialTicks, lastChasingProgress, chasingProgress));
		int height = SCHEMATIC_TABLE_PROGRESS.height;
		graphics.drawTexture(SCHEMATIC_TABLE_PROGRESS.location, sx + 70, sy + 57, SCHEMATIC_TABLE_PROGRESS.startX,
			SCHEMATIC_TABLE_PROGRESS.startY, width, height);
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();

		boolean finished = handler.getSlot(1)
			.hasStack();

		if (handler.contentHolder.isUploading || finished) {
			if (finished) {
				chasingProgress = lastChasingProgress = progress = 1;
			} else {
				lastChasingProgress = chasingProgress;
				progress = handler.contentHolder.uploadingProgress;
				chasingProgress += (progress - chasingProgress) * .5f;
			}
			confirmButton.active = false;

			if (schematicsLabel != null) {
				schematicsLabel.colored(0xCCDDFF);
				String uploadingSchematic = handler.contentHolder.uploadingSchematic;
				schematicsLabel.text = uploadingSchematic == null ? null : Components.literal(uploadingSchematic);
			}
			if (schematicsArea != null)
				schematicsArea.visible = false;

		} else {
			progress = 0;
			chasingProgress = lastChasingProgress = 0;
			confirmButton.active = true;

			if (schematicsLabel != null)
				schematicsLabel.colored(0xFFFFFF);
			if (schematicsArea != null) {
				schematicsArea.writingTo(schematicsLabel);
				schematicsArea.visible = true;
			}
		}
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}
