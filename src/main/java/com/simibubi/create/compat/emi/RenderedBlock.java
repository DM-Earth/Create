package com.simibubi.create.compat.emi;

import com.simibubi.create.foundation.gui.element.GuiGameElement;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import java.util.List;

public record RenderedBlock(BlockState state) implements EmiRenderable, DrawableWidgetConsumer {
	@Nullable
	public static RenderedBlock of(EmiIngredient ingredient) {
		List<EmiStack> stacks = ingredient.getEmiStacks();
		if (stacks.size() == 0)
			return null;
		return of(stacks.get(0));
	}

	@Nullable
	public static RenderedBlock of(EmiStack stack) {
		ItemStack item = stack.getItemStack();
		if (item.isEmpty())
			return null;
		if (!(item.getItem() instanceof BlockItem block))
			return null;
		return new RenderedBlock(block.getBlock().getDefaultState());
	}
	@Override
	public void render(DrawContext graphics, int x, int y, float delta) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(74, 51, 100);
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 20;

		GuiGameElement.of(state)
				.lighting(CreateEmiAnimations.DEFAULT_LIGHTING)
				.scale(scale)
				.render(graphics);

		matrixStack.pop();
	}
}
