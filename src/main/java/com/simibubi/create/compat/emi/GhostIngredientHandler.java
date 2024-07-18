package com.simibubi.create.compat.emi;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AbstractContainerScreenAccessor;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GhostIngredientHandler<T extends GhostItemMenu<?>>
		implements EmiDragDropHandler<AbstractSimiContainerScreen<T>> {

	public static final int INVENTORY_SIZE = 36;

	@Override
	public boolean dropStack(AbstractSimiContainerScreen<T> gui, EmiIngredient ingredient, int x, int y) {
		List<EmiStack> stacks = ingredient.getEmiStacks();
		if (!(gui instanceof AbstractContainerScreenAccessor access) || stacks.size() != 1)
			return false;
		ItemStack stack = stacks.get(0).getItemStack();
		if (stack.isEmpty())
			return false;
		boolean isAttributeFilter = gui instanceof AttributeFilterScreen;

		for (int i = INVENTORY_SIZE; i < gui.getScreenHandler().slots.size(); i++) {
			Slot slot = gui.getScreenHandler().slots.get(i);
			if (slot.isEnabled()) {
				Rect2i slotArea = new Rect2i(access.port_lib$getGuiLeft() + slot.x, access.port_lib$getGuiTop() + slot.y, 16, 16);
				if (slotArea.contains(x, y)) {
					acceptStack(gui, isAttributeFilter, i - INVENTORY_SIZE, stack);
					return true;
				}
			}
			// Only accept items in 1st slot. 2nd is used for functionality, don't wanna
			// override that one
			if (isAttributeFilter)
				break;
		}

		return false;
	}

	private void acceptStack(AbstractSimiContainerScreen<T> gui, boolean isAttributeFilter, int slotIndex, ItemStack stack) {
		stack = ItemHandlerHelper.copyStackWithSize(stack, 1);
		gui.getScreenHandler().ghostInventory.setStackInSlot(slotIndex, stack);

		if (isAttributeFilter)
			return;

		// sync new filter contents with server
		AllPackets.getChannel().sendToServer(new GhostItemSubmitPacket(stack, slotIndex));
	}
}
