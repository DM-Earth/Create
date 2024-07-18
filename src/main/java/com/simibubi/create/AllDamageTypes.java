package com.simibubi.create;

import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.entity.damage.DamageEffects;
import net.minecraft.entity.damage.DamageScaling;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public class AllDamageTypes {
	public static final RegistryKey<DamageType>
			CRUSH = key("crush"),
			CUCKOO_SURPRISE = key("cuckoo_surprise"),
			FAN_FIRE = key("fan_fire"),
			FAN_LAVA = key("fan_lava"),
			DRILL = key("mechanical_drill"),
			ROLLER = key("mechanical_roller"),
			SAW = key("mechanical_saw"),
			POTATO_CANNON = key("potato_cannon"),
			RUN_OVER = key("run_over");

	private static RegistryKey<DamageType> key(String name) {
		return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Create.asResource(name));
	}

	public static void bootstrap(Registerable<DamageType> ctx) {
		new DamageTypeBuilder(CRUSH).scaling(DamageScaling.ALWAYS).register(ctx);
		new DamageTypeBuilder(CUCKOO_SURPRISE).scaling(DamageScaling.ALWAYS).exhaustion(0.1f).register(ctx);
		new DamageTypeBuilder(FAN_FIRE).effects(DamageEffects.BURNING).register(ctx);
		new DamageTypeBuilder(FAN_LAVA).effects(DamageEffects.BURNING).register(ctx);
		new DamageTypeBuilder(DRILL).register(ctx);
		new DamageTypeBuilder(ROLLER).register(ctx);
		new DamageTypeBuilder(SAW).register(ctx);
		new DamageTypeBuilder(POTATO_CANNON).register(ctx);
		new DamageTypeBuilder(RUN_OVER).register(ctx);
	}
}
