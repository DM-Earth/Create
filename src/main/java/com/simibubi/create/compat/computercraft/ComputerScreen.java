package com.simibubi.create.compat.computercraft;

import java.util.function.Supplier;

import javax.annotation.Nullable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.ElementWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.Lang;

public class ComputerScreen extends AbstractSimiScreen {

	private final AllGuiTextures background = AllGuiTextures.COMPUTER;

	private final Supplier<Text> displayTitle;
	private final RenderWindowFunction additional;
	private final Screen previousScreen;
	private final Supplier<Boolean> hasAttachedComputer;

	private AbstractSimiWidget computerWidget;
	private IconButton confirmButton;

	public ComputerScreen(Text title, @Nullable RenderWindowFunction additional, Screen previousScreen, Supplier<Boolean> hasAttachedComputer) {
		this(title, () -> title, additional, previousScreen, hasAttachedComputer);
	}

	public ComputerScreen(Text title, Supplier<Text> displayTitle, @Nullable RenderWindowFunction additional, Screen previousScreen, Supplier<Boolean> hasAttachedComputer) {
		super(title);
		this.displayTitle = displayTitle;
		this.additional = additional;
		this.previousScreen = previousScreen;
		this.hasAttachedComputer = hasAttachedComputer;
	}

	@Override
	public void tick() {
		if (!hasAttachedComputer.get())
			client.setScreen(previousScreen);

		super.tick();
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height);
		super.init();

		int x = guiLeft;
		int y = guiTop;

		Mods.COMPUTERCRAFT.executeIfInstalled(() -> () -> {
			computerWidget = new ElementWidget(x + 33, y + 38)
					.showingElement(GuiGameElement.of(Mods.COMPUTERCRAFT.getBlock("computer_advanced")));
			computerWidget.getToolTip().add(Lang.translate("gui.attached_computer.hint").component());
			addDrawableChild(computerWidget);
		});

		confirmButton = new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::close);
		addDrawableChild(confirmButton);
	}



	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);

		graphics.drawText(textRenderer, displayTitle.get(),
			Math.round(x + background.width / 2.0F - textRenderer.getWidth(displayTitle.get()) / 2.0F), y + 4, 0x442000, false);
		graphics.drawTextWrapped(textRenderer, Lang.translate("gui.attached_computer.controlled")
			.component(), x + 55, y + 32, 111, 0x7A7A7A);

		if (additional != null)
			additional.render(graphics, mouseX, mouseY, partialTicks, x, y, background);
	}

	@FunctionalInterface
	public interface RenderWindowFunction {

		void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks, int guiLeft, int guiTop, AllGuiTextures background);

	}

}
