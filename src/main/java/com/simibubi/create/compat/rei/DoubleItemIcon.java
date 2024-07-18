package com.simibubi.create.compat.rei;

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.element.GuiGameElement;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class DoubleItemIcon implements Renderer {

	private Supplier<ItemStack> primarySupplier;
	private Supplier<ItemStack> secondarySupplier;
	private ItemStack primaryStack;
	private ItemStack secondaryStack;
	private Point pos;

	public DoubleItemIcon(Supplier<ItemStack> primary, Supplier<ItemStack> secondary) {
		this.primarySupplier = primary;
		this.secondarySupplier = secondary;
	}

	public DoubleItemIcon setPos(Point pos) {
		this.pos = pos;
		return this;
	}

	@Override
	public void render(DrawContext graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
		if (primaryStack == null) {
			primaryStack = primarySupplier.get();
			secondaryStack = secondarySupplier.get();
		}

		MatrixStack matrixStack = graphics.getMatrices();
		RenderSystem.enableDepthTest();
		matrixStack.push();
		if(pos == null)
			matrixStack.translate(bounds.getCenterX() - 9, bounds.getCenterY() - 9, 0);
		else
			matrixStack.translate(pos.getX(), pos.getY(), 0);

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
