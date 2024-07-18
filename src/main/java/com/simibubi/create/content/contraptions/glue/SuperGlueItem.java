package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.item.CustomMaxCountItem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.Filter.Result;

public class SuperGlueItem extends Item {

	public static ActionResult glueItemAlwaysPlacesWhenUsed(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (hitResult != null) {
			BlockState blockState = world
				.getBlockState(hitResult
					.getBlockPos());
			if (blockState.getBlock()instanceof AbstractChassisBlock cb)
				if (cb.getGlueableSide(blockState, hitResult.getSide()) != null)
					return ActionResult.PASS;
		}

		if (player.getStackInHand(hand).getItem() instanceof SuperGlueItem)
			return ActionResult.FAIL;
		return ActionResult.PASS;
	}

	public SuperGlueItem(Settings properties) {
		super(properties.maxDamage(99));
	}

	@Override
	public boolean canMine(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer) {
		return false;
	}

	@Override
	public boolean isDamageable() {
		return true;
	}

	public static void onBroken(PlayerEntity player) {}

	@Environment(EnvType.CLIENT)
	public static void spawnParticles(World world, BlockPos pos, Direction direction, boolean fullBlock) {
		Vec3d vec = Vec3d.of(direction.getVector());
		Vec3d plane = VecHelper.axisAlingedPlaneOf(vec);
		Vec3d facePos = VecHelper.getCenterOf(pos)
			.add(vec.multiply(.5f));

		float distance = fullBlock ? 1f : .25f + .25f * (world.random.nextFloat() - .5f);
		plane = plane.multiply(distance);
		ItemStack stack = new ItemStack(Items.SLIME_BALL);

		for (int i = fullBlock ? 40 : 15; i > 0; i--) {
			Vec3d offset = VecHelper.rotate(plane, 360 * world.random.nextFloat(), direction.getAxis());
			Vec3d motion = offset.normalize()
				.multiply(1 / 16f);
			if (fullBlock)
				offset =
					new Vec3d(MathHelper.clamp(offset.x, -.5, .5), MathHelper.clamp(offset.y, -.5, .5), MathHelper.clamp(offset.z, -.5, .5));
			Vec3d particlePos = facePos.add(offset);
			world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), particlePos.x, particlePos.y,
				particlePos.z, motion.x, motion.y, motion.z);
		}

	}

}
