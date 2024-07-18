package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.config.ui.ConfigHelper;
import com.simibubi.create.foundation.utility.Components;

import io.github.fabricators_of_create.porting_lib.config.ConfigType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Examples:
 * /create config client - to open Create's ConfigGui with the client config already selected
 * /create config "botania:common" - to open Create's ConfigGui with Botania's common config already selected
 * /create config "create:client.client.rainbowDebug" set false - to disable Create's rainbow debug for the sender
 */
public class ConfigCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("config")
				.executes(ctx -> {
					ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
					AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.configScreen.name(), ""), player);

					return Command.SINGLE_SUCCESS;
				})
				.then(CommandManager.argument("path", StringArgumentType.string())
						.executes(ctx -> {
							ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
							AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.configScreen.name(), StringArgumentType.getString(ctx, "path")), player);

							return Command.SINGLE_SUCCESS;
						})
						.then(CommandManager.literal("set")
								.requires(cs -> cs.hasPermissionLevel(2))
								.then(CommandManager.argument("value", StringArgumentType.string())
										.executes(ctx -> {
											String path = StringArgumentType.getString(ctx, "path");
											String value = StringArgumentType.getString(ctx, "value");


											ConfigHelper.ConfigPath configPath;
											try {
												configPath = ConfigHelper.ConfigPath.parse(path);
											} catch (IllegalArgumentException e) {
												ctx.getSource().sendError(Components.literal(e.getMessage()));
												return 0;
											}

											if (configPath.getType() == ConfigType.CLIENT) {
												ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
												AllPackets.getChannel().sendToClient(new SConfigureConfigPacket("SET" + path, value), player);

												return Command.SINGLE_SUCCESS;
											}

											try {
												ConfigHelper.setConfigValue(configPath, value);
												ctx.getSource().sendFeedback(() -> Components.literal("Great Success!"), false);
												return Command.SINGLE_SUCCESS;
											} catch (ConfigHelper.InvalidValueException e) {
												ctx.getSource().sendError(Components.literal("Config could not be set the the specified value!"));
												return 0;
											} catch (Exception e) {
												ctx.getSource().sendError(Components.literal("Something went wrong while trying to set config value. Check the server logs for more information"));
												Create.LOGGER.warn("Exception during server-side config value set:", e);
												return 0;
											}
										})
								)
						)
				);
	}

}
