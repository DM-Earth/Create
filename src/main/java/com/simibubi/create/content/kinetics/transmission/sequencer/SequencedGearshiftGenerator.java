package com.simibubi.create.content.kinetics.transmission.sequencer;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction.Axis;

public class SequencedGearshiftGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return state.get(SequencedGearshiftBlock.VERTICAL) ? 90 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return state.get(SequencedGearshiftBlock.HORIZONTAL_AXIS) == Axis.X ? 90 : 0;
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		String variant = "idle";
		int seq = state.get(SequencedGearshiftBlock.STATE);
		if (seq > 0)
			variant = "seq_" + seq;
		return prov.models()
			.getExistingFile(prov.modLoc("block/" + ctx.getName() + "/" + variant));
	}

}
