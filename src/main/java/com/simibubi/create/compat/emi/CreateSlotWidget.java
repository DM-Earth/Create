package com.simibubi.create.compat.emi;

import java.util.List;

import com.simibubi.create.foundation.mixin.fabric.ClientTextTooltipAccessor;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import com.tterrag.registrate.util.entry.FluidEntry;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import io.github.fabricators_of_create.porting_lib.util.FluidTextUtil;
import io.github.fabricators_of_create.porting_lib.util.FluidUnit;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CreateSlotWidget extends SlotWidget {
	public CreateSlotWidget(EmiIngredient stack, int x, int y) {
		super(stack, x, y);
	}

	@Override
	public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
		List<TooltipComponent> tooltip = super.getTooltip(mouseX, mouseY);
		if (stack instanceof EmiStack emiStack && emiStack.getKey() instanceof Fluid fluid) {
			// add custom fluid tooltip
			FluidVariant variant = FluidVariant.of(fluid, emiStack.getNbt());
			addCreateAmount(tooltip, variant);
			removeEmiAmount(tooltip, variant);
		}
		return tooltip;
	}

	private void addCreateAmount(List<TooltipComponent> tooltip, FluidVariant fluid) {
		FluidUnit unit = AllConfigs.client().fluidUnitType.get();
		String amount = FluidTextUtil.getUnicodeMillibuckets(stack.getAmount(), unit, AllConfigs.client().simplifyFluidUnit.get());

		Text amountComponent = Text.literal(" " + amount)
				.append(Lang.translateDirect(unit.getTranslationKey()))
				.formatted(Formatting.GOLD);

		MutableText fluidName = FluidVariantAttributes.getName(fluid)
				.copy()
				.append(amountComponent);

		TooltipComponent component = toTooltip(fluidName);

		if (tooltip.isEmpty()) {
			tooltip.add(component);
		} else {
			tooltip.set(0, toTooltip(fluidName));
		}
	}

	private void removeEmiAmount(List<TooltipComponent> tooltip, FluidVariant entry) {
		Fluid fluid = entry.getFluid();
		String namespace = Registries.FLUID.getId(fluid).getNamespace();
		String modName = FabricLoader.getInstance().getModContainer(namespace).map(c -> c.getMetadata().getName()).orElse(null);
		if (modName == null)
			return;
		int indexOfModName = -1;
		for (int i = 0; i < tooltip.size(); i++) {
			TooltipComponent component = tooltip.get(i);
			if (component instanceof ClientTextTooltipAccessor text) {
				OrderedText contents = text.create$text();
				StringBuilder string = new StringBuilder();
				contents.accept((what, style, c) -> {
					string.append((char) c);
					return true;
				});
				if (string.toString().equals(modName)) {
					indexOfModName = i;
					break;
				}
			}
		}
		if (indexOfModName != -1) {
			// emi amount is always(?) right above the mod name
			int indexOfAmount = indexOfModName - 1;
			if (indexOfAmount > 0) {
				tooltip.remove(indexOfAmount);
			}
		}
	}

	private static TooltipComponent toTooltip(Text component) {
		return TooltipComponent.of(component.asOrderedText());
	}
}
