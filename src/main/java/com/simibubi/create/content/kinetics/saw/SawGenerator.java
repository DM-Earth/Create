package com.simibubi.create.content.kinetics.saw;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

public class SawGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return state.get(SawBlock.FACING) == Direction.DOWN ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		Direction facing = state.get(SawBlock.FACING);
		boolean axisAlongFirst = state.get(SawBlock.AXIS_ALONG_FIRST_COORDINATE);
		if (facing.getAxis()
			.isVertical())
			return (axisAlongFirst ? 270 : 0) + (state.get(SawBlock.FLIPPED) ? 180 : 0);
		return horizontalAngle(facing);
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		String path = "block/" + ctx.getName() + "/";
		String orientation = state.get(SawBlock.FACING)
			.getAxis()
			.isVertical() ? "vertical" : "horizontal";

		return prov.models()
			.getExistingFile(prov.modLoc(path + orientation));
	}

}
