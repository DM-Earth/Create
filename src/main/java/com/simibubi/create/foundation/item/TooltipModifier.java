package com.simibubi.create.foundation.item;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.utility.AttachedRegistry;

public interface TooltipModifier {
	AttachedRegistry<Item, TooltipModifier> REGISTRY = new AttachedRegistry<>(Registries.ITEM);

	TooltipModifier EMPTY = new TooltipModifier() {
		@Override
		public void modify(ItemStack stack, PlayerEntity player, TooltipContext flags, List<Text> tooltip) {
		}

		@Override
		public TooltipModifier andThen(TooltipModifier after) {
			return after;
		}
	};

	void modify(ItemStack stack, PlayerEntity player, TooltipContext flags, List<Text> tooltip);

	default TooltipModifier andThen(TooltipModifier after) {
		if (after == EMPTY) {
			return this;
		}
		return (stack, player, flags, tooltip) -> {
			modify(stack, player, flags, tooltip);
			after.modify(stack, player, flags, tooltip);
		};
	}

	static TooltipModifier mapNull(@Nullable TooltipModifier modifier) {
		if (modifier == null) {
			return EMPTY;
		}
		return modifier;
	}
}
