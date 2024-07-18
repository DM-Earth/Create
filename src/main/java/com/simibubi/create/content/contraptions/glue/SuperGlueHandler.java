package com.simibubi.create.content.contraptions.glue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;
import com.simibubi.create.foundation.utility.worldWrappers.RayTraceWorld;

public class SuperGlueHandler {

	public static void glueListensForBlockPlacement(ItemPlacementContext context, BlockPos pos, BlockState state) {
		WorldAccess world = context.getWorld();
		PlayerEntity entity = context.getPlayer();

		if (entity == null || AdventureUtil.isAdventure(entity))
			return;
		if (world.isClient())
			return;

		Set<SuperGlueEntity> cached = new HashSet<>();
		for (Direction direction : Iterate.directions) {
			BlockPos relative = pos.offset(direction);
			if (SuperGlueEntity.isGlued(world, pos, direction, cached)
				&& BlockMovementChecks.isMovementNecessary(world.getBlockState(relative), entity.getWorld(), relative))
				AllPackets.getChannel().sendToClientsTrackingAndSelf(new GlueEffectPacket(pos, direction, true), entity);
		}

		glueInOffHandAppliesOnBlockPlace(context.getWorld().getBlockState(context.getBlockPos().offset(context.getSide().getOpposite())), pos, entity);
	}

	public static void glueInOffHandAppliesOnBlockPlace(BlockState placedAgainst, BlockPos pos, PlayerEntity placer) {
		ItemStack itemstack = placer.getOffHandStack();
		if (!AllItems.SUPER_GLUE.isIn(itemstack))
			return;
		if (AllItems.WRENCH.isIn(placer.getMainHandStack()))
			return;
		if (placedAgainst == IPlacementHelper.ID)
			return;

		double distance = ReachUtil.reach(placer);
		Vec3d start = placer.getCameraPosVec(1);
		Vec3d look = placer.getRotationVec(1);
		Vec3d end = start.add(look.x * distance, look.y * distance, look.z * distance);
		World world = placer.getWorld();

		RayTraceWorld rayTraceWorld =
			new RayTraceWorld(world, (p, state) -> p.equals(pos) ? Blocks.AIR.getDefaultState() : state);
		BlockHitResult ray =
			rayTraceWorld.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, placer));

		Direction face = ray.getSide();
		if (face == null || ray.getType() == Type.MISS)
			return;

		BlockPos gluePos = ray.getBlockPos();
		if (!gluePos.offset(face)
			.equals(pos)) {
			return;
		}

		if (SuperGlueEntity.isGlued(world, gluePos, face, null))
			return;

		SuperGlueEntity entity = new SuperGlueEntity(world, SuperGlueEntity.span(gluePos, gluePos.offset(face)));
		NbtCompound compoundnbt = itemstack.getNbt();
		if (compoundnbt != null)
			EntityType.loadFromEntityNbt(world, placer, entity, compoundnbt);

		if (SuperGlueEntity.isValidFace(world, gluePos, face)) {
			if (!world.isClient) {
				world.spawnEntity(entity);
				AllPackets.getChannel().sendToClientsTracking(new GlueEffectPacket(gluePos, face, true), entity);
			}
			itemstack.damage(1, placer, SuperGlueItem::onBroken);
		}
	}

}
