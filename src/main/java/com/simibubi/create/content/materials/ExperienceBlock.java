package com.simibubi.create.content.materials;

import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ExperienceBlock extends Block {

	public ExperienceBlock(Settings pProperties) {
		super(pProperties);
	}

	@Override
	public void randomDisplayTick(BlockState pState, World pLevel, BlockPos pPos, Random pRand) {
		if (pRand.nextInt(5) != 0)
			return;
		Vec3d vec3 = VecHelper.clampComponentWise(VecHelper.offsetRandomly(Vec3d.ZERO, pRand, .75f), .55f)
			.add(VecHelper.getCenterOf(pPos));
		pLevel.addParticle(ParticleTypes.END_ROD, vec3.x, vec3.y, vec3.z, pRand.nextGaussian() * 0.005D,
			pRand.nextGaussian() * 0.005D, pRand.nextGaussian() * 0.005D);
	}

}
