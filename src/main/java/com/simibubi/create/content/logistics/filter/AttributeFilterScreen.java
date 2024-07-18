package com.simibubi.create.content.logistics.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu.WhitelistMode;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;

public class AttributeFilterScreen extends AbstractFilterScreen<AttributeFilterMenu> {

	private static final String PREFIX = "gui.attribute_filter.";

	private Text addDESC = Lang.translateDirect(PREFIX + "add_attribute");
	private Text addInvertedDESC = Lang.translateDirect(PREFIX + "add_inverted_attribute");

	private Text allowDisN = Lang.translateDirect(PREFIX + "allow_list_disjunctive");
	private Text allowDisDESC = Lang.translateDirect(PREFIX + "allow_list_disjunctive.description");
	private Text allowConN = Lang.translateDirect(PREFIX + "allow_list_conjunctive");
	private Text allowConDESC = Lang.translateDirect(PREFIX + "allow_list_conjunctive.description");
	private Text denyN = Lang.translateDirect(PREFIX + "deny_list");
	private Text denyDESC = Lang.translateDirect(PREFIX + "deny_list.description");

	private Text referenceH = Lang.translateDirect(PREFIX + "add_reference_item");
	private Text noSelectedT = Lang.translateDirect(PREFIX + "no_selected_attributes");
	private Text selectedT = Lang.translateDirect(PREFIX + "selected_attributes");

	private IconButton whitelistDis, whitelistCon, blacklist;
	private Indicator whitelistDisIndicator, whitelistConIndicator, blacklistIndicator;
	private IconButton add;
	private IconButton addInverted;

	private ItemStack lastItemScanned = ItemStack.EMPTY;
	private List<ItemAttribute> attributesOfItem = new ArrayList<>();
	private List<Text> selectedAttributes = new ArrayList<>();
	private SelectionScrollInput attributeSelector;
	private Label attributeSelectorLabel;

	public AttributeFilterScreen(AttributeFilterMenu menu, PlayerInventory inv, Text title) {
		super(menu, inv, title, AllGuiTextures.ATTRIBUTE_FILTER);
	}

	@Override
	protected void init() {
		setWindowOffset(-11, 7);
		super.init();

		int screenX = x;
		int screenY = y;

		whitelistDis = new IconButton(screenX + 47, screenY + 61, AllIcons.I_WHITELIST_OR);
		whitelistDis.withCallback(() -> {
			handler.whitelistMode = WhitelistMode.WHITELIST_DISJ;
			sendOptionUpdate(Option.WHITELIST);
		});
		whitelistDis.setToolTip(allowDisN);
		whitelistCon = new IconButton(screenX + 65, screenY + 61, AllIcons.I_WHITELIST_AND);
		whitelistCon.withCallback(() -> {
			handler.whitelistMode = WhitelistMode.WHITELIST_CONJ;
			sendOptionUpdate(Option.WHITELIST2);
		});
		whitelistCon.setToolTip(allowConN);
		blacklist = new IconButton(screenX + 83, screenY + 61, AllIcons.I_WHITELIST_NOT);
		blacklist.withCallback(() -> {
			handler.whitelistMode = WhitelistMode.BLACKLIST;
			sendOptionUpdate(Option.BLACKLIST);
		});
		blacklist.setToolTip(denyN);

		whitelistDisIndicator = new Indicator(screenX + 47, screenY + 55, Components.immutableEmpty());
		whitelistConIndicator = new Indicator(screenX + 65, screenY + 55, Components.immutableEmpty());
		blacklistIndicator = new Indicator(screenX + 83, screenY + 55, Components.immutableEmpty());

		addRenderableWidgets(blacklist, whitelistCon, whitelistDis, blacklistIndicator, whitelistConIndicator,
			whitelistDisIndicator);

		addDrawableChild(add = new IconButton(screenX + 182, screenY + 23, AllIcons.I_ADD));
		addDrawableChild(addInverted = new IconButton(screenX + 200, screenY + 23, AllIcons.I_ADD_INVERTED_ATTRIBUTE));
		add.withCallback(() -> {
			handleAddedAttibute(false);
		});
		add.setToolTip(addDESC);
		addInverted.withCallback(() -> {
			handleAddedAttibute(true);
		});
		addInverted.setToolTip(addInvertedDESC);

		handleIndicators();

		attributeSelectorLabel = new Label(screenX + 43, screenY + 28, Components.immutableEmpty()).colored(0xF3EBDE)
			.withShadow();
		attributeSelector = new SelectionScrollInput(screenX + 39, screenY + 23, 137, 18);
		attributeSelector.forOptions(Arrays.asList(Components.immutableEmpty()));
		attributeSelector.removeCallback();
		referenceItemChanged(handler.ghostInventory.getStackInSlot(0));

		addDrawableChild(attributeSelector);
		addDrawableChild(attributeSelectorLabel);

		selectedAttributes.clear();
		selectedAttributes.add((handler.selectedAttributes.isEmpty() ? noSelectedT : selectedT).copyContentOnly()
			.formatted(Formatting.YELLOW));
		handler.selectedAttributes.forEach(at -> selectedAttributes.add(Components.literal("- ")
			.append(at.getFirst()
				.format(at.getSecond()))
			.formatted(Formatting.GRAY)));
	}

	private void referenceItemChanged(ItemStack stack) {
		lastItemScanned = stack;

		if (stack.isEmpty()) {
			attributeSelector.active = false;
			attributeSelector.visible = false;
			attributeSelectorLabel.text = referenceH.copyContentOnly()
				.formatted(Formatting.ITALIC);
			add.active = false;
			addInverted.active = false;
			attributeSelector.calling(s -> {
			});
			return;
		}

		add.active = true;

		addInverted.active = true;
		attributeSelector.titled(stack.getName()
			.copyContentOnly()
			.append("..."));
		attributesOfItem.clear();
		for (ItemAttribute itemAttribute : ItemAttribute.types)
			attributesOfItem.addAll(itemAttribute.listAttributesOf(stack, client.world));
		List<Text> options = attributesOfItem.stream()
			.map(a -> a.format(false))
			.collect(Collectors.toList());
		attributeSelector.forOptions(options);
		attributeSelector.active = true;
		attributeSelector.visible = true;
		attributeSelector.setState(0);
		attributeSelector.calling(i -> {
			attributeSelectorLabel.setTextAndTrim(options.get(i), true, 112);
			ItemAttribute selected = attributesOfItem.get(i);
			for (Pair<ItemAttribute, Boolean> existing : handler.selectedAttributes) {
				NbtCompound testTag = new NbtCompound();
				NbtCompound testTag2 = new NbtCompound();
				existing.getFirst()
					.serializeNBT(testTag);
				selected.serializeNBT(testTag2);
				if (testTag.equals(testTag2)) {
					add.active = false;
					addInverted.active = false;
					return;
				}
			}
			add.active = true;
			addInverted.active = true;
		});
		attributeSelector.onChanged();
	}

	@Override
	public void renderForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		ItemStack stack = handler.ghostInventory.getStackInSlot(1);
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(0, 0, 150);
		graphics.drawItemInSlot(textRenderer, stack, x + 22, y + 59,
			String.valueOf(selectedAttributes.size() - 1));
		matrixStack.pop();

		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		ItemStack stackInSlot = handler.ghostInventory.getStackInSlot(0);
		if (!ItemStack.areEqual(stackInSlot, lastItemScanned))
			referenceItemChanged(stackInSlot);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext graphics, int mouseX, int mouseY) {
		if (this.handler.getCursorStack().isEmpty() && this.focusedSlot != null && this.focusedSlot.hasStack()) {
			if (this.focusedSlot.id == 37) {
				graphics.drawTooltip(textRenderer, selectedAttributes, mouseX, mouseY);
				return;
			}
			graphics.drawItemTooltip(textRenderer, this.focusedSlot.getStack(), mouseX, mouseY);
		}
		super.drawMouseoverTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected List<IconButton> getTooltipButtons() {
		return Arrays.asList(blacklist, whitelistCon, whitelistDis);
	}

	@Override
	protected List<MutableText> getTooltipDescriptions() {
		return Arrays.asList(denyDESC.copyContentOnly(), allowConDESC.copyContentOnly(), allowDisDESC.copyContentOnly());
	}

	@Override
	protected List<Indicator> getIndicators() {
		return Arrays.asList(blacklistIndicator, whitelistConIndicator, whitelistDisIndicator);
	}

	protected boolean handleAddedAttibute(boolean inverted) {
		int index = attributeSelector.getState();
		if (index >= attributesOfItem.size())
			return false;
		add.active = false;
		addInverted.active = false;
		NbtCompound tag = new NbtCompound();
		ItemAttribute itemAttribute = attributesOfItem.get(index);
		itemAttribute.serializeNBT(tag);
		AllPackets.getChannel()
			.sendToServer(new FilterScreenPacket(inverted ? Option.ADD_INVERTED_TAG : Option.ADD_TAG, tag));
		handler.appendSelectedAttribute(itemAttribute, inverted);
		if (handler.selectedAttributes.size() == 1)
			selectedAttributes.set(0, selectedT.copyContentOnly()
				.formatted(Formatting.YELLOW));
		selectedAttributes.add(Components.literal("- ").append(itemAttribute.format(inverted))
			.formatted(Formatting.GRAY));
		return true;
	}

	@Override
	protected void contentsCleared() {
		selectedAttributes.clear();
		selectedAttributes.add(noSelectedT.copyContentOnly()
			.formatted(Formatting.YELLOW));
		if (!lastItemScanned.isEmpty()) {
			add.active = true;
			addInverted.active = true;
		}
	}

	@Override
	protected boolean isButtonEnabled(IconButton button) {
		if (button == blacklist)
			return handler.whitelistMode != WhitelistMode.BLACKLIST;
		if (button == whitelistCon)
			return handler.whitelistMode != WhitelistMode.WHITELIST_CONJ;
		if (button == whitelistDis)
			return handler.whitelistMode != WhitelistMode.WHITELIST_DISJ;
		return true;
	}

	@Override
	protected boolean isIndicatorOn(Indicator indicator) {
		if (indicator == blacklistIndicator)
			return handler.whitelistMode == WhitelistMode.BLACKLIST;
		if (indicator == whitelistConIndicator)
			return handler.whitelistMode == WhitelistMode.WHITELIST_CONJ;
		if (indicator == whitelistDisIndicator)
			return handler.whitelistMode == WhitelistMode.WHITELIST_DISJ;
		return false;
	}

}
