package com.simibubi.create.content.logistics.filter.attribute;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.Components;

public class EnchantAttribute implements ItemAttribute {
    public static final EnchantAttribute EMPTY = new EnchantAttribute(null);

    private final Enchantment enchantment;

    public EnchantAttribute(@Nullable Enchantment enchantment) {
        this.enchantment = enchantment;
    }

    @Override
    public boolean appliesTo(ItemStack itemStack) {
        return EnchantmentHelper.get(itemStack).containsKey(enchantment);
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
        return EnchantmentHelper.get(itemStack).keySet().stream().map(EnchantAttribute::new).collect(Collectors.toList());
    }

    @Override
    public String getTranslationKey() {
        return "has_enchant";
    }

    @Override
    public Object[] getTranslationParameters() {
        String parameter = "";
        if(enchantment != null)
            parameter = Components.translatable(enchantment.getTranslationKey()).getString();
        return new Object[] { parameter };
    }

    @Override
    public void writeNBT(NbtCompound nbt) {
        if (enchantment == null)
            return;
        Identifier id = Registries.ENCHANTMENT.getId(enchantment);
        if (id == null)
            return;
        nbt.putString("id", id.toString());
    }

    @Override
    public ItemAttribute readNBT(NbtCompound nbt) {
        return nbt.contains("id") ? new EnchantAttribute(Registries.ENCHANTMENT.get(Identifier.tryParse(nbt.getString("id")))) : EMPTY;
    }
}
