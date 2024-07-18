package com.simibubi.create.foundation.damageTypes;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllDamageTypes;

public class CreateDamageSources {
	public static DamageSource crush(World level) {
		return source(AllDamageTypes.CRUSH, level);
	}

	public static DamageSource cuckooSurprise(World level) {
		return source(AllDamageTypes.CUCKOO_SURPRISE, level);
	}

	public static DamageSource fanFire(World level) {
		return source(AllDamageTypes.FAN_FIRE, level);
	}

	public static DamageSource fanLava(World level) {
		return source(AllDamageTypes.FAN_LAVA, level);
	}

	public static DamageSource drill(World level) {
		return source(AllDamageTypes.DRILL, level);
	}

	public static DamageSource roller(World level) {
		return source(AllDamageTypes.ROLLER, level);
	}

	public static DamageSource saw(World level) {
		return source(AllDamageTypes.SAW, level);
	}

	public static DamageSource potatoCannon(World level, Entity causingEntity, Entity directEntity) {
		return source(AllDamageTypes.POTATO_CANNON, level, causingEntity, directEntity);
	}

	public static DamageSource runOver(World level, Entity entity) {
		return source(AllDamageTypes.RUN_OVER, level, entity);
	}

	private static DamageSource source(RegistryKey<DamageType> key, WorldView level) {
		Registry<DamageType> registry = level.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
		return new DamageSource(registry.entryOf(key));
	}

	private static DamageSource source(RegistryKey<DamageType> key, WorldView level, @Nullable Entity entity) {
		Registry<DamageType> registry = level.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
		return new DamageSource(registry.entryOf(key), entity);
	}

	private static DamageSource source(RegistryKey<DamageType> key, WorldView level, @Nullable Entity causingEntity, @Nullable Entity directEntity) {
		Registry<DamageType> registry = level.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
		return new DamageSource(registry.entryOf(key), causingEntity, directEntity);
	}
}
