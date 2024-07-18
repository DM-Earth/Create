package com.simibubi.create.content.redstone.diodes;

import java.util.Vector;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import com.tterrag.registrate.providers.DataGenContext;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockModelProvider;

public class PoweredLatchGenerator extends AbstractDiodeGenerator {

	@Override
	protected <T extends Block> Vector<ModelFile> createModels(DataGenContext<Block, T> ctx, BlockModelProvider prov) {
		Vector<ModelFile> models = makeVector(2);
		String name = ctx.getName();
		Identifier off = existing("latch_off");
		Identifier on = existing("latch_on");

		models.add(prov.withExistingParent(name, off)
			.texture("top", texture(ctx, "idle")));
		models.add(prov.withExistingParent(name + "_powered", on)
			.texture("top", texture(ctx, "powering")));

		return models;
	}

	@Override
	protected int getModelIndex(BlockState state) {
		return state.get(PoweredLatchBlock.POWERING) ? 1 : 0;
	}

}
