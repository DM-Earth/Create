package com.simibubi.create.content.redstone.link;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

public class RedstoneLinkGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		Direction facing = state.get(RedstoneLinkBlock.FACING);
		return facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 270;
	}

	@Override
	protected int getYRotation(BlockState state) {
		Direction facing = state.get(RedstoneLinkBlock.FACING);
		return facing.getAxis()
			.isVertical() ? 180 : horizontalAngle(facing);
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		String variant = state.get(RedstoneLinkBlock.RECEIVER) ? "receiver" : "transmitter";
		if (state.get(RedstoneLinkBlock.FACING).getAxis().isHorizontal())
			variant += "_vertical";
		if (state.get(RedstoneLinkBlock.POWERED))
			variant += "_powered";

		return prov.models().getExistingFile(prov.modLoc("block/redstone_link/" + variant));
	}

}
