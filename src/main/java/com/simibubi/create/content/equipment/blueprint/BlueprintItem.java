package com.simibubi.create.content.equipment.blueprint;

import java.util.Collection;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Ingredient.Entry;
import net.minecraft.recipe.Ingredient.StackEntry;
import net.minecraft.recipe.Ingredient.TagEntry;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu.WhitelistMode;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.ItemAttribute;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.util.MultiItemValue;

public class BlueprintItem extends Item {

	public BlueprintItem(Settings p_i48487_1_) {
		super(p_i48487_1_);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		Direction face = ctx.getSide();
		PlayerEntity player = ctx.getPlayer();
		ItemStack stack = ctx.getStack();
		BlockPos pos = ctx.getBlockPos()
				.offset(face);

		if (player != null && !player.canPlaceOn(pos, face, stack))
			return ActionResult.FAIL;

		World world = ctx.getWorld();
		AbstractDecorationEntity hangingentity = new BlueprintEntity(world, pos, face, face.getAxis()
				.isHorizontal() ? Direction.DOWN : ctx.getHorizontalPlayerFacing());
		NbtCompound compoundnbt = stack.getNbt();

		if (compoundnbt != null)
			EntityType.loadFromEntityNbt(world, player, hangingentity, compoundnbt);
		if (!hangingentity.canStayAttached())
			return ActionResult.CONSUME;
		if (!world.isClient) {
			hangingentity.onPlace();
			world.spawnEntity(hangingentity);
		}

		stack.decrement(1);
		return ActionResult.success(world.isClient);
	}

	protected boolean canPlace(PlayerEntity p_200127_1_, Direction p_200127_2_, ItemStack p_200127_3_,
							   BlockPos p_200127_4_) {
		return p_200127_1_.canPlaceOn(p_200127_4_, p_200127_2_, p_200127_3_);
	}

	public static void assignCompleteRecipe(World level, ItemStackHandler inv, Recipe<?> recipe) {
		DefaultedList<Ingredient> ingredients = recipe.getIngredients();

		for (int i = 0; i < 9; i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
		inv.setStackInSlot(9, recipe.getOutput(level.getRegistryManager()));

		if (recipe instanceof ShapedRecipe) {
			ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
			for (int row = 0; row < shapedRecipe.getHeight(); row++)
				for (int col = 0; col < shapedRecipe.getWidth(); col++)
					inv.setStackInSlot(row * 3 + col,
							convertIngredientToFilter(ingredients.get(row * shapedRecipe.getWidth() + col)));
		} else {
			for (int i = 0; i < ingredients.size(); i++)
				inv.setStackInSlot(i, convertIngredientToFilter(ingredients.get(i)));
		}
	}

	private static ItemStack convertIngredientToFilter(Ingredient ingredient) {
		Ingredient.Entry[] acceptedItems = ingredient.entries;
		if (acceptedItems == null || acceptedItems.length > 18)
			return ItemStack.EMPTY;
		if (acceptedItems.length == 0)
			return ItemStack.EMPTY;
		if (acceptedItems.length == 1)
			return convertIItemListToFilter(acceptedItems[0]);

		ItemStack result = AllItems.FILTER.asStack();
		ItemStackHandler filterItems = FilterItem.getFilterItems(result);
		for (int i = 0; i < acceptedItems.length; i++)
			filterItems.setStackInSlot(i, convertIItemListToFilter(acceptedItems[i]));
		result.getOrCreateNbt()
				.put("Items", filterItems.serializeNBT());
		return result;
	}

	private static ItemStack convertIItemListToFilter(Entry itemList) {
		Collection<ItemStack> stacks = itemList.getStacks();
		if (itemList instanceof StackEntry) {
			for (ItemStack itemStack : stacks)
				return itemStack;
		}

		if (itemList instanceof TagEntry) {
			Identifier resourcelocation = new Identifier(JsonHelper.getString(itemList.toJson(), "tag"));
			ItemStack filterItem = AllItems.ATTRIBUTE_FILTER.asStack();
			filterItem.getOrCreateNbt()
					.putInt("WhitelistMode", WhitelistMode.WHITELIST_DISJ.ordinal());
			NbtList attributes = new NbtList();
			ItemAttribute at = new ItemAttribute.InTag(TagKey.of(RegistryKeys.ITEM, resourcelocation));
			NbtCompound compoundNBT = new NbtCompound();
			at.serializeNBT(compoundNBT);
			compoundNBT.putBoolean("Inverted", false);
			attributes.add(compoundNBT);
			filterItem.getOrCreateNbt()
					.put("MatchedAttributes", attributes);
			return filterItem;
		}

		if (itemList instanceof MultiItemValue) {
			ItemStack result = AllItems.FILTER.asStack();
			ItemStackHandler filterItems = FilterItem.getFilterItems(result);
			int i = 0;
			for (ItemStack itemStack : stacks) {
				if (i >= 18)
					break;
				filterItems.setStackInSlot(i++, itemStack);
			}
			NbtCompound tag = result.getOrCreateNbt();
			tag.put("Items", filterItems.serializeNBT());
			tag.putBoolean("RespectNBT", true);
			return result;
		}

		return ItemStack.EMPTY;
	}

}
