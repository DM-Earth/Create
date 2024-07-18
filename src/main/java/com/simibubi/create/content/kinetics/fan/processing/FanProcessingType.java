package com.simibubi.create.content.kinetics.fan.processing;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface FanProcessingType {
	boolean isValidAt(World level, BlockPos pos);

	int getPriority();

	boolean canProcess(ItemStack stack, World level);

	@Nullable
	List<ItemStack> process(ItemStack stack, World level);

	void spawnProcessingParticles(World level, Vec3d pos);

	void morphAirFlow(AirFlowParticleAccess particleAccess, Random random);

	void affectEntity(Entity entity, World level);

	static FanProcessingType parse(String str) {
		Identifier id = Identifier.tryParse(str);
		if (id == null) {
			return AllFanProcessingTypes.NONE;
		}
		FanProcessingType type = FanProcessingTypeRegistry.getType(id);
		if (type == null) {
			return AllFanProcessingTypes.NONE;
		}
		return type;
	}

	static FanProcessingType getAt(World level, BlockPos pos) {
		for (FanProcessingType type : FanProcessingTypeRegistry.getSortedTypesView()) {
			if (type.isValidAt(level, pos)) {
				return type;
			}
		}
		return AllFanProcessingTypes.NONE;
	}

	interface AirFlowParticleAccess {
		void setColor(int color);

		void setAlpha(float alpha);

		void spawnExtraParticle(ParticleEffect options, float speedMultiplier);
	}
}
