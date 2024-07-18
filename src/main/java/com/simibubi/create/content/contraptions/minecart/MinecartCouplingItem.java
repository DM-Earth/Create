package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.AllTags;

import com.simibubi.create.foundation.utility.AdventureUtil;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class MinecartCouplingItem extends Item {

	public MinecartCouplingItem(Settings p_i48487_1_) {
		super(p_i48487_1_);
	}

	public static ActionResult handleInteractionWithMinecart(PlayerEntity player, World world, Hand hand, Entity interacted, @Nullable EntityHitResult hitResult) {
		if (player.isSpectator()) // forge checks this, fabric does not
			return ActionResult.PASS;
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;
		if (!(interacted instanceof AbstractMinecartEntity))
			return ActionResult.PASS;
		AbstractMinecartEntity minecart = (AbstractMinecartEntity) interacted;
		if (player == null)
			return ActionResult.PASS;
		MinecartController controller = minecart.create$getController();

		ItemStack heldItem = player.getStackInHand(hand);
		if (AllItems.MINECART_COUPLING.isIn(heldItem)) {
			if (!onCouplingInteractOnMinecart(player.getWorld(), minecart, player, controller))
				return ActionResult.PASS;
		} else if (AllItems.WRENCH.isIn(heldItem)) {
			if (!onWrenchInteractOnMinecart(player.getWorld(), minecart, player, controller))
				return ActionResult.PASS;
		} else
			return ActionResult.PASS;

		return ActionResult.SUCCESS;
	}

	protected static boolean onCouplingInteractOnMinecart(World world,
		AbstractMinecartEntity minecart, PlayerEntity player, MinecartController controller) {
		if (controller.isFullyCoupled()) {
			if (world.isClient) // fabric: on forge this only runs on server, here we only run
				// on client to avoid an incorrect message due to differences in timing across loaders.
				// on forge, the process is client -> server -> packet -> couple
				// on fabric, the process is client -> packet -> couple -> server
				CouplingHandler.status(player, "two_couplings_max");
			return true;
		}
		if (world != null && world.isClient)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> cartClicked(player, minecart));
		return true;
	}

	private static boolean onWrenchInteractOnMinecart(World world, AbstractMinecartEntity minecart, PlayerEntity player,
		MinecartController controller) {
		int couplings = (controller.isConnectedToCoupling() ? 1 : 0) + (controller.isLeadingCoupling() ? 1 : 0);
		if (couplings == 0)
			return false;
		if (world.isClient)
			return true;

		for (boolean forward : Iterate.trueAndFalse) {
			if (controller.hasContraptionCoupling(forward))
				couplings--;
		}

		CouplingHandler.status(player, "removed");
		controller.decouple();
		if (!player.isCreative())
			player.getInventory()
				.offerOrDrop(new ItemStack(AllItems.MINECART_COUPLING.get(), couplings));
		return true;
	}

	@Environment(EnvType.CLIENT)
	private static void cartClicked(PlayerEntity player, AbstractMinecartEntity interacted) {
		CouplingHandlerClient.onCartClicked(player, (AbstractMinecartEntity) interacted);
	}

}
