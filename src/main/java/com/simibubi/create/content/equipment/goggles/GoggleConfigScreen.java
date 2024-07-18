package com.simibubi.create.content.equipment.goggles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class GoggleConfigScreen extends AbstractSimiScreen {

	private int offsetX;
	private int offsetY;
	private final List<Text> tooltip;

	public GoggleConfigScreen() {
		Text componentSpacing = Components.literal("    ");
		tooltip = new ArrayList<>();
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay1")));
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay2")
				.formatted(Formatting.GRAY)));
		tooltip.add(Components.immutableEmpty());
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay3")));
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay4")));
		tooltip.add(Components.immutableEmpty());
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay5")
				.formatted(Formatting.GRAY)));
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay6")
				.formatted(Formatting.GRAY)));
		tooltip.add(Components.immutableEmpty());
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay7")));
		tooltip.add(componentSpacing.copyContentOnly()
			.append(Lang.translateDirect("gui.config.overlay8")));
	}

	@Override
	protected void init() {
		this.width = client.getWindow()
			.getScaledWidth();
		this.height = client.getWindow()
			.getScaledHeight();

		offsetX = AllConfigs.client().overlayOffsetX.get();
		offsetY = AllConfigs.client().overlayOffsetY.get();
	}

	@Override
	public void removed() {
		AllConfigs.client().overlayOffsetX.set(offsetX);
		AllConfigs.client().overlayOffsetY.set(offsetY);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		updateOffset(x, y);

		return true;
	}

	@Override
	public boolean mouseDragged(double p_mouseDragged_1_, double p_mouseDragged_3_, int p_mouseDragged_5_,
		double p_mouseDragged_6_, double p_mouseDragged_8_) {
		updateOffset(p_mouseDragged_1_, p_mouseDragged_3_);

		return true;
	}

	private void updateOffset(double windowX, double windowY) {
		offsetX = (int) (windowX - (this.width / 2));
		offsetY = (int) (windowY - (this.height / 2));

		int titleLinesCount = 1;
		int tooltipTextWidth = 0;
		for (StringVisitable textLine : tooltip) {
			int textLineWidth = client.textRenderer.getWidth(textLine);
			if (textLineWidth > tooltipTextWidth)
				tooltipTextWidth = textLineWidth;
		}
		int tooltipHeight = 8;
		if (tooltip.size() > 1) {
			tooltipHeight += (tooltip.size() - 1) * 10;
			if (tooltip.size() > titleLinesCount)
				tooltipHeight += 2; // gap between title lines and next lines
		}

		offsetX = MathHelper.clamp(offsetX, -(width / 2) - 5, (width / 2) - tooltipTextWidth - 20);
		offsetY = MathHelper.clamp(offsetY, -(height / 2) + 17, (height / 2) - tooltipHeight + 5);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int posX = this.width / 2 + offsetX;
		int posY = this.height / 2 + offsetY;
		graphics.drawTooltip(textRenderer, tooltip, posX, posY);

		// UIRenderHelper.breadcrumbArrow(ms, 50, 50, 100, 50, 20, 10, 0x80aa9999, 0x10aa9999);
		// UIRenderHelper.breadcrumbArrow(ms, 100, 80, 0, -50, 20, -10, 0x80aa9999, 0x10aa9999);

		ItemStack item = AllItems.GOGGLES.asStack();
		GuiGameElement.of(item)
			.at(posX + 10, posY - 16, 450)
			.render(graphics);
		// GuiGameElement.of(item).at(0, 0, 450).render(ms);
	}
}
