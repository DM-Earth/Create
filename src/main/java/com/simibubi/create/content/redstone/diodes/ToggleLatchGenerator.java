package com.simibubi.create.content.redstone.diodes;

import java.util.Vector;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import com.tterrag.registrate.providers.DataGenContext;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockModelProvider;

public class ToggleLatchGenerator extends AbstractDiodeGenerator {

	@Override
	protected <T extends Block> Vector<ModelFile> createModels(DataGenContext<Block, T> ctx, BlockModelProvider prov) {
		String name = ctx.getName();
		Vector<ModelFile> models = makeVector(4);
		Identifier off = existing("latch_off");
		Identifier on = existing("latch_on");

		models.add(prov.getExistingFile(off));
		models.add(prov.withExistingParent(name + "_off_powered", off)
			.texture("top", texture(ctx, "powered")));
		models.add(prov.getExistingFile(on));
		models.add(prov.withExistingParent(name + "_on_powered", on)
			.texture("top", texture(ctx, "powered_powering")));

		return models;
	}

	@Override
	protected int getModelIndex(BlockState state) {
		return (state.get(ToggleLatchBlock.POWERING) ? 2 : 0) + (state.get(ToggleLatchBlock.POWERED) ? 1 : 0);
	}

}
