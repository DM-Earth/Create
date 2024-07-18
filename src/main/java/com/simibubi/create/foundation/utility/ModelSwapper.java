package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.simibubi.create.foundation.block.render.CustomBlockModels;
import com.simibubi.create.foundation.item.render.CustomItemModels;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItems;

import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBake;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

public class ModelSwapper implements AfterBake {

	protected CustomBlockModels customBlockModels = new CustomBlockModels();
	protected CustomItemModels customItemModels = new CustomItemModels();

	private Map<Identifier, NonNullFunction<BakedModel, ? extends BakedModel>> swaps = null;

	public CustomBlockModels getCustomBlockModels() {
		return customBlockModels;
	}

	public CustomItemModels getCustomItemModels() {
		return customItemModels;
	}

	public void registerListeners() {
		ModelLoadingPlugin.register(ctx -> ctx.modifyModelAfterBake().register(this));
	}

	@Override
	public BakedModel modifyModelAfterBake(BakedModel model, Context context) {
		if (swaps == null)
			collectSwaps();
		NonNullFunction<BakedModel, ? extends BakedModel> swap = swaps.get(context.id());
		return swap != null ? swap.apply(model) : model;
	}

	private void collectSwaps() {
		this.swaps = new HashMap<>();

		customBlockModels.forEach((block, swapper) -> getAllBlockStateModelLocations(block).forEach(id -> swaps.put(id, swapper)));
		customItemModels.forEach((item, swapper) -> swaps.put(getItemModelLocation(item), swapper));
		CustomRenderedItems.forEach(item -> swaps.put(getItemModelLocation(item), CustomRenderedItemModel::new));
	}

	public static List<ModelIdentifier> getAllBlockStateModelLocations(Block block) {
		List<ModelIdentifier> models = new ArrayList<>();
		Identifier blockRl = RegisteredObjects.getKeyOrThrow(block);
		block.getStateManager()
			.getStates()
			.forEach(state -> {
				models.add(BlockModels.getModelId(blockRl, state));
			});
		return models;
	}

	public static ModelIdentifier getItemModelLocation(Item item) {
		return new ModelIdentifier(RegisteredObjects.getKeyOrThrow(item), "inventory");
	}

}
