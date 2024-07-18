package com.simibubi.create.content.fluids.potion;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.Pair;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PotionFluidHandler {

	public static Pair<FluidStack, ItemStack> emptyPotion(ItemStack stack, boolean simulate) {
		FluidStack fluid = getFluidFromPotionItem(stack);
		if (!simulate)
			stack.decrement(1);
		return Pair.of(fluid, new ItemStack(Items.GLASS_BOTTLE));
	}

	public static FluidIngredient potionIngredient(Potion potion, long amount) {
		return FluidIngredient.fromFluidStack(FluidHelper.copyStackWithAmount(PotionFluidHandler
			.getFluidFromPotionItem(PotionUtil.setPotion(new ItemStack(Items.POTION), potion)), amount));
	}

	public static FluidStack getFluidFromPotionItem(ItemStack stack) {
		Potion potion = PotionUtil.getPotion(stack);
		List<StatusEffectInstance> list = PotionUtil.getCustomPotionEffects(stack);
		BottleType bottleTypeFromItem = bottleTypeFromItem(stack.getItem());
		if (potion == Potions.EMPTY)
			return FluidStack.EMPTY;
		if (potion == Potions.WATER && list.isEmpty() && bottleTypeFromItem == BottleType.REGULAR)
			return new FluidStack(Fluids.WATER, FluidConstants.BOTTLE);
		FluidStack fluid = PotionFluid.withEffects(FluidConstants.BOTTLE, potion, list);
		NbtCompound tagInfo = fluid.getTag();
		NBTHelper.writeEnum(tagInfo, "Bottle", bottleTypeFromItem);
		FluidVariant variant = FluidVariant.of(fluid.getFluid(), tagInfo);
		return new FluidStack(variant, fluid.getAmount(), tagInfo);
	}

	public static FluidStack getFluidFromPotion(Potion potion, BottleType bottleType, long amount) {
		if (potion == Potions.WATER && bottleType == BottleType.REGULAR)
			return new FluidStack(Fluids.WATER, amount);
		FluidStack fluid = PotionFluid.of(amount, potion);
		NBTHelper.writeEnum(fluid.getOrCreateTag(), "Bottle", bottleType);
		return new FluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getTag());
	}

	public static BottleType bottleTypeFromItem(Item item) {
		if (item == Items.LINGERING_POTION)
			return BottleType.LINGERING;
		if (item == Items.SPLASH_POTION)
			return BottleType.SPLASH;
		return BottleType.REGULAR;
	}

	public static ItemConvertible itemFromBottleType(BottleType type) {
		switch (type) {
		case LINGERING:
			return Items.LINGERING_POTION;
		case SPLASH:
			return Items.SPLASH_POTION;
		case REGULAR:
		default:
			return Items.POTION;
		}
	}

	public static long getRequiredAmountForFilledBottle(ItemStack stack, FluidStack availableFluid) {
		return FluidConstants.BOTTLE;
	}

	public static ItemStack fillBottle(ItemStack stack, FluidStack availableFluid) {
		NbtCompound tag = availableFluid.getOrCreateTag();
		ItemStack potionStack = new ItemStack(itemFromBottleType(NBTHelper.readEnum(tag, "Bottle", BottleType.class)));
		PotionUtil.setPotion(potionStack, PotionUtil.getPotion(tag));
		PotionUtil.setCustomPotionEffects(potionStack, PotionUtil.getCustomPotionEffects(tag));
		return potionStack;
	}

	// Modified version of PotionUtils#addPotionTooltip
	@Environment(EnvType.CLIENT)
	public static void addPotionTooltip(FluidStack fs, List<Text> tooltip, float p_185182_2_) {
		addPotionTooltip(fs.getType(), tooltip, p_185182_2_);
	}

	@Environment(EnvType.CLIENT)
	public static void addPotionTooltip(FluidVariant fs, List<Text> tooltip, float p_185182_2_) {
		List<StatusEffectInstance> list = PotionUtil.getPotionEffects(fs.getNbt());
		List<net.minecraft.util.Pair<String, EntityAttributeModifier>> list1 = Lists.newArrayList();
		if (list.isEmpty()) {
			tooltip.add((Components.translatable("effect.none")).formatted(Formatting.GRAY));
		} else {
			for (StatusEffectInstance effectinstance : list) {
				MutableText textcomponent = Components.translatable(effectinstance.getTranslationKey());
				StatusEffect effect = effectinstance.getEffectType();
				Map<EntityAttribute, EntityAttributeModifier> map = effect.getAttributeModifiers();
				if (!map.isEmpty()) {
					for (Entry<EntityAttribute, EntityAttributeModifier> entry : map.entrySet()) {
						EntityAttributeModifier attributemodifier = entry.getValue();
						EntityAttributeModifier attributemodifier1 = new EntityAttributeModifier(attributemodifier.getName(),
							effect.adjustModifierAmount(effectinstance.getAmplifier(), attributemodifier),
							attributemodifier.getOperation());
						list1.add(new net.minecraft.util.Pair<>(
							entry.getKey().getTranslationKey(),
							attributemodifier1));
					}
				}

				if (effectinstance.getAmplifier() > 0) {
					textcomponent.append(" ")
						.append(Components.translatable("potion.potency." + effectinstance.getAmplifier()).getString());
				}

				if (effectinstance.getDuration() > 20) {
					textcomponent.append(" (")
						.append(StatusEffectUtil.getDurationText(effectinstance, p_185182_2_))
						.append(")");
				}

				tooltip.add(textcomponent.formatted(effect.getCategory()
					.getFormatting()));
			}
		}

		if (!list1.isEmpty()) {
			tooltip.add(Components.immutableEmpty());
			tooltip.add((Components.translatable("potion.whenDrank")).formatted(Formatting.DARK_PURPLE));

			for (net.minecraft.util.Pair<String, EntityAttributeModifier> tuple : list1) {
				EntityAttributeModifier attributemodifier2 = tuple.getRight();
				double d0 = attributemodifier2.getValue();
				double d1;
				if (attributemodifier2.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_BASE
					&& attributemodifier2.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
					d1 = attributemodifier2.getValue();
				} else {
					d1 = attributemodifier2.getValue() * 100.0D;
				}

				if (d0 > 0.0D) {
					tooltip.add((Components.translatable(
						"attribute.modifier.plus." + attributemodifier2.getOperation()
							.getId(),
						ItemStack.MODIFIER_FORMAT.format(d1),
						Components.translatable(tuple.getLeft())))
							.formatted(Formatting.BLUE));
				} else if (d0 < 0.0D) {
					d1 = d1 * -1.0D;
					tooltip.add((Components.translatable(
						"attribute.modifier.take." + attributemodifier2.getOperation()
							.getId(),
						ItemStack.MODIFIER_FORMAT.format(d1),
						Components.translatable(tuple.getLeft())))
							.formatted(Formatting.RED));
				}
			}
		}

	}

}
