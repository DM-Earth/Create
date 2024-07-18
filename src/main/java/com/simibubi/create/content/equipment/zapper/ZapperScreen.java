package com.simibubi.create.content.equipment.zapper;

import java.util.Vector;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;

public abstract class ZapperScreen extends AbstractSimiScreen {

	protected final Text patternSection = Lang.translateDirect("gui.terrainzapper.patternSection");

	protected AllGuiTextures background;
	protected ItemStack zapper;
	protected Hand hand;

	protected float animationProgress;

	protected Text title;
	protected Vector<IconButton> patternButtons = new Vector<>(6);
	private IconButton confirmButton;
	protected int brightColor;
	protected int fontColor;

	protected PlacementPatterns currentPattern;

	public ZapperScreen(AllGuiTextures background, ItemStack zapper, Hand hand) {
		this.background = background;
		this.zapper = zapper;
		this.hand = hand;
		title = Components.immutableEmpty();
		brightColor = 0xFEFEFE;
		fontColor = AllGuiTextures.FONT_COLOR;

		NbtCompound nbt = zapper.getOrCreateNbt();
		currentPattern = NBTHelper.readEnum(nbt, "Pattern", PlacementPatterns.class);
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height);
		setWindowOffset(-10, 0);
		super.init();

		animationProgress = 0;

		int x = guiLeft;
		int y = guiTop;

		confirmButton =
			new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			close();
		});
		addDrawableChild(confirmButton);

		patternButtons.clear();
		for (int row = 0; row <= 1; row++) {
			for (int col = 0; col <= 2; col++) {
				int id = patternButtons.size();
				PlacementPatterns pattern = PlacementPatterns.values()[id];
				IconButton patternButton = new IconButton(x + background.width - 76 + col * 18, y + 21 + row * 18, pattern.icon);
				patternButton.withCallback(() -> {
					patternButtons.forEach(b -> b.active = true);
					patternButton.active = false;
					currentPattern = pattern;
				});
				patternButton.setToolTip(Lang.translateDirect("gui.terrainzapper.pattern." + pattern.translationKey));
				patternButtons.add(patternButton);
			}
		}

		patternButtons.get(currentPattern.ordinal()).active = false;

		addRenderableWidgets(patternButtons);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		drawOnBackground(graphics, x, y);

		renderBlock(graphics, x, y);
		renderZapper(graphics, x, y);
	}

	protected void drawOnBackground(DrawContext graphics, int x, int y) {
		graphics.drawText(textRenderer, title, x + 11, y + 4, 0x54214F, false);
	}

	@Override
	public void tick() {
		super.tick();
		animationProgress += 5;
	}

	@Override
	public void removed() {
		ConfigureZapperPacket packet = getConfigurationPacket();
		packet.configureZapper(zapper);
		AllPackets.getChannel().sendToServer(packet);
	}

	protected void renderZapper(DrawContext graphics, int x, int y) {
		GuiGameElement.of(zapper)
				.scale(4)
				.at(x + background.width, y + background.height - 48, -200)
				.render(graphics);
	}

	@SuppressWarnings("deprecation")
	protected void renderBlock(DrawContext graphics, int x, int y) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(x + 32, y + 42, 120);
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25f));
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45f));
		ms.scale(20, 20, 20);

		BlockState state = Blocks.AIR.getDefaultState();
		if (zapper.hasNbt() && zapper.getNbt()
			.contains("BlockUsed"))
			state = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), zapper.getNbt()
				.getCompound("BlockUsed"));

		GuiGameElement.of(state)
			.render(graphics);
		ms.pop();
	}

	protected abstract ConfigureZapperPacket getConfigurationPacket();

}
