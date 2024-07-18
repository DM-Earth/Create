package com.simibubi.create.content.legacy;

import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.item.EntityTickListenerItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class NoGravMagicalDohickyItem extends Item implements EntityTickListenerItem {

	public NoGravMagicalDohickyItem(Settings p_i48487_1_) {
		super(p_i48487_1_);
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		World world = entity.getWorld();
		Vec3d pos = entity.getPos();
		NbtCompound persistentData = entity.getCustomData();

		if (world.isClient) {
			if (world.random.nextFloat() < getIdleParticleChance(entity)) {
				Vec3d ppos = VecHelper.offsetRandomly(pos, world.random, .5f);
				world.addParticle(ParticleTypes.END_ROD, ppos.x, pos.y, ppos.z, 0, -.1f, 0);
			}

			if (entity.isSilent() && !persistentData.getBoolean("PlayEffects")) {
				Vec3d basemotion = new Vec3d(0, 1, 0);
				world.addParticle(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 0, 0, 0);
				for (int i = 0; i < 20; i++) {
					Vec3d motion = VecHelper.offsetRandomly(basemotion, world.random, 1);
					world.addParticle(ParticleTypes.WITCH, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
					world.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
				}
				persistentData.putBoolean("PlayEffects", true);
			}

			return false;
		}

		entity.setNoGravity(true);

		if (!persistentData.contains("JustCreated"))
			return false;
		onCreated(entity, persistentData);
		return false;
	}

	protected float getIdleParticleChance(ItemEntity entity) {
		return MathHelper.clamp(entity.getStack()
			.getCount() - 10, 5, 100) / 64f;
	}

	protected void onCreated(ItemEntity entity, NbtCompound persistentData) {
		//entity.LIFETIME = 6000;
		persistentData.remove("JustCreated");

		// just a flag to tell the client to play an effect
		entity.setSilent(true);
	}

}
