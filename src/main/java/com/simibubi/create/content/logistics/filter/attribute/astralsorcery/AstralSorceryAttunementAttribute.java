package com.simibubi.create.content.logistics.filter.attribute.astralsorcery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.Components;

public class AstralSorceryAttunementAttribute implements ItemAttribute {
    String constellationName;

    public AstralSorceryAttunementAttribute(String constellationName) {
        this.constellationName = constellationName;
    }

    @Override
    public boolean appliesTo(ItemStack itemStack) {
        NbtCompound nbt = extractAstralNBT(itemStack);
        String constellation = nbt.contains("constellation") ? nbt.getString("constellation") : nbt.getString("constellationName");

        // Special handling for shifting stars
        Identifier itemResource = Registries.ITEM.getId(itemStack.getItem());
        if (itemResource.toString().contains("shifting_star_")) {
            constellation = itemResource.toString().replace("shifting_star_", "");
        }

        return constellation.equals(constellationName);
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
        NbtCompound nbt = extractAstralNBT(itemStack);
        String constellation = nbt.contains("constellation") ? nbt.getString("constellation") : nbt.getString("constellationName");

        // Special handling for shifting stars
        Identifier itemResource = Registries.ITEM.getId(itemStack.getItem());
        if (itemResource.toString().contains("shifting_star_")) {
            constellation = itemResource.toString().replace("shifting_star_", "");
        }

        List<ItemAttribute> atts = new ArrayList<>();
        if(constellation.length() > 0) {
            atts.add(new AstralSorceryAttunementAttribute(constellation));
        }
        return atts;
    }

    @Override
    public String getTranslationKey() {
        return "astralsorcery_constellation";
    }

    @Override
    public Object[] getTranslationParameters() {
        Identifier constResource = new Identifier(constellationName);
        String something = Components.translatable(String.format("%s.constellation.%s", constResource.getNamespace(), constResource.getPath())).getString();
        return new Object[] { something };
    }

    @Override
    public void writeNBT(NbtCompound nbt) {
        nbt.putString("constellation", this.constellationName);
    }

    @Override
    public ItemAttribute readNBT(NbtCompound nbt) {
        return new AstralSorceryAttunementAttribute(nbt.getString("constellation"));
    }

    private NbtCompound extractAstralNBT(ItemStack stack) {
        return stack.getNbt() != null ? stack.getNbt().getCompound("astralsorcery") : new NbtCompound();
    }
}
