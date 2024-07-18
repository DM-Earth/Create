package com.simibubi.create.foundation.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public abstract class FluidIngredient implements Predicate<FluidStack> {

	public static final FluidIngredient EMPTY = new FluidStackIngredient();

	public List<FluidStack> matchingFluidStacks;

	public static FluidIngredient fromTag(TagKey<Fluid> tag, long amount) {
		FluidTagIngredient ingredient = new FluidTagIngredient();
		ingredient.tag = tag;
		ingredient.amountRequired = amount;
		return ingredient;
	}

	public static FluidIngredient fromFluid(Fluid fluid, long amount) {
		FluidStackIngredient ingredient = new FluidStackIngredient();
		ingredient.fluid = fluid;
		ingredient.amountRequired = amount;
		ingredient.fixFlowing();
		return ingredient;
	}

	public static FluidIngredient fromFluidStack(FluidStack fluidStack) {
		FluidStackIngredient ingredient = new FluidStackIngredient();
		ingredient.fluid = fluidStack.getFluid();
		ingredient.amountRequired = fluidStack.getAmount();
		ingredient.fixFlowing();
		if (fluidStack.hasTag())
			ingredient.tagToMatch = fluidStack.getTag();
		return ingredient;
	}

	protected long amountRequired;

	protected abstract boolean testInternal(FluidStack t);

	protected abstract void readInternal(PacketByteBuf buffer);

	protected abstract void writeInternal(PacketByteBuf buffer);

	protected abstract void readInternal(JsonObject json);

	protected abstract void writeInternal(JsonObject json);

	protected abstract List<FluidStack> determineMatchingFluidStacks();

	public long getRequiredAmount() {
		return amountRequired;
	}

	public List<FluidStack> getMatchingFluidStacks() {
		if (matchingFluidStacks != null)
			return matchingFluidStacks;
		return matchingFluidStacks = determineMatchingFluidStacks();
	}

	@Override
	public boolean test(FluidStack t) {
		if (t == null)
			throw new IllegalArgumentException("FluidStack cannot be null");
		return testInternal(t);
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeBoolean(this instanceof FluidTagIngredient);
		buffer.writeVarLong(amountRequired);
		writeInternal(buffer);
	}

	public static FluidIngredient read(PacketByteBuf buffer) {
		boolean isTagIngredient = buffer.readBoolean();
		FluidIngredient ingredient = isTagIngredient ? new FluidTagIngredient() : new FluidStackIngredient();
		ingredient.amountRequired = buffer.readVarLong();
		ingredient.readInternal(buffer);
		return ingredient;
	}

	public JsonObject serialize() {
		JsonObject json = new JsonObject();
		writeInternal(json);
		json.addProperty("amount", amountRequired);
		return json;
	}

	public static boolean isFluidIngredient(@Nullable JsonElement je) {
		if (je == null || je.isJsonNull())
			return false;
		if (!je.isJsonObject())
			return false;
		JsonObject json = je.getAsJsonObject();
		if (json.has("fluidTag"))
			return true;
		else if (json.has("fluid"))
			return true;
		return false;
	}

	public static FluidIngredient deserialize(@Nullable JsonElement je) {
		if (!isFluidIngredient(je))
			throw new JsonSyntaxException("Invalid fluid ingredient: " + Objects.toString(je));

		JsonObject json = je.getAsJsonObject();
		FluidIngredient ingredient = json.has("fluidTag") ? new FluidTagIngredient() : new FluidStackIngredient();
		ingredient.readInternal(json);

		if (!json.has("amount"))
			throw new JsonSyntaxException("Fluid ingredient has to define an amount");
		ingredient.amountRequired = JsonHelper.getInt(json, "amount");
		return ingredient;
	}

	public static class FluidStackIngredient extends FluidIngredient {

		protected Fluid fluid;
		protected NbtCompound tagToMatch;

		public FluidStackIngredient() {
			tagToMatch = new NbtCompound();
		}

		void fixFlowing() {
			if (fluid instanceof FlowableFluid)
				fluid = ((FlowableFluid) fluid).getStill();
		}

		@Override
		protected boolean testInternal(FluidStack t) {
			if (!t.getFluid()
				.matchesType(fluid))
				return false;
			if (tagToMatch.isEmpty())
				return true;
			NbtCompound tag = t.getOrCreateTag();
			return tag.copy()
				.copyFrom(tagToMatch)
				.equals(tag);
		}

		@Override
		protected void readInternal(PacketByteBuf buffer) {
			fluid = Registries.FLUID.get(buffer.readIdentifier());
			tagToMatch = buffer.readNbt();
		}

		@Override
		protected void writeInternal(PacketByteBuf buffer) {
			buffer.writeIdentifier(Registries.FLUID.getId(fluid));
			buffer.writeNbt(tagToMatch);
		}

		@Override
		protected void readInternal(JsonObject json) {
			FluidStack stack = FluidHelper.deserializeFluidStack(json);
			fluid = stack.getFluid();
			tagToMatch = stack.getOrCreateTag();
		}

		@Override
		protected void writeInternal(JsonObject json) {
			json.addProperty("fluid", RegisteredObjects.getKeyOrThrow(fluid)
				.toString());
			json.add("nbt", JsonParser.parseString(tagToMatch.toString()));
		}

		@Override
		protected List<FluidStack> determineMatchingFluidStacks() {
			return ImmutableList.of(tagToMatch.isEmpty() ? new FluidStack(fluid, amountRequired)
				: new FluidStack(FluidVariant.of(fluid, tagToMatch), amountRequired, tagToMatch));
		}

	}

	public static class FluidTagIngredient extends FluidIngredient {

		protected TagKey<Fluid> tag;

		@SuppressWarnings("deprecation")
		@Override
		protected boolean testInternal(FluidStack t) {
			if (tag == null) {
				for (FluidStack accepted : getMatchingFluidStacks())
					if (accepted.getFluid()
						.matchesType(t.getFluid()))
						return true;
				return false;
			}
			return t.getFluid().isIn(tag);
		}

		@Override
		protected void readInternal(PacketByteBuf buffer) {
			int size = buffer.readVarInt();
			matchingFluidStacks = new ArrayList<>(size);
			for (int i = 0; i < size; i++)
				matchingFluidStacks.add(FluidStack.readFromPacket(buffer));
		}

		@Override
		protected void writeInternal(PacketByteBuf buffer) {
			// Tag has to be resolved on the server before sending
			List<FluidStack> matchingFluidStacks = getMatchingFluidStacks();
			buffer.writeVarInt(matchingFluidStacks.size());
			matchingFluidStacks.stream()
				.forEach(stack -> stack.writeToPacket(buffer));
		}

		@Override
		protected void readInternal(JsonObject json) {
			Identifier name = new Identifier(JsonHelper.getString(json, "fluidTag"));
			tag = TagKey.of(RegistryKeys.FLUID, name);
		}

		@Override
		protected void writeInternal(JsonObject json) {
			json.addProperty("fluidTag", tag.id()
				.toString());
		}

		@Override
		protected List<FluidStack> determineMatchingFluidStacks() {
			List<FluidStack> stacks = new ArrayList<>();
			for (RegistryEntry<Fluid> holder : Registries.FLUID.iterateEntries(tag)) {
				Fluid f = holder.value();
				if (f instanceof FlowableFluid flowing) f = flowing.getStill();
				stacks.add(new FluidStack(f, amountRequired));
			}
			return stacks;
		}

	}

}
