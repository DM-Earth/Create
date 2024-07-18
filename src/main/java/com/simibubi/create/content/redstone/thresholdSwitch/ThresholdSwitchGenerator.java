package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public class ThresholdSwitchGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.get(ThresholdSwitchBlock.FACING)) + 180;
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		int level = state.get(ThresholdSwitchBlock.LEVEL);
		String path = "threshold_switch/block_" + Lang.asId(state.get(ThresholdSwitchBlock.TARGET)
			.name());
		return prov.models()
			.withExistingParent(path + "_" + level, Create.asResource("block/" + path))
			.texture("level", Create.asResource("block/threshold_switch/level_" + level));
	}

}
