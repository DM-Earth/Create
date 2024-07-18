package com.simibubi.create.compat.jei;

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.element.GuiGameElement;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class DoubleItemIcon implements IDrawable {

	private Supplier<ItemStack> primarySupplier;
	private Supplier<ItemStack> secondarySupplier;
	private ItemStack primaryStack;
	private ItemStack secondaryStack;

	public DoubleItemIcon(Supplier<ItemStack> primary, Supplier<ItemStack> secondary) {
		this.primarySupplier = primary;
		this.secondarySupplier = secondary;
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
		if (primaryStack == null) {
			primaryStack = primarySupplier.get();
			secondaryStack = secondarySupplier.get();
		}

		RenderSystem.enableDepthTest();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 0);

		matrixStack.push();
		matrixStack.translate(1, 1, 0);
		GuiGameElement.of(primaryStack)
			.render(graphics);
		matrixStack.pop();

		matrixStack.push();
		matrixStack.translate(10, 10, 100);
		matrixStack.scale(.5f, .5f, .5f);
		GuiGameElement.of(secondaryStack)
			.render(graphics);
		matrixStack.pop();

		matrixStack.pop();
	}

}
