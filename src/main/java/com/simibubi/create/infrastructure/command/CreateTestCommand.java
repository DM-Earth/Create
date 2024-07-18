package com.simibubi.create.infrastructure.command;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicExport.SchematicExportResult;
import com.simibubi.create.content.schematics.client.SchematicAndQuillHandler;
import com.simibubi.create.foundation.utility.Components;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;

/**
 * This command allows for quick exporting of GameTests.
 * It is only registered in a client development environment. It is not safe in production or multiplayer.
 */
public class CreateTestCommand {
	private static final Path gametests = FabricLoader.getInstance().getGameDir()
			.getParent()
			.resolve("src/main/resources/data/create/structures/gametest")
			.toAbsolutePath();

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return literal("test")
				.then(literal("export")
						.then(argument("path", StringArgumentType.greedyString())
								.suggests(CreateTestCommand::getSuggestions)
								.executes(ctx -> handleExport(
										ctx.getSource(),
										ctx.getSource().getWorld(),
										StringArgumentType.getString(ctx, "path")
								))
						)
				);
	}

	private static int handleExport(ServerCommandSource source, ServerWorld level, String path) {
		SchematicAndQuillHandler handler = CreateClient.SCHEMATIC_AND_QUILL_HANDLER;
		if (handler.firstPos == null || handler.secondPos == null) {
			source.sendError(Components.literal("You must select an area with the Schematic and Quill first."));
			return 0;
		}
		SchematicExportResult result = SchematicExport.saveSchematic(
				gametests, path, true,
				level, handler.firstPos, handler.secondPos
		);
		if (result == null)
			source.sendError(Components.literal("Failed to export, check logs").formatted(Formatting.RED));
		else {
			sendSuccess(source, "Successfully exported test!", Formatting.GREEN);
			sendSuccess(source, "Overwritten: " + result.overwritten(), Formatting.AQUA);
			sendSuccess(source, "File: " + result.file(), Formatting.GRAY);
		}
		return 0;
	}

	private static void sendSuccess(ServerCommandSource source, String text, Formatting color) {
		source.sendFeedback(() -> Components.literal(text).formatted(color), true);
	}

	// find existing tests and folders for autofill
	private static CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context,
																 SuggestionsBuilder builder) throws CommandSyntaxException {
		String path = builder.getRemaining();
		if (!path.contains("/") || path.contains(".."))
			return findInDir(gametests, builder);
		int lastSlash = path.lastIndexOf("/");
		Path subDir = gametests.resolve(path.substring(0, lastSlash));
		if (Files.exists(subDir))
			findInDir(subDir, builder);
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> findInDir(Path dir, SuggestionsBuilder builder) {
		try (Stream<Path> paths = Files.list(dir)) {
			paths.filter(p -> Files.isDirectory(p) || p.toString().endsWith(".nbt"))
					.forEach(path -> {
						String file = path.toString()
								.replaceAll("\\\\", "/")
								.substring(gametests.toString().length() + 1);
						if (Files.isDirectory(path))
							file += "/";
						builder.suggest(file);
					});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return builder.buildFuture();
	}
}
