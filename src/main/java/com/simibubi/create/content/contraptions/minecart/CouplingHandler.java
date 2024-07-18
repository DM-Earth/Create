package com.simibubi.create.content.contraptions.minecart;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.LazyOptional;

public class CouplingHandler {

	public static boolean preventEntitiesFromMoutingOccupiedCart(Entity vehicle, Entity passenger) {
		if (vehicle instanceof AbstractMinecartEntity cart) {
			if (passenger instanceof AbstractContraptionEntity)
				return true;
			MinecartController controller = cart.create$getController();
			if (controller.isCoupledThroughContraption()) {
				return false;
			}
		}
		return true;
	}

	public static void forEachLoadedCoupling(World world, Consumer<Couple<MinecartController>> consumer) {
		if (world == null)
			return;
		Set<UUID> cartsWithCoupling = CapabilityMinecartController.loadedMinecartsWithCoupling.get(world);
		if (cartsWithCoupling == null)
			return;
		cartsWithCoupling.forEach(id -> {
			MinecartController controller = CapabilityMinecartController.getIfPresent(world, id);
			if (controller == null)
				return;
			if (!controller.isLeadingCoupling())
				return;
			UUID coupledCart = controller.getCoupledCart(true);
			MinecartController coupledController = CapabilityMinecartController.getIfPresent(world, coupledCart);
			if (coupledController == null)
				return;
			consumer.accept(Couple.create(controller, coupledController));
		});
	}

	public static boolean tryToCoupleCarts(@Nullable PlayerEntity player, World world, int cartId1, int cartId2) {
		Entity entity1 = world.getEntityById(cartId1);
		Entity entity2 = world.getEntityById(cartId2);

		if (!(entity1 instanceof AbstractMinecartEntity))
			return false;
		if (!(entity2 instanceof AbstractMinecartEntity))
			return false;

		String tooMany = "two_couplings_max";
		String unloaded = "unloaded";
		String noLoops = "no_loops";
		String tooFar = "too_far";

		int distanceTo = (int) entity1.getPos()
			.distanceTo(entity2.getPos());
		boolean contraptionCoupling = player == null;

		if (distanceTo < 2) {
			if (contraptionCoupling)
				return false; // dont allow train contraptions with <2 distance
			distanceTo = 2;
		}

		if (distanceTo > AllConfigs.server().kinetics.maxCartCouplingLength.get()) {
			status(player, tooFar);
			return false;
		}

		AbstractMinecartEntity cart1 = (AbstractMinecartEntity) entity1;
		AbstractMinecartEntity cart2 = (AbstractMinecartEntity) entity2;
		UUID mainID = cart1.getUuid();
		UUID connectedID = cart2.getUuid();
		MinecartController mainController = CapabilityMinecartController.getIfPresent(world, mainID);
		MinecartController connectedController = CapabilityMinecartController.getIfPresent(world, connectedID);

		if (mainController == null || connectedController == null) {
			status(player, unloaded);
			return false;
		}
		if (mainController.isFullyCoupled() || connectedController.isFullyCoupled()) {
			status(player, tooMany);
			return false;
		}

		if (mainController.isLeadingCoupling() && mainController.getCoupledCart(true)
			.equals(connectedID) || connectedController.isLeadingCoupling()
				&& connectedController.getCoupledCart(true)
					.equals(mainID))
			return false;

		for (boolean main : Iterate.trueAndFalse) {
			MinecartController current = main ? mainController : connectedController;
			boolean forward = current.isLeadingCoupling();
			int safetyCount = 1000;

			while (true) {
				if (safetyCount-- <= 0) {
					Create.LOGGER.warn("Infinite loop in coupling iteration");
					return false;
				}

				current = getNextInCouplingChain(world, current, forward);
				if (current == null) {
					status(player, unloaded);
					return false;
				}
				if (current == connectedController) {
					status(player, noLoops);
					return false;
				}
				if (current == MinecartController.EMPTY)
					break;
			}
		}

		if (!contraptionCoupling) {
			for (Hand hand : Hand.values()) {
				if (player.isCreative())
					break;
				ItemStack heldItem = player.getStackInHand(hand);
				if (!AllItems.MINECART_COUPLING.isIn(heldItem))
					continue;
				heldItem.decrement(1);
				break;
			}
		}

		mainController.prepareForCoupling(true);
		connectedController.prepareForCoupling(false);

		mainController.coupleWith(true, connectedID, distanceTo, contraptionCoupling);
		connectedController.coupleWith(false, mainID, distanceTo, contraptionCoupling);
		return true;
	}

	@Nullable
	/**
	 * MinecartController.EMPTY if none connected, null if not yet loaded
	 */
	public static MinecartController getNextInCouplingChain(World world, MinecartController controller,
		boolean forward) {
		UUID coupledCart = controller.getCoupledCart(forward);
		if (coupledCart == null)
			return MinecartController.empty();
		return CapabilityMinecartController.getIfPresent(world, coupledCart);
	}

	public static void status(PlayerEntity player, String key) {
		if (player == null)
			return;
		player.sendMessage(Lang.translateDirect("minecart_coupling." + key), true);
	}

}
