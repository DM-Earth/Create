package com.simibubi.create.compat.rei;

import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter /*implements IIngredientSubtypeInterpreter<FluidStack>*/ {

//	@Override
	public String apply(FluidStack ingredient) {
		if (!ingredient.hasTag())
			return "";

		NbtCompound tag = ingredient.getOrCreateTag();
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
