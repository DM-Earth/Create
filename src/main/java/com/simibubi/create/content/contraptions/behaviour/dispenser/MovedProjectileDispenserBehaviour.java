package com.simibubi.create.content.contraptions.behaviour.dispenser;

import javax.annotation.Nullable;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.mixin.accessor.AbstractProjectileDispenseBehaviorAccessor;

public abstract class MovedProjectileDispenserBehaviour extends MovedDefaultDispenseItemBehaviour {

	@Override
	protected ItemStack dispenseStack(ItemStack itemStack, MovementContext context, BlockPos pos, Vec3d facing) {
		double x = pos.getX() + facing.x * .7 + .5;
		double y = pos.getY() + facing.y * .7 + .5;
		double z = pos.getZ() + facing.z * .7 + .5;
		ProjectileEntity projectile = this.getProjectileEntity(context.world, x, y, z, itemStack.copy());
		if (projectile == null)
			return itemStack;
		Vec3d effectiveMovementVec = facing.multiply(getProjectileVelocity()).add(context.motion);
		projectile.setVelocity(effectiveMovementVec.x, effectiveMovementVec.y, effectiveMovementVec.z, (float) effectiveMovementVec.length(), this.getProjectileInaccuracy());
		context.world.spawnEntity(projectile);
		itemStack.decrement(1);
		return itemStack;
	}

	@Override
	protected void playDispenseSound(WorldAccess world, BlockPos pos) {
		world.syncWorldEvent(1002, pos, 0);
	}

	@Nullable
	protected abstract ProjectileEntity getProjectileEntity(World world, double x, double y, double z, ItemStack itemStack);

	protected float getProjectileInaccuracy() {
		return 6.0F;
	}

	protected float getProjectileVelocity() {
		return 1.1F;
	}

	public static MovedProjectileDispenserBehaviour of(ProjectileDispenserBehavior vanillaBehaviour) {
		AbstractProjectileDispenseBehaviorAccessor accessor = (AbstractProjectileDispenseBehaviorAccessor) vanillaBehaviour;
		return new MovedProjectileDispenserBehaviour() {
			@Override
			protected ProjectileEntity getProjectileEntity(World world, double x, double y, double z, ItemStack itemStack) {
				return accessor.create$callCreateProjectile(world, new SimplePos(x, y, z), itemStack);
			}

			@Override
			protected float getProjectileInaccuracy() {
				return accessor.create$callGetVariation();
			}

			@Override
			protected float getProjectileVelocity() {
				return accessor.create$callGetForce();
			}
		};
	}
}
