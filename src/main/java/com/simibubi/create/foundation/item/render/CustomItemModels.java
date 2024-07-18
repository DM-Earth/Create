package com.simibubi.create.foundation.item.render;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

public class CustomItemModels {

	private final Multimap<Identifier, NonNullFunction<BakedModel, ? extends BakedModel>> modelFuncs = MultimapBuilder.hashKeys().arrayListValues().build();
	private final Map<Item, NonNullFunction<BakedModel, ? extends BakedModel>> finalModelFuncs = new IdentityHashMap<>();
	private boolean funcsLoaded = false;

	public void register(Identifier item, NonNullFunction<BakedModel, ? extends BakedModel> func) {
		modelFuncs.put(item, func);
	}

	public void forEach(NonNullBiConsumer<Item, NonNullFunction<BakedModel, ? extends BakedModel>> consumer) {
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
			Item item = Registries.ITEM.get(location);

			NonNullFunction<BakedModel, ? extends BakedModel> finalFunc = null;
			for (NonNullFunction<BakedModel, ? extends BakedModel> func : funcList) {
				if (finalFunc == null) {
					finalFunc = func;
				} else {
					finalFunc = finalFunc.andThen(func);
				}
			}

			finalModelFuncs.put(item, finalFunc);
		});
	}

}
