package com.simibubi.create.content.decoration.palettes;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.Arrays;
import java.util.function.Supplier;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonnullType;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;

public abstract class PaletteBlockPartial<B extends Block> {

	public static final PaletteBlockPartial<StairsBlock> STAIR = new Stairs();
	public static final PaletteBlockPartial<SlabBlock> SLAB = new Slab(false);
	public static final PaletteBlockPartial<SlabBlock> UNIQUE_SLAB = new Slab(true);
	public static final PaletteBlockPartial<WallBlock> WALL = new Wall();

	public static final PaletteBlockPartial<?>[] ALL_PARTIALS = { STAIR, SLAB, WALL };
	public static final PaletteBlockPartial<?>[] FOR_POLISHED = { STAIR, UNIQUE_SLAB, WALL };

	private String name;

	private PaletteBlockPartial(String name) {
		this.name = name;
	}

	public @NonnullType BlockBuilder<B, CreateRegistrate> create(String variantName, PaletteBlockPattern pattern,
		BlockEntry<? extends Block> block, AllPaletteStoneTypes variant) {
		String patternName = Lang.nonPluralId(pattern.createName(variantName));
		String blockName = patternName + "_" + this.name;

		BlockBuilder<B, CreateRegistrate> blockBuilder = Create.REGISTRATE
			.block(blockName, p -> createBlock(block))
			.blockstate((c, p) -> generateBlockState(c, p, variantName, pattern, block))
			.recipe((c, p) -> createRecipes(variant, block, c, p))
			.transform(b -> transformBlock(b, variantName, pattern));

		ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> itemBuilder = blockBuilder.item()
			.transform(b -> transformItem(b, variantName, pattern));

		if (canRecycle())
			itemBuilder.tag(variant.materialTag);

		return itemBuilder.build();
	}

	protected Identifier getTexture(String variantName, PaletteBlockPattern pattern, int index) {
		return PaletteBlockPattern.toLocation(variantName, pattern.getTexture(index));
	}

	protected BlockBuilder<B, CreateRegistrate> transformBlock(BlockBuilder<B, CreateRegistrate> builder,
		String variantName, PaletteBlockPattern pattern) {
		getBlockTags().forEach(builder::tag);
		return builder.transform(pickaxeOnly());
	}

	protected ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> transformItem(
		ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> builder, String variantName,
		PaletteBlockPattern pattern) {
		getItemTags().forEach(builder::tag);
		return builder;
	}

	protected boolean canRecycle() {
		return true;
	}

	protected abstract Iterable<TagKey<Block>> getBlockTags();

	protected abstract Iterable<TagKey<Item>> getItemTags();

	protected abstract B createBlock(Supplier<? extends Block> block);

	protected abstract void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock, DataGenContext<Block, ? extends Block> c,
		RegistrateRecipeProvider p);

	protected abstract void generateBlockState(DataGenContext<Block, B> ctx, RegistrateBlockstateProvider prov,
		String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block);

	private static class Stairs extends PaletteBlockPartial<StairsBlock> {

		public Stairs() {
			super("stairs");
		}

		@Override
		protected StairsBlock createBlock(Supplier<? extends Block> block) {
			return new StairsBlock(block.get().getDefaultState(), Settings.copy(block.get()));
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, StairsBlock> ctx, RegistrateBlockstateProvider prov,
			String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block) {
			prov.stairsBlock(ctx.get(), getTexture(variantName, pattern, 0));
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockTags.STAIRS);
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(ItemTags.STAIRS);
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
			DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.stairs(DataIngredient.items(patternBlock.get()), category, c::get, c.getName(), false);
			p.stonecutting(DataIngredient.tag(type.materialTag), category, c::get, 1);
		}

	}

	private static class Slab extends PaletteBlockPartial<SlabBlock> {

		private boolean customSide;

		public Slab(boolean customSide) {
			super("slab");
			this.customSide = customSide;
		}

		@Override
		protected SlabBlock createBlock(Supplier<? extends Block> block) {
			return new SlabBlock(Settings.copy(block.get()));
		}

		@Override
		protected boolean canRecycle() {
			return false;
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, SlabBlock> ctx, RegistrateBlockstateProvider prov,
			String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block) {
			String name = ctx.getName();
			Identifier mainTexture = getTexture(variantName, pattern, 0);
			Identifier sideTexture = customSide ? getTexture(variantName, pattern, 1) : mainTexture;

			ModelFile bottom = prov.models()
				.slab(name, sideTexture, mainTexture, mainTexture);
			ModelFile top = prov.models()
				.slabTop(name + "_top", sideTexture, mainTexture, mainTexture);
			ModelFile doubleSlab;

			if (customSide) {
				doubleSlab = prov.models()
					.cubeColumn(name + "_double", sideTexture, mainTexture);
			} else {
				doubleSlab = prov.models()
					.getExistingFile(prov.modLoc(pattern.createName(variantName)));
			}

			prov.slabBlock(ctx.get(), bottom, top, doubleSlab);
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockTags.SLABS);
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(ItemTags.SLABS);
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
			DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.slab(DataIngredient.items(patternBlock.get()), category, c::get, c.getName(), false);
			p.stonecutting(DataIngredient.tag(type.materialTag), category, c::get, 2);
			DataIngredient ingredient = DataIngredient.items(c.get());
			ShapelessRecipeJsonBuilder.create(category, patternBlock.get())
				.input(ingredient)
				.input(ingredient)
				.criterion("has_" + c.getName(), ingredient.getCritereon(p))
				.offerTo(p, Create.ID + ":" + c.getName() + "_recycling");
		}

		@Override
		protected BlockBuilder<SlabBlock, CreateRegistrate> transformBlock(
				BlockBuilder<SlabBlock, CreateRegistrate> builder,
				String variantName, PaletteBlockPattern pattern) {
			builder.loot((lt, block) -> lt.addDrop(block, lt.slabDrops(block)));
			return super.transformBlock(builder, variantName, pattern);
		}

	}

	private static class Wall extends PaletteBlockPartial<WallBlock> {

		public Wall() {
			super("wall");
		}

		@Override
		protected WallBlock createBlock(Supplier<? extends Block> block) {
			return new WallBlock(Settings.copy(block.get()).solid());
		}

		@Override
		protected ItemBuilder<BlockItem, BlockBuilder<WallBlock, CreateRegistrate>> transformItem(
			ItemBuilder<BlockItem, BlockBuilder<WallBlock, CreateRegistrate>> builder, String variantName,
			PaletteBlockPattern pattern) {
			builder.model((c, p) -> p.wallInventory(c.getName(), getTexture(variantName, pattern, 0)));
			return super.transformItem(builder, variantName, pattern);
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, WallBlock> ctx, RegistrateBlockstateProvider prov,
			String variantName, PaletteBlockPattern pattern, Supplier<? extends Block> block) {
			prov.wallBlock(ctx.get(), pattern.createName(variantName), getTexture(variantName, pattern, 0));
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockTags.WALLS);
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(ItemTags.WALLS);
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
			DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.stonecutting(DataIngredient.tag(type.materialTag), category, c::get, 1);
			DataIngredient ingredient = DataIngredient.items(patternBlock.get());
			ShapedRecipeJsonBuilder.create(category, c.get(), 6)
				.pattern("XXX")
				.pattern("XXX")
				.input('X', ingredient)
				.criterion("has_" + p.safeName(ingredient), ingredient.getCritereon(p))
				.offerTo(p, p.safeId(c.get()));
		}

	}

}