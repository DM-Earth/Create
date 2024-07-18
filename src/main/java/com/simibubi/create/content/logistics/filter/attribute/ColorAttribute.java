package com.simibubi.create.content.logistics.filter.attribute;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.FireworkStarItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.DyeColor;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.util.TagUtil;

public class ColorAttribute implements ItemAttribute {
	public static final ColorAttribute EMPTY = new ColorAttribute(DyeColor.PURPLE);

	public final DyeColor color;

	public ColorAttribute(DyeColor color) {
		this.color = color;
	}

	@Override
	public boolean appliesTo(ItemStack itemStack) {
		return findMatchingDyeColors(itemStack).stream().anyMatch(color::equals);
	}

	@Override
	public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
		return findMatchingDyeColors(itemStack).stream().map(ColorAttribute::new).collect(Collectors.toList());
	}

	private Collection<DyeColor> findMatchingDyeColors(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();

		DyeColor color = TagUtil.getColorFromStack(stack);
		if (color != null)
			return Collections.singletonList(color);

		Set<DyeColor> colors = new HashSet<>();
		if (stack.getItem() instanceof FireworkRocketItem && nbt != null) {
			NbtList listnbt = nbt.getCompound("Fireworks").getList("Explosions", 10);
			for (int i = 0; i < listnbt.size(); i++) {
				colors.addAll(getFireworkStarColors(listnbt.getCompound(i)));
			}
		}

		if (stack.getItem() instanceof FireworkStarItem && nbt != null) {
			colors.addAll(getFireworkStarColors(nbt.getCompound("Explosion")));
		}

		Arrays.stream(DyeColor.values()).filter(c -> RegisteredObjects.getKeyOrThrow(stack.getItem()).getPath().startsWith(c.getName() + "_")).forEach(colors::add);

		return colors;
	}

	private Collection<DyeColor> getFireworkStarColors(NbtCompound compound) {
		Set<DyeColor> colors = new HashSet<>();
		Arrays.stream(compound.getIntArray("Colors")).mapToObj(DyeColor::byFireworkColor).forEach(colors::add);
		Arrays.stream(compound.getIntArray("FadeColors")).mapToObj(DyeColor::byFireworkColor).forEach(colors::add);
		return colors;
	}

	@Override
	public String getTranslationKey() {
		return "color";
	}

	@Override
	public Object[] getTranslationParameters() {
		return new Object[] { I18n.translate("color.minecraft." + color.getName()) };
	}

	@Override
	public void writeNBT(NbtCompound nbt) {
		nbt.putInt("id", color.getId());
	}

	@Override
	public ItemAttribute readNBT(NbtCompound nbt) {
		return nbt.contains("id") ?
			new ColorAttribute(DyeColor.byId(nbt.getInt("id")))
			: EMPTY;
	}
}
