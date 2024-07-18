package com.simibubi.create.foundation.data;

import static com.simibubi.create.Create.REGISTRATE;
import static net.minecraft.state.property.Properties.EAST;
import static net.minecraft.state.property.Properties.NORTH;
import static net.minecraft.state.property.Properties.SOUTH;
import static net.minecraft.state.property.Properties.WEST;

import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.PaneBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;

public class MetalBarsGen {

	public static <P extends PaneBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> barsBlockState(
		String name, boolean specialEdge) {
		return (c, p) -> {

			ModelFile post_ends = barsSubModel(p, name, "post_ends", specialEdge);
			ModelFile post = barsSubModel(p, name, "post", specialEdge);
			ModelFile cap = barsSubModel(p, name, "cap", specialEdge);
			ModelFile cap_alt = barsSubModel(p, name, "cap_alt", specialEdge);
			ModelFile side = barsSubModel(p, name, "side", specialEdge);
			ModelFile side_alt = barsSubModel(p, name, "side_alt", specialEdge);

			p.getMultipartBuilder(c.get())
				.part()
				.modelFile(post_ends)
				.addModel()
				.end()
				.part()
				.modelFile(post)
				.addModel()
				.condition(NORTH, false)
				.condition(EAST, false)
				.condition(SOUTH, false)
				.condition(WEST, false)
				.end()
				.part()
				.modelFile(cap)
				.addModel()
				.condition(NORTH, true)
				.condition(EAST, false)
				.condition(SOUTH, false)
				.condition(WEST, false)
				.end()
				.part()
				.modelFile(cap)
				.rotationY(90)
				.addModel()
				.condition(NORTH, false)
				.condition(EAST, true)
				.condition(SOUTH, false)
				.condition(WEST, false)
				.end()
				.part()
				.modelFile(cap_alt)
				.addModel()
				.condition(NORTH, false)
				.condition(EAST, false)
				.condition(SOUTH, true)
				.condition(WEST, false)
				.end()
				.part()
				.modelFile(cap_alt)
				.rotationY(90)
				.addModel()
				.condition(NORTH, false)
				.condition(EAST, false)
				.condition(SOUTH, false)
				.condition(WEST, true)
				.end()
				.part()
				.modelFile(side)
				.addModel()
				.condition(NORTH, true)
				.end()
				.part()
				.modelFile(side)
				.rotationY(90)
				.addModel()
				.condition(EAST, true)
				.end()
				.part()
				.modelFile(side_alt)
				.addModel()
				.condition(SOUTH, true)
				.end()
				.part()
				.modelFile(side_alt)
				.rotationY(90)
				.addModel()
				.condition(WEST, true)
				.end();
		};
	}

	private static ModelFile barsSubModel(RegistrateBlockstateProvider p, String name, String suffix,
		boolean specialEdge) {
		Identifier barsTexture = p.modLoc("block/bars/" + name + "_bars");
		Identifier edgeTexture = specialEdge ? p.modLoc("block/bars/" + name + "_bars_edge") : barsTexture;
		return p.models()
			.withExistingParent(name + "_" + suffix, p.modLoc("block/bars/" + suffix))
			.texture("bars", barsTexture)
			.texture("particle", barsTexture)
			.texture("edge", edgeTexture);
	}

	public static BlockEntry<PaneBlock> createBars(String name, boolean specialEdge,
		Supplier<DataIngredient> ingredient, MapColor color) {
		return REGISTRATE.block(name + "_bars", PaneBlock::new)
			.addLayer(() -> RenderLayer::getCutoutMipped)
			.initialProperties(() -> Blocks.IRON_BARS)
			.properties(p -> p.sounds(BlockSoundGroup.COPPER)
				.mapColor(color))
			.tag(AllBlockTags.WRENCH_PICKUP.tag)
			.tag(AllBlockTags.FAN_TRANSPARENT.tag)
			.transform(TagGen.pickaxeOnly())
			.blockstate(barsBlockState(name, specialEdge))
			.item()
			.model((c, p) -> {
				Identifier barsTexture = p.modLoc("block/bars/" + name + "_bars");
				p.withExistingParent(c.getName(), Create.asResource("item/bars"))
					.texture("bars", barsTexture)
					.texture("edge", specialEdge ? p.modLoc("block/bars/" + name + "_bars_edge") : barsTexture);
			})
			.recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 4))
			.build()
			.register();
	}

}
