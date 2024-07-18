package com.simibubi.create.foundation.data;

import com.simibubi.create.Create;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockStateProvider;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;

public class ModelGen {

	public static ModelFile createOvergrown(DataGenContext<Block, ? extends Block> ctx, BlockStateProvider prov,
											Identifier block, Identifier overlay) {
		return createOvergrown(ctx, prov, block, block, block, overlay);
	}

	public static ModelFile createOvergrown(DataGenContext<Block, ? extends Block> ctx, BlockStateProvider prov,
		Identifier side, Identifier top, Identifier bottom, Identifier overlay) {
		return prov.models()
			.withExistingParent(ctx.getName(), Create.asResource("block/overgrown"))
			.texture("particle", side)
			.texture("side", side)
			.texture("top", top)
			.texture("bottom", bottom)
			.texture("overlay", overlay);
	}

	public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel() {
		return b -> b.model(AssetLookup::customItemModel)
			.build();
	}

	public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel(String... path) {
		return b -> b.model(AssetLookup.customBlockItemModel(path))
			.build();
	}

}
