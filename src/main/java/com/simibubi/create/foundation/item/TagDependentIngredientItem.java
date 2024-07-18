package com.simibubi.create.foundation.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

public class TagDependentIngredientItem extends Item {

	private TagKey<Item> tag;

	public TagDependentIngredientItem(Settings properties, TagKey<Item> tag) {
		super(properties);
		this.tag = tag;
	}

	public boolean shouldHide() {
		for (RegistryEntry<Item> ignored : Registries.ITEM.iterateEntries(this.tag)) {
			return false; // at least 1 present
		}
		return true; // none present
	}

}
