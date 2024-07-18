package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockModelProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class SpecialCopycatPanelBlockState extends SpecialBlockStateGen {

	private String name;

	public SpecialCopycatPanelBlockState(String name) {
		this.name = name;
	}

	@Override
	protected int getXRotation(BlockState state) {
		return facing(state) == Direction.UP ? 0 : facing(state) == Direction.DOWN ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(facing(state));
	}

	private Direction facing(BlockState state) {
		return state.get(FacingBlock.FACING);
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		BlockModelProvider models = prov.models();
		return facing(state).getAxis() == Axis.Y
			? models.getExistingFile(prov.modLoc("block/copycat_panel/" + name + "_vertical"))
			: models.getExistingFile(prov.modLoc("block/copycat_panel/" + name));
	}

}
