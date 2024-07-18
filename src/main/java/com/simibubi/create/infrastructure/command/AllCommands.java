package com.simibubi.create.infrastructure.command;

import java.util.Collections;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class AllCommands {

	public static final Predicate<ServerCommandSource> SOURCE_IS_PLAYER = cs -> cs.getEntity() instanceof PlayerEntity;

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

		LiteralCommandNode<ServerCommandSource> util = buildUtilityCommands();

		LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("create")
				.requires(cs -> cs.hasPermissionLevel(0))
				// general purpose
				.then(new ToggleDebugCommand().register())
				.then(FabulousWarningCommand.register())
				.then(OverlayConfigCommand.register())
				.then(DumpRailwaysCommand.register())
				.then(FixLightingCommand.register())
				.then(DebugInfoCommand.register())
				.then(HighlightCommand.register())
				.then(KillTrainCommand.register())
				.then(PassengerCommand.register())
				.then(CouplingCommand.register())
				.then(ConfigCommand.register())
				.then(PonderCommand.register())
				.then(CloneCommand.register())
				.then(GlueCommand.register())


				// utility
				.then(util);

		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isDevelopmentEnvironment() && loader.getEnvironmentType() == EnvType.CLIENT)
			root.then(CreateTestCommand.register());

		LiteralCommandNode<ServerCommandSource> createRoot = dispatcher.register(root);

		createRoot.addChild(buildRedirect("u", util));

		CommandNode<ServerCommandSource> c = dispatcher.findNode(Collections.singleton("c"));
		if (c != null)
			return;

		dispatcher.getRoot()
			.addChild(buildRedirect("c", createRoot));

	}

	private static LiteralCommandNode<ServerCommandSource> buildUtilityCommands() {

		return CommandManager.literal("util")
				.then(ReplaceInCommandBlocksCommand.register())
				.then(ClearBufferCacheCommand.register())
				.then(CameraDistanceCommand.register())
				.then(CameraAngleCommand.register())
				.then(FlySpeedCommand.register())
				//.then(DebugValueCommand.register())
				//.then(KillTPSCommand.register())
				.build();

	}

	/**
	 * *****
	 * https://github.com/VelocityPowered/Velocity/blob/8abc9c80a69158ebae0121fda78b55c865c0abad/proxy/src/main/java/com/velocitypowered/proxy/util/BrigadierUtils.java#L38
	 * *****
	 * <p>
	 * Returns a literal node that redirects its execution to
	 * the given destination node.
	 *
	 * @param alias       the command alias
	 * @param destination the destination node
	 *
	 * @return the built node
	 */
	public static LiteralCommandNode<ServerCommandSource> buildRedirect(final String alias, final LiteralCommandNode<ServerCommandSource> destination) {
		// Redirects only work for nodes with children, but break the top argument-less command.
		// Manually adding the root command after setting the redirect doesn't fix it.
		// See https://github.com/Mojang/brigadier/issues/46). Manually clone the node instead.
		LiteralArgumentBuilder<ServerCommandSource> builder = LiteralArgumentBuilder
				.<ServerCommandSource>literal(alias)
				.requires(destination.getRequirement())
				.forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
				.executes(destination.getCommand());
		for (CommandNode<ServerCommandSource> child : destination.getChildren()) {
			builder.then(child);
		}
		return builder.build();
	}

}
