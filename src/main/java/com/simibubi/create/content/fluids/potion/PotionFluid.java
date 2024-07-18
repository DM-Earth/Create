package com.simibubi.create.content.fluids.potion;

import java.util.Collection;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.util.Identifier;
import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

public class PotionFluid extends VirtualFluid {

	public PotionFluid(Properties properties) {
		super(properties);
	}

	public static FluidStack of(long amount, Potion potion) {
		FluidStack fluidStack = new FluidStack(AllFluids.POTION.get()
				.getStill(), amount);
		return addPotionToFluidStack(fluidStack, potion);
	}

	public static FluidStack withEffects(long amount, Potion potion, List<StatusEffectInstance> customEffects) {
		FluidStack fluidStack = of(amount, potion);
		return appendEffects(fluidStack, customEffects);
	}

	public static FluidStack addPotionToFluidStack(FluidStack fs, Potion potion) {
		Identifier resourcelocation = RegisteredObjects.getKeyOrThrow(potion);
		if (potion == Potions.EMPTY) {
			fs.removeChildTag("Potion");
			return new FluidStack(fs.getFluid(), fs.getAmount(), fs.getTag());
		}
		fs.getOrCreateTag()
				.putString("Potion", resourcelocation.toString());
		return new FluidStack(fs.getFluid(), fs.getAmount(), fs.getTag());
	}

	public static FluidStack appendEffects(FluidStack fs, Collection<StatusEffectInstance> customEffects) {
		if (customEffects.isEmpty())
			return fs;
		NbtCompound compoundnbt = fs.getOrCreateTag();
		NbtList listnbt = compoundnbt.getList("CustomPotionEffects", 9);
		for (StatusEffectInstance effectinstance : customEffects)
			listnbt.add(effectinstance.writeNbt(new NbtCompound()));
		compoundnbt.put("CustomPotionEffects", listnbt);
		return new FluidStack(fs.getFluid(), fs.getAmount(), fs.getTag());
	}

	public enum BottleType {
		REGULAR, SPLASH, LINGERING;
	}
/*
	// fabric: PotionFluidVariantRenderHandler and PotionFluidVariantAttributeHandler in AllFluids
	public static class PotionFluidAttributes extends FluidAttributes {

		public PotionFluidAttributes(Builder builder, Fluid fluid) {
			super(builder, fluid);
		}

		@Override
		public int getColor(FluidStack stack) {
			CompoundTag tag = stack.getOrCreateTag();
			int color = PotionUtils.getColor(PotionUtils.getAllEffects(tag)) | 0xff000000;
			return color;
		}

		@Override
		public Component getDisplayName(FluidStack stack) {
			return Components.translatable(getTranslationKey(stack));
		}

		@Override
		public String getTranslationKey(FluidStack stack) {
			CompoundTag tag = stack.getOrCreateTag();
			ItemLike itemFromBottleType =
					PotionFluidHandler.itemFromBottleType(NBTHelper.readEnum(tag, "Bottle", BottleType.class));
			return PotionUtils.getPotion(tag)
					.getName(itemFromBottleType.asItem()
							.getDescriptionId() + ".effect.");
		}

	}*/

}
