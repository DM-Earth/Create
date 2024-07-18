package com.simibubi.create.infrastructure.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.simibubi.create.content.contraptions.minecart.CouplingHandler;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;

public class CouplingCommand {

	public static final SimpleCommandExceptionType ONLY_MINECARTS_ALLOWED =
		new SimpleCommandExceptionType(Components.literal("Only Minecarts can be coupled"));
	public static final SimpleCommandExceptionType SAME_DIMENSION =
		new SimpleCommandExceptionType(Components.literal("Minecarts have to be in the same Dimension"));
	public static final DynamicCommandExceptionType TWO_CARTS =
		new DynamicCommandExceptionType(a -> Components.literal(
			"Your selector targeted " + a + " entities. You can only couple 2 Minecarts at a time."));

	public static ArgumentBuilder<ServerCommandSource, ?> register() {

		return CommandManager.literal("coupling")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.literal("add")
				.then(CommandManager.argument("cart1", EntityArgumentType.entity())
					.then(CommandManager.argument("cart2", EntityArgumentType.entity())
						.executes(ctx -> {
							Entity cart1 = EntityArgumentType.getEntity(ctx, "cart1");
							if (!(cart1 instanceof AbstractMinecartEntity))
								throw ONLY_MINECARTS_ALLOWED.create();

							Entity cart2 = EntityArgumentType.getEntity(ctx, "cart2");
							if (!(cart2 instanceof AbstractMinecartEntity))
								throw ONLY_MINECARTS_ALLOWED.create();

							if (!cart1.getEntityWorld()
								.equals(cart2.getEntityWorld()))
								throw SAME_DIMENSION.create();

							Entity source = ctx.getSource()
								.getEntity();

							CouplingHandler.tryToCoupleCarts(
								source instanceof PlayerEntity ? (PlayerEntity) source : null, cart1.getEntityWorld(),
								cart1.getId(), cart2.getId());

							return Command.SINGLE_SUCCESS;
						})))
				.then(CommandManager.argument("carts", EntityArgumentType.entities())
					.executes(ctx -> {
						Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "carts");
						if (entities.size() != 2)
							throw TWO_CARTS.create(entities.size());

						ArrayList<? extends Entity> eList = Lists.newArrayList(entities);
						Entity cart1 = eList.get(0);
						if (!(cart1 instanceof AbstractMinecartEntity))
							throw ONLY_MINECARTS_ALLOWED.create();

						Entity cart2 = eList.get(1);
						if (!(cart2 instanceof AbstractMinecartEntity))
							throw ONLY_MINECARTS_ALLOWED.create();

						if (!cart1.getEntityWorld()
							.equals(cart2.getEntityWorld()))
							throw SAME_DIMENSION.create();

						Entity source = ctx.getSource()
							.getEntity();

						CouplingHandler.tryToCoupleCarts(source instanceof PlayerEntity ? (PlayerEntity) source : null,
							cart1.getEntityWorld(), cart1.getId(), cart2.getId());

						return Command.SINGLE_SUCCESS;
					})))
			.then(CommandManager.literal("remove")
				.then(CommandManager.argument("cart1", EntityArgumentType.entity())
					.then(CommandManager.argument("cart2", EntityArgumentType.entity())
						.executes(ctx -> {
							Entity cart1 = EntityArgumentType.getEntity(ctx, "cart1");
							if (!(cart1 instanceof AbstractMinecartEntity))
								throw ONLY_MINECARTS_ALLOWED.create();

							Entity cart2 = EntityArgumentType.getEntity(ctx, "cart2");
							if (!(cart2 instanceof AbstractMinecartEntity))
								throw ONLY_MINECARTS_ALLOWED.create();

							MinecartController cart1Controller = ((AbstractMinecartEntity) cart1).create$getController();

							int cart1Couplings = (cart1Controller.isConnectedToCoupling() ? 1 : 0)
								+ (cart1Controller.isLeadingCoupling() ? 1 : 0);
							if (cart1Couplings == 0) {
								ctx.getSource()
									.sendFeedback(() -> Components.literal("Minecart has no Couplings Attached"), true);
								return 0;
							}

							for (boolean bool : Iterate.trueAndFalse) {
								UUID coupledCart = cart1Controller.getCoupledCart(bool);
								if (coupledCart == null)
									continue;

								if (coupledCart != cart2.getUuid())
									continue;

								MinecartController cart2Controller =
									CapabilityMinecartController.getIfPresent(cart1.getEntityWorld(), coupledCart);
								if (cart2Controller == null)
									return 0;

								cart1Controller.removeConnection(bool);
								cart2Controller.removeConnection(!bool);
								return Command.SINGLE_SUCCESS;
							}

							ctx.getSource()
								.sendFeedback(() -> Components.literal("The specified Carts are not coupled"), true);

							return 0;
						}))))
			.then(CommandManager.literal("removeAll")
				.then(CommandManager.argument("cart", EntityArgumentType.entity())
					.executes(ctx -> {
						Entity cart = EntityArgumentType.getEntity(ctx, "cart");
						if (!(cart instanceof AbstractMinecartEntity))
							throw ONLY_MINECARTS_ALLOWED.create();

						MinecartController controller = ((AbstractMinecartEntity) cart).create$getController();

						int couplings =
							(controller.isConnectedToCoupling() ? 1 : 0) + (controller.isLeadingCoupling() ? 1 : 0);
						if (couplings == 0) {
							ctx.getSource()
								.sendFeedback(() -> Components.literal("Minecart has no Couplings Attached"), true);
							return 0;
						}

						controller.decouple();

						ctx.getSource()
							.sendFeedback(() ->
								Components.literal("Removed " + couplings + " couplings from the Minecart"), true);

						return couplings;
					})));

	}

}
