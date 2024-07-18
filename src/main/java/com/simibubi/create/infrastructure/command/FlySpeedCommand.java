package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.foundation.utility.Components;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.ClientboundPlayerAbilitiesPacketAccessor;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class FlySpeedCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("flySpeed")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.argument("speed", FloatArgumentType.floatArg(0))
				.then(CommandManager.argument("target", EntityArgumentType.player())
					.executes(ctx -> sendFlySpeedUpdate(ctx, EntityArgumentType.getPlayer(ctx, "target"),
						FloatArgumentType.getFloat(ctx, "speed"))))
				.executes(ctx -> sendFlySpeedUpdate(ctx, ctx.getSource()
					.getPlayerOrThrow(), FloatArgumentType.getFloat(ctx, "speed"))))
			.then(CommandManager.literal("reset")
				.then(CommandManager.argument("target", EntityArgumentType.player())
					.executes(ctx -> sendFlySpeedUpdate(ctx, EntityArgumentType.getPlayer(ctx, "target"), 0.05f)))
				.executes(ctx -> sendFlySpeedUpdate(ctx, ctx.getSource()
					.getPlayerOrThrow(), 0.05f))

			);
	}

	private static int sendFlySpeedUpdate(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, float speed) {
		PlayerAbilitiesS2CPacket packet = new PlayerAbilitiesS2CPacket(player.getAbilities());
		((ClientboundPlayerAbilitiesPacketAccessor) packet).port_lib$setFlyingSpeed(speed);
		player.networkHandler.sendPacket(packet);

		ctx.getSource()
			.sendFeedback(() -> Components.literal("Temporarily set " + player.getName()
				.getString() + "'s Flying Speed to: " + speed), true);

		return Command.SINGLE_SUCCESS;
	}

}
