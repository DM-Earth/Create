package com.simibubi.create.foundation.gui.element;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public abstract class StencilElement extends RenderElement {

	@Override
	public void render(DrawContext graphics) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		transform(ms);
		prepareStencil(ms);
		renderStencil(graphics);
		prepareElement(ms);
		renderElement(graphics);
		cleanUp(ms);
		ms.pop();
	}

	protected abstract void renderStencil(DrawContext graphics);

	protected abstract void renderElement(DrawContext graphics);

	protected void transform(MatrixStack ms) {
		ms.translate(x, y, z);
	}

	protected void prepareStencil(MatrixStack ms) {
		GL11.glDisable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilMask(~0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.stencilFunc(GL11.GL_NEVER, 1, 0xFF);
	}

	protected void prepareElement(MatrixStack ms) {
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
	}

	protected void cleanUp(MatrixStack ms) {
		GL11.glDisable(GL11.GL_STENCIL_TEST);

	}
}
