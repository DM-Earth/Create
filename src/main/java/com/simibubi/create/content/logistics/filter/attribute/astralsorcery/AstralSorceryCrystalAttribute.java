package com.simibubi.create.content.logistics.filter.attribute.astralsorcery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.Components;

public class AstralSorceryCrystalAttribute implements ItemAttribute {
    String traitName;

    public AstralSorceryCrystalAttribute(String traitName) {
        this.traitName = traitName;
    }

    @Override
    public boolean appliesTo(ItemStack itemStack) {
        for (NbtElement trait : extractTraitList(itemStack)) {
            if(((NbtCompound) trait).getString("property").equals(this.traitName))
                return true;
        }
        return false;
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
        NbtList traits = extractTraitList(itemStack);
        List<ItemAttribute> atts = new ArrayList<>();
        for (int i = 0; i < traits.size(); i++) {
            atts.add(new AstralSorceryCrystalAttribute(traits.getCompound(i).getString("property")));
        }
        return atts;
    }

    @Override
    public String getTranslationKey() {
        return "astralsorcery_crystal";
    }

    @Override
    public Object[] getTranslationParameters() {
        Identifier traitResource = new Identifier(traitName);
        String something = Components.translatable(String.format("crystal.property.%s.%s.name", traitResource.getNamespace(), traitResource.getPath())).getString();
        return new Object[] { something };
    }

    @Override
    public void writeNBT(NbtCompound nbt) {
        nbt.putString("property", this.traitName);
    }

    @Override
    public ItemAttribute readNBT(NbtCompound nbt) {
        return new AstralSorceryCrystalAttribute(nbt.getString("property"));
    }

    private NbtList extractTraitList(ItemStack stack) {
        return stack.getNbt() != null ? stack.getNbt().getCompound("astralsorcery").getCompound("crystalProperties").getList("attributes", 10) : new NbtList();
    }
}
