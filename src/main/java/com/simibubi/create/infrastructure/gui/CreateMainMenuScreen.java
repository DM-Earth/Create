package com.simibubi.create.infrastructure.gui;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.config.ui.BaseConfigScreen;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.element.BoxElement;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.ponder.ui.PonderTagIndexScreen;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.terraformersmc.modmenu.gui.ModsScreen;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ScreenAccessor;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.TitleScreenAccessor;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class CreateMainMenuScreen extends AbstractSimiScreen {

	public static final CubeMapRenderer PANORAMA_RESOURCES =
		new CubeMapRenderer(Create.asResource("textures/gui/title/background/panorama"));
	public static final Identifier PANORAMA_OVERLAY_TEXTURES =
		new Identifier("textures/gui/title/background/panorama_overlay.png");
	public static final RotatingCubeMapRenderer PANORAMA = new RotatingCubeMapRenderer(PANORAMA_RESOURCES);

	private static final Text CURSEFORGE_TOOLTIP = Components.literal("CurseForge").styled(s -> s.withColor(0xFC785C).withBold(true));
	private static final Text MODRINTH_TOOLTIP = Components.literal("Modrinth").styled(s -> s.withColor(0x3FD32B).withBold(true));

	public static final String CURSEFORGE_LINK = "https://www.curseforge.com/minecraft/mc-mods/create-fabric";
	public static final String MODRINTH_LINK = "https://modrinth.com/mod/create-fabric";
	public static final String ISSUE_TRACKER_LINK = "https://github.com/Fabricators-of-Create/Create/issues";
	public static final String SUPPORT_LINK = "https://github.com/Creators-of-Create/Create/wiki/Supporting-the-Project";

	protected final Screen parent;
	protected boolean returnOnClose;

	private RotatingCubeMapRenderer vanillaPanorama;
	private long firstRenderTime;
	private ButtonWidget gettingStarted;

	public final boolean fromTitleOrMods;

	public CreateMainMenuScreen(Screen parent) {
		this.parent = parent;
		returnOnClose = true;
		fromTitleOrMods = (parent instanceof TitleScreen) || Mods.MODMENU.runIfInstalled(() ->
				() -> parent instanceof ModsScreen).orElse(Boolean.FALSE);
		if (parent instanceof TitleScreen titleScreen)
			vanillaPanorama = ((TitleScreenAccessor) titleScreen).port_lib$getPanorama();
		else
			vanillaPanorama = new RotatingCubeMapRenderer(TitleScreen.PANORAMA_CUBE_MAP);
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		if (firstRenderTime == 0L)
			this.firstRenderTime = Util.getMeasuringTimeMs();
		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		float f = (float) (Util.getMeasuringTimeMs() - this.firstRenderTime) / 1000.0F;
		float alpha = MathHelper.clamp(f, 0.0F, 1.0F);
		float elapsedPartials = client.getLastFrameDuration();

		if (fromTitleOrMods) {
			if (alpha < 1 && parent instanceof TitleScreen)
				vanillaPanorama.render(elapsedPartials, 1);
			PANORAMA.render(elapsedPartials, alpha);

			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
				GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			graphics.drawTexture(PANORAMA_OVERLAY_TEXTURES, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
		}

		RenderSystem.enableDepthTest();

		MatrixStack ms = graphics.getMatrices();

		for (int side : Iterate.positiveAndNegative) {
			ms.push();
			ms.translate(width / 2, 60, 200);
			ms.scale(24 * side, 24 * side, 32);
			ms.translate(-1.75 * ((alpha * alpha) / 2f + .5f), .25f, 0);
			TransformStack.cast(ms)
				.rotateX(45);
			GuiGameElement.of(AllBlocks.LARGE_COGWHEEL.getDefaultState())
				.rotateBlock(0, Util.getMeasuringTimeMs() / 32f * side, 0)
				.render(graphics);
			ms.translate(-1, 0, -1);
			GuiGameElement.of(AllBlocks.COGWHEEL.getDefaultState())
				.rotateBlock(0, Util.getMeasuringTimeMs() / -16f * side + 22.5f, 0)
				.render(graphics);
			ms.pop();
		}

		ms.push();
		ms.translate(width / 2 - 32, 32, -10);
		ms.push();
		ms.scale(0.25f, 0.25f, 0.25f);
		AllGuiTextures.LOGO.render(graphics, 0, 0);
		ms.pop();
		new BoxElement().withBackground(0x88_000000)
			.flatBorder(new Color(0x01_000000))
			.at(-32, 56, 100)
			.withBounds(128, 11)
			.render(graphics);
		ms.pop();

		ms.push();
		ms.translate(0, 0, 200);
		graphics.drawCenteredTextWithShadow(textRenderer, Components.literal(Create.NAME).formatted(Formatting.BOLD)
			.append(
				Components.literal(" v" + Create.VERSION).formatted(Formatting.BOLD, Formatting.WHITE)),
			width / 2, 89, 0xFF_E4BB67);
		ms.pop();

		RenderSystem.disableDepthTest();
	}

	protected void init() {
		super.init();
		returnOnClose = true;
		this.addButtons();
	}

	private void addButtons() {
		int yStart = height / 4 + 40;
		int center = width / 2;
		int bHeight = 20;
		int bShortWidth = 98;
		int bLongWidth = 200;

		addDrawableChild(ButtonWidget.builder(Lang.translateDirect("menu.return"), $ -> linkTo(parent))
			.dimensions(center - 100, yStart + 92, bLongWidth, bHeight)
			.build());
		addDrawableChild(ButtonWidget.builder(Lang.translateDirect("menu.configure"), $ -> linkTo(BaseConfigScreen.forCreate(this)))
			.dimensions(center - 100, yStart + 24 + -16, bLongWidth, bHeight)
			.build());

		gettingStarted = ButtonWidget.builder(Lang.translateDirect("menu.ponder_index"), $ -> linkTo(new PonderTagIndexScreen()))
			.dimensions(center + 2, yStart + 48 + -16, bShortWidth, bHeight)
			.build();
		gettingStarted.active = !fromTitleOrMods;
		addDrawableChild(gettingStarted);

		addDrawableChild(new PlatformIconButton(center - 100, yStart + 48 + -16, bShortWidth / 2, bHeight,
			AllGuiTextures.CURSEFORGE_LOGO, 0.085f,
			b -> linkTo(CURSEFORGE_LINK),
			Tooltip.of(CURSEFORGE_TOOLTIP)));
		addDrawableChild(new PlatformIconButton(center - 50, yStart + 48 + -16, bShortWidth / 2, bHeight,
			AllGuiTextures.MODRINTH_LOGO, 0.0575f,
			b -> linkTo(MODRINTH_LINK),
			Tooltip.of(MODRINTH_TOOLTIP)));

		addDrawableChild(ButtonWidget.builder(Lang.translateDirect("menu.report_bugs"), $ -> linkTo(ISSUE_TRACKER_LINK))
			.dimensions(center + 2, yStart + 68, bShortWidth, bHeight)
			.build());
		addDrawableChild(ButtonWidget.builder(Lang.translateDirect("menu.support"), $ -> linkTo(SUPPORT_LINK))
			.dimensions(center - 100, yStart + 68, bShortWidth, bHeight)
			.build());
	}

	@Override
	protected void renderWindowForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
		((ScreenAccessor) this).port_lib$getRenderables().forEach(w -> w.render(graphics, mouseX, mouseY, partialTicks));

		if (fromTitleOrMods) {
			if (mouseX < gettingStarted.getX() || mouseX > gettingStarted.getX() + 98)
				return;
			if (mouseY < gettingStarted.getY() || mouseY > gettingStarted.getY() + 20)
				return;
			graphics.drawTooltip(textRenderer,
				TooltipHelper.cutTextComponent(Lang.translateDirect("menu.only_ingame"), Palette.ALL_GRAY), mouseX,
				mouseY);
		}
	}

	private void linkTo(Screen screen) {
		returnOnClose = false;
		ScreenOpener.open(screen);
	}

	private void linkTo(String url) {
		returnOnClose = false;
		ScreenOpener.open(new ConfirmLinkScreen((p_213069_2_) -> {
			if (p_213069_2_)
				Util.getOperatingSystem()
					.open(url);
			this.client.setScreen(this);
		}, url, true));
	}

	@Override
	public boolean shouldPause() {
		return true;
	}

	protected static class PlatformIconButton extends ButtonWidget {
		protected final AllGuiTextures icon;
		protected final float scale;

		public PlatformIconButton(int pX, int pY, int pWidth, int pHeight, AllGuiTextures icon, float scale, PressAction pOnPress, Tooltip tooltip) {
			super(pX, pY, pWidth, pHeight, Components.immutableEmpty(), pOnPress, DEFAULT_NARRATION_SUPPLIER);
			this.icon = icon;
			this.scale = scale;
			setTooltip(tooltip);
		}

		@Override
		protected void renderButton(DrawContext graphics, int pMouseX, int pMouseY, float pt) {
			super.renderButton(graphics, pMouseX, pMouseY, pt);
			MatrixStack pPoseStack = graphics.getMatrices();
			pPoseStack.push();
			pPoseStack.translate(getX() + width / 2 - (icon.width * scale) / 2, getY() + height / 2 - (icon.height * scale) / 2, 0);
			pPoseStack.scale(scale, scale, 1);
			icon.render(graphics, 0, 0);
			pPoseStack.pop();
		}
	}

}
