package com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.RaycastHelper;

public class EdgeInteractionHandler {

	public static ActionResult onBlockActivated(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
//		Level world = event.getWorld();
		BlockPos pos = hitResult.getBlockPos();//event.getPos();
//		Player player = event.getPlayer();
//		InteractionHand hand = event.getHand();
		ItemStack heldItem = player.getStackInHand(hand);

		if (player.isSneaking() || player.isSpectator() || AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		EdgeInteractionBehaviour behaviour = BlockEntityBehaviour.get(world, pos, EdgeInteractionBehaviour.TYPE);
		if (behaviour == null)
			return ActionResult.PASS;
		if (!behaviour.requiredPredicate.test(heldItem.getItem()))
			return ActionResult.PASS;
		BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, 10);
		if (ray == null)
			return ActionResult.PASS;

		Direction activatedDirection = getActivatedDirection(world, pos, ray.getSide(), ray.getPos(), behaviour);
		if (activatedDirection == null)
			return ActionResult.PASS;

		if (!world.isClient())
			behaviour.connectionCallback.apply(world, pos, pos.offset(activatedDirection));
//		event.setCanceled(true);
//		event.setCancellationResult(InteractionResult.SUCCESS);
		world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, .25f, .1f);
		return ActionResult.SUCCESS;
	}

	public static List<Direction> getConnectiveSides(World world, BlockPos pos, Direction face,
		EdgeInteractionBehaviour behaviour) {
		List<Direction> sides = new ArrayList<>(6);
		if (BlockHelper.hasBlockSolidSide(world.getBlockState(pos.offset(face)), world, pos.offset(face), face.getOpposite()))
			return sides;

		for (Direction direction : Iterate.directions) {
			if (direction.getAxis() == face.getAxis())
				continue;
			BlockPos neighbourPos = pos.offset(direction);
			if (BlockHelper.hasBlockSolidSide(world.getBlockState(neighbourPos.offset(face)), world, neighbourPos.offset(face),
				face.getOpposite()))
				continue;
			if (!behaviour.connectivityPredicate.test(world, pos, face, direction))
				continue;
			sides.add(direction);
		}

		return sides;
	}

	public static Direction getActivatedDirection(World world, BlockPos pos, Direction face, Vec3d hit,
		EdgeInteractionBehaviour behaviour) {
		for (Direction facing : getConnectiveSides(world, pos, face, behaviour)) {
			Box bb = getBB(pos, facing);
			if (bb.contains(hit))
				return facing;
		}
		return null;
	}

	static Box getBB(BlockPos pos, Direction direction) {
		Box bb = new Box(pos);
		Vec3i vec = direction.getVector();
		int x = vec.getX();
		int y = vec.getY();
		int z = vec.getZ();
		double margin = 10 / 16f;
		double absX = Math.abs(x) * margin;
		double absY = Math.abs(y) * margin;
		double absZ = Math.abs(z) * margin;

		bb = bb.shrink(absX, absY, absZ);
		bb = bb.offset(absX / 2d, absY / 2d, absZ / 2d);
		bb = bb.offset(x / 2d, y / 2d, z / 2d);
		bb = bb.expand(1 / 256f);
		return bb;
	}

}
