package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class PassengerCommand {

	static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("passenger")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.argument("rider", EntityArgumentType.entity())
				.then(CommandManager.argument("vehicle", EntityArgumentType.entity())
					.executes(ctx -> {
						run(ctx.getSource(), EntityArgumentType.getEntity(ctx, "vehicle"),
							EntityArgumentType.getEntity(ctx, "rider"), 0);
						return 1;
					})
					.then(CommandManager.argument("seatIndex", IntegerArgumentType.integer(0))
						.executes(ctx -> {
							run(ctx.getSource(), EntityArgumentType.getEntity(ctx, "vehicle"),
								EntityArgumentType.getEntity(ctx, "rider"),
								IntegerArgumentType.getInteger(ctx, "seatIndex"));
							return 1;
						}))));
	}

	private static void run(ServerCommandSource source, Entity vehicle, Entity rider, int index) {
		if (vehicle == rider)
			return;
		if (rider instanceof CarriageContraptionEntity)
			return;
		if (rider instanceof ControlledContraptionEntity)
			return;
		
		if (vehicle instanceof AbstractContraptionEntity ace) {
			if (ace.getContraption()
				.getSeats()
				.size() > index)
				ace.addSittingPassenger(rider, index);
			return;
		}
		
		rider.startRiding(vehicle, true);
	}
}
