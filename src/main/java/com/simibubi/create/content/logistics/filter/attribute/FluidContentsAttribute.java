package com.simibubi.create.content.logistics.filter.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.simibubi.create.content.logistics.filter.ItemAttribute;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class FluidContentsAttribute implements ItemAttribute {
	public static final FluidContentsAttribute EMPTY = new FluidContentsAttribute(null);

	private final Fluid fluid;

	public FluidContentsAttribute(@Nullable Fluid fluid) {
		this.fluid = fluid;
	}

	@Override
	public boolean appliesTo(ItemStack itemStack) {
		return extractFluids(itemStack).contains(fluid);
	}

	@Override
	public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
		return extractFluids(itemStack).stream().map(FluidContentsAttribute::new).collect(Collectors.toList());
	}

	@Override
	public String getTranslationKey() {
		return "has_fluid";
	}

	@Override
	public Object[] getTranslationParameters() {
		String parameter = "";
		 if (fluid != null)
		 	parameter = FluidVariantAttributes.getName(FluidVariant.of(fluid)).getString();
		return new Object[] { parameter };
	}

	@Override
	public void writeNBT(NbtCompound nbt) {
		if (fluid == null)
			return;
		Identifier id = Registries.FLUID.getId(fluid);
		if (id == null)
			return;
		nbt.putString("id", id.toString());
	}

	@Override
	public ItemAttribute readNBT(NbtCompound nbt) {
		return nbt.contains("id") ? new FluidContentsAttribute(Registries.FLUID.get(Identifier.tryParse(nbt.getString("id")))) : EMPTY;
	}

	private List<Fluid> extractFluids(ItemStack stack) {
		List<Fluid> fluids = new ArrayList<>();

//        LazyOptional<IFluidHandlerItem> capability =
//                stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
//
//        capability.ifPresent((cap) -> {
//            for(int i = 0; i < cap.getTanks(); i++) {
//                fluids.add(cap.getFluidInTank(i).getFluid());
//            }
//        });

		return fluids;
	}
}
