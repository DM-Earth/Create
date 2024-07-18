package com.simibubi.create.compat.rei;

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.element.GuiGameElement;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class ItemIcon implements Renderer {

	private Supplier<ItemStack> supplier;
	private ItemStack stack;

	public ItemIcon(Supplier<ItemStack> stack) {
		this.supplier = stack;
	}

	@Override
	public void render(DrawContext graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
		if (stack == null) {
			stack = supplier.get();
		}

		int xOffset = bounds.x;
		int yOffset = bounds.y;

		MatrixStack matrixStack = graphics.getMatrices();
		RenderSystem.enableDepthTest();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 0);

		GuiGameElement.of(stack)
				.render(graphics);

		matrixStack.pop();
	}
}
