package com.simibubi.create.foundation.ponder.ui;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.glfw.GLFW;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.ponder.PonderLocalization;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

public abstract class NavigatableSimiScreen extends AbstractSimiScreen {

	public static final String THINK_BACK = PonderLocalization.LANG_PREFIX + "think_back";

	protected int depthPointX, depthPointY;
	public final LerpedFloat transition = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, .1f, LerpedFloat.Chaser.LINEAR);
	protected final LerpedFloat arrowAnimation = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, 0.075f, LerpedFloat.Chaser.LINEAR);
	protected PonderButton backTrack;

	public NavigatableSimiScreen() {
		Window window = MinecraftClient.getInstance().getWindow();
		depthPointX = window.getScaledWidth() / 2;
		depthPointY = window.getScaledHeight() / 2;
	}

	@Override
	public void close() {
		ScreenOpener.clearStack();
		super.close();
	}

	@Override
	public void tick() {
		super.tick();
		transition.tickChaser();
		arrowAnimation.tickChaser();
	}

	@Override
	protected void init() {
		super.init();

		backTrack = null;
		List<Screen> screenHistory = ScreenOpener.getScreenHistory();
		if (screenHistory.isEmpty())
			return;
		if (!(screenHistory.get(0) instanceof NavigatableSimiScreen))
			return;

		NavigatableSimiScreen screen = (NavigatableSimiScreen) screenHistory.get(0);

		addDrawableChild(backTrack = new PonderButton(31, height - 31 - 20).enableFade(0, 5)
			.withCallback(() -> ScreenOpener.openPreviousScreen(this, Optional.empty())));
		backTrack.fade(1);

		screen.initBackTrackIcon(backTrack);
	}

	/**
	 * Called when {@code this} represents the previous screen to
	 * initialize the {@code backTrack} icon of the current screen.
	 *
	 * @param backTrack The backTrack button of the current screen.
	 */
	protected abstract void initBackTrackIcon(PonderButton backTrack);

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
//		renderZeloBreadcrumbs(ms, mouseX, mouseY, partialTicks);
		if (backTrack == null)
			return;

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, 0, 500);
		if (backTrack.isSelected()) {
			MutableText translate = Lang.translateDirect(backTrackingLangKey());
			graphics.drawText(textRenderer, translate, 41 - textRenderer.getWidth(translate) / 2, height - 16,
				Theme.i(Theme.Key.TEXT_DARKER), false);
			if (MathHelper.approximatelyEquals(arrowAnimation.getValue(), arrowAnimation.getChaseTarget())) {
				arrowAnimation.setValue(1);
				arrowAnimation.setValue(1);// called twice to also set the previous value to 1
			}
		}
		ms.pop();
	}

	protected String backTrackingLangKey() {
		return THINK_BACK;
	}

	@Override
	protected void renderWindowBackground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		if (backTrack != null) {
			int x = (int) MathHelper.lerp(arrowAnimation.getValue(partialTicks), -9, 21);
			int maxX = backTrack.getX() + backTrack.getWidth();

			if (x + 30 < backTrack.getX())
				UIRenderHelper.breadcrumbArrow(graphics, x + 30, height - 51, 0, maxX - (x + 30), 20, 5,
					Theme.p(Theme.Key.PONDER_BACK_ARROW));

			UIRenderHelper.breadcrumbArrow(graphics, x, height - 51, 0, 30, 20, 5, Theme.p(Theme.Key.PONDER_BACK_ARROW));
			UIRenderHelper.breadcrumbArrow(graphics, x - 30, height - 51, 0, 30, 20, 5, Theme.p(Theme.Key.PONDER_BACK_ARROW));
		}

		if (transition.getChaseTarget() == 0 || transition.settled()) {
			renderBackground(graphics);
			return;
		}

		renderBackground(graphics);

		MatrixStack ms = graphics.getMatrices();
		
//		Screen lastScreen = ScreenOpener.getPreviouslyRenderedScreen();
		float transitionValue = transition.getValue(partialTicks);
		float scale = 1 + 0.5f * transitionValue;

		
		/*
		 * Looks like this stopped working sometime before 1.18
		 * Now commented as it does mess with the background alpha since 1.20
		 */
		
		// draw last screen into buffer
//		if (lastScreen != null && lastScreen != this && !transition.settled()) {
//			ms.pushPose();
//			UIRenderHelper.framebuffer.clear(Minecraft.ON_OSX);
//			ms.translate(0, 0, -1000);
//			UIRenderHelper.framebuffer.bindWrite(true);
//			PonderTooltipHandler.enable = false;
//			// Using 0,0 for mouse coords to hide tooltips
//			lastScreen.render(graphics, 0, 0, partialTicks);
//			PonderTooltipHandler.enable = true;
//
//			ms.popPose();
//			ms.pushPose();
//
//			// use the buffer texture
//			minecraft.getMainRenderTarget()
//				.bindWrite(true);
//
//			Window window = minecraft.getWindow();
//			int dpx = window.getGuiScaledWidth() / 2;
//			int dpy = window.getGuiScaledHeight() / 2;
//			if (lastScreen instanceof NavigatableSimiScreen navigableScreen) {
//				dpx = navigableScreen.depthPointX;
//				dpy = navigableScreen.depthPointY;
//			}
//
//			ms.translate(dpx, dpy, 0);
//			ms.scale(scale, scale, 1);
//			ms.translate(-dpx, -dpy, 0);
//			RenderSystem.enableBlend();
//			RenderSystem.defaultBlendFunc();
//			UIRenderHelper.drawFramebuffer(1f - Math.abs(transitionValue));
//			RenderSystem.disableBlend();
//			ms.popPose();
//		}

		// modify current screen as well
		scale = transitionValue > 0 ? 1 - 0.5f * (1 - transitionValue) : 1 + .5f * (1 + transitionValue);
		ms.translate(depthPointX, depthPointY, 0);
		ms.scale(scale, scale, 1);
		ms.translate(-depthPointX, -depthPointY, 0);
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (code == GLFW.GLFW_KEY_BACKSPACE) {
			ScreenOpener.openPreviousScreen(this, Optional.empty());
			return true;
		}
		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	public void centerScalingOn(int x, int y) {
		depthPointX = x;
		depthPointY = y;
	}

	public void centerScalingOnMouse() {
		Window w = client.getWindow();
		double mouseX = client.mouse.getX() * w.getScaledWidth() / w.getWidth();
		double mouseY = client.mouse.getY() * w.getScaledHeight() / w.getHeight();
		centerScalingOn((int) mouseX, (int) mouseY);
	}

	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		return false;
	}

	public void shareContextWith(NavigatableSimiScreen other) {}

	protected void renderZeloBreadcrumbs(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		List<Screen> history = ScreenOpener.getScreenHistory();
		if (history.isEmpty())
			return;

		history.add(0, client.currentScreen);
		int spacing = 20;

		List<String> names = history.stream()
			.map(NavigatableSimiScreen::screenTitle)
			.collect(Collectors.toList());

		int bWidth = names.stream()
			.mapToInt(s -> textRenderer.getWidth(s) + spacing)
			.sum();

		MutableInt x = new MutableInt(width - bWidth);
		MutableInt y = new MutableInt(height - 18);
		MutableBoolean first = new MutableBoolean(true);

		if (x.getValue() < 25)
			x.setValue(25);

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, 0, 600);
		names.forEach(s -> {
			int sWidth = textRenderer.getWidth(s);
			UIRenderHelper.breadcrumbArrow(graphics, x.getValue(), y.getValue(), 0, sWidth + spacing, 14, spacing / 2,
					new Color(0xdd101010), new Color(0x44101010));
			graphics.drawText(textRenderer, s, x.getValue() + 5, y.getValue() + 3, first.getValue() ? 0xffeeffee : 0xffddeeff, false);
			first.setFalse();

			x.add(sWidth + spacing);
		});
		ms.pop();
	}

	private static String screenTitle(Screen screen) {
		if (screen instanceof NavigatableSimiScreen)
			return ((NavigatableSimiScreen) screen).getBreadcrumbTitle();
		return "<";
	}

	protected String getBreadcrumbTitle() {
		return this.getClass()
			.getSimpleName();
	}
}
