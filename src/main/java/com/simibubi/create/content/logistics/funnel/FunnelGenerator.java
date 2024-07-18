package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockModelBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class FunnelGenerator extends SpecialBlockStateGen {

	private String type;
	private Identifier blockTexture;
	private boolean hasFilter;

	public FunnelGenerator(String type, boolean hasFilter) {
		this.type = type;
		this.hasFilter = hasFilter;
		this.blockTexture = Create.asResource("block/" + type + "_block");
	}

	@Override
	protected int getXRotation(BlockState state) {
		return state.get(FunnelBlock.FACING) == Direction.DOWN ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.get(FunnelBlock.FACING)) + 180;
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> c, RegistrateBlockstateProvider p,
												BlockState s) {
		String prefix = "block/funnel/";
		String powered = s.get(FunnelBlock.POWERED) ? "_powered" : "_unpowered";
		String closed = s.get(FunnelBlock.POWERED) ? "_closed" : "_open";
		String extracting = s.get(FunnelBlock.EXTRACTING) ? "_push" : "_pull";
		Direction facing = s.get(FunnelBlock.FACING);
		boolean horizontal = facing.getAxis()
			.isHorizontal();
		String parent = horizontal ? "horizontal" : hasFilter ? "vertical" : "vertical_filterless";

		BlockModelBuilder model = p.models()
			.withExistingParent("block/" + type + "_funnel_" + parent + extracting + powered,
				p.modLoc(prefix + "block_" + parent))
			.texture("particle", blockTexture)
			.texture("base", p.modLoc(prefix + type + "_funnel"))
			.texture("redstone", p.modLoc(prefix + type + "_funnel" + powered))
			.texture("direction", p.modLoc(prefix + type + "_funnel" + extracting));

		if (horizontal)
			return model.texture("block", blockTexture);

		return model.texture("frame", p.modLoc(prefix + type + "_funnel_frame"))
			.texture("open", p.modLoc(prefix + "funnel" + closed));
	}

	public static NonNullBiConsumer<DataGenContext<Item, FunnelItem>, RegistrateItemModelProvider> itemModel(
		String type) {
		String prefix = "block/funnel/";
		Identifier blockTexture = Create.asResource("block/" + type + "_block");
		return (c, p) -> {
			p.withExistingParent("item/" + type + "_funnel", p.modLoc("block/funnel/item"))
				.texture("particle", blockTexture)
				.texture("block", blockTexture)
				.texture("base", p.modLoc(prefix + type + "_funnel"))
				.texture("direction", p.modLoc(prefix + type + "_funnel_neutral"))
				.texture("redstone", p.modLoc(prefix + type + "_funnel_unpowered"));
		};
	}

}