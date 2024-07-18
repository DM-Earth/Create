package com.simibubi.create.content.equipment.zapper;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.AllPackets;

public class ShootableGadgetItemMethods {

	public static void applyCooldown(PlayerEntity player, ItemStack item, Hand hand, Predicate<ItemStack> predicate,
		int cooldown) {
		if (cooldown <= 0)
			return;

		boolean gunInOtherHand =
			predicate.test(player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND));
		player.getItemCooldownManager()
			.set(item.getItem(), gunInOtherHand ? cooldown * 2 / 3 : cooldown);
	}

	public static void sendPackets(PlayerEntity player, Function<Boolean, ? extends ShootGadgetPacket> factory) {
		if (!(player instanceof ServerPlayerEntity))
			return;
		AllPackets.getChannel().sendToClientsTracking(factory.apply(false), player);
		AllPackets.getChannel().sendToClient(factory.apply(true), (ServerPlayerEntity) player);
	}

	public static boolean shouldSwap(PlayerEntity player, ItemStack item, Hand hand, Predicate<ItemStack> predicate) {
		boolean isSwap = item.getNbt()
			.contains("_Swap");
		boolean mainHand = hand == Hand.MAIN_HAND;
		boolean gunInOtherHand = predicate.test(player.getStackInHand(mainHand ? Hand.OFF_HAND : Hand.MAIN_HAND));

		// Pass To Offhand
		if (mainHand && isSwap && gunInOtherHand)
			return true;
		if (mainHand && !isSwap && gunInOtherHand)
			item.getNbt()
				.putBoolean("_Swap", true);
		if (!mainHand && isSwap)
			item.getNbt()
				.remove("_Swap");
		if (!mainHand && gunInOtherHand)
			player.getStackInHand(Hand.MAIN_HAND)
				.getNbt()
				.remove("_Swap");

		// (#574) fabric: on forge, this condition is patched into startUsingItem
		// skipping it causes an item to be used forever, only allowing 1 use before releasing and re-pressing the use button.
		if (item.getMaxUseTime() > 0) {
			player.setCurrentHand(hand);
		}
		return false;
	}

	public static Vec3d getGunBarrelVec(PlayerEntity player, boolean mainHand, Vec3d rightHandForward) {
		Vec3d start = player.getPos()
			.add(0, player.getStandingEyeHeight(), 0);
		float yaw = (float) ((player.getYaw()) / -180 * Math.PI);
		float pitch = (float) ((player.getPitch()) / -180 * Math.PI);
		int flip = mainHand == (player.getMainArm() == Arm.RIGHT) ? -1 : 1;
		Vec3d barrelPosNoTransform = new Vec3d(flip * rightHandForward.x, rightHandForward.y, rightHandForward.z);
		Vec3d barrelPos = start.add(barrelPosNoTransform.rotateX(pitch)
			.rotateY(yaw));
		return barrelPos;
	}

}
