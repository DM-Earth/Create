package com.simibubi.create.infrastructure.command;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.ponder.PonderRegistry;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PonderCommand {
	public static final SuggestionProvider<ServerCommandSource> ITEM_PONDERS = SuggestionProviders.register(new Identifier("all_ponders"), (iSuggestionProviderCommandContext, builder) -> CommandSource.suggestIdentifiers(PonderRegistry.ALL.keySet().stream(), builder));

	static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("ponder")
				.requires(cs -> cs.hasPermissionLevel(0))
				.executes(ctx -> openScene("index", ctx.getSource().getPlayerOrThrow()))
				.then(CommandManager.argument("scene", IdentifierArgumentType.identifier())
						.suggests(ITEM_PONDERS)
						.executes(ctx -> openScene(IdentifierArgumentType.getIdentifier(ctx, "scene").toString(), ctx.getSource().getPlayerOrThrow()))
						.then(CommandManager.argument("targets", EntityArgumentType.players())
								.requires(cs -> cs.hasPermissionLevel(2))
								.executes(ctx -> openScene(IdentifierArgumentType.getIdentifier(ctx, "scene").toString(), EntityArgumentType.getPlayers(ctx, "targets")))
						)
				);

	}

	private static int openScene(String sceneId, ServerPlayerEntity player) {
		return openScene(sceneId, ImmutableList.of(player));
	}

	private static int openScene(String sceneId, Collection<? extends ServerPlayerEntity> players) {
		for (ServerPlayerEntity player : players) {
			if (player instanceof FakePlayer)
				continue;

			AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.openPonder.name(), sceneId), player);
		}
		return Command.SINGLE_SUCCESS;
	}
}
