package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType;

import io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlazeBurnerHandler {

	public static boolean onThrowableImpact(ProjectileImpactEvent event) {
		splashExtinguishesBurner(event.getProjectile(), event.getRayTraceResult());
		return thrownEggsGetEatenByBurner(event.getProjectile(), event.getRayTraceResult());
	}

	public static boolean thrownEggsGetEatenByBurner(ProjectileEntity projectile, HitResult hitResult) {
//		Projectile projectile = event.getProjectile();
		if (!(projectile instanceof EggEntity))
			return false;

		if (hitResult
			.getType() != HitResult.Type.BLOCK)
			return false;

		BlockEntity blockEntity = projectile.getWorld()
			.getBlockEntity(BlockPos.ofFloored(hitResult
				.getPos()));
		if (!(blockEntity instanceof BlazeBurnerBlockEntity)) {
			return false;
		}

//		event.setCanceled(true);
		projectile.setVelocity(Vec3d.ZERO);
		projectile.discard();

		World world = projectile.getWorld();
		if (world.isClient)
			return false;

		BlazeBurnerBlockEntity heater = (BlazeBurnerBlockEntity) blockEntity;
		if (!heater.isCreative()) {
			if (heater.activeFuel != FuelType.SPECIAL) {
				heater.activeFuel = FuelType.NORMAL;
				heater.remainingBurnTime =
					MathHelper.clamp(heater.remainingBurnTime + 80, 0, BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY);
				heater.updateBlockState();
				heater.notifyUpdate();
			}
		}

		AllSoundEvents.BLAZE_MUNCH.playOnServer(world, heater.getPos());
		return true;
	}

	public static void splashExtinguishesBurner(ProjectileEntity projectile, HitResult hitResult) {
//		Projectile projectile = event.getProjectile();
		if (projectile.getWorld().isClient)
			return;
		if (!(projectile instanceof PotionEntity))
			return;
		PotionEntity entity = (PotionEntity) projectile;

		if (hitResult
			.getType() != HitResult.Type.BLOCK)
			return;

		ItemStack stack = entity.getStack();
		Potion potion = PotionUtil.getPotion(stack);
		if (potion == Potions.WATER && PotionUtil.getPotionEffects(stack)
			.isEmpty()) {
			BlockHitResult result = (BlockHitResult) hitResult;
			World world = entity.getWorld();
			Direction face = result.getSide();
			BlockPos pos = result.getBlockPos()
				.offset(face);

			extinguishLitBurners(world, pos, face);
			extinguishLitBurners(world, pos.offset(face.getOpposite()), face);

			for (Direction face1 : Direction.Type.HORIZONTAL) {
				extinguishLitBurners(world, pos.offset(face1), face1);
			}
		}
	}

	private static void extinguishLitBurners(World world, BlockPos pos, Direction direction) {
		BlockState state = world.getBlockState(pos);
		if (AllBlocks.LIT_BLAZE_BURNER.has(state)) {
			world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
				2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
			world.setBlockState(pos, AllBlocks.BLAZE_BURNER.getDefaultState());
		}
	}

}
