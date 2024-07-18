package com.simibubi.create.infrastructure.command;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;

public class CameraAngleCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("angle")
				.requires(cs -> cs.hasPermissionLevel(2))
				.then(CommandManager.argument("players", EntityArgumentType.players())
						.then(CommandManager.literal("yaw")
								.then(CommandManager.argument("degrees", FloatArgumentType.floatArg())
										.executes(context -> updateCameraAngle(context, true))
								)
						).then(CommandManager.literal("pitch")
								.then(CommandManager.argument("degrees", FloatArgumentType.floatArg())
										.executes(context -> updateCameraAngle(context, false))
								)
						).then(CommandManager.literal("mode")
								.then(CommandManager.literal("linear")
										.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.LINEAR.name()))
										.then(CommandManager.argument("speed", FloatArgumentType.floatArg(0))
												.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.LINEAR.name(), FloatArgumentType.getFloat(context, "speed")))
										)
								).then(CommandManager.literal("exponential")
										.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.EXPONENTIAL.name()))
										.then(CommandManager.argument("speed", FloatArgumentType.floatArg(0))
												.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.EXPONENTIAL.name(), FloatArgumentType.getFloat(context, "speed")))
										)
								)
						)
				);
	}

	private static int updateCameraAngle(CommandContext<ServerCommandSource> ctx, boolean yaw) throws CommandSyntaxException {
		AtomicInteger targets = new AtomicInteger(0);

		float angleTarget = FloatArgumentType.getFloat(ctx, "degrees");
		String optionName = (yaw ? SConfigureConfigPacket.Actions.camAngleYawTarget : SConfigureConfigPacket.Actions.camAnglePitchTarget).name();

		getPlayersFromContext(ctx).forEach(player -> {
			AllPackets.getChannel().sendToClient(
					new SConfigureConfigPacket(optionName, String.valueOf(angleTarget)), player
			);
			targets.incrementAndGet();
		});

		return targets.get();
	}

	private static int updateCameraAnimationMode(CommandContext<ServerCommandSource> ctx, String value) throws CommandSyntaxException {
		AtomicInteger targets = new AtomicInteger(0);

		getPlayersFromContext(ctx).forEach(player -> {
			AllPackets.getChannel().sendToClient(
					new SConfigureConfigPacket(SConfigureConfigPacket.Actions.camAngleFunction.name(), value), player
			);
			targets.incrementAndGet();
		});

		return targets.get();
	}

	private static int updateCameraAnimationMode(CommandContext<ServerCommandSource> ctx, String value, float speed) throws CommandSyntaxException {
		AtomicInteger targets = new AtomicInteger(0);

		getPlayersFromContext(ctx).forEach(player -> {
			AllPackets.getChannel().sendToClient(
					new SConfigureConfigPacket(SConfigureConfigPacket.Actions.camAngleFunction.name(), value + ":" + speed), player
			);
			targets.incrementAndGet();
		});

		return targets.get();
	}

	private static Collection<ServerPlayerEntity> getPlayersFromContext(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		return EntityArgumentType.getPlayers(ctx, "players");
	}
}
