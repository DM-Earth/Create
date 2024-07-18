package com.simibubi.create.content.processing.sequenced;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class SequencedAssemblyRecipe implements Recipe<Inventory> {

	protected Identifier id;
	protected SequencedAssemblyRecipeSerializer serializer;

	protected Ingredient ingredient;
	protected DefaultedList<Ingredient> ingredientList;
	protected List<SequencedRecipe<?>> sequence;
	protected int loops;
	protected ProcessingOutput transitionalItem;

	public final List<ProcessingOutput> resultPool;

	public SequencedAssemblyRecipe(Identifier recipeId, SequencedAssemblyRecipeSerializer serializer) {
		this.id = recipeId;
		this.serializer = serializer;
		sequence = new ArrayList<>();
		resultPool = new ArrayList<>();
		loops = 5;
	}

	public static <C extends Inventory, R extends ProcessingRecipe<C>> Optional<R> getRecipe(World world, C inv,
		RecipeType<R> type, Class<R> recipeClass) {
		return getRecipe(world, inv, type, recipeClass, r -> r.matches(inv, world));
	}

	public static <C extends Inventory, R extends ProcessingRecipe<C>> Optional<R> getRecipe(World world, C inv,
		RecipeType<R> type, Class<R> recipeClass, Predicate<? super R> recipeFilter) {
		return getRecipes(world, inv.getStack(0), type, recipeClass).filter(recipeFilter)
			.findFirst();
	}

	public static <R extends ProcessingRecipe<?>> Optional<R> getRecipe(World world, ItemStack item,
		RecipeType<R> type, Class<R> recipeClass) {
		List<SequencedAssemblyRecipe> all = world.getRecipeManager()
			.listAllOfType(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType());
		for (SequencedAssemblyRecipe sequencedAssemblyRecipe : all) {
			if (!sequencedAssemblyRecipe.appliesTo(item))
				continue;
			SequencedRecipe<?> nextRecipe = sequencedAssemblyRecipe.getNextRecipe(item);
			ProcessingRecipe<?> recipe = nextRecipe.getRecipe();
			if (recipe.getType() != type || !recipeClass.isInstance(recipe))
				continue;
			recipe.enforceNextResult(() -> sequencedAssemblyRecipe.advance(item));
			return Optional.of(recipeClass.cast(recipe));
		}
		return Optional.empty();
	}

	public static <R extends ProcessingRecipe<?>> Stream<R> getRecipes(World world, ItemStack item,
		RecipeType<R> type, Class<R> recipeClass) {
		List<SequencedAssemblyRecipe> all = world.getRecipeManager()
			.listAllOfType(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType());

		return all.stream()
				.filter(it -> it.appliesTo(item))
				.map(it -> Pair.of(it, it.getNextRecipe(item).getRecipe()))
				.filter(it -> it.getSecond()
						.getType() == type && recipeClass.isInstance(it.getSecond()))
				.map(it -> {
					it.getSecond()
							.enforceNextResult(() -> it.getFirst().advance(item));
					return it.getSecond();
				})
				.map(recipeClass::cast);
	}

	private ItemStack advance(ItemStack input) {
		int step = getStep(input);
		if ((step + 1) / sequence.size() >= loops)
			return rollResult();

		ItemStack advancedItem = ItemHandlerHelper.copyStackWithSize(getTransitionalItem(), 1);
		NbtCompound itemTag = advancedItem.getOrCreateNbt();
		NbtCompound tag = new NbtCompound();
		tag.putString("id", id.toString());
		tag.putInt("Step", step + 1);
		tag.putFloat("Progress", (step + 1f) / (sequence.size() * loops));
		itemTag.put("SequencedAssembly", tag);
		advancedItem.setNbt(itemTag);
		return advancedItem;
	}

	public int getLoops() {
		return loops;
	}

	public void addAdditionalIngredientsAndMachines(List<Ingredient> list) {
		sequence.forEach(sr -> sr.getAsAssemblyRecipe()
			.addAssemblyIngredients(list));
		Set<ItemConvertible> machines = new HashSet<>();
		sequence.forEach(sr -> sr.getAsAssemblyRecipe()
			.addRequiredMachines(machines));
		machines.stream()
			.map(Ingredient::ofItems)
			.forEach(list::add);
	}

	public void addAdditionalFluidIngredients(List<FluidIngredient> list) {
		sequence.forEach(sr -> sr.getAsAssemblyRecipe()
			.addAssemblyFluidIngredients(list));
	}

	private ItemStack rollResult() {
		float totalWeight = 0;
		for (ProcessingOutput entry : resultPool)
			totalWeight += entry.getChance();
		float number = Create.RANDOM.nextFloat() * totalWeight;
		for (ProcessingOutput entry : resultPool) {
			number -= entry.getChance();
			if (number < 0)
				return entry.getStack()
					.copy();
		}
		return ItemStack.EMPTY;
	}

	private boolean appliesTo(ItemStack input) {
		if (ingredient.test(input))
			return true;
		if (input.hasNbt()) {
			if (getTransitionalItem().getItem() == input.getItem()) {
				if (input.getNbt().contains("SequencedAssembly")) {
					NbtCompound tag = input.getNbt().getCompound("SequencedAssembly");
					String id = tag.getString("id");
					return id.equals(this.id.toString());
				}
			}
		}
		return false;
	}

	private SequencedRecipe<?> getNextRecipe(ItemStack input) {
		return sequence.get(getStep(input) % sequence.size());
	}

	private int getStep(ItemStack input) {
		if (!input.hasNbt())
			return 0;
		NbtCompound tag = input.getNbt();
		if (!tag.contains("SequencedAssembly"))
			return 0;
		int step = tag.getCompound("SequencedAssembly")
			.getInt("Step");
		return step;
	}

	@Override
	public boolean matches(Inventory inv, World p_77569_2_) {
		return false;
	}

	@Override
	public ItemStack craft(Inventory inv, DynamicRegistryManager registryAccess) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean fits(int p_194133_1_, int p_194133_2_) {
		return false;
	}

	@Override
	public ItemStack getOutput(DynamicRegistryManager registryAccess) {
		return resultPool.get(0)
			.getStack();
	}

	public float getOutputChance() {
		float totalWeight = 0;
		for (ProcessingOutput entry : resultPool)
			totalWeight += entry.getChance();
		return resultPool.get(0)
			.getChance() / totalWeight;
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return serializer;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	@Override
	public RecipeType<?> getType() {
		return AllRecipeTypes.SEQUENCED_ASSEMBLY.getType();
	}

	@Environment(EnvType.CLIENT)
	public static void addToTooltip(ItemStack stack, List<Text> tooltip) {
		if (!stack.hasNbt() || !stack.getNbt()
			.contains("SequencedAssembly"))
			return;
		NbtCompound compound = stack.getNbt()
			.getCompound("SequencedAssembly");
		Identifier resourceLocation = new Identifier(compound.getString("id"));
		Optional<? extends Recipe<?>> optionalRecipe = MinecraftClient.getInstance().world.getRecipeManager()
			.get(resourceLocation);
		if (!optionalRecipe.isPresent())
			return;
		Recipe<?> recipe = optionalRecipe.get();
		if (!(recipe instanceof SequencedAssemblyRecipe))
			return;

		SequencedAssemblyRecipe sequencedAssemblyRecipe = (SequencedAssemblyRecipe) recipe;
		int length = sequencedAssemblyRecipe.sequence.size();
		int step = sequencedAssemblyRecipe.getStep(stack);
		int total = length * sequencedAssemblyRecipe.loops;
		tooltip.add(Components.immutableEmpty());
		tooltip.add(Lang.translateDirect("recipe.sequenced_assembly")
			.formatted(Formatting.GRAY));
		tooltip.add(Lang.translateDirect("recipe.assembly.progress", step, total)
			.formatted(Formatting.DARK_GRAY));

		int remaining = total - step;
		for (int i = 0; i < length; i++) {
			if (i >= remaining)
				break;
			SequencedRecipe<?> sequencedRecipe = sequencedAssemblyRecipe.sequence.get((i + step) % length);
			Text textComponent = sequencedRecipe.getAsAssemblyRecipe()
				.getDescriptionForAssembly();
			if (i == 0)
				tooltip.add(Lang.translateDirect("recipe.assembly.next", textComponent)
					.formatted(Formatting.AQUA));
			else
				tooltip.add(Components.literal("-> ").append(textComponent)
					.formatted(Formatting.DARK_AQUA));
		}

	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	@Override
	public DefaultedList<Ingredient> getIngredients() {
		if (ingredientList == null) {
			ingredientList = DefaultedList.of();
			ingredientList.add(ingredient);
			for (SequencedRecipe<?> recipe : this.sequence) {
				ingredientList.addAll(recipe.getRecipe().getIngredients());
			}
		}
		return ingredientList;
	}

	public List<SequencedRecipe<?>> getSequence() {
		return sequence;
	}

	public ItemStack getTransitionalItem() {
		return transitionalItem.getStack();
	}

}
