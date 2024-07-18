package com.simibubi.create.foundation.gui;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.RenderTargetExtensions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Couple;

public class UIRenderHelper {

	/**
	 * An FBO that has a stencil buffer for use wherever stencil are necessary. Forcing the main FBO to have a stencil
	 * buffer will cause GL error spam when using fabulous graphics.
	 */
	public static CustomRenderTarget framebuffer;

	public static void init() {
		RenderSystem.recordRenderCall(() -> {
			Window mainWindow = MinecraftClient.getInstance().getWindow();
			framebuffer = CustomRenderTarget.create(mainWindow);
		});
	}

	public static void updateWindowSize(Window mainWindow) {
		if (framebuffer != null)
			framebuffer.resize(mainWindow.getFramebufferWidth(), mainWindow.getFramebufferHeight(), MinecraftClient.IS_SYSTEM_MAC);
	}

	public static void drawFramebuffer(float alpha) {
		framebuffer.renderWithAlpha(alpha);
	}

	/**
	 * Switch from src to dst, after copying the contents of src to dst.
	 */
	public static void swapAndBlitColor(Framebuffer src, Framebuffer dst) {
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.fbo);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.fbo);
		GlStateManager._glBlitFrameBuffer(0, 0, src.viewportWidth, src.viewportHeight, 0, 0, dst.viewportWidth, dst.viewportHeight, GL30.GL_COLOR_BUFFER_BIT, GL20.GL_LINEAR);

		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, dst.fbo);
	}

	public static void streak(DrawContext graphics, float angle, int x, int y, int breadth, int length) {
		streak(graphics, angle, x, y, breadth, length, Theme.i(Theme.Key.STREAK));
	}
	// angle in degrees; 0° -> fading to the right
	// x and y specify the middle point of the starting edge
	// breadth is the total width of the streak

	public static void streak(DrawContext graphics, float angle, int x, int y, int breadth, int length, int color) {
		int a1 = 0xa0 << 24;
		int a2 = 0x80 << 24;
		int a3 = 0x10 << 24;
		int a4 = 0x00 << 24;

		color &= 0x00FFFFFF;
		int c1 = a1 | color;
		int c2 = a2 | color;
		int c3 = a3 | color;
		int c4 = a4 | color;

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(x, y, 0);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle - 90));

		streak(graphics, breadth / 2, length, c1, c2, c3, c4);

		ms.pop();
	}

	public static void streak(DrawContext graphics, float angle, int x, int y, int breadth, int length, Color c) {
		Color color = c.copy().setImmutable();
		int c1 = color.scaleAlpha(0.625f).getRGB();
		int c2 = color.scaleAlpha(0.5f).getRGB();
		int c3 = color.scaleAlpha(0.0625f).getRGB();
		int c4 = color.scaleAlpha(0f).getRGB();

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(x, y, 0);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle - 90));

		streak(graphics, breadth / 2, length, c1, c2, c3, c4);

		ms.pop();
	}

	private static void streak(DrawContext graphics, int width, int height, int c1, int c2, int c3, int c4) {
		double split1 = .5;
		double split2 = .75;
		graphics.fillGradient(-width, 0, width, (int) (split1 * height), 0, c1, c2);
		graphics.fillGradient(-width, (int) (split1 * height), width, (int) (split2 * height), 0, c2, c3);
		graphics.fillGradient(-width, (int) (split2 * height), width, height, 0, c3, c4);
	}

	public static void angledGradient(DrawContext graphics, float angle, int x, int y, int breadth, int length, Couple<Color> c) {
		angledGradient(graphics, angle, x, y, 0, breadth, length, c);
	}

	public static void angledGradient(DrawContext graphics, float angle, int x, int y, int z, int breadth, int length, Couple<Color> c) {
		angledGradient(graphics, angle, x, y, z, breadth, length, c.getFirst(), c.getSecond());
	}

	public static void angledGradient(DrawContext graphics, float angle, int x, int y, int breadth, int length, Color color1, Color color2) {
		angledGradient(graphics, angle, x, y, 0, breadth, length, color1, color2);
	}

	/**
	 * x and y specify the middle point of the starting edge
	 *
	 * @param angle   the angle of the gradient in degrees; 0° means from left to right
	 * @param color1  the color at the starting edge
	 * @param color2  the color at the ending edge
	 * @param breadth the total width of the gradient
	 */
	public static void angledGradient(DrawContext graphics, float angle, int x, int y, int z, int breadth, int length, Color color1, Color color2) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(x, y, z);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle - 90));

		int w = breadth / 2;
		graphics.fillGradient(-w, 0, w, length, 0, color1.getRGB(), color2.getRGB());

		ms.pop();
	}

	public static void breadcrumbArrow(DrawContext graphics, int x, int y, int z, int width, int height, int indent, Couple<Color> colors) {breadcrumbArrow(graphics, x, y, z, width, height, indent, colors.getFirst(), colors.getSecond());}

	// draws a wide chevron-style breadcrumb arrow pointing left
	public static void breadcrumbArrow(DrawContext graphics, int x, int y, int z, int width, int height, int indent, Color startColor, Color endColor) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(x - indent, y, z);

		breadcrumbArrow(graphics, width, height, indent, startColor, endColor);

		matrixStack.pop();
	}

	private static void breadcrumbArrow(DrawContext graphics, int width, int height, int indent, Color c1, Color c2) {

		/*
		 * 0,0       x1,y1 ********************* x4,y4 ***** x7,y7
		 *       ****                                     ****
		 *   ****                                     ****
		 * x0,y0     x2,y2                       x5,y5
		 *   ****                                     ****
		 *       ****                                     ****
		 *           x3,y3 ********************* x6,y6 ***** x8,y8
		 *
		 */

		float x0 = 0, y0 = height / 2f;
		float x1 = indent, y1 = 0;
		float x2 = indent, y2 = height / 2f;
		float x3 = indent, y3 = height;
		float x4 = width, y4 = 0;
		float x5 = width, y5 = height / 2f;
		float x6 = width, y6 = height;
		float x7 = indent + width, y7 = 0;
		float x8 = indent + width, y8 = height;

		indent = Math.abs(indent);
		width = Math.abs(width);
		Color fc1 = Color.mixColors(c1, c2, 0);
		Color fc2 = Color.mixColors(c1, c2, (indent) / (width + 2f * indent));
		Color fc3 = Color.mixColors(c1, c2, (indent + width) / (width + 2f * indent));
		Color fc4 = Color.mixColors(c1, c2, 1);

//		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		Matrix4f model = graphics.getMatrices().peek().getPositionMatrix();
		bufferbuilder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

		bufferbuilder.vertex(model, x0, y0, 0).color(fc1.getRed(), fc1.getGreen(), fc1.getBlue(), fc1.getAlpha()).next();
		bufferbuilder.vertex(model, x1, y1, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();
		bufferbuilder.vertex(model, x2, y2, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();

		bufferbuilder.vertex(model, x0, y0, 0).color(fc1.getRed(), fc1.getGreen(), fc1.getBlue(), fc1.getAlpha()).next();
		bufferbuilder.vertex(model, x2, y2, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();
		bufferbuilder.vertex(model, x3, y3, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();

		bufferbuilder.vertex(model, x3, y3, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();
		bufferbuilder.vertex(model, x1, y1, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();
		bufferbuilder.vertex(model, x4, y4, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();

		bufferbuilder.vertex(model, x3, y3, 0).color(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha()).next();
		bufferbuilder.vertex(model, x4, y4, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();
		bufferbuilder.vertex(model, x6, y6, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();

		bufferbuilder.vertex(model, x5, y5, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();
		bufferbuilder.vertex(model, x4, y4, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();
		bufferbuilder.vertex(model, x7, y7, 0).color(fc4.getRed(), fc4.getGreen(), fc4.getBlue(), fc4.getAlpha()).next();

		bufferbuilder.vertex(model, x6, y6, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();
		bufferbuilder.vertex(model, x5, y5, 0).color(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha()).next();
		bufferbuilder.vertex(model, x8, y8, 0).color(fc4.getRed(), fc4.getGreen(), fc4.getBlue(), fc4.getAlpha()).next();

		tessellator.draw();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
//		RenderSystem.enableTexture();
	}

	//just like AbstractGui#drawTexture, but with a color at every vertex
	public static void drawColoredTexture(DrawContext graphics, Color c, int x, int y, int tex_left, int tex_top, int width, int height) {
		drawColoredTexture(graphics, c, x, y, 0, (float) tex_left, (float) tex_top, width, height, 256, 256);
	}

	public static void drawColoredTexture(DrawContext graphics, Color c, int x, int y, int z, float tex_left, float tex_top, int width, int height, int sheet_width, int sheet_height) {
		drawColoredTexture(graphics, c, x, x + width, y, y + height, z, width, height, tex_left, tex_top, sheet_width, sheet_height);
	}

	public static void drawStretched(DrawContext graphics, int left, int top, int w, int h, int z, AllGuiTextures tex) {
		tex.bind();
		drawTexturedQuad(graphics.getMatrices().peek()
			.getPositionMatrix(), Color.WHITE, left, left + w, top, top + h, z, tex.startX / 256f, (tex.startX + tex.width) / 256f,
			tex.startY / 256f, (tex.startY + tex.height) / 256f);
	}

	public static void drawCropped(DrawContext graphics, int left, int top, int w, int h, int z, AllGuiTextures tex) {
		tex.bind();
		drawTexturedQuad(graphics.getMatrices().peek()
			.getPositionMatrix(), Color.WHITE, left, left + w, top, top + h, z, tex.startX / 256f, (tex.startX + w) / 256f,
			tex.startY / 256f, (tex.startY + h) / 256f);
	}

	private static void drawColoredTexture(DrawContext graphics, Color c, int left, int right, int top, int bot, int z, int tex_width, int tex_height, float tex_left, float tex_top, int sheet_width, int sheet_height) {
		drawTexturedQuad(graphics.getMatrices().peek().getPositionMatrix(), c, left, right, top, bot, z, (tex_left + 0.0F) / (float) sheet_width, (tex_left + (float) tex_width) / (float) sheet_width, (tex_top + 0.0F) / (float) sheet_height, (tex_top + (float) tex_height) / (float) sheet_height);
	}

	private static void drawTexturedQuad(Matrix4f m, Color c, int left, int right, int top, int bot, int z, float u1, float u2, float v1, float v2) {
		Tessellator tesselator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
		bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
		bufferbuilder.vertex(m, (float) left , (float) bot, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).texture(u1, v2).next();
		bufferbuilder.vertex(m, (float) right, (float) bot, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).texture(u2, v2).next();
		bufferbuilder.vertex(m, (float) right, (float) top, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).texture(u2, v1).next();
		bufferbuilder.vertex(m, (float) left , (float) top, (float) z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).texture(u1, v1).next();
		tesselator.draw();
		RenderSystem.disableBlend();
	}

	public static void flipForGuiRender(MatrixStack poseStack) {
		poseStack.multiplyPositionMatrix(new Matrix4f().scaling(1, -1, 1));
	}

	public static class CustomRenderTarget extends Framebuffer implements RenderTargetExtensions {

		public CustomRenderTarget(boolean useDepth) {
			super(useDepth);
		}

		public static CustomRenderTarget create(Window mainWindow) {
			CustomRenderTarget framebuffer = new CustomRenderTarget(true);
			framebuffer.resize(mainWindow.getFramebufferWidth(), mainWindow.getFramebufferHeight(), MinecraftClient.IS_SYSTEM_MAC);
			framebuffer.setClearColor(0, 0, 0, 0);
			framebuffer.enableStencil();
			return framebuffer;
		}

		public void renderWithAlpha(float alpha) {
			Window window = MinecraftClient.getInstance().getWindow();

			float vx = (float) window.getScaledWidth();
			float vy = (float) window.getScaledHeight();
			float tx = (float) viewportWidth / (float) textureWidth;
			float ty = (float) viewportHeight / (float) textureHeight;

//			RenderSystem.enableTexture();
			RenderSystem.enableDepthTest();
			RenderSystem.setShader(() -> MinecraftClient.getInstance().gameRenderer.blitScreenProgram);
			RenderSystem.getShader().addSampler("DiffuseSampler", colorAttachment);

			beginRead();

			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuffer();
			bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);

			bufferbuilder.vertex(0, vy, 0).color(1, 1, 1, alpha).texture(0, 0).next();
			bufferbuilder.vertex(vx, vy, 0).color(1, 1, 1, alpha).texture(tx, 0).next();
			bufferbuilder.vertex(vx, 0, 0).color(1, 1, 1, alpha).texture(tx, ty).next();
			bufferbuilder.vertex(0, 0, 0).color(1, 1, 1, alpha).texture(0, ty).next();

			tessellator.draw();
			endRead();
		}

	}

}
