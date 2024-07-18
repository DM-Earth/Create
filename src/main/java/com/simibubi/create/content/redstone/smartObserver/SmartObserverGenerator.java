package com.simibubi.create.content.redstone.smartObserver;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public class SmartObserverGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return switch (state.get(SmartObserverBlock.TARGET)) {
		case CEILING -> -90;
		case WALL -> 0;
		case FLOOR -> 90;
		};
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.get(ThresholdSwitchBlock.FACING)) + 180;
	}

	@Override
	public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
												BlockState state) {
		return AssetLookup.forPowered(ctx, prov)
			.apply(state);
	}

}
