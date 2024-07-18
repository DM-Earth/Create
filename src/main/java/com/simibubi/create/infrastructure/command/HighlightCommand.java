package com.simibubi.create.infrastructure.command;

import java.util.Collection;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

public class HighlightCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("highlight")
			.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
				.then(CommandManager.argument("players", EntityArgumentType.players())
					.executes(ctx -> {
						Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "players");
						BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(ctx, "pos");

						for (ServerPlayerEntity p : players) {
							AllPackets.getChannel().sendToClient(new HighlightPacket(pos), p);
						}

						return players.size();
					}))
				// .requires(AllCommands.sourceIsPlayer)
				.executes(ctx -> {
					BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(ctx, "pos");

					AllPackets.getChannel().sendToClient(new HighlightPacket(pos), (ServerPlayerEntity) ctx.getSource().getEntity());

					return Command.SINGLE_SUCCESS;
				}))
			// .requires(AllCommands.sourceIsPlayer)
			.executes(ctx -> {
				ServerPlayerEntity player = ctx.getSource()
					.getPlayerOrThrow();
				return highlightAssemblyExceptionFor(player, ctx.getSource());
			});

	}

	private static void sendMissMessage(ServerCommandSource source) {
		source.sendFeedback(() ->
			Components.literal("Try looking at a Block that has failed to assemble a Contraption and try again."),
			true);
	}

	private static int highlightAssemblyExceptionFor(ServerPlayerEntity player, ServerCommandSource source) {
		double distance = ReachUtil.reach(player);
		Vec3d start = player.getCameraPosVec(1);
		Vec3d look = player.getRotationVec(1);
		Vec3d end = start.add(look.x * distance, look.y * distance, look.z * distance);
		World world = player.getWorld();

		BlockHitResult ray = world.raycast(
			new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
		if (ray.getType() == HitResult.Type.MISS) {
			sendMissMessage(source);
			return 0;
		}

		BlockPos pos = ray.getBlockPos();
		BlockEntity be = world.getBlockEntity(pos);
		if (!(be instanceof IDisplayAssemblyExceptions)) {
			sendMissMessage(source);
			return 0;
		}

		IDisplayAssemblyExceptions display = (IDisplayAssemblyExceptions) be;
		AssemblyException exception = display.getLastAssemblyException();
		if (exception == null) {
			sendMissMessage(source);
			return 0;
		}

		if (!exception.hasPosition()) {
			source.sendFeedback(() -> Components.literal("Can't highlight a specific position for this issue"), true);
			return Command.SINGLE_SUCCESS;
		}

		BlockPos p = exception.getPosition();
		String command = "/create highlight " + p.getX() + " " + p.getY() + " " + p.getZ();
		return player.server.getCommandManager()
			.executeWithPrefix(source, command);
	}
}
