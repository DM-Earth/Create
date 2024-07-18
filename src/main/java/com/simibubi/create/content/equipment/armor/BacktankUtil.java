package com.simibubi.create.content.equipment.armor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class BacktankUtil {

	private static final List<Function<LivingEntity, List<ItemStack>>> BACKTANK_SUPPLIERS = new ArrayList<>();

	static {
		addBacktankSupplier(entity -> {
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemStack itemStack : entity.getArmorItems())
				if (!itemStack.isEmpty() && AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(itemStack))
					stacks.add(itemStack);

			return stacks;
		});
	}

	public static List<ItemStack> getAllWithAir(LivingEntity entity) {
		List<ItemStack> all = new ArrayList<>();

		for (Function<LivingEntity, List<ItemStack>> supplier : BACKTANK_SUPPLIERS) {
			List<ItemStack> result = supplier.apply(entity);

			for (ItemStack stack : result)
				if (hasAirRemaining(stack))
					all.add(stack);
		}

		// Sort with ascending order (we want to prioritize the most empty so things actually run out)
		all.sort((a, b) -> Float.compare(getAir(a), getAir(b)));

		return all;
	}

	public static boolean hasAirRemaining(ItemStack backtank) {
		return getAir(backtank) > 0;
	}

	public static float getAir(ItemStack backtank) {
		NbtCompound tag = backtank.getOrCreateNbt();
		return Math.min(tag.getFloat("Air"), maxAir(backtank));
	}

	public static void consumeAir(LivingEntity entity, ItemStack backtank, float i) {
		NbtCompound tag = backtank.getOrCreateNbt();
		int maxAir = maxAir(backtank);
		float air = getAir(backtank);
		float newAir = Math.max(air - i, 0);
		tag.putFloat("Air", Math.min(newAir, maxAir));
		backtank.setNbt(tag);

		if (!(entity instanceof ServerPlayerEntity player))
			return;

		sendWarning(player, air, newAir, maxAir / 10f);
		sendWarning(player, air, newAir, 1);
	}

	private static void sendWarning(ServerPlayerEntity player, float air, float newAir, float threshold) {
		if (newAir > threshold)
			return;
		if (air <= threshold)
			return;

		boolean depleted = threshold == 1;
		MutableText component = Lang.translateDirect(depleted ? "backtank.depleted" : "backtank.low");

		AllSoundEvents.DENY.play(player.getWorld(), null, player.getBlockPos(), 1, 1.25f);
		AllSoundEvents.STEAM.play(player.getWorld(), null, player.getBlockPos(), .5f, .5f);

		player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 40, 10));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(
			Components.literal("\u26A0 ").formatted(depleted ? Formatting.RED : Formatting.GOLD)
				.append(component.formatted(Formatting.GRAY))));
		player.networkHandler.sendPacket(new TitleS2CPacket(Components.immutableEmpty()));
	}

	public static int maxAir(ItemStack backtank) {
		return maxAir(EnchantmentHelper.getLevel(AllEnchantments.CAPACITY.get(), backtank));
	}

	public static int maxAir(int enchantLevel) {
		return AllConfigs.server().equipment.airInBacktank.get()
			+ AllConfigs.server().equipment.enchantedBacktankCapacity.get() * enchantLevel;
	}

	public static int maxAirWithoutEnchants() {
		return AllConfigs.server().equipment.airInBacktank.get();
	}

	public static boolean canAbsorbDamage(LivingEntity entity, int usesPerTank) {
		if (usesPerTank == 0)
			return true;
		if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative())
			return true;
		List<ItemStack> backtanks = getAllWithAir(entity);
		if (backtanks.isEmpty())
			return false;
		float cost = ((float) maxAirWithoutEnchants()) / usesPerTank;
		consumeAir(entity, backtanks.get(0), cost);
		return true;
	}

	// For Air-using tools

	public static boolean isBarVisible(ItemStack stack, int usesPerTank) {
		if (usesPerTank == 0)
			return false;
		PlayerEntity player = EnvExecutor.callWhenOn(EnvType.CLIENT, () -> () -> MinecraftClient.getInstance().player);
		if (player == null)
			return false;
		List<ItemStack> backtanks = getAllWithAir(player);
		if (backtanks.isEmpty())
			return stack.isDamaged();
		return true;
	}

	public static int getBarWidth(ItemStack stack, int usesPerTank) {
		if (usesPerTank == 0)
			return 13;
		PlayerEntity player = EnvExecutor.callWhenOn(EnvType.CLIENT, () -> () -> MinecraftClient.getInstance().player);
		if (player == null)
			return 13;

		List<ItemStack> backtanks = getAllWithAir(player);

		if (backtanks.isEmpty())
			return Math.round(13.0F - (float) stack.getDamage() / stack.getMaxDamage() * 13.0F);

		if (backtanks.size() == 1)
			return backtanks.get(0)
				.getItem()
				.getItemBarStep(backtanks.get(0));

		// If there is more than one backtank, average the bar widths.
		int sumBarWidth = backtanks.stream()
			.map(backtank -> backtank.getItem()
				.getItemBarStep(backtank))
			.reduce(0, Integer::sum);

		return Math.round((float) sumBarWidth / backtanks.size());
	}

	public static int getBarColor(ItemStack stack, int usesPerTank) {
		if (usesPerTank == 0)
			return 0;
		PlayerEntity player = EnvExecutor.callWhenOn(EnvType.CLIENT, () -> () -> MinecraftClient.getInstance().player);
		if (player == null)
			return 0;
		List<ItemStack> backtanks = getAllWithAir(player);

		// Fallback colour
		if (backtanks.isEmpty())
			return MathHelper.hsvToRgb(Math.max(0.0F, 1.0F - (float) stack.getDamage() / stack.getMaxDamage()) / 3.0F,
				1.0F, 1.0F);

		// Just return the "first" backtank for the bar color since that's the one we are consuming from
		return backtanks.get(0)
			.getItem()
			.getItemBarColor(backtanks.get(0));
	}

	/**
	 * Use this method to add custom entry points to the backtank item stack supplier, e.g. getting them from custom
	 * slots or items.
	 */
	public static void addBacktankSupplier(Function<LivingEntity, List<ItemStack>> supplier) {
		BACKTANK_SUPPLIERS.add(supplier);
	}
}
