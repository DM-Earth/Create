package com.simibubi.create.content.redstone.displayLink;

import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.ElementWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.ponder.ui.PonderTagScreen;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;


public class DisplayLinkScreen extends AbstractSimiScreen {

	private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);

	private AllGuiTextures background;
	private DisplayLinkBlockEntity blockEntity;
	private IconButton confirmButton;

	BlockState sourceState;
	BlockState targetState;
	List<DisplaySource> sources;
	DisplayTarget target;

	ScrollInput sourceTypeSelector;
	Label sourceTypeLabel;
	ScrollInput targetLineSelector;
	Label targetLineLabel;
	AbstractSimiWidget sourceWidget;
	AbstractSimiWidget targetWidget;

	Couple<ModularGuiLine> configWidgets;

	public DisplayLinkScreen(DisplayLinkBlockEntity be) {
		this.background = AllGuiTextures.DATA_GATHERER;
		this.blockEntity = be;
		sources = Collections.emptyList();
		configWidgets = Couple.create(ModularGuiLine::new);
		target = null;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height);
		super.init();
		clearChildren();

		int x = guiLeft;
		int y = guiTop;


		initGathererOptions();

		confirmButton = new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::close);
		addDrawableChild(confirmButton);
	}

	@Override
	public void tick() {
		super.tick();
		if (sourceState != null && sourceState.getBlock() != client.world.getBlockState(blockEntity.getSourcePosition())
				.getBlock()
				|| targetState != null && targetState.getBlock() != client.world.getBlockState(blockEntity.getTargetPosition())
				.getBlock())
			initGathererOptions();
	}

	@SuppressWarnings("deprecation")
	private void initGathererOptions() {
		ClientWorld level = client.world;
		sourceState = level.getBlockState(blockEntity.getSourcePosition());
		targetState = level.getBlockState(blockEntity.getTargetPosition());

		ItemStack asItem;
		int x = guiLeft;
		int y = guiTop;

		Block sourceBlock = sourceState.getBlock();
		Block targetBlock = targetState.getBlock();

		asItem = sourceBlock.getPickStack(level, blockEntity.getSourcePosition(), sourceState);
		ItemStack sourceIcon = asItem == null || asItem.isEmpty() ? FALLBACK : asItem;
		asItem = targetBlock.getPickStack(level, blockEntity.getTargetPosition(), targetState);
		ItemStack targetIcon = asItem == null || asItem.isEmpty() ? FALLBACK : asItem;

		sources = AllDisplayBehaviours.sourcesOf(level, blockEntity.getSourcePosition());
		target = AllDisplayBehaviours.targetOf(level, blockEntity.getTargetPosition());

		remove(targetLineSelector);
		remove(targetLineLabel);
		remove(sourceTypeSelector);
		remove(sourceTypeLabel);
		remove(sourceWidget);
		remove(targetWidget);

		configWidgets.forEach(s -> s.forEach(this::remove));

		targetLineSelector = null;
		sourceTypeSelector = null;

		if (target != null) {
			DisplayTargetStats stats = target.provideStats(new DisplayLinkContext(level, blockEntity));
			int rows = stats.maxRows();
			int startIndex = Math.min(blockEntity.targetLine, rows);

			targetLineLabel = new Label(x + 65, y + 109, Components.immutableEmpty()).withShadow();
			targetLineLabel.text = target.getLineOptionText(startIndex);

			if (rows > 1) {
				targetLineSelector = new ScrollInput(x + 61, y + 105, 135, 16).withRange(0, rows)
						.titled(Lang.translateDirect("display_link.display_on"))
						.inverted()
						.calling(i -> targetLineLabel.text = target.getLineOptionText(i))
						.setState(startIndex);
				addDrawableChild(targetLineSelector);
			}

			addDrawableChild(targetLineLabel);
		}

		sourceWidget = new ElementWidget(x + 37, y + 26)
				.showingElement(GuiGameElement.of(sourceIcon))
				.withCallback((mX, mY) -> {
					ScreenOpener.open(new PonderTagScreen(AllPonderTags.DISPLAY_SOURCES));
				});

		sourceWidget.getToolTip().addAll(List.of(
				Lang.translateDirect("display_link.reading_from"),
				sourceState.getBlock().getName()
						.styled(s -> s.withColor(sources.isEmpty() ? 0xF68989 : 0xF2C16D)),
				Lang.translateDirect("display_link.attached_side"),
				Lang.translateDirect("display_link.view_compatible")
						.formatted(Formatting.GRAY)
		));

		addDrawableChild(sourceWidget);

		targetWidget = new ElementWidget(x + 37, y + 105)
				.showingElement(GuiGameElement.of(targetIcon))
				.withCallback((mX, mY) -> {
					ScreenOpener.open(new PonderTagScreen(AllPonderTags.DISPLAY_TARGETS));
				});

		targetWidget.getToolTip().addAll(List.of(
				Lang.translateDirect("display_link.writing_to"),
				targetState.getBlock().getName()
						.styled(s -> s.withColor(target == null ? 0xF68989 : 0xF2C16D)),
				Lang.translateDirect("display_link.targeted_location"),
				Lang.translateDirect("display_link.view_compatible")
						.formatted(Formatting.GRAY)
		));

		addDrawableChild(targetWidget);

		if (!sources.isEmpty()) {
			int startIndex = Math.max(sources.indexOf(blockEntity.activeSource), 0);

			sourceTypeLabel = new Label(x + 65, y + 30, Components.immutableEmpty()).withShadow();
			sourceTypeLabel.text = sources.get(startIndex)
					.getName();

			if (sources.size() > 1) {
				List<Text> options = sources.stream()
						.map(DisplaySource::getName)
						.toList();
				sourceTypeSelector = new SelectionScrollInput(x + 61, y + 26, 135, 16).forOptions(options)
						.writingTo(sourceTypeLabel)
						.titled(Lang.translateDirect("display_link.information_type"))
						.calling(this::initGathererSourceSubOptions)
						.setState(startIndex);
				sourceTypeSelector.onChanged();
				addDrawableChild(sourceTypeSelector);
			} else
				initGathererSourceSubOptions(0);

			addDrawableChild(sourceTypeLabel);
		}

	}

	private void initGathererSourceSubOptions(int i) {
		DisplaySource source = sources.get(i);
		source.populateData(new DisplayLinkContext(blockEntity.getWorld(), blockEntity));

		if (targetLineSelector != null)
			targetLineSelector
					.titled(source instanceof SingleLineDisplaySource ? Lang.translateDirect("display_link.display_on")
							: Lang.translateDirect("display_link.display_on_multiline"));

		configWidgets.forEach(s -> {
			s.forEach(this::remove);
			s.clear();
		});

		DisplayLinkContext context = new DisplayLinkContext(client.world, blockEntity);
		configWidgets.forEachWithContext((s, first) -> source.initConfigurationWidgets(context,
				new ModularGuiLineBuilder(textRenderer, s, guiLeft + 60, guiTop + (first ? 51 : 72)), first));
		configWidgets
				.forEach(s -> s.loadValues(blockEntity.getSourceConfig(), this::addDrawableChild, this::addDrawable));
	}

	@Override
	public void close() {
		super.close();
		NbtCompound sourceData = new NbtCompound();

		if (!sources.isEmpty()) {
			sourceData.putString("Id",
					sources.get(sourceTypeSelector == null ? 0 : sourceTypeSelector.getState()).id.toString());
			configWidgets.forEach(s -> s.saveValues(sourceData));
		}

		AllPackets.getChannel().sendToServer(new DisplayLinkConfigurationPacket(blockEntity.getPos(), sourceData,
				targetLineSelector == null ? 0 : targetLineSelector.getState()));
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		MutableText header = Lang.translateDirect("display_link.title");
		graphics.drawText(textRenderer, header, x + background.width / 2 - textRenderer.getWidth(header) / 2, y + 4, 0x592424, false);

		if (sources.isEmpty())
			graphics.drawTextWithShadow(textRenderer, Lang.translateDirect("display_link.no_source"), x + 65, y + 30, 0xD3D3D3);
		if (target == null)
			graphics.drawTextWithShadow(textRenderer, Lang.translateDirect("display_link.no_target"), x + 65, y + 109, 0xD3D3D3);

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, guiTop + 46, 0);
		configWidgets.getFirst()
				.renderWidgetBG(guiLeft, graphics);
		ms.translate(0, 21, 0);
		configWidgets.getSecond()
				.renderWidgetBG(guiLeft, graphics);
		ms.pop();

		ms.push();
		TransformStack.cast(ms)
				.pushPose()
				.translate(x + background.width + 4, y + background.height + 4, 100)
				.scale(40)
				.rotateX(-22)
				.rotateY(63);
		GuiGameElement.of(blockEntity.getCachedState()
						.with(DisplayLinkBlock.FACING, Direction.UP))
				.render(graphics);
		ms.pop();
	}

	@Override
	protected void remove(Element p_169412_) {
		if (p_169412_ != null)
			super.remove(p_169412_);
	}

}
