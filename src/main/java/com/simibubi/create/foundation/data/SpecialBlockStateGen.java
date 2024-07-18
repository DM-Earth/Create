package com.simibubi.create.foundation.data;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

public abstract class SpecialBlockStateGen {

	protected Property<?>[] getIgnoredProperties() {
		return new Property<?>[0];
	}

	public final <T extends Block> void generate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStatesExcept(state -> {
				return ConfiguredModel.builder()
					.modelFile(getModel(ctx, prov, state))
					.rotationX((getXRotation(state) + 360) % 360)
					.rotationY((getYRotation(state) + 360) % 360)
					.build();
			}, getIgnoredProperties());
	}

	protected int horizontalAngle(Direction direction) {
		if (direction.getAxis()
			.isVertical())
			return 0;
		return (int) direction.asRotation();
	}

	protected abstract int getXRotation(BlockState state);

	protected abstract int getYRotation(BlockState state);

	public abstract <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx,
														 RegistrateBlockstateProvider prov, BlockState state);

}
