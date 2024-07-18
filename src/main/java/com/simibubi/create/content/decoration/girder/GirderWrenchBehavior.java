package com.simibubi.create.content.decoration.girder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;

public class GirderWrenchBehavior {

	@Environment(EnvType.CLIENT)
	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult result))
			return;

		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();
		PlayerEntity player = mc.player;
		ItemStack heldItem = player.getMainHandStack();

		if (player.isSneaking())
			return;

		if (!AllBlocks.METAL_GIRDER.has(world.getBlockState(pos)))
			return;

		if (!AllItems.WRENCH.isIn(heldItem))
			return;

		Pair<Direction, Action> dirPair = getDirectionAndAction(result, world, pos);
		if (dirPair == null)
			return;

		Vec3d center = VecHelper.getCenterOf(pos);
		Vec3d edge = center.add(Vec3d.of(dirPair.getFirst()
			.getVector())
			.multiply(0.4));
		Direction.Axis[] axes = Arrays.stream(Iterate.axes)
			.filter(axis -> axis != dirPair.getFirst()
				.getAxis())
			.toArray(Direction.Axis[]::new);

		double normalMultiplier = dirPair.getSecond() == Action.PAIR ? 4 : 1;
		Vec3d corner1 = edge
			.add(Vec3d.of(Direction.from(axes[0], Direction.AxisDirection.POSITIVE)
				.getVector())
				.multiply(0.3))
			.add(Vec3d.of(Direction.from(axes[1], Direction.AxisDirection.POSITIVE)
				.getVector())
				.multiply(0.3))
			.add(Vec3d.of(dirPair.getFirst()
				.getVector())
				.multiply(0.1 * normalMultiplier));

		normalMultiplier = dirPair.getSecond() == Action.HORIZONTAL ? 9 : 2;
		Vec3d corner2 = edge
			.add(Vec3d.of(Direction.from(axes[0], Direction.AxisDirection.NEGATIVE)
				.getVector())
				.multiply(0.3))
			.add(Vec3d.of(Direction.from(axes[1], Direction.AxisDirection.NEGATIVE)
				.getVector())
				.multiply(0.3))
			.add(Vec3d.of(dirPair.getFirst()
				.getOpposite()
				.getVector())
				.multiply(0.1 * normalMultiplier));

		CreateClient.OUTLINER.showAABB("girderWrench", new Box(corner1, corner2))
			.lineWidth(1 / 32f)
			.colored(new Color(127, 127, 127));
	}

	@Nullable
	private static Pair<Direction, Action> getDirectionAndAction(BlockHitResult result, World world, BlockPos pos) {
		List<Pair<Direction, Action>> validDirections = getValidDirections(world, pos);

		if (validDirections.isEmpty())
			return null;

		List<Direction> directions = IPlacementHelper.orderedByDistance(pos, result.getPos(),
			validDirections.stream()
				.map(Pair::getFirst)
				.toList());

		if (directions.isEmpty())
			return null;

		Direction dir = directions.get(0);
		return validDirections.stream()
			.filter(pair -> pair.getFirst() == dir)
			.findFirst()
			.orElseGet(() -> Pair.of(dir, Action.SINGLE));
	}

	public static List<Pair<Direction, Action>> getValidDirections(BlockView level, BlockPos pos) {
		BlockState blockState = level.getBlockState(pos);

		if (!AllBlocks.METAL_GIRDER.has(blockState))
			return Collections.emptyList();

		return Arrays.stream(Iterate.directions)
			.<Pair<Direction, Action>>mapMulti((direction, consumer) -> {
				BlockState other = level.getBlockState(pos.offset(direction));

				if (!blockState.get(GirderBlock.X) && !blockState.get(GirderBlock.Z))
					return;

				// up and down
				if (direction.getAxis() == Direction.Axis.Y) {
					// no other girder in target dir
					if (!AllBlocks.METAL_GIRDER.has(other)) {
						if (!blockState.get(GirderBlock.X) ^ !blockState.get(GirderBlock.Z))
							consumer.accept(Pair.of(direction, Action.SINGLE));
						return;
					}
					// this girder is a pole or cross
					if (blockState.get(GirderBlock.X) == blockState.get(GirderBlock.Z))
						return;
					// other girder is a pole or cross
					if (other.get(GirderBlock.X) == other.get(GirderBlock.Z))
						return;
					// toggle up/down connection for both
					consumer.accept(Pair.of(direction, Action.PAIR));

					return;
				}

//					if (AllBlocks.METAL_GIRDER.has(other))
//						consumer.accept(Pair.of(direction, Action.HORIZONTAL));

			})
			.toList();
	}

	public static boolean handleClick(World level, BlockPos pos, BlockState state, BlockHitResult result) {
		Pair<Direction, Action> dirPair = getDirectionAndAction(result, level, pos);
		if (dirPair == null)
			return false;
		if (level.isClient)
			return true;
		if (!state.get(GirderBlock.X) && !state.get(GirderBlock.Z))
			return false;

		Direction dir = dirPair.getFirst();

		BlockPos otherPos = pos.offset(dir);
		BlockState other = level.getBlockState(otherPos);

		if (dir == Direction.UP) {
			level.setBlockState(pos, postProcess(state.cycle(GirderBlock.TOP)), 2 | 16);
			if (dirPair.getSecond() == Action.PAIR && AllBlocks.METAL_GIRDER.has(other))
				level.setBlockState(otherPos, postProcess(other.cycle(GirderBlock.BOTTOM)), 2 | 16);
			return true;
		}

		if (dir == Direction.DOWN) {
			level.setBlockState(pos, postProcess(state.cycle(GirderBlock.BOTTOM)), 2 | 16);
			if (dirPair.getSecond() == Action.PAIR && AllBlocks.METAL_GIRDER.has(other))
				level.setBlockState(otherPos, postProcess(other.cycle(GirderBlock.TOP)), 2 | 16);
			return true;
		}

//		if (dirPair.getSecond() == Action.HORIZONTAL) {
//			BooleanProperty property = dir.getAxis() == Direction.Axis.X ? GirderBlock.X : GirderBlock.Z;
//			level.setBlock(pos, state.cycle(property), 2 | 16);
//
//			return true;
//		}

		return true;
	}

	private static BlockState postProcess(BlockState newState) {
		if (newState.get(GirderBlock.TOP) && newState.get(GirderBlock.BOTTOM))
			return newState;
		if (newState.get(GirderBlock.AXIS) != Axis.Y)
			return newState;
		return newState.with(GirderBlock.AXIS, newState.get(GirderBlock.X) ? Axis.X : Axis.Z);
	}

	private enum Action {
		SINGLE, PAIR, HORIZONTAL
	}

}
