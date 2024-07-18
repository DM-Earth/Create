package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.List;
import java.util.Vector;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.content.equipment.zapper.ConfigureZapperPacket;
import com.simibubi.create.content.equipment.zapper.ZapperScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;

public class WorldshaperScreen extends ZapperScreen {

	protected final Text placementSection = Lang.translateDirect("gui.terrainzapper.placement");
	protected final Text toolSection = Lang.translateDirect("gui.terrainzapper.tool");
	protected final List<Text> brushOptions =
		Lang.translatedOptions("gui.terrainzapper.brush", "cuboid", "sphere", "cylinder", "surface", "cluster");

	protected Vector<IconButton> toolButtons;
	protected Vector<IconButton> placementButtons;

	protected ScrollInput brushInput;
	protected Label brushLabel;
	protected Vector<ScrollInput> brushParams = new Vector<>(3);
	protected Vector<Label> brushParamLabels = new Vector<>(3);
	protected IconButton followDiagonals;
	protected IconButton acrossMaterials;
	protected Indicator followDiagonalsIndicator;
	protected Indicator acrossMaterialsIndicator;

	protected TerrainBrushes currentBrush;
	protected int[] currentBrushParams = new int[] { 1, 1, 1 };
	protected boolean currentFollowDiagonals;
	protected boolean currentAcrossMaterials;
	protected TerrainTools currentTool;
	protected PlacementOptions currentPlacement;

	public WorldshaperScreen(ItemStack zapper, Hand hand) {
		super(AllGuiTextures.TERRAINZAPPER, zapper, hand);
		fontColor = 0x767676;
		title = zapper.getName();

		NbtCompound nbt = zapper.getOrCreateNbt();
		currentBrush = NBTHelper.readEnum(nbt, "Brush", TerrainBrushes.class);
		if (nbt.contains("BrushParams", NbtElement.COMPOUND_TYPE)) {
			BlockPos paramsData = NbtHelper.toBlockPos(nbt.getCompound("BrushParams"));
			currentBrushParams[0] = paramsData.getX();
			currentBrushParams[1] = paramsData.getY();
			currentBrushParams[2] = paramsData.getZ();
			if (currentBrushParams[1] == 0) {
				currentFollowDiagonals = true;
			}
			if (currentBrushParams[2] == 0) {
				currentAcrossMaterials = true;
			}
		}
		currentTool = NBTHelper.readEnum(nbt, "Tool", TerrainTools.class);
		currentPlacement = NBTHelper.readEnum(nbt, "Placement", PlacementOptions.class);
	}

	@Override
	protected void init() {
		super.init();

		int x = guiLeft;
		int y = guiTop;

		brushLabel = new Label(x + 61, y + 25, Components.immutableEmpty()).withShadow();
		brushInput = new SelectionScrollInput(x + 56, y + 20, 77, 18).forOptions(brushOptions)
			.titled(Lang.translateDirect("gui.terrainzapper.brush"))
			.writingTo(brushLabel)
			.calling(brushIndex -> {
				currentBrush = TerrainBrushes.values()[brushIndex];
				initBrushParams(x, y);
			});

		brushInput.setState(currentBrush.ordinal());

		addDrawableChild(brushLabel);
		addDrawableChild(brushInput);

		initBrushParams(x, y);
	}

	protected void initBrushParams(int x, int y) {
		Brush currentBrush = this.currentBrush.get();

		// Brush Params

		removeWidgets(brushParamLabels);
		removeWidgets(brushParams);

		brushParamLabels.clear();
		brushParams.clear();

		for (int index = 0; index < 3; index++) {
			Label label = new Label(x + 65 + 20 * index, y + 45, Components.immutableEmpty()).withShadow();

			final int finalIndex = index;
			ScrollInput input = new ScrollInput(x + 56 + 20 * index, y + 40, 18, 18)
				.withRange(currentBrush.getMin(index), currentBrush.getMax(index) + 1)
				.writingTo(label)
				.titled(currentBrush.getParamLabel(index)
					.copyContentOnly())
				.calling(state -> {
					currentBrushParams[finalIndex] = state;
					label.setX(x + 65 + 20 * finalIndex - textRenderer.getWidth(label.text) / 2);
				});
			input.setState(currentBrushParams[index]);
			input.onChanged();

			if (index >= currentBrush.amtParams) {
				input.visible = false;
				label.visible = false;
				input.active = false;
			}

			brushParamLabels.add(label);
			brushParams.add(input);
		}

		addRenderableWidgets(brushParamLabels);
		addRenderableWidgets(brushParams);

		// Connectivity Options

		if (followDiagonals != null) {
			remove(followDiagonals);
			remove(followDiagonalsIndicator);
			remove(acrossMaterials);
			remove(acrossMaterialsIndicator);
			followDiagonals = null;
			followDiagonalsIndicator = null;
			acrossMaterials = null;
			acrossMaterialsIndicator = null;
		}

		if (currentBrush.hasConnectivityOptions()) {
			int x1 = x + 7 + 4 * 18;
			int y1 = y + 79;
			followDiagonalsIndicator = new Indicator(x1, y1 - 6, Components.immutableEmpty());
			followDiagonals = new IconButton(x1, y1, AllIcons.I_FOLLOW_DIAGONAL);
			x1 += 18;
			acrossMaterialsIndicator = new Indicator(x1, y1 - 6, Components.immutableEmpty());
			acrossMaterials = new IconButton(x1, y1, AllIcons.I_FOLLOW_MATERIAL);

			followDiagonals.withCallback(() -> {
				followDiagonalsIndicator.state = followDiagonalsIndicator.state == State.OFF ? State.ON : State.OFF;
				currentFollowDiagonals = !currentFollowDiagonals;
			});
			followDiagonals.setToolTip(Lang.translateDirect("gui.terrainzapper.searchDiagonal"));
			acrossMaterials.withCallback(() -> {
				acrossMaterialsIndicator.state = acrossMaterialsIndicator.state == State.OFF ? State.ON : State.OFF;
				currentAcrossMaterials = !currentAcrossMaterials;
			});
			acrossMaterials.setToolTip(Lang.translateDirect("gui.terrainzapper.searchFuzzy"));
			addDrawableChild(followDiagonals);
			addDrawableChild(followDiagonalsIndicator);
			addDrawableChild(acrossMaterials);
			addDrawableChild(acrossMaterialsIndicator);
			if (currentFollowDiagonals)
				followDiagonalsIndicator.state = State.ON;
			if (currentAcrossMaterials)
				acrossMaterialsIndicator.state = State.ON;
		}

		// Tools

		if (toolButtons != null)
			removeWidgets(toolButtons);

		TerrainTools[] toolValues = currentBrush.getSupportedTools();
		toolButtons = new Vector<>(toolValues.length);
		for (int id = 0; id < toolValues.length; id++) {
			TerrainTools tool = toolValues[id];
			IconButton toolButton = new IconButton(x + 7 + id * 18, y + 79, tool.icon);
			toolButton.withCallback(() -> {
				toolButtons.forEach(b -> b.active = true);
				toolButton.active = false;
				currentTool = tool;
			});
			toolButton.setToolTip(Lang.translateDirect("gui.terrainzapper.tool." + tool.translationKey));
			toolButtons.add(toolButton);
		}

		int toolIndex = -1;
		for (int i = 0; i < toolValues.length; i++)
			if (currentTool == toolValues[i])
				toolIndex = i;
		if (toolIndex == -1) {
			currentTool = toolValues[0];
			toolIndex = 0;
		}
		toolButtons.get(toolIndex).active = false;

		addRenderableWidgets(toolButtons);

		// Placement Options

		if (placementButtons != null)
			removeWidgets(placementButtons);

		if (currentBrush.hasPlacementOptions()) {
			PlacementOptions[] placementValues = PlacementOptions.values();
			placementButtons = new Vector<>(placementValues.length);
			for (int id = 0; id < placementValues.length; id++) {
				PlacementOptions option = placementValues[id];
				IconButton placementButton = new IconButton(x + 136 + id * 18, y + 79, option.icon);
				placementButton.withCallback(() -> {
					placementButtons.forEach(b -> b.active = true);
					placementButton.active = false;
					currentPlacement = option;
				});
				placementButton.setToolTip(Lang.translateDirect("gui.terrainzapper.placement." + option.translationKey));
				placementButtons.add(placementButton);
			}

			placementButtons.get(currentPlacement.ordinal()).active = false;

			addRenderableWidgets(placementButtons);
		}
	}

	@Override
	protected void drawOnBackground(DrawContext graphics, int x, int y) {
		super.drawOnBackground(graphics, x, y);

		Brush currentBrush = this.currentBrush.get();
		for (int index = 2; index >= currentBrush.amtParams; index--)
			AllGuiTextures.TERRAINZAPPER_INACTIVE_PARAM.render(graphics, x + 56 + 20 * index, y + 40);

		graphics.drawText(textRenderer, toolSection, x + 7, y + 69, fontColor, false);
		if (currentBrush.hasPlacementOptions())
			graphics.drawText(textRenderer, placementSection, x + 136, y + 69, fontColor, false);
	}

	@Override
	protected ConfigureZapperPacket getConfigurationPacket() {
		int brushParamX = currentBrushParams[0];
		int brushParamY = followDiagonalsIndicator != null ? followDiagonalsIndicator.state == State.ON ? 0 : 1
			: currentBrushParams[1];
		int brushParamZ = acrossMaterialsIndicator != null ? acrossMaterialsIndicator.state == State.ON ? 0 : 1
			: currentBrushParams[2];
		return new ConfigureWorldshaperPacket(hand, currentPattern, currentBrush, brushParamX, brushParamY, brushParamZ, currentTool, currentPlacement);
	}

}
