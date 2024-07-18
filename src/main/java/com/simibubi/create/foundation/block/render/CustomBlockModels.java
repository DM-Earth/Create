package com.simibubi.create.foundation.block.render;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

public class CustomBlockModels {

	private final Multimap<Identifier, NonNullFunction<BakedModel, ? extends BakedModel>> modelFuncs = MultimapBuilder.hashKeys().arrayListValues().build();
	private final Map<Block, NonNullFunction<BakedModel, ? extends BakedModel>> finalModelFuncs = new IdentityHashMap<>();
	private boolean funcsLoaded = false;

	public void register(Identifier block, NonNullFunction<BakedModel, ? extends BakedModel> func) {
		modelFuncs.put(block, func);
	}

	public void forEach(NonNullBiConsumer<Block, NonNullFunction<BakedModel, ? extends BakedModel>> consumer) {
		loadEntriesIfMissing();
		finalModelFuncs.forEach(consumer);
	}

	private void loadEntriesIfMissing() {
		if (!funcsLoaded) {
			loadEntries();
			funcsLoaded = true;
		}
	}

	private void loadEntries() {
		finalModelFuncs.clear();
		modelFuncs.asMap().forEach((location, funcList) -> {
			Block block = Registries.BLOCK.get(location);

			NonNullFunction<BakedModel, ? extends BakedModel> finalFunc = null;
			for (NonNullFunction<BakedModel, ? extends BakedModel> func : funcList) {
				if (finalFunc == null) {
					finalFunc = func;
				} else {
					finalFunc = finalFunc.andThen(func);
				}
			}

			finalModelFuncs.put(block, finalFunc);
		});
	}

}
