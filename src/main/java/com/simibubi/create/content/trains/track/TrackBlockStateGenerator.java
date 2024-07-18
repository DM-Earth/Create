package com.simibubi.create.content.trains.track;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public class TrackBlockStateGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return state.get(TrackBlock.SHAPE)
			.getModelRotation();
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		TrackShape value = state.get(TrackBlock.SHAPE);
		if (value == TrackShape.NONE)
			return prov.models()
				.getExistingFile(prov.mcLoc("block/air"));
		return prov.models()
			.getExistingFile(Create.asResource("block/track/" + value.getModel()));
	}

}
