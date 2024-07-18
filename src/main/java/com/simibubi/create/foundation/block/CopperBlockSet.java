package com.simibubi.create.foundation.block;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.block.OxidizableBlock;
import net.minecraft.block.OxidizableSlabBlock;
import net.minecraft.block.OxidizableStairsBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelProvider;

import org.apache.commons.lang3.ArrayUtils;

import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

public class CopperBlockSet {
	protected static final OxidationLevel[] WEATHER_STATES = OxidationLevel.values();
	protected static final int WEATHER_STATE_COUNT = WEATHER_STATES.length;

	protected static final Map<OxidationLevel, Block> BASE_BLOCKS = new EnumMap<>(OxidationLevel.class);
	static {
		BASE_BLOCKS.put(OxidationLevel.UNAFFECTED, Blocks.COPPER_BLOCK);
		BASE_BLOCKS.put(OxidationLevel.EXPOSED, Blocks.EXPOSED_COPPER);
		BASE_BLOCKS.put(OxidationLevel.WEATHERED, Blocks.WEATHERED_COPPER);
		BASE_BLOCKS.put(OxidationLevel.OXIDIZED, Blocks.OXIDIZED_COPPER);
	}

	public static final Variant<?>[] DEFAULT_VARIANTS =
		new Variant<?>[] { BlockVariant.INSTANCE, SlabVariant.INSTANCE, StairVariant.INSTANCE };

	protected final String name;
	protected final String generalDirectory; // Leave empty for root folder
	protected final Variant<?>[] variants;
	protected final Map<Variant<?>, BlockEntry<?>[]> entries = new HashMap<>();
	protected final NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe;
	protected final String endTextureName;

	public CopperBlockSet(AbstractRegistrate<?> registrate, String name, String endTextureName, Variant<?>[] variants) {
		this(registrate, name, endTextureName, variants, NonNullBiConsumer.noop(), "copper/");
	}

	public CopperBlockSet(AbstractRegistrate<?> registrate, String name, String endTextureName, Variant<?>[] variants, String generalDirectory) {
		this(registrate, name, endTextureName, variants, NonNullBiConsumer.noop(), generalDirectory);
	}

	public CopperBlockSet(AbstractRegistrate<?> registrate, String name, String endTextureName, Variant<?>[] variants, NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe) {
		this(registrate, name, endTextureName, variants, mainBlockRecipe, "copper/");
	}

	public CopperBlockSet(AbstractRegistrate<?> registrate, String name, String endTextureName, Variant<?>[] variants,
		NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe, String generalDirectory) {
		this.name = name;
		this.generalDirectory = generalDirectory;
		this.endTextureName = endTextureName;
		this.variants = variants;
		this.mainBlockRecipe = mainBlockRecipe;
		for (boolean waxed : Iterate.falseAndTrue) {
			for (Variant<?> variant : this.variants) {
				BlockEntry<?>[] entries =
					waxed ? this.entries.get(variant) : new BlockEntry<?>[WEATHER_STATE_COUNT * 2];
				for (OxidationLevel state : WEATHER_STATES) {
					int index = getIndex(state, waxed);
					BlockEntry<?> entry = createEntry(registrate, variant, state, waxed);
					entries[index] = entry;

					if (waxed) {
						CopperRegistries.addWaxable(() -> entries[getIndex(state, false)].get(), () -> entry.get());
					} else if (state != OxidationLevel.UNAFFECTED) {
						CopperRegistries.addWeathering(
							() -> entries[getIndex(WEATHER_STATES[state.ordinal() - 1], false)].get(),
							() -> entry.get());
					}
				}
				if (!waxed)
					this.entries.put(variant, entries);
			}
		}
	}

	protected <T extends Block> BlockEntry<?> createEntry(AbstractRegistrate<?> registrate, Variant<T> variant,
		OxidationLevel state, boolean waxed) {
		String name = "";
		if (waxed) {
			name += "waxed_";
		}
		name += getWeatherStatePrefix(state);
		name += this.name;

		String suffix = variant.getSuffix();
		if (!suffix.equals(""))
			name = Lang.nonPluralId(name);

		name += suffix;

		Block baseBlock = BASE_BLOCKS.get(state);
		BlockBuilder<T, ?> builder = registrate.block(name, variant.getFactory(this, state, waxed))
			.initialProperties(() -> baseBlock)
			.loot((lt, block) -> variant.generateLootTable(lt, block, this, state, waxed))
			.blockstate((ctx, prov) -> variant.generateBlockState(ctx, prov, this, state, waxed))
			.recipe((c, p) -> variant.generateRecipes(entries.get(BlockVariant.INSTANCE)[state.ordinal()], c, p))
			.transform(TagGen.pickaxeOnly())
			.tag(BlockTags.NEEDS_STONE_TOOL)
			.simpleItem();

		if (variant == BlockVariant.INSTANCE && state == OxidationLevel.UNAFFECTED)
			builder.recipe((c, p) -> mainBlockRecipe.accept(c, p));

		if (waxed) {
			builder.recipe((ctx, prov) -> {
				Block unwaxed = get(variant, state, false).get();
				ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ctx.get())
					.input(unwaxed)
					.input(Items.HONEYCOMB)
					.criterion("has_unwaxed", RegistrateRecipeProvider.conditionsFromItem(unwaxed))
					.offerTo(prov, new Identifier(ctx.getId()
						.getNamespace(), "crafting/" + generalDirectory + ctx.getName() + "_from_honeycomb"));
			});
		}

		return builder.register();
	}

	protected int getIndex(OxidationLevel state, boolean waxed) {
		return state.ordinal() + (waxed ? WEATHER_STATE_COUNT : 0);
	}

	public String getName() {
		return name;
	}

	public String getEndTextureName() {
		return endTextureName;
	}

	public Variant<?>[] getVariants() {
		return variants;
	}

	public boolean hasVariant(Variant<?> variant) {
		return ArrayUtils.contains(variants, variant);
	}

	public BlockEntry<?> get(Variant<?> variant, OxidationLevel state, boolean waxed) {
		BlockEntry<?>[] entries = this.entries.get(variant);
		if (entries != null) {
			return entries[getIndex(state, waxed)];
		}
		return null;
	}

	public BlockEntry<?> getStandard() {
		return get(BlockVariant.INSTANCE, OxidationLevel.UNAFFECTED, false);
	}

	public static String getWeatherStatePrefix(OxidationLevel state) {
		if (state != OxidationLevel.UNAFFECTED) {
			return state.name()
				.toLowerCase(Locale.ROOT) + "_";
		}
		return "";
	}

	public interface Variant<T extends Block> {
		String getSuffix();

		NonNullFunction<Settings, T> getFactory(CopperBlockSet blocks, OxidationLevel state, boolean waxed);

		default void generateLootTable(RegistrateBlockLootTables lootTable, T block, CopperBlockSet blocks,
			OxidationLevel state, boolean waxed) {
			lootTable.addDrop(block);
		}

		void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, T> ctx, RegistrateRecipeProvider prov);

		void generateBlockState(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, CopperBlockSet blocks,
			OxidationLevel state, boolean waxed);
	}

	public static class BlockVariant implements Variant<Block> {
		public static final BlockVariant INSTANCE = new BlockVariant();

		protected BlockVariant() {}

		@Override
		public String getSuffix() {
			return "";
		}

		@Override
		public NonNullFunction<Settings, Block> getFactory(CopperBlockSet blocks, OxidationLevel state, boolean waxed) {
			if (waxed) {
				return Block::new;
			} else {
				return p -> new OxidizableBlock(state, p);
			}
		}

		@Override
		public void generateBlockState(DataGenContext<Block, Block> ctx, RegistrateBlockstateProvider prov,
			CopperBlockSet blocks, OxidationLevel state, boolean waxed) {
			Block block = ctx.get();
			String path = RegisteredObjects.getKeyOrThrow(block)
				.getPath();
			String baseLoc = ModelProvider.BLOCK_FOLDER + "/" + blocks.generalDirectory + getWeatherStatePrefix(state);

			Identifier texture = prov.modLoc(baseLoc + blocks.getName());
			if (Objects.equals(blocks.getName(), blocks.getEndTextureName())) {
				// End texture and base texture are equal, so we should use cube_all.
				prov.simpleBlock(block, prov.models().cubeAll(path, texture));
			} else {
				// End texture and base texture aren't equal, so we should use cube_column.
				Identifier endTexture = prov.modLoc(baseLoc + blocks.getEndTextureName());
				prov.simpleBlock(block, prov.models()
						.cubeColumn(path, texture, endTexture));
			}

		}

		@Override
		public void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, Block> ctx,
			RegistrateRecipeProvider prov) {}

	}

	public static class SlabVariant implements Variant<SlabBlock> {
		public static final SlabVariant INSTANCE = new SlabVariant();

		protected SlabVariant() {}

		@Override
		public String getSuffix() {
			return "_slab";
		}

		@Override
		public NonNullFunction<Settings, SlabBlock> getFactory(CopperBlockSet blocks, OxidationLevel state,
			boolean waxed) {
			if (waxed) {
				return SlabBlock::new;
			} else {
				return p -> new OxidizableSlabBlock(state, p);
			}
		}

		@Override
		public void generateLootTable(RegistrateBlockLootTables lootTable, SlabBlock block, CopperBlockSet blocks,
			OxidationLevel state, boolean waxed) {
			lootTable.addDrop(block, lootTable.slabDrops(block));
		}

		@Override
		public void generateBlockState(DataGenContext<Block, SlabBlock> ctx, RegistrateBlockstateProvider prov,
			CopperBlockSet blocks, OxidationLevel state, boolean waxed) {
			Identifier fullModel =
				prov.modLoc(ModelProvider.BLOCK_FOLDER + "/" + getWeatherStatePrefix(state) + blocks.getName());

			String baseLoc = ModelProvider.BLOCK_FOLDER + "/" + blocks.generalDirectory + getWeatherStatePrefix(state);
			Identifier texture = prov.modLoc(baseLoc + blocks.getName());
			Identifier endTexture = prov.modLoc(baseLoc + blocks.getEndTextureName());

			prov.slabBlock(ctx.get(), fullModel, texture, endTexture, endTexture);
		}

		@Override
		public void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, SlabBlock> ctx,
			RegistrateRecipeProvider prov) {
			prov.slab(DataIngredient.items(blockVariant.get()), RecipeCategory.BUILDING_BLOCKS, ctx::get, null, true);
		}
	}

	public static class StairVariant implements Variant<StairsBlock> {
		public static final StairVariant INSTANCE = new StairVariant(BlockVariant.INSTANCE);

		protected final Variant<?> parent;

		protected StairVariant(Variant<?> parent) {
			this.parent = parent;
		}

		@Override
		public String getSuffix() {
			return "_stairs";
		}

		@Override
		public NonNullFunction<Settings, StairsBlock> getFactory(CopperBlockSet blocks, OxidationLevel state,
			boolean waxed) {
			if (!blocks.hasVariant(parent)) {
				throw new IllegalStateException(
					"Cannot add StairVariant '" + toString() + "' without parent Variant '" + parent.toString() + "'!");
			}
			Supplier<BlockState> defaultStateSupplier = () -> blocks.get(parent, state, waxed)
				.getDefaultState();
			if (waxed) {
				return p -> new StairsBlock(defaultStateSupplier.get(), p);
			} else {
				return p -> {
					OxidizableStairsBlock block =
						new OxidizableStairsBlock(state, Blocks.AIR.getDefaultState(), p);
					// WeatheringCopperStairBlock does not have a constructor that takes a Supplier,
					// so setting the field directly is the easiest solution
					// fabric: unnecessary
//					ObfuscationReflectionHelper.setPrivateValue(StairBlock.class, block, defaultStateSupplier,
//						"stateSupplier");
					return block;
				};
			}
		}

		@Override
		public void generateBlockState(DataGenContext<Block, StairsBlock> ctx, RegistrateBlockstateProvider prov,
			CopperBlockSet blocks, OxidationLevel state, boolean waxed) {
			String baseLoc = ModelProvider.BLOCK_FOLDER + "/" + blocks.generalDirectory + getWeatherStatePrefix(state);
			Identifier texture = prov.modLoc(baseLoc + blocks.getName());
			Identifier endTexture = prov.modLoc(baseLoc + blocks.getEndTextureName());
			prov.stairsBlock(ctx.get(), texture, endTexture, endTexture);
		}

		@Override
		public void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, StairsBlock> ctx,
			RegistrateRecipeProvider prov) {
			prov.stairs(DataIngredient.items(blockVariant.get()), RecipeCategory.BUILDING_BLOCKS, ctx::get, null, true);
		}
	}
}
