package com.simibubi.create.infrastructure.command;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.Components;

public class CloneCommand {

	private static final Dynamic2CommandExceptionType CLONE_TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
		(arg1, arg2) -> Components.translatable("commands.clone.toobig", arg1, arg2));

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("clone")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.argument("begin", BlockPosArgumentType.blockPos())
				.then(CommandManager.argument("end", BlockPosArgumentType.blockPos())
					.then(CommandManager.argument("destination", BlockPosArgumentType.blockPos())
						.then(CommandManager.literal("skipBlocks")
							.executes(ctx -> doClone(ctx.getSource(), BlockPosArgumentType.getLoadedBlockPos(ctx, "begin"),
								BlockPosArgumentType.getLoadedBlockPos(ctx, "end"),
								BlockPosArgumentType.getLoadedBlockPos(ctx, "destination"), false)))
						.executes(ctx -> doClone(ctx.getSource(), BlockPosArgumentType.getLoadedBlockPos(ctx, "begin"),
							BlockPosArgumentType.getLoadedBlockPos(ctx, "end"),
							BlockPosArgumentType.getLoadedBlockPos(ctx, "destination"), true)))))
			.executes(ctx -> {
				ctx.getSource()
					.sendFeedback(() -> Components.literal(
						"Clones all blocks as well as super glue from the specified area to the target destination"),
						true);

				return Command.SINGLE_SUCCESS;
			});

	}

	private static int doClone(ServerCommandSource source, BlockPos begin, BlockPos end, BlockPos destination,
		boolean cloneBlocks) throws CommandSyntaxException {
		BlockBox sourceArea = BlockBox.create(begin, end);
		BlockPos destinationEnd = destination.add(sourceArea.getDimensions());
		BlockBox destinationArea = BlockBox.create(destination, destinationEnd);

		int i = sourceArea.getBlockCountX() * sourceArea.getBlockCountY() * sourceArea.getBlockCountZ();
		if (i > 32768)
			throw CLONE_TOO_BIG_EXCEPTION.create(32768, i);

		ServerWorld world = source.getWorld();

		if (!world.isRegionLoaded(begin, end) || !world.isRegionLoaded(destination, destinationEnd))
			throw BlockPosArgumentType.UNLOADED_EXCEPTION.create();

		BlockPos diffToTarget = new BlockPos(destinationArea.getMinX() - sourceArea.getMinX(),
			destinationArea.getMinY() - sourceArea.getMinY(), destinationArea.getMinZ() - sourceArea.getMinZ());

		int blockPastes = cloneBlocks ? cloneBlocks(sourceArea, world, diffToTarget) : 0;
		int gluePastes = cloneGlue(sourceArea, world, diffToTarget);

		if (cloneBlocks)
			source.sendFeedback(() -> Components.literal("Successfully cloned " + blockPastes + " Blocks"), true);

		source.sendFeedback(() -> Components.literal("Successfully applied glue " + gluePastes + " times"), true);
		return blockPastes + gluePastes;

	}

	private static int cloneGlue(BlockBox sourceArea, ServerWorld world, BlockPos diffToTarget) {
		int gluePastes = 0;

		Box bb = new Box(sourceArea.getMinX(), sourceArea.getMinY(), sourceArea.getMinZ(), sourceArea.getMaxX() + 1,
			sourceArea.getMaxY() + 1, sourceArea.getMaxZ() + 1);
		for (SuperGlueEntity g : SuperGlueEntity.collectCropped(world, bb)) {
			g.setPosition(g.getPos()
				.add(Vec3d.of(diffToTarget)));
			world.spawnEntity(g);
			gluePastes++;
		}

		return gluePastes;
	}

	private static int cloneBlocks(BlockBox sourceArea, ServerWorld world, BlockPos diffToTarget) {
		int blockPastes = 0;

		List<StructureTemplate.StructureBlockInfo> blocks = Lists.newArrayList();
		List<StructureTemplate.StructureBlockInfo> beBlocks = Lists.newArrayList();

		for (int z = sourceArea.getMinZ(); z <= sourceArea.getMaxZ(); ++z) {
			for (int y = sourceArea.getMinY(); y <= sourceArea.getMaxY(); ++y) {
				for (int x = sourceArea.getMinX(); x <= sourceArea.getMaxX(); ++x) {
					BlockPos currentPos = new BlockPos(x, y, z);
					BlockPos newPos = currentPos.add(diffToTarget);
					CachedBlockPosition cached = new CachedBlockPosition(world, currentPos, false);
					BlockState state = cached.getBlockState();
					BlockEntity be = world.getBlockEntity(currentPos);
					if (be != null) {
						NbtCompound nbt = be.createNbtWithIdentifyingData();
						beBlocks.add(new StructureTemplate.StructureBlockInfo(newPos, state, nbt));
					} else {
						blocks.add(new StructureTemplate.StructureBlockInfo(newPos, state, null));
					}
				}
			}
		}

		List<StructureTemplate.StructureBlockInfo> allBlocks = Lists.newArrayList();
		allBlocks.addAll(blocks);
		allBlocks.addAll(beBlocks);

		List<StructureTemplate.StructureBlockInfo> reverse = Lists.reverse(allBlocks);

		for (StructureTemplate.StructureBlockInfo info : reverse) {
			BlockEntity be = world.getBlockEntity(info.pos());
			Clearable.clear(be);
			world.setBlockState(info.pos(), Blocks.BARRIER.getDefaultState(), 2);
		}

		for (StructureTemplate.StructureBlockInfo info : allBlocks) {
			if (world.setBlockState(info.pos(), info.state(), 2))
				blockPastes++;
		}

		for (StructureTemplate.StructureBlockInfo info : beBlocks) {
			BlockEntity be = world.getBlockEntity(info.pos());
			if (be != null && info.nbt() != null) {
				info.nbt().putInt("x", info.pos().getX());
				info.nbt().putInt("y", info.pos().getY());
				info.nbt().putInt("z", info.pos().getZ());
				be.readNbt(info.nbt());
				be.markDirty();
			}

			// idk why the state is set twice for a be, but its done like this in the
			// original clone command
			world.setBlockState(info.pos(), info.state(), 2);
		}

		for (StructureTemplate.StructureBlockInfo info : reverse) {
			world.updateNeighbors(info.pos(), info.state().getBlock());
		}

		world.getBlockTickScheduler()
			.scheduleTicks(sourceArea, diffToTarget);

		return blockPastes;
	}

}
