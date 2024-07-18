package com.simibubi.create.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.content.kinetics.BlockStressValues.IStressValueProvider;
import com.simibubi.create.foundation.config.ConfigBase;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.config.ModConfigSpec.Builder;
import io.github.fabricators_of_create.porting_lib.config.ModConfigSpec.ConfigValue;

public class CStress extends ConfigBase implements IStressValueProvider {

	private final Map<Identifier, ConfigValue<Double>> capacities = new HashMap<>();
	private final Map<Identifier, ConfigValue<Double>> impacts = new HashMap<>();

	@Override
	public void registerAll(Builder builder) {
		builder.comment(".", Comments.su, Comments.impact)
			.push("impact");
		BlockStressDefaults.DEFAULT_IMPACTS.forEach((r, i) -> {
			if (r.getNamespace()
				.equals(Create.ID))
				getImpacts().put(r, builder.define(r.getPath(), i));
		});
		builder.pop();

		builder.comment(".", Comments.su, Comments.capacity)
			.push("capacity");
		BlockStressDefaults.DEFAULT_CAPACITIES.forEach((r, i) -> {
			if (r.getNamespace()
				.equals(Create.ID))
				getCapacities().put(r, builder.define(r.getPath(), i));
		});
		builder.pop();
	}

	@Override
	public double getImpact(Block block) {
		block = redirectValues(block);
		Identifier key = RegisteredObjects.getKeyOrThrow(block);
		ConfigValue<Double> value = getImpacts().get(key);
		if (value != null)
			return value.get();
		return 0;
	}

	@Override
	public double getCapacity(Block block) {
		block = redirectValues(block);
		Identifier key = RegisteredObjects.getKeyOrThrow(block);
		ConfigValue<Double> value = getCapacities().get(key);
		if (value != null)
			return value.get();
		return 0;
	}

	@Override
	public Couple<Integer> getGeneratedRPM(Block block) {
		block = redirectValues(block);
		Identifier key = RegisteredObjects.getKeyOrThrow(block);
		Supplier<Couple<Integer>> supplier = BlockStressDefaults.GENERATOR_SPEEDS.get(key);
		if (supplier == null)
			return null;
		return supplier.get();
	}

	@Override
	public boolean hasImpact(Block block) {
		block = redirectValues(block);
		Identifier key = RegisteredObjects.getKeyOrThrow(block);
		return getImpacts().containsKey(key);
	}

	@Override
	public boolean hasCapacity(Block block) {
		block = redirectValues(block);
		Identifier key = RegisteredObjects.getKeyOrThrow(block);
		return getCapacities().containsKey(key);
	}

	protected Block redirectValues(Block block) {
		return block;
	}

	@Override
	public String getName() {
		return "stressValues.v" + BlockStressDefaults.FORCED_UPDATE_VERSION;
	}

	public Map<Identifier, ConfigValue<Double>> getImpacts() {
		return impacts;
	}

	public Map<Identifier, ConfigValue<Double>> getCapacities() {
		return capacities;
	}

	private static class Comments {
		static String su = "[in Stress Units]";
		static String impact =
			"Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
		static String capacity = "Configure how much stress a source can accommodate for.";
	}

}
