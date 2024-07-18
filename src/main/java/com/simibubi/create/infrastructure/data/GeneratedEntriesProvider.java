package com.simibubi.create.infrastructure.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.DataOutput;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Util;
import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.worldgen.AllBiomeModifiers;
import com.simibubi.create.infrastructure.worldgen.AllConfiguredFeatures;
import com.simibubi.create.infrastructure.worldgen.AllPlacedFeatures;

import io.github.fabricators_of_create.porting_lib.data.DatapackBuiltinEntriesProvider;

public class GeneratedEntriesProvider extends DatapackBuiltinEntriesProvider {

	public static final RegistryBuilder BUILDER = Util.make(new RegistryBuilder(), GeneratedEntriesProvider::addBootstraps);

	public GeneratedEntriesProvider(DataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
		super(output, registries, BUILDER, Set.of(Create.ID));
	}

	// fabric: this must be reused in the entrypoint, moved to a method
	public static void addBootstraps(RegistryBuilder builder) {
		builder.addRegistry(RegistryKeys.DAMAGE_TYPE, AllDamageTypes::bootstrap)
				.addRegistry(RegistryKeys.CONFIGURED_FEATURE, AllConfiguredFeatures::bootstrap)
				.addRegistry(RegistryKeys.PLACED_FEATURE, AllPlacedFeatures::bootstrap);
		// fabric: biome modifiers not a registry, remove
	}

	@Override
	public String getName() {
		return "Create's Generated Registry Entries";
	}
}
