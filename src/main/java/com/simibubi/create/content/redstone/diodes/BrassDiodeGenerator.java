package com.simibubi.create.content.redstone.diodes;

import java.util.Vector;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import com.tterrag.registrate.providers.DataGenContext;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockModelProvider;

public class BrassDiodeGenerator extends AbstractDiodeGenerator {

	@Override
	protected <T extends Block> Vector<ModelFile> createModels(DataGenContext<Block, T> ctx, BlockModelProvider prov) {
		Vector<ModelFile> models = makeVector(4);
		String name = ctx.getName();
		Identifier template = existing(name);

		models.add(prov.getExistingFile(template));
		models.add(prov.withExistingParent(name + "_powered", template)
			.texture("top", texture(ctx, "powered")));
		models.add(prov.withExistingParent(name + "_powering", template)
			.texture("torch", poweredTorch())
			.texture("top", texture(ctx, "powering")));
		models.add(prov.withExistingParent(name + "_powered_powering", template)
			.texture("torch", poweredTorch())
			.texture("top", texture(ctx, "powered_powering")));

		return models;
	}

	@Override
	protected int getModelIndex(BlockState state) {
		return (state.get(BrassDiodeBlock.POWERING) ^ state.get(BrassDiodeBlock.INVERTED) ? 2 : 0)
			+ (state.get(BrassDiodeBlock.POWERED) ? 1 : 0);
	}

}