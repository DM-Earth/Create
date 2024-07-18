package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

public class WhistleGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.get(WhistleBlock.FACING));
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		String wall = state.get(WhistleBlock.WALL) ? "wall" : "floor";
		String size = state.get(WhistleBlock.SIZE)
			.asString();
		boolean powered = state.get(WhistleBlock.POWERED);
		ModelFile model = AssetLookup.partialBaseModel(ctx, prov, size, wall);
		if (!powered)
			return model;
		Identifier parentLocation = model.getLocation();
		return prov.models()
			.withExistingParent(parentLocation.getPath() + "_powered", parentLocation)
			.texture("2", Create.asResource("block/copper_redstone_plate_powered"));
	}

}
