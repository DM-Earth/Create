package com.simibubi.create.compat.jei;

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.element.GuiGameElement;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class ItemIcon implements IDrawable {

	private Supplier<ItemStack> supplier;
	private ItemStack stack;

	public ItemIcon(Supplier<ItemStack> stack) {
		this.supplier = stack;
	}

	@Override
	public int getWidth() {
		return 18;
	}

	@Override
	public int getHeight() {
		return 18;
	}

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		if (stack == null) {
			stack = supplier.get();
		}

		RenderSystem.enableDepthTest();
		matrixStack.push();
		matrixStack.translate(xOffset + 1, yOffset + 1, 0);

		GuiGameElement.of(stack)
			.render(graphics);

		matrixStack.pop();
	}


}
