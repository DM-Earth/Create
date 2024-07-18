package com.simibubi.create.foundation.data;

import java.util.concurrent.CompletableFuture;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.DamageTypeTags;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.Create;

public class DamageTypeTagGen extends FabricTagProvider<DamageType> {
	public DamageTypeTagGen(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> lookupProvider) {
		super(output, RegistryKeys.DAMAGE_TYPE, lookupProvider);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup provider) {
		getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ARMOR)
				.add(AllDamageTypes.CRUSH, AllDamageTypes.FAN_FIRE, AllDamageTypes.FAN_LAVA, AllDamageTypes.DRILL, AllDamageTypes.SAW);
		getOrCreateTagBuilder(DamageTypeTags.IS_FIRE)
				.add(AllDamageTypes.FAN_FIRE, AllDamageTypes.FAN_LAVA);
		getOrCreateTagBuilder(DamageTypeTags.IS_EXPLOSION)
				.add(AllDamageTypes.CUCKOO_SURPRISE);
	}

	@Override
	public String getName() {
		return "Create's Damage Type Tags";
	}
}
