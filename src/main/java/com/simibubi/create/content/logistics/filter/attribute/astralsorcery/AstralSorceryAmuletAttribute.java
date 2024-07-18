package com.simibubi.create.content.logistics.filter.attribute.astralsorcery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.Components;

public class AstralSorceryAmuletAttribute implements ItemAttribute {
    String enchName;
    int enchType;

    public AstralSorceryAmuletAttribute(String enchName, int enchType) {
        this.enchName = enchName;
        this.enchType = enchType;
    }

    @Override
    public boolean appliesTo(ItemStack itemStack) {
        for (NbtElement trait : extractTraitList(itemStack)) {
            if(((NbtCompound) trait).getString("ench").equals(this.enchName)
                    && ((NbtCompound)trait).getInt("type") == this.enchType)
                return true;
        }
        return false;
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
        NbtList traits = extractTraitList(itemStack);
        List<ItemAttribute> atts = new ArrayList<>();
        for (int i = 0; i < traits.size(); i++) {
            atts.add(new AstralSorceryAmuletAttribute(
                    traits.getCompound(i).getString("ench"),
                    traits.getCompound(i).getInt("type")));
        }
        return atts;
    }

    @Override
    public String getTranslationKey() {
        return "astralsorcery_amulet";
    }

    @Override
    public Object[] getTranslationParameters() {
        String something = "";

        Enchantment enchant = Registries.ENCHANTMENT.get(Identifier.tryParse(enchName));
        if(enchant != null) {
            something = Components.translatable(enchant.getTranslationKey()).getString();
        }

        if(enchType == 1) something = "existing " + something;

        return new Object[] { something };
    }

    @Override
    public void writeNBT(NbtCompound nbt) {
        nbt.putString("enchName", this.enchName);
        nbt.putInt("enchType", this.enchType);
    }

    @Override
    public ItemAttribute readNBT(NbtCompound nbt) {
        return new AstralSorceryAmuletAttribute(nbt.getString("enchName"), nbt.getInt("enchType"));
    }

    private NbtList extractTraitList(ItemStack stack) {
        return stack.getNbt() != null ? stack.getNbt().getCompound("astralsorcery").getList("amuletEnchantments", 10) : new NbtList();
    }
}
