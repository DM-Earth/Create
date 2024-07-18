package com.simibubi.create.foundation.data;

import static com.simibubi.create.Create.REGISTRATE;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.WoodType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.GlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.WindowBlock;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.GlassPaneCTBehaviour;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.tags.Tags;

public class WindowGen {

	private static Settings glassProperties(Settings p) {
		return p.allowsSpawning(WindowGen::never)
			.solidBlock(WindowGen::never)
			.suffocates(WindowGen::never)
			.blockVision(WindowGen::never);
	}

	private static boolean never(BlockState p_235436_0_, BlockView p_235436_1_, BlockPos p_235436_2_) {
		return false;
	}

	private static Boolean never(BlockState p_235427_0_, BlockView p_235427_1_, BlockPos p_235427_2_,
		EntityType<?> p_235427_3_) {
		return false;
	}

	public static BlockEntry<WindowBlock> woodenWindowBlock(WoodType woodType, Block planksBlock) {
		return woodenWindowBlock(woodType, planksBlock, () -> RenderLayer::getCutoutMipped, false);
	}

	public static BlockEntry<WindowBlock> customWindowBlock(String name, Supplier<? extends ItemConvertible> ingredient,
		Supplier<CTSpriteShiftEntry> ct, Supplier<Supplier<RenderLayer>> renderType, boolean translucent,
		Supplier<MapColor> color) {
		NonNullFunction<String, Identifier> end_texture = n -> Create.asResource(palettesDir() + name + "_end");
		NonNullFunction<String, Identifier> side_texture = n -> Create.asResource(palettesDir() + n);
		return windowBlock(name, ingredient, ct, renderType, translucent, end_texture, side_texture, color);
	}

	public static BlockEntry<WindowBlock> woodenWindowBlock(WoodType woodType, Block planksBlock,
		Supplier<Supplier<RenderLayer>> renderType, boolean translucent) {
		String woodName = woodType.name();
		String name = woodName + "_window";
		NonNullFunction<String, Identifier> end_texture =
			$ -> new Identifier("block/" + woodName + "_planks");
		NonNullFunction<String, Identifier> side_texture = n -> Create.asResource(palettesDir() + n);
		return windowBlock(name, () -> planksBlock, () -> AllSpriteShifts.getWoodenWindow(woodType), renderType,
			translucent, end_texture, side_texture, planksBlock::getDefaultMapColor);
	}

	public static BlockEntry<WindowBlock> windowBlock(String name, Supplier<? extends ItemConvertible> ingredient,
		Supplier<CTSpriteShiftEntry> ct, Supplier<Supplier<RenderLayer>> renderType, boolean translucent,
		NonNullFunction<String, Identifier> endTexture, NonNullFunction<String, Identifier> sideTexture,
		Supplier<MapColor> color) {
		return REGISTRATE.block(name, p -> new WindowBlock(p, translucent))
			.onRegister(connectedTextures(() -> new HorizontalCTBehaviour(ct.get())))
			.addLayer(renderType)
			.recipe((c, p) -> ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, c.get(), 2)
				.pattern(" # ")
				.pattern("#X#")
				.input('#', ingredient.get())
				.input('X', DataIngredient.tag(Tags.Items.GLASS_COLORLESS))
				.criterion("has_ingredient", RegistrateRecipeProvider.conditionsFromItem(ingredient.get()))
				.offerTo(p::accept))
			.initialProperties(() -> Blocks.GLASS)
			.properties(WindowGen::glassProperties)
			.properties(p -> p.mapColor(color.get()))
			.loot((t, g) -> t.addDropWithSilkTouch(g))
			.blockstate((c, p) -> p.simpleBlock(c.get(), p.models()
				.cubeColumn(c.getName(), sideTexture.apply(c.getName()), endTexture.apply(c.getName()))))
			.tag(BlockTags.IMPERMEABLE)
			.simpleItem()
			.register();
	}

	public static BlockEntry<ConnectedGlassBlock> framedGlass(String name,
		Supplier<ConnectedTextureBehaviour> behaviour) {
		return REGISTRATE.block(name, ConnectedGlassBlock::new)
			.onRegister(connectedTextures(behaviour))
			.addLayer(() -> RenderLayer::getCutout)
			.initialProperties(() -> Blocks.GLASS)
			.properties(WindowGen::glassProperties)
			.loot((t, g) -> t.addDropWithSilkTouch(g))
			.recipe((c, p) -> p.stonecutting(DataIngredient.tag(Tags.Items.GLASS_COLORLESS),
				RecipeCategory.BUILDING_BLOCKS, c::get))
			.blockstate((c, p) -> BlockStateGen.cubeAll(c, p, "palettes/", "framed_glass"))
			.tag(Tags.Blocks.GLASS_COLORLESS, BlockTags.IMPERMEABLE)
			.item()
			.tag(Tags.Items.GLASS_COLORLESS)
			.model((c, p) -> p.cubeColumn(c.getName(), p.modLoc(palettesDir() + c.getName()),
				p.modLoc("block/palettes/framed_glass")))
			.build()
			.register();
	}

	public static BlockEntry<ConnectedGlassPaneBlock> framedGlassPane(String name, Supplier<? extends Block> parent,
		Supplier<CTSpriteShiftEntry> ctshift) {
		Identifier sideTexture = Create.asResource(palettesDir() + "framed_glass");
		Identifier itemSideTexture = Create.asResource(palettesDir() + name);
		Identifier topTexture = Create.asResource(palettesDir() + "framed_glass_pane_top");
		Supplier<Supplier<RenderLayer>> renderType = () -> RenderLayer::getCutoutMipped;
		return connectedGlassPane(name, parent, ctshift, sideTexture, itemSideTexture, topTexture, renderType);
	}

	public static BlockEntry<ConnectedGlassPaneBlock> customWindowPane(String name, Supplier<? extends Block> parent,
		Supplier<CTSpriteShiftEntry> ctshift, Supplier<Supplier<RenderLayer>> renderType) {
		Identifier topTexture = Create.asResource(palettesDir() + name + "_pane_top");
		Identifier sideTexture = Create.asResource(palettesDir() + name);
		return connectedGlassPane(name, parent, ctshift, sideTexture, sideTexture, topTexture, renderType);
	}

	public static BlockEntry<ConnectedGlassPaneBlock> woodenWindowPane(WoodType woodType,
		Supplier<? extends Block> parent) {
		return woodenWindowPane(woodType, parent, () -> RenderLayer::getCutoutMipped);
	}

	public static BlockEntry<ConnectedGlassPaneBlock> woodenWindowPane(WoodType woodType,
		Supplier<? extends Block> parent, Supplier<Supplier<RenderLayer>> renderType) {
		String woodName = woodType.name();
		String name = woodName + "_window";
		Identifier topTexture = new Identifier("block/" + woodName + "_planks");
		Identifier sideTexture = Create.asResource(palettesDir() + name);
		return connectedGlassPane(name, parent, () -> AllSpriteShifts.getWoodenWindow(woodType), sideTexture,
			sideTexture, topTexture, renderType);
	}

	public static BlockEntry<GlassPaneBlock> standardGlassPane(String name, Supplier<? extends Block> parent,
		Identifier sideTexture, Identifier topTexture, Supplier<Supplier<RenderLayer>> renderType) {
		NonNullBiConsumer<DataGenContext<Block, GlassPaneBlock>, RegistrateBlockstateProvider> stateProvider =
			(c, p) -> p.paneBlock(c.get(), sideTexture, topTexture);
		return glassPane(name, parent, sideTexture, topTexture, GlassPaneBlock::new, renderType, $ -> {
		}, stateProvider);
	}

	private static BlockEntry<ConnectedGlassPaneBlock> connectedGlassPane(String name, Supplier<? extends Block> parent,
		Supplier<CTSpriteShiftEntry> ctshift, Identifier sideTexture, Identifier itemSideTexture,
		Identifier topTexture, Supplier<Supplier<RenderLayer>> renderType) {
		NonNullConsumer<? super ConnectedGlassPaneBlock> connectedTextures =
			connectedTextures(() -> new GlassPaneCTBehaviour(ctshift.get()));
		String CGPparents = "block/connected_glass_pane/";
		String prefix = name + "_pane_";

		Function<RegistrateBlockstateProvider, ModelFile> post =
			getPaneModelProvider(CGPparents, prefix, "post", sideTexture, topTexture),
			side = getPaneModelProvider(CGPparents, prefix, "side", sideTexture, topTexture),
			sideAlt = getPaneModelProvider(CGPparents, prefix, "side_alt", sideTexture, topTexture),
			noSide = getPaneModelProvider(CGPparents, prefix, "noside", sideTexture, topTexture),
			noSideAlt = getPaneModelProvider(CGPparents, prefix, "noside_alt", sideTexture, topTexture);

		NonNullBiConsumer<DataGenContext<Block, ConnectedGlassPaneBlock>, RegistrateBlockstateProvider> stateProvider =
			(c, p) -> p.paneBlock(c.get(), post.apply(p), side.apply(p), sideAlt.apply(p), noSide.apply(p),
				noSideAlt.apply(p));

		return glassPane(name, parent, itemSideTexture, topTexture, ConnectedGlassPaneBlock::new, renderType,
			connectedTextures, stateProvider);
	}

	private static Function<RegistrateBlockstateProvider, ModelFile> getPaneModelProvider(String CGPparents,
		String prefix, String partial, Identifier sideTexture, Identifier topTexture) {
		return p -> p.models()
			.withExistingParent(prefix + partial, Create.asResource(CGPparents + partial))
			.texture("pane", sideTexture)
			.texture("edge", topTexture);
	}

	private static <G extends GlassPaneBlock> BlockEntry<G> glassPane(String name, Supplier<? extends Block> parent,
		Identifier sideTexture, Identifier topTexture, NonNullFunction<Settings, G> factory,
		Supplier<Supplier<RenderLayer>> renderType, NonNullConsumer<? super G> connectedTextures,
		NonNullBiConsumer<DataGenContext<Block, G>, RegistrateBlockstateProvider> stateProvider) {
		name += "_pane";

		return REGISTRATE.block(name, factory)
			.onRegister(connectedTextures)
			.addLayer(renderType)
			.initialProperties(() -> Blocks.GLASS_PANE)
			.properties(p -> p.mapColor(parent.get()
				.getDefaultMapColor()))
			.blockstate(stateProvider)
			.recipe((c, p) -> ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, c.get(), 16)
				.pattern("###")
				.pattern("###")
				.input('#', parent.get())
				.criterion("has_ingredient", RegistrateRecipeProvider.conditionsFromItem(parent.get()))
				.offerTo(p::accept))
			.tag(Tags.Blocks.GLASS_PANES)
			.loot((t, g) -> t.addDropWithSilkTouch(g))
			.item()
			.tag(Tags.Items.GLASS_PANES)
			.model((c, p) -> p.withExistingParent(c.getName(), Create.asResource("item/pane"))
				.texture("pane", sideTexture)
				.texture("edge", topTexture))
			.build()
			.register();
	}

	private static String palettesDir() {
		return "block/palettes/";
	}

}
