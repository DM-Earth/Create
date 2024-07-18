package com.simibubi.create.compat.jei;

import java.util.List;

import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter implements IIngredientSubtypeInterpreter<IJeiFluidIngredient> {

	@Override
	public String apply(IJeiFluidIngredient ingredient, UidContext context) {
		if (ingredient.getTag().isEmpty())
			return IIngredientSubtypeInterpreter.NONE;

		NbtCompound tag = ingredient.getTag().get();
		Potion potionType = PotionUtil.getPotion(tag);
		String potionTypeString = potionType.finishTranslationKey("");
		String bottleType = NBTHelper.readEnum(tag, "Bottle", BottleType.class)
				.toString();

		StringBuilder stringBuilder = new StringBuilder(potionTypeString);
		List<StatusEffectInstance> effects = PotionUtil.getCustomPotionEffects(tag);

		stringBuilder.append(";")
				.append(bottleType);
		for (StatusEffectInstance effect : potionType.getEffects())
			stringBuilder.append(";")
					.append(effect);
		for (StatusEffectInstance effect : effects)
			stringBuilder.append(";")
					.append(effect);
		return stringBuilder.toString();
	}

}
