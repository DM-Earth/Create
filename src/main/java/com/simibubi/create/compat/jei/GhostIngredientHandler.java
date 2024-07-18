package com.simibubi.create.compat.jei;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AbstractContainerScreenAccessor;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GhostIngredientHandler<T extends GhostItemMenu<?>>
		implements IGhostIngredientHandler<AbstractSimiContainerScreen<T>> {

	@Override
	public <I> List<Target<I>> getTargetsTyped(AbstractSimiContainerScreen<T> gui, ITypedIngredient<I> ingredient,
		boolean doStart) {
		boolean isAttributeFilter = gui instanceof AttributeFilterScreen;
		List<Target<I>> targets = new LinkedList<>();

		if (ingredient.getType() == VanillaTypes.ITEM_STACK) {
			for (int i = 36; i < gui.getScreenHandler().slots.size(); i++) {
				if (gui.getScreenHandler().slots.get(i)
						.isEnabled())
					targets.add(new GhostTarget<>(gui, i - 36, isAttributeFilter));

				// Only accept items in 1st slot. 2nd is used for functionality, don't wanna
				// override that one
				if (isAttributeFilter)
					break;
			}
		}

		return targets;
	}

	@Override
	public void onComplete() {}

	@Override
	public boolean shouldHighlightTargets() {
		// TODO change to false and highlight the slots ourself in some better way
		return true;
	}

	private static class GhostTarget<I, T extends GhostItemMenu<?>> implements Target<I> {

		private final Rect2i area;
		private final AbstractSimiContainerScreen<T> gui;
		private final int slotIndex;
		private final boolean isAttributeFilter;

		public GhostTarget(AbstractSimiContainerScreen<T> gui, int slotIndex, boolean isAttributeFilter) {
			this.gui = gui;
			this.slotIndex = slotIndex;
			this.isAttributeFilter = isAttributeFilter;
			Slot slot = gui.getScreenHandler().slots.get(slotIndex + 36);
			AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) gui;
			this.area = new Rect2i(access.port_lib$getGuiLeft() + slot.x, access.port_lib$getGuiTop() + slot.y, 16, 16);
		}

		@Override
		public Rect2i getArea() {
			return area;
		}

		@Override
		public void accept(I ingredient) {
			ItemStack stack = ((ItemStack) ingredient).copy();
			stack.setCount(1);
			gui.getScreenHandler().ghostInventory.setStackInSlot(slotIndex, stack);

			if (isAttributeFilter)
				return;

			// sync new filter contents with server
			AllPackets.getChannel().sendToServer(new GhostItemSubmitPacket(stack, slotIndex));
		}
	}
}
