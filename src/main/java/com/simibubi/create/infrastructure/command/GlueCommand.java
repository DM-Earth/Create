package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class GlueCommand {
	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("glue")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
				.then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
					.executes(ctx -> {
						BlockPos from = BlockPosArgumentType.getLoadedBlockPos(ctx, "from");
						BlockPos to = BlockPosArgumentType.getLoadedBlockPos(ctx, "to");

						ServerWorld world = ctx.getSource()
							.getWorld();

						SuperGlueEntity entity = new SuperGlueEntity(world, SuperGlueEntity.span(from, to));
						entity.playPlaceSound();
						world.spawnEntity(entity);
						return 1;
					})));

	}
}
