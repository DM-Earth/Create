package com.simibubi.create.foundation.recipe;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import com.simibubi.create.Create;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class BlockTagIngredient implements CustomIngredient {
	protected final TagKey<Block> tag;

	protected BlockTagIngredient(TagKey<Block> tag) {
		this.tag = tag;
	}

	public static BlockTagIngredient create(TagKey<Block> tag) {
		return new BlockTagIngredient(tag);
	}

	public TagKey<Block> getTag() {
		return tag;
	}

	@Override
	public boolean requiresTesting() {
		return false;
	}

	@Override
	public List<ItemStack> getMatchingStacks() {
		ImmutableList.Builder<ItemStack> stacks = ImmutableList.builder();
		for (RegistryEntry<Block> block : Registries.BLOCK.iterateEntries(tag)) {
			stacks.add(new ItemStack(block.value().asItem()));
		}
		return stacks.build();
	}

	@Override
	public boolean test(ItemStack stack) {
		return Block.getBlockFromItem(stack.getItem()).getDefaultState().isIn(tag);
	}

	@Override
	public CustomIngredientSerializer<?> getSerializer() {
		return Serializer.INSTANCE;
	}

	public static class Serializer implements CustomIngredientSerializer<BlockTagIngredient> {
		public static final Identifier ID = Create.asResource("block_tag_ingredient");
		public static final Serializer INSTANCE = new Serializer();

		@Override
		public Identifier getIdentifier() {
			return ID;
		}

		@Override
		public BlockTagIngredient read(JsonObject json) {
			Identifier rl = new Identifier(JsonHelper.getString(json, "tag"));
			TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, rl);
			return new BlockTagIngredient(tag);
		}

		@Override
		public void write(JsonObject json, BlockTagIngredient ingredient) {
			json.addProperty("tag", ingredient.tag.id().toString());
		}

		@Override
		public BlockTagIngredient read(PacketByteBuf buffer) {
			Identifier rl = buffer.readIdentifier();
			TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, rl);
			return new BlockTagIngredient(tag);
		}

		@Override
		public void write(PacketByteBuf buf, BlockTagIngredient ingredient) {
			buf.writeIdentifier(ingredient.tag.id());
		}
	}
}
