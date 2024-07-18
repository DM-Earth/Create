package com.simibubi.create.foundation.mixin.fabric;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.foundation.ponder.PonderWorld;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.EntityAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	public abstract World getWorld();

	// AbstractMinecart does not override remove, so we have to inject here.
	@Inject(method = "remove", at = @At("HEAD"))
	private void removeMinecartController(RemovalReason reason, CallbackInfo ci) {
		//noinspection ConstantValue
		if ((Object) this instanceof AbstractMinecartEntity cart) {
			CapabilityMinecartController.onCartRemoved(getWorld(), cart);
		}
	}

	/**
	 * @author AeiouEnigma
	 * @reason We stan Lithium's collision optimizations but need to ensure they aren't applied in Create's PonderWorld.
	 */
	@Inject(method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
	private static void create$stopLithiumCollisionChangesInPonderWorld(@Nullable Entity entity, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> shapes, CallbackInfoReturnable<Vec3d> ci) {
		if (world instanceof PonderWorld) {
			// Vanilla copy
			ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(shapes.size() + 1);
			if (!shapes.isEmpty()) {
				builder.addAll(shapes);
			}

			WorldBorder worldBorder = world.getWorldBorder();
			boolean bl = entity != null && worldBorder.canCollide(entity, entityBoundingBox.stretch(movement));
			if (bl) {
				builder.add(worldBorder.asVoxelShape());
			}

			builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.stretch(movement)));
			// Prevent Lithium's changes from executing for PonderWorlds
			ci.setReturnValue(EntityAccessor.port_lib$collideWithShapes(movement, entityBoundingBox, builder.build()));

		}
	}
}
