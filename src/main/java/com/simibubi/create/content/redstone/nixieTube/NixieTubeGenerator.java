package com.simibubi.create.content.redstone.nixieTube;

import com.simibubi.create.content.redstone.nixieTube.DoubleFaceAttachedBlock.DoubleAttachFace;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public class NixieTubeGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return state.get(NixieTubeBlock.FACE)
			.xRot();
	}

	@Override
	protected int getYRotation(BlockState state) {
		DoubleAttachFace face = state.get(NixieTubeBlock.FACE);
		return horizontalAngle(state.get(NixieTubeBlock.FACING))
			+ (face == DoubleAttachFace.WALL || face == DoubleAttachFace.WALL_REVERSED ? 180 : 0);
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		return prov.models()
			.withExistingParent(ctx.getName(), prov.modLoc("block/nixie_tube/block"));
	}

}
