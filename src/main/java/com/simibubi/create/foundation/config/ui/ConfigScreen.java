package com.simibubi.create.foundation.config.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.element.StencilElement;
import com.simibubi.create.foundation.utility.animation.Force;
import com.simibubi.create.foundation.utility.animation.PhysicalFloat;
import com.simibubi.create.infrastructure.gui.CreateMainMenuScreen;

public abstract class ConfigScreen extends AbstractSimiScreen {

	/*
	 *
	 * TODO
	 *
	 * reduce number of packets sent to the server when saving a bunch of values
	 *
	 * FIXME
	 *
	 * tooltips are hidden underneath the scrollbar, if the bar is near the middle
	 *
	 */

	public static final Map<String, TriConsumer<Screen, DrawContext, Float>> backgrounds = new HashMap<>();
	public static final PhysicalFloat cogSpin = PhysicalFloat.create().withLimit(10f).withDrag(0.3).addForce(new Force.Static(.2f));
	public static final BlockState cogwheelState = AllBlocks.LARGE_COGWHEEL.getDefaultState().with(CogWheelBlock.AXIS, Direction.Axis.Y);
	public static String modID = null;
	protected final Screen parent;

	public ConfigScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public void tick() {
		super.tick();
		cogSpin.tick();
	}

//	@Override
//	public void renderBackground(GuiGraphics graphics) {
//		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(this, graphics));
//	}

	@Override
	protected void renderWindowBackground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		if (this.client != null && this.client.world != null) {
			//in game
			graphics.fill(0, 0, this.width, this.height, 0xb0_282c34);
		} else {
			//in menus
			renderMenuBackground(graphics, partialTicks);
		}

		new StencilElement() {
			@Override
			protected void renderStencil(DrawContext graphics) {
				renderCog(graphics, partialTicks);
			}

			@Override
			protected void renderElement(DrawContext graphics) {
				graphics.fill(-200, -200, 200, 200, 0x60_000000);
			}
		}.at(width * 0.5f, height * 0.5f, 0).render(graphics);

		super.renderWindowBackground(graphics, mouseX, mouseY, partialTicks);

	}

	@Override
	protected void prepareFrame() {
		UIRenderHelper.swapAndBlitColor(client.getFramebuffer(), UIRenderHelper.framebuffer);
		RenderSystem.clear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
	}

	@Override
	protected void endFrame() {
		UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, client.getFramebuffer());
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		cogSpin.bump(3, -delta * 5);

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return true;
	}

	public static String toHumanReadable(String key) {
		String s = key.replaceAll("_", " ");
		s = Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(s)).map(StringUtils::capitalize).collect(Collectors.joining(" "));
		s = StringUtils.normalizeSpace(s);
		return s;
	}

	/**
	 * By default ConfigScreens will render the Create Panorama as
	 * their background when opened from the Main- or ModList-Menu.
	 * If your addon wants to render something else, please add to the
	 * backgrounds Map in this Class with your modID as the key.
	 */
	protected void renderMenuBackground(DrawContext graphics, float partialTicks) {
		TriConsumer<Screen, DrawContext, Float> customBackground = backgrounds.get(modID);
		if (customBackground != null) {
			customBackground.accept(this, graphics, partialTicks);
			return;
		}

		float elapsedPartials = client.getLastFrameDuration();
		CreateMainMenuScreen.PANORAMA.render(elapsedPartials, 1);

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		graphics.drawTexture(CreateMainMenuScreen.PANORAMA_OVERLAY_TEXTURES, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);

		graphics.fill(0, 0, this.width, this.height, 0x90_282c34);
	}

	protected void renderCog(DrawContext graphics, float partialTicks) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();

		ms.translate(-100, 100, -100);
		ms.scale(200, 200, 1);
		GuiGameElement.of(cogwheelState)
				.rotateBlock(22.5, cogSpin.getValue(partialTicks), 22.5)
				.render(graphics);

		ms.pop();
	}

	@Override
	public void renderBackgroundTexture(@NotNull DrawContext graphics) {
	}
}
