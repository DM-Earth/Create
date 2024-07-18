package com.simibubi.create.infrastructure.command;

import org.apache.commons.lang3.mutable.MutableInt;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CommandBlockExecutor;

public class ReplaceInCommandBlocksCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("replaceInCommandBlocks")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.argument("begin", BlockPosArgumentType.blockPos())
				.then(CommandManager.argument("end", BlockPosArgumentType.blockPos())
					.then(CommandManager.argument("toReplace", StringArgumentType.string())
						.then(CommandManager.argument("replaceWith", StringArgumentType.string())
							.executes(ctx -> {
								doReplace(ctx.getSource(), BlockPosArgumentType.getLoadedBlockPos(ctx, "begin"),
									BlockPosArgumentType.getLoadedBlockPos(ctx, "end"),
									StringArgumentType.getString(ctx, "toReplace"),
									StringArgumentType.getString(ctx, "replaceWith"));
								return 1;
							})))));

	}

	private static void doReplace(ServerCommandSource source, BlockPos from, BlockPos to, String toReplace,
		String replaceWith) {
		ServerWorld world = source.getWorld();
		MutableInt blocks = new MutableInt(0);
		BlockPos.stream(from, to)
			.forEach(pos -> {
				BlockState blockState = world.getBlockState(pos);
				if (!(blockState.getBlock() instanceof CommandBlock))
					return;
				BlockEntity blockEntity = world.getBlockEntity(pos);
				if (!(blockEntity instanceof CommandBlockBlockEntity))
					return;
				CommandBlockBlockEntity cb = (CommandBlockBlockEntity) blockEntity;
				CommandBlockExecutor commandBlockLogic = cb.getCommandExecutor();
				String command = commandBlockLogic.getCommand();
				if (command.indexOf(toReplace) != -1)
					blocks.increment();
				commandBlockLogic.setCommand(command.replaceAll(toReplace, replaceWith));
				cb.markDirty();
				world.updateListeners(pos, blockState, blockState, 2);
			});
		int intValue = blocks.intValue();
		if (intValue == 0) {
			source.sendFeedback(() -> Components.literal("Couldn't find \"" + toReplace + "\" anywhere."), true);
			return;
		}
		source.sendFeedback(() -> Components.literal("Replaced occurrences in " + intValue + " blocks."), true);
	}

}
