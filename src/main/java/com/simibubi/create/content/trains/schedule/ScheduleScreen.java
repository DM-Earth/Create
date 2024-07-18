package com.simibubi.create.content.trains.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ScreenAccessor;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.ImmutableList;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.IntAttached;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;

public class ScheduleScreen extends AbstractSimiContainerScreen<ScheduleMenu> {

	private static final int CARD_HEADER = 22;
	private static final int CARD_WIDTH = 195;

	private List<Rect2i> extraAreas = Collections.emptyList();

	private List<LerpedFloat> horizontalScrolls = new ArrayList<>();
	private LerpedFloat scroll = LerpedFloat.linear()
		.startWithValue(0);

	private Schedule schedule;

	private IconButton confirmButton;
	private IconButton cyclicButton;
	private Indicator cyclicIndicator;

	private IconButton resetProgress;
	private IconButton skipProgress;

	private ScheduleInstruction editingDestination;
	private ScheduleWaitCondition editingCondition;
	private SelectionScrollInput scrollInput;
	private Label scrollInputLabel;
	private IconButton editorConfirm, editorDelete;
	private ModularGuiLine editorSubWidgets;
	private Consumer<Boolean> onEditorClose;

	private DestinationSuggestions destinationSuggestions;

	public ScheduleScreen(ScheduleMenu menu, PlayerInventory inv, Text title) {
		super(menu, inv, title);
		schedule = new Schedule();
		NbtCompound tag = menu.contentHolder.getOrCreateNbt()
			.getCompound("Schedule");
		if (!tag.isEmpty())
			schedule = Schedule.fromTag(tag);
		menu.slotsActive = false;
		editorSubWidgets = new ModularGuiLine();
	}

	@Override
	protected void init() {
		AllGuiTextures bg = AllGuiTextures.SCHEDULE;
		setWindowSize(bg.width, bg.height);
		super.init();
		clearChildren();

		confirmButton = new IconButton(x + bg.width - 42, y + bg.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> client.player.closeHandledScreen());
		addDrawableChild(confirmButton);

		cyclicIndicator = new Indicator(x + 21, y + 196, Components.immutableEmpty());
		cyclicIndicator.state = schedule.cyclic ? State.ON : State.OFF;
		addDrawableChild(cyclicIndicator);

		cyclicButton = new IconButton(x + 21, y + 202, AllIcons.I_REFRESH);
		cyclicButton.withCallback(() -> {
			schedule.cyclic = !schedule.cyclic;
			cyclicIndicator.state = schedule.cyclic ? State.ON : State.OFF;
		});

		List<Text> tip = cyclicButton.getToolTip();
		tip.add(Lang.translateDirect("schedule.loop"));
		tip.add(Lang.translateDirect("schedule.loop1")
			.formatted(Formatting.GRAY));
		tip.add(Lang.translateDirect("schedule.loop2")
			.formatted(Formatting.GRAY));

		addDrawableChild(cyclicButton);

		resetProgress = new IconButton(x + 45, y + 202, AllIcons.I_PRIORITY_VERY_HIGH);
		resetProgress.withCallback(() -> {
			schedule.savedProgress = 0;
			resetProgress.active = false;
		});
		resetProgress.active = schedule.savedProgress > 0 && !schedule.entries.isEmpty();
		resetProgress.setToolTip(Lang.translateDirect("schedule.reset"));
		addDrawableChild(resetProgress);

		skipProgress = new IconButton(x + 63, y + 202, AllIcons.I_PRIORITY_LOW);
		skipProgress.withCallback(() -> {
			schedule.savedProgress++;
			schedule.savedProgress %= schedule.entries.size();
			resetProgress.active = schedule.savedProgress > 0;
		});
		skipProgress.active = schedule.entries.size() > 1;
		skipProgress.setToolTip(Lang.translateDirect("schedule.skip"));
		addDrawableChild(skipProgress);

		stopEditing();
		extraAreas = ImmutableList.of(new Rect2i(x + bg.width, y + bg.height - 56, 48, 48));
		horizontalScrolls.clear();
		for (int i = 0; i < schedule.entries.size(); i++)
			horizontalScrolls.add(LerpedFloat.linear()
				.startWithValue(0));
	}

	protected void startEditing(IScheduleInput field, Consumer<Boolean> onClose, boolean allowDeletion) {
		onEditorClose = onClose;
		confirmButton.visible = false;
		cyclicButton.visible = false;
		cyclicIndicator.visible = false;
		skipProgress.visible = false;
		resetProgress.visible = false;

		scrollInput = new SelectionScrollInput(x + 56, y + 65, 143, 16);
		scrollInputLabel = new Label(x + 59, y + 69, Components.immutableEmpty()).withShadow();
		editorConfirm = new IconButton(x + 56 + 168, y + 65 + 22, AllIcons.I_CONFIRM);
		if (allowDeletion)
			editorDelete = new IconButton(x + 56 - 45, y + 65 + 22, AllIcons.I_TRASH);
		handler.slotsActive = true;
		handler.targetSlotsActive = field.slotsTargeted();

		for (int i = 0; i < field.slotsTargeted(); i++) {
			ItemStack item = field.getItem(i);
			handler.ghostInventory.setStackInSlot(i, item);
			AllPackets.getChannel().sendToServer(new GhostItemSubmitPacket(item, i));
		}

		if (field instanceof ScheduleInstruction instruction) {
			int startIndex = 0;
			for (int i = 0; i < Schedule.INSTRUCTION_TYPES.size(); i++)
				if (Schedule.INSTRUCTION_TYPES.get(i)
					.getFirst()
					.equals(instruction.getId()))
					startIndex = i;
			editingDestination = instruction;
			updateEditorSubwidgets(editingDestination);
			scrollInput.forOptions(Schedule.getTypeOptions(Schedule.INSTRUCTION_TYPES))
				.titled(Lang.translateDirect("schedule.instruction_type"))
				.writingTo(scrollInputLabel)
				.calling(index -> {
					ScheduleInstruction newlyCreated = Schedule.INSTRUCTION_TYPES.get(index)
						.getSecond()
						.get();
					if (editingDestination.getId()
						.equals(newlyCreated.getId()))
						return;
					editingDestination = newlyCreated;
					updateEditorSubwidgets(editingDestination);
				})
				.setState(startIndex);
		}

		if (field instanceof ScheduleWaitCondition cond) {
			int startIndex = 0;
			for (int i = 0; i < Schedule.CONDITION_TYPES.size(); i++)
				if (Schedule.CONDITION_TYPES.get(i)
					.getFirst()
					.equals(cond.getId()))
					startIndex = i;
			editingCondition = cond;
			updateEditorSubwidgets(editingCondition);
			scrollInput.forOptions(Schedule.getTypeOptions(Schedule.CONDITION_TYPES))
				.titled(Lang.translateDirect("schedule.condition_type"))
				.writingTo(scrollInputLabel)
				.calling(index -> {
					ScheduleWaitCondition newlyCreated = Schedule.CONDITION_TYPES.get(index)
						.getSecond()
						.get();
					if (editingCondition.getId()
						.equals(newlyCreated.getId()))
						return;
					editingCondition = newlyCreated;
					updateEditorSubwidgets(editingCondition);
				})
				.setState(startIndex);
		}

		addDrawableChild(scrollInput);
		addDrawableChild(scrollInputLabel);
		addDrawableChild(editorConfirm);
		if (allowDeletion)
			addDrawableChild(editorDelete);
	}

	private void onDestinationEdited(String text) {
		if (destinationSuggestions != null)
			destinationSuggestions.refresh();
	}

	protected void stopEditing() {
		confirmButton.visible = true;
		cyclicButton.visible = true;
		cyclicIndicator.visible = true;
		skipProgress.visible = true;
		resetProgress.visible = true;

		if (editingCondition == null && editingDestination == null)
			return;

		destinationSuggestions = null;

		remove(scrollInput);
		remove(scrollInputLabel);
		remove(editorConfirm);
		remove(editorDelete);

		IScheduleInput editing = editingCondition == null ? editingDestination : editingCondition;
		for (int i = 0; i < editing.slotsTargeted(); i++) {
			editing.setItem(i, handler.ghostInventory.getStackInSlot(i));
			AllPackets.getChannel().sendToServer(new GhostItemSubmitPacket(ItemStack.EMPTY, i));
		}

		editorSubWidgets.saveValues(editing.getData());
		editorSubWidgets.forEach(this::remove);
		editorSubWidgets.clear();

		editingCondition = null;
		editingDestination = null;
		editorConfirm = null;
		editorDelete = null;
		handler.slotsActive = false;
		init();
	}

	protected void updateEditorSubwidgets(IScheduleInput field) {
		destinationSuggestions = null;
		handler.targetSlotsActive = field.slotsTargeted();

		editorSubWidgets.forEach(this::remove);
		editorSubWidgets.clear();
		field.initConfigurationWidgets(
			new ModularGuiLineBuilder(textRenderer, editorSubWidgets, x + 77, y + 92).speechBubble());
		editorSubWidgets.loadValues(field.getData(), this::addDrawableChild, this::addDrawable);

		if (!(field instanceof DestinationInstruction))
			return;

		editorSubWidgets.forEach(e -> {
			if (!(e instanceof TextFieldWidget destinationBox))
				return;
			destinationSuggestions = new DestinationSuggestions(this.client, this, destinationBox, this.textRenderer,
				getViableStations(field), y + 33);
			destinationSuggestions.setWindowActive(true);
			destinationSuggestions.refresh();
			destinationBox.setChangedListener(this::onDestinationEdited);
		});
	}

	private List<IntAttached<String>> getViableStations(IScheduleInput field) {
		GlobalRailwayManager railwayManager = Create.RAILWAYS.sided(null);
		Set<TrackGraph> viableGraphs = new HashSet<>(railwayManager.trackNetworks.values());

		for (ScheduleEntry entry : schedule.entries) {
			if (!(entry.instruction instanceof DestinationInstruction destination))
				continue;
			if (destination == field)
				continue;
			String filter = destination.getFilterForRegex();
			if (filter.isBlank())
				continue;
			Graphs: for (Iterator<TrackGraph> iterator = viableGraphs.iterator(); iterator.hasNext();) {
				TrackGraph trackGraph = iterator.next();
				for (GlobalStation station : trackGraph.getPoints(EdgePointType.STATION)) {
					if (station.name.matches(filter))
						continue Graphs;
				}
				iterator.remove();
			}
		}

		if (viableGraphs.isEmpty())
			viableGraphs = new HashSet<>(railwayManager.trackNetworks.values());

		Vec3d position = client.player.getPos();
		Set<String> visited = new HashSet<>();

		return viableGraphs.stream()
			.flatMap(g -> g.getPoints(EdgePointType.STATION)
				.stream())
			.filter(station -> station.blockEntityPos != null)
			.filter(station -> visited.add(station.name))
			.map(station -> IntAttached.with((int) Vec3d.ofBottomCenter(station.blockEntityPos)
				.distanceTo(position), station.name))
			.toList();
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		scroll.tickChaser();
		for (LerpedFloat lerpedFloat : horizontalScrolls)
			lerpedFloat.tickChaser();

		if (destinationSuggestions != null)
			destinationSuggestions.tick();

		schedule.savedProgress =
			schedule.entries.isEmpty() ? 0 : MathHelper.clamp(schedule.savedProgress, 0, schedule.entries.size() - 1);
		resetProgress.active = schedule.savedProgress > 0;
		skipProgress.active = schedule.entries.size() > 1;
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		partialTicks = client.getTickDelta();

		if (handler.slotsActive)
			super.render(graphics, mouseX, mouseY, partialTicks);
		else {
			renderBackground(graphics);
			drawBackground(graphics, partialTicks, mouseX, mouseY);
			for (Drawable widget : ((ScreenAccessor) this).port_lib$getRenderables())
				widget.render(graphics, mouseX, mouseY, partialTicks);
			renderForeground(graphics, mouseX, mouseY, partialTicks);
		}
	}

	protected void renderSchedule(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		MatrixStack matrixStack = graphics.getMatrices();
		UIRenderHelper.swapAndBlitColor(client.getFramebuffer(), UIRenderHelper.framebuffer);

		UIRenderHelper.drawStretched(graphics, x + 33, y + 16, 3, 173, 200, AllGuiTextures.SCHEDULE_STRIP_DARK);

		int yOffset = 25;
		List<ScheduleEntry> entries = schedule.entries;
		float scrollOffset = -scroll.getValue(partialTicks);

		for (int i = 0; i <= entries.size(); i++) {

			if (schedule.savedProgress == i && !schedule.entries.isEmpty()) {
				matrixStack.push();
				float expectedY = scrollOffset + y + yOffset + 4;
				float actualY = MathHelper.clamp(expectedY, y + 18, y + 170);
				matrixStack.translate(0, actualY, 0);
				(expectedY == actualY ? AllGuiTextures.SCHEDULE_POINTER : AllGuiTextures.SCHEDULE_POINTER_OFFSCREEN)
					.render(graphics, x, 0);
				matrixStack.pop();
			}

			startStencil(graphics, x + 16, y + 16, 220, 173);
			matrixStack.push();
			matrixStack.translate(0, scrollOffset, 0);
			if (i == 0 || entries.size() == 0)
				UIRenderHelper.drawStretched(graphics, x + 33, y + 16, 3, 10,
					-100, AllGuiTextures.SCHEDULE_STRIP_LIGHT);

			if (i == entries.size()) {
				if (i > 0)
					yOffset += 9;
				AllGuiTextures.SCHEDULE_STRIP_END.render(graphics, x + 29, y + yOffset);
				AllGuiTextures.SCHEDULE_CARD_NEW.render(graphics, x + 43, y + yOffset);
				matrixStack.pop();
				endStencil();
				break;
			}

			ScheduleEntry scheduleEntry = entries.get(i);
			int cardY = yOffset;
			int cardHeight = renderScheduleEntry(graphics, scheduleEntry, cardY, mouseX, mouseY, partialTicks);
			yOffset += cardHeight;

			if (i + 1 < entries.size()) {
				AllGuiTextures.SCHEDULE_STRIP_DOTTED.render(graphics, x + 29, y + yOffset - 3);
				yOffset += 10;
			}

			matrixStack.pop();
			endStencil();

			if (!scheduleEntry.instruction.supportsConditions())
				continue;

			float h = cardHeight - 26;
			float y1 = cardY + 24 + scrollOffset;
			float y2 = y1 + h;
			if (y2 > 189)
				h -= y2 - 189;
			if (y1 < 16) {
				float correction = 16 - y1;
				y1 += correction;
				h -= correction;
			}

			if (h <= 0)
				continue;

			startStencil(graphics, x + 43, y + y1, 161, h);
			matrixStack.push();
			matrixStack.translate(0, scrollOffset, 0);
			renderScheduleConditions(graphics, scheduleEntry, cardY, mouseX, mouseY, partialTicks, cardHeight, i);
			matrixStack.pop();
			endStencil();

			if (isConditionAreaScrollable(scheduleEntry)) {
				startStencil(graphics, x + 16, y + 16, 220, 173);
				matrixStack.push();
				matrixStack.translate(0, scrollOffset, 0);
				int center = (cardHeight - 8 + CARD_HEADER) / 2;
				float chaseTarget = horizontalScrolls.get(i)
					.getChaseTarget();
				if (!MathHelper.approximatelyEquals(chaseTarget, 0))
					AllGuiTextures.SCHEDULE_SCROLL_LEFT.render(graphics, x + 40, y + cardY + center);
				if (!MathHelper.approximatelyEquals(chaseTarget, scheduleEntry.conditions.size() - 1))
					AllGuiTextures.SCHEDULE_SCROLL_RIGHT.render(graphics, x + 203, y + cardY + center);
				matrixStack.pop();
				endStencil();
			}
		}

		int zLevel = 200;
		graphics.fillGradient(x + 16, y + 16, x + 16 + 220, y + 16 + 10, zLevel, 0x77000000,
			0x00000000);
		graphics.fillGradient(x + 16, y + 179, x + 16 + 220, y + 179 + 10, zLevel, 0x00000000,
			0x77000000);
		UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, client.getFramebuffer());
	}

	public int renderScheduleEntry(DrawContext graphics, ScheduleEntry entry, int yOffset, int mouseX, int mouseY,
		float partialTicks) {
		int zLevel = -100;

		AllGuiTextures light = AllGuiTextures.SCHEDULE_CARD_LIGHT;
		AllGuiTextures medium = AllGuiTextures.SCHEDULE_CARD_MEDIUM;
		AllGuiTextures dark = AllGuiTextures.SCHEDULE_CARD_DARK;

		int cardWidth = CARD_WIDTH;
		int cardHeader = CARD_HEADER;
		int maxRows = 0;
		for (List<ScheduleWaitCondition> list : entry.conditions)
			maxRows = Math.max(maxRows, list.size());
		boolean supportsConditions = entry.instruction.supportsConditions();
		int cardHeight = cardHeader + (supportsConditions ? 24 + maxRows * 18 : 4);

		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(x + 25, y + yOffset, 0);

		UIRenderHelper.drawStretched(graphics, 0, 1, cardWidth, cardHeight - 2, zLevel, light);
		UIRenderHelper.drawStretched(graphics, 1, 0, cardWidth - 2, cardHeight, zLevel, light);
		UIRenderHelper.drawStretched(graphics, 1, 1, cardWidth - 2, cardHeight - 2, zLevel, dark);
		UIRenderHelper.drawStretched(graphics, 2, 2, cardWidth - 4, cardHeight - 4, zLevel, medium);
		UIRenderHelper.drawStretched(graphics, 2, 2, cardWidth - 4, cardHeader, zLevel,
			supportsConditions ? light : medium);

		AllGuiTextures.SCHEDULE_CARD_REMOVE.render(graphics, cardWidth - 14, 2);
		AllGuiTextures.SCHEDULE_CARD_DUPLICATE.render(graphics, cardWidth - 14, cardHeight - 14);

		int i = schedule.entries.indexOf(entry);
		if (i > 0)
			AllGuiTextures.SCHEDULE_CARD_MOVE_UP.render(graphics, cardWidth, cardHeader - 14);
		if (i < schedule.entries.size() - 1)
			AllGuiTextures.SCHEDULE_CARD_MOVE_DOWN.render(graphics, cardWidth, cardHeader);

		UIRenderHelper.drawStretched(graphics, 8, 0, 3, cardHeight + 10, zLevel,
			AllGuiTextures.SCHEDULE_STRIP_LIGHT);
		(supportsConditions ? AllGuiTextures.SCHEDULE_STRIP_TRAVEL : AllGuiTextures.SCHEDULE_STRIP_ACTION)
			.render(graphics, 4, 6);

		if (supportsConditions)
			AllGuiTextures.SCHEDULE_STRIP_WAIT.render(graphics, 4, 28);

		Pair<ItemStack, Text> destination = entry.instruction.getSummary();
		renderInput(graphics, destination, 26, 5, false, 100);
		entry.instruction.renderSpecialIcon(graphics, 30, 5);

		matrixStack.pop();

		return cardHeight;
	}

	public void renderScheduleConditions(DrawContext graphics, ScheduleEntry entry, int yOffset, int mouseX,
		int mouseY, float partialTicks, int cardHeight, int entryIndex) {
		int cardWidth = CARD_WIDTH;
		int cardHeader = CARD_HEADER;

		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(x + 25, y + yOffset, 0);
		int xOffset = 26;
		float scrollOffset = getConditionScroll(entry, partialTicks, entryIndex);

		matrixStack.push();
		matrixStack.translate(-scrollOffset, 0, 0);

		for (List<ScheduleWaitCondition> list : entry.conditions) {
			int maxWidth = getConditionColumnWidth(list);
			for (int i = 0; i < list.size(); i++) {
				ScheduleWaitCondition scheduleWaitCondition = list.get(i);
				Math.max(maxWidth, renderInput(graphics, scheduleWaitCondition.getSummary(), xOffset, 29 + i * 18,
					i != 0, maxWidth));
				scheduleWaitCondition.renderSpecialIcon(graphics, xOffset + 4, 29 + i * 18);
			}

			AllGuiTextures.SCHEDULE_CONDITION_APPEND.render(graphics, xOffset + (maxWidth - 10) / 2,
				29 + list.size() * 18);
			xOffset += maxWidth + 10;
		}

		AllGuiTextures.SCHEDULE_CONDITION_NEW.render(graphics, xOffset - 3, 29);
		matrixStack.pop();

		if (xOffset + 16 > cardWidth - 26) {
			TransformStack.cast(matrixStack)
				.rotateZ(-90);
			int zLevel = 200;
			graphics.fillGradient(-cardHeight + 2, 18, -2 - cardHeader, 28, zLevel, 0x44000000, 0x00000000);
			graphics.fillGradient(-cardHeight + 2, cardWidth - 26, -2 - cardHeader, cardWidth - 16, zLevel, 0x00000000,
				0x44000000);
		}

		matrixStack.pop();
	}

	private boolean isConditionAreaScrollable(ScheduleEntry entry) {
		int xOffset = 26;
		for (List<ScheduleWaitCondition> list : entry.conditions)
			xOffset += getConditionColumnWidth(list) + 10;
		return xOffset + 16 > CARD_WIDTH - 26;
	}

	private float getConditionScroll(ScheduleEntry entry, float partialTicks, int entryIndex) {
		float scrollOffset = 0;
		float scrollIndex = horizontalScrolls.get(entryIndex)
			.getValue(partialTicks);
		for (List<ScheduleWaitCondition> list : entry.conditions) {
			int maxWidth = getConditionColumnWidth(list);
			float partialOfThisColumn = Math.min(1, scrollIndex);
			scrollOffset += (maxWidth + 10) * partialOfThisColumn;
			scrollIndex -= partialOfThisColumn;
		}
		return scrollOffset;
	}

	private int getConditionColumnWidth(List<ScheduleWaitCondition> list) {
		int maxWidth = 0;
		for (ScheduleWaitCondition scheduleWaitCondition : list)
			maxWidth = Math.max(maxWidth, getFieldSize(32, scheduleWaitCondition.getSummary()));
		return maxWidth;
	}

	protected int renderInput(DrawContext graphics, Pair<ItemStack, Text> pair, int x, int y, boolean clean,
		int minSize) {
		ItemStack stack = pair.getFirst();
		Text text = pair.getSecond();
		boolean hasItem = !stack.isEmpty();
		int fieldSize = Math.min(getFieldSize(minSize, pair), 150);
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();

		AllGuiTextures left =
			clean ? AllGuiTextures.SCHEDULE_CONDITION_LEFT_CLEAN : AllGuiTextures.SCHEDULE_CONDITION_LEFT;
		AllGuiTextures middle = AllGuiTextures.SCHEDULE_CONDITION_MIDDLE;
		AllGuiTextures item = AllGuiTextures.SCHEDULE_CONDITION_ITEM;
		AllGuiTextures right = AllGuiTextures.SCHEDULE_CONDITION_RIGHT;

		matrixStack.translate(x, y, 0);
		UIRenderHelper.drawStretched(graphics, 0, 0, fieldSize, 16, -100, middle);
		left.render(graphics, clean ? 0 : -3, 0);
		right.render(graphics, fieldSize - 2, 0);
		if (hasItem)
			item.render(graphics, 3, 0);
		if (hasItem) {
			item.render(graphics, 3, 0);
			if (stack.getItem() != Items.STRUCTURE_VOID)
				GuiGameElement.of(stack)
					.at(4, 0)
					.render(graphics);
		}

		if (text != null)
			graphics.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(text, 120)
				.getString(), hasItem ? 28 : 8, 4, 0xff_f2f2ee);

		matrixStack.pop();
		return fieldSize;
	}

	private Text clickToEdit = Lang.translateDirect("gui.schedule.lmb_edit")
		.formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
	private Text rClickToDelete = Lang.translateDirect("gui.schedule.rmb_remove")
		.formatted(Formatting.DARK_GRAY, Formatting.ITALIC);

	public boolean action(@Nullable DrawContext graphics, double mouseX, double mouseY, int click) {
		if (editingCondition != null || editingDestination != null)
			return false;

		Text empty = Components.immutableEmpty();

		int mx = (int) mouseX;
		int my = (int) mouseY;
		int sx = mx - x - 25;
		int sy = my - y - 25;
		if (sx < 0 || sx >= 205)
			return false;
		if (sy < 0 || sy >= 173)
			return false;
		sy += scroll.getValue(0);

		List<ScheduleEntry> entries = schedule.entries;
		for (int i = 0; i < entries.size(); i++) {
			ScheduleEntry entry = entries.get(i);
			int maxRows = 0;
			for (List<ScheduleWaitCondition> list : entry.conditions)
				maxRows = Math.max(maxRows, list.size());
			int cardHeight = CARD_HEADER + (entry.instruction.supportsConditions() ? 24 + maxRows * 18 : 4);

			if (sy >= cardHeight + 5) {
				sy -= cardHeight + 10;
				if (sy < 0)
					return false;
				continue;
			}

			int fieldSize = getFieldSize(100, entry.instruction.getSummary());
			if (sx > 25 && sx <= 25 + fieldSize && sy > 4 && sy <= 20) {
				List<Text> components = new ArrayList<>();
				components.addAll(entry.instruction.getTitleAs("instruction"));
				components.add(empty);
				components.add(clickToEdit);
				renderActionTooltip(graphics, components, mx, my);
				if (click == 0)
					startEditing(entry.instruction, confirmed -> {
						if (confirmed)
							entry.instruction = editingDestination;
					}, false);
				return true;
			}

			if (sx > 180 && sx <= 192) {
				if (sy > 0 && sy <= 14) {
					renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.remove_entry")),
						mx, my);
					if (click == 0) {
						entries.remove(entry);
						init();
					}
					return true;
				}
				if (y > cardHeight - 14) {
					renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.duplicate")), mx,
						my);
					if (click == 0) {
						entries.add(entries.indexOf(entry), entry.clone());
						init();
					}
					return true;
				}
			}

			if (sx > 194) {
				if (sy > 7 && sy <= 20 && i > 0) {
					renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.move_up")), mx,
						my);
					if (click == 0) {
						entries.remove(entry);
						entries.add(i - 1, entry);
						init();
					}
					return true;
				}
				if (sy > 20 && sy <= 33 && i < entries.size() - 1) {
					renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.move_down")), mx,
						my);
					if (click == 0) {
						entries.remove(entry);
						entries.add(i + 1, entry);
						init();
					}
					return true;
				}
			}

			int center = (cardHeight - 8 + CARD_HEADER) / 2;
			if (sy > center - 1 && sy <= center + 7 && isConditionAreaScrollable(entry)) {
				float chaseTarget = horizontalScrolls.get(i)
					.getChaseTarget();
				if (sx > 12 && sx <= 19 && !MathHelper.approximatelyEquals(chaseTarget, 0)) {
					if (click == 0)
						horizontalScrolls.get(i)
							.chase(chaseTarget - 1, 0.5f, Chaser.EXP);
					return true;
				}
				if (sx > 177 && sx <= 184 && !MathHelper.approximatelyEquals(chaseTarget, entry.conditions.size() - 1)) {
					if (click == 0)
						horizontalScrolls.get(i)
							.chase(chaseTarget + 1, 0.5f, Chaser.EXP);
					return true;
				}
			}

			sx -= 18;
			sy -= 28;
			if (sx < 0 || sy < 0 || sx > 160)
				return false;
			sx += getConditionScroll(entry, 0, i) - 8;

			List<List<ScheduleWaitCondition>> columns = entry.conditions;
			for (int j = 0; j < columns.size(); j++) {
				List<ScheduleWaitCondition> conditions = columns.get(j);
				if (sx < 0)
					return false;
				int w = getConditionColumnWidth(conditions);
				if (sx >= w) {
					sx -= w + 10;
					continue;
				}

				int row = sy / 18;
				if (row < conditions.size() && row >= 0) {
					boolean canRemove = conditions.size() > 1 || columns.size() > 1;
					List<Text> components = new ArrayList<>();
					components.add(Lang.translateDirect("schedule.condition_type")
						.formatted(Formatting.GRAY));
					ScheduleWaitCondition condition = conditions.get(row);
					components.addAll(condition.getTitleAs("condition"));
					components.add(empty);
					components.add(clickToEdit);
					if (canRemove)
						components.add(rClickToDelete);
					renderActionTooltip(graphics, components, mx, my);
					if (canRemove && click == 1) {
						conditions.remove(row);
						if (conditions.isEmpty())
							columns.remove(conditions);
					}
					if (click == 0)
						startEditing(condition, confirmed -> {
							conditions.remove(row);
							if (confirmed) {
								conditions.add(row, editingCondition);
								return;
							}
							if (conditions.isEmpty())
								columns.remove(conditions);
						}, canRemove);
					return true;
				}

				if (sy > 18 * conditions.size() && sy <= 18 * conditions.size() + 10 && x >= w / 2 - 5 && x < w / 2 + 5) {
					renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.add_condition")), mx, my);
					if (click == 0)
						startEditing(new ScheduledDelay(), confirmed -> {
							if (confirmed)
								conditions.add(editingCondition);
						}, true);
					return true;
				}

				return false;
			}

			if (sx < 0 || sx > 15 || sy > 20)
				return false;

			renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.alternative_condition")),
				mx, my);
			if (click == 0)
				startEditing(new ScheduledDelay(), confirmed -> {
					if (!confirmed)
						return;
					ArrayList<ScheduleWaitCondition> conditions = new ArrayList<>();
					conditions.add(editingCondition);
					columns.add(conditions);
				}, true);
			return true;
		}

		if (sx < 18 || sx > 33 || sy > 14)
			return false;

		renderActionTooltip(graphics, ImmutableList.of(Lang.translateDirect("gui.schedule.add_entry")), mx, my);
		if (click == 0)
			startEditing(new DestinationInstruction(), confirmed -> {
				if (!confirmed)
					return;

				ScheduleEntry entry = new ScheduleEntry();
				ScheduledDelay delay = new ScheduledDelay();
				ArrayList<ScheduleWaitCondition> initialConditions = new ArrayList<>();
				initialConditions.add(delay);
				entry.instruction = editingDestination;
				entry.conditions.add(initialConditions);
				schedule.entries.add(entry);
			}, true);
		return true;
	}

	private void renderActionTooltip(@Nullable DrawContext graphics, List<Text> tooltip, int mx, int my) {
		if (graphics != null)
			graphics.drawTooltip(textRenderer, tooltip, Optional.empty(), mx, my);
	}

	private int getFieldSize(int minSize, Pair<ItemStack, Text> pair) {
		ItemStack stack = pair.getFirst();
		Text text = pair.getSecond();
		boolean hasItem = !stack.isEmpty();
		return Math.max((text == null ? 0 : textRenderer.getWidth(text)) + (hasItem ? 20 : 0) + 16, minSize);
	}

	protected void startStencil(DrawContext graphics, float x, float y, float w, float h) {
		RenderSystem.clear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

		GL11.glDisable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilMask(~0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.stencilFunc(GL11.GL_NEVER, 1, 0xFF);

		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(x, y, 0);
		matrixStack.scale(w, h, 1);
		graphics.fillGradient(0, 0, 1, 1, -100, 0xff000000, 0xff000000);
		matrixStack.pop();

		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
	}

	protected void endStencil() {
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		if (destinationSuggestions != null
			&& destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
			return true;
		if (editorConfirm != null && editorConfirm.isMouseOver(pMouseX, pMouseY) && onEditorClose != null) {
			onEditorClose.accept(true);
			stopEditing();
			return true;
		}
		if (editorDelete != null && editorDelete.isMouseOver(pMouseX, pMouseY) && onEditorClose != null) {
			onEditorClose.accept(false);
			stopEditing();
			return true;
		}
		if (action(null, pMouseX, pMouseY, pButton))
			return true;

		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}

	@Override
	public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
		if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
			return true;
		if (editingCondition == null && editingDestination == null)
			return super.keyPressed(pKeyCode, pScanCode, pModifiers);
		InputUtil.Key mouseKey = InputUtil.fromKeyCode(pKeyCode, pScanCode);
		boolean hitEnter = getFocused() instanceof TextFieldWidget && (pKeyCode == 257 || pKeyCode == 335);
		boolean hitE = getFocused() == null && KeyBindingHelper.isActiveAndMatches(client.options.inventoryKey, mouseKey);
		if (hitE || hitEnter) {
			onEditorClose.accept(true);
			stopEditing();
			return true;
		}
		return super.keyPressed(pKeyCode, pScanCode, pModifiers);
	}

	@Override
	public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
		if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(MathHelper.clamp(pDelta, -1.0D, 1.0D)))
			return true;
		if (editingCondition != null || editingDestination != null)
			return super.mouseScrolled(pMouseX, pMouseY, pDelta);

		if (hasShiftDown()) {
			List<ScheduleEntry> entries = schedule.entries;
			int sy = (int) (pMouseY - y - 25 + scroll.getValue());
			for (int i = 0; i < entries.size(); i++) {
				ScheduleEntry entry = entries.get(i);
				int maxRows = 0;
				for (List<ScheduleWaitCondition> list : entry.conditions)
					maxRows = Math.max(maxRows, list.size());
				int cardHeight = CARD_HEADER + 24 + maxRows * 18;

				if (sy >= cardHeight) {
					sy -= cardHeight + 9;
					if (sy < 0)
						break;
					continue;
				}

				if (!isConditionAreaScrollable(entry))
					break;
				if (sy < 24)
					break;
				if (pMouseX < x + 25)
					break;
				if (pMouseX > x + 205)
					break;
				float chaseTarget = horizontalScrolls.get(i)
					.getChaseTarget();
				if (pDelta > 0 && !MathHelper.approximatelyEquals(chaseTarget, 0)) {
					horizontalScrolls.get(i)
						.chase(chaseTarget - 1, 0.5f, Chaser.EXP);
					return true;
				}
				if (pDelta < 0 && !MathHelper.approximatelyEquals(chaseTarget, entry.conditions.size() - 1)) {
					horizontalScrolls.get(i)
						.chase(chaseTarget + 1, 0.5f, Chaser.EXP);
					return true;
				}
				return false;
			}
		}

		float chaseTarget = scroll.getChaseTarget();
		float max = 40 - 173;
		for (ScheduleEntry scheduleEntry : schedule.entries) {
			int maxRows = 0;
			for (List<ScheduleWaitCondition> list : scheduleEntry.conditions)
				maxRows = Math.max(maxRows, list.size());
			max += CARD_HEADER + 24 + maxRows * 18 + 10;
		}
		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = MathHelper.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
	}

	@Override
	protected void renderForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		MatrixStack matrixStack = graphics.getMatrices();
		if (destinationSuggestions != null) {
			matrixStack.push();
			matrixStack.translate(0, 0, 500);
			destinationSuggestions.render(graphics, mouseX, mouseY);
			matrixStack.pop();
		}

		super.renderForeground(graphics, mouseX, mouseY, partialTicks);

		GuiGameElement.of(handler.contentHolder).<GuiGameElement
			.GuiRenderBuilder>at(x + AllGuiTextures.SCHEDULE.width, y + AllGuiTextures.SCHEDULE.height - 56,
				-200)
			.scale(3)
			.render(graphics);
		action(graphics, mouseX, mouseY, -1);

		if (editingCondition == null && editingDestination == null)
			return;

		int sx = x + 53;
		int sy = y + 87;
		if (mouseX < sx || mouseY < sy || mouseX >= sx + 120 || mouseY >= sy + 18)
			return;

		IScheduleInput rendered = editingCondition == null ? editingDestination : editingCondition;

		for (int i = 0; i < Math.max(1, rendered.slotsTargeted()); i++) {
			List<Text> secondLineTooltip = rendered.getSecondLineTooltip(i);
			if (secondLineTooltip == null || (focusedSlot != handler.getSlot(36 + i) || !focusedSlot.getStack()
				.isEmpty()))
				continue;
			renderActionTooltip(graphics, secondLineTooltip, mouseX, mouseY);
		}
	}

	@Override
	protected void drawBackground(DrawContext graphics, float pPartialTick, int pMouseX, int pMouseY) {
		AllGuiTextures.SCHEDULE.render(graphics, x, y);
		OrderedText formattedcharsequence = title.asOrderedText();
		int center = x + (AllGuiTextures.SCHEDULE.width - 8) / 2;
		graphics.drawText(textRenderer, formattedcharsequence, center - textRenderer.getWidth(formattedcharsequence) / 2,
			y + 4, 0x505050, false);
		renderSchedule(graphics, pMouseX, pMouseY, pPartialTick);

		if (editingCondition == null && editingDestination == null)
			return;

		graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
		AllGuiTextures.SCHEDULE_EDITOR.render(graphics, x - 2, y + 40);
		AllGuiTextures.PLAYER_INVENTORY.render(graphics, x + 38, y + 122);
		graphics.drawText(textRenderer, playerInventoryTitle, x + 46, y + 128, 0x505050, false);

		formattedcharsequence = editingCondition == null ? Lang.translateDirect("schedule.instruction.editor")
			.asOrderedText()
			: Lang.translateDirect("schedule.condition.editor")
				.asOrderedText();
		graphics.drawText(textRenderer, formattedcharsequence, center - textRenderer.getWidth(formattedcharsequence) / 2,
			y + 44, 0x505050, false);

		IScheduleInput rendered = editingCondition == null ? editingDestination : editingCondition;

		for (int i = 0; i < rendered.slotsTargeted(); i++)
			AllGuiTextures.SCHEDULE_EDITOR_ADDITIONAL_SLOT.render(graphics, x + 53 + 20 * i, y + 87);

		if (rendered.slotsTargeted() == 0 && !rendered.renderSpecialIcon(graphics, x + 54, y + 88)) {
			Pair<ItemStack, Text> summary = rendered.getSummary();
			ItemStack icon = summary.getFirst();
			if (icon.isEmpty())
				icon = rendered.getSecondLineIcon();
			if (icon.isEmpty())
				AllGuiTextures.SCHEDULE_EDITOR_INACTIVE_SLOT.render(graphics, x + 53, y + 87);
			else
				GuiGameElement.of(icon)
					.at(x + 54, y + 88)
					.render(graphics);
		}

		MatrixStack pPoseStack = graphics.getMatrices();
		pPoseStack.push();
		pPoseStack.translate(0, y + 87, 0);
		editorSubWidgets.renderWidgetBG(x + 77, graphics);
		pPoseStack.pop();
	}

	@Override
	public void removed() {
		super.removed();
		AllPackets.getChannel().sendToServer(new ScheduleEditPacket(schedule));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

	public TextRenderer getFont() {
		return textRenderer;
	}

}
