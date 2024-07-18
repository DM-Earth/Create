package com.simibubi.create.content.equipment.extendoGrip;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ExtendoGripItem extends Item  {
	public static final int MAX_DAMAGE = 200;

	public static final EntityAttributeModifier singleRangeAttributeModifier =
		new EntityAttributeModifier(UUID.fromString("7f7dbdb2-0d0d-458a-aa40-ac7633691f66"), "Range modifier", 3,
			EntityAttributeModifier.Operation.ADDITION);
	public static final EntityAttributeModifier doubleRangeAttributeModifier =
		new EntityAttributeModifier(UUID.fromString("8f7dbdb2-0d0d-458a-aa40-ac7633691f66"), "Range modifier", 5,
			EntityAttributeModifier.Operation.ADDITION);

	private static final Supplier<Multimap<EntityAttribute, EntityAttributeModifier>> rangeModifier = Suppliers.memoize(() ->
	// Holding an ExtendoGrip
	ImmutableMultimap.of(ReachEntityAttributes.REACH, singleRangeAttributeModifier, ReachEntityAttributes.ATTACK_RANGE, singleRangeAttributeModifier));
	private static final Supplier<Multimap<EntityAttribute, EntityAttributeModifier>> doubleRangeModifier = Suppliers.memoize(() ->
	// Holding two ExtendoGrips o.O
	ImmutableMultimap.of(ReachEntityAttributes.REACH, doubleRangeAttributeModifier, ReachEntityAttributes.ATTACK_RANGE, doubleRangeAttributeModifier));

	private static DamageSource lastActiveDamageSource;

	public ExtendoGripItem(Settings properties) {
		super(properties.maxDamageIfAbsent(MAX_DAMAGE));
	}

	public static final String EXTENDO_MARKER = "createExtendo";
	public static final String DUAL_EXTENDO_MARKER = "createDualExtendo";

	public static void holdingExtendoGripIncreasesRange(LivingEntity entity) {
		if (!(entity instanceof PlayerEntity))
			return;

		PlayerEntity player = (PlayerEntity) entity;

		NbtCompound persistentData = player.getCustomData();
		boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getOffHandStack());
		boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getMainHandStack());
		boolean holdingDualExtendo = inOff && inMain;
		boolean holdingExtendo = inOff ^ inMain;
		holdingExtendo &= !holdingDualExtendo;
		boolean wasHoldingExtendo = persistentData.contains(EXTENDO_MARKER);
		boolean wasHoldingDualExtendo = persistentData.contains(DUAL_EXTENDO_MARKER);

		if (holdingExtendo != wasHoldingExtendo) {
			if (!holdingExtendo) {
				player.getAttributes()
					.removeModifiers(rangeModifier.get());
				persistentData.remove(EXTENDO_MARKER);
			} else {
				AllAdvancements.EXTENDO_GRIP.awardTo(player);
				player.getAttributes()
					.addTemporaryModifiers(rangeModifier.get());
				persistentData.putBoolean(EXTENDO_MARKER, true);
			}
		}

		if (holdingDualExtendo != wasHoldingDualExtendo) {
			if (!holdingDualExtendo) {
				player.getAttributes()
					.removeModifiers(doubleRangeModifier.get());
				persistentData.remove(DUAL_EXTENDO_MARKER);
			} else {
				AllAdvancements.EXTENDO_GRIP_DUAL.awardTo(player);
				player.getAttributes()
					.addTemporaryModifiers(doubleRangeModifier.get());
				persistentData.putBoolean(DUAL_EXTENDO_MARKER, true);
			}
		}

	}

	public static void addReachToJoiningPlayersHoldingExtendo(Entity entity, @Nullable NbtCompound persistentData) {
		if (!(entity instanceof PlayerEntity player) || persistentData == null) return;
//		Player player = event.getPlayer();
//		CompoundTag persistentData = player.getCustomData();

		if (persistentData.contains(DUAL_EXTENDO_MARKER))
			player.getAttributes()
				.addTemporaryModifiers(doubleRangeModifier.get());
		else if (persistentData.contains(EXTENDO_MARKER))
			player.getAttributes()
				.addTemporaryModifiers(rangeModifier.get());
	}

	public static void consumeDurabilityOnBlockBreak(World level, PlayerEntity player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
		findAndDamageExtendoGrip(player);
	}

	public static void consumeDurabilityOnPlace(ItemPlacementContext blockPlaceContext, BlockPos blockPos, BlockState blockState) {
		findAndDamageExtendoGrip(blockPlaceContext.getPlayer());
	}

	private static void findAndDamageExtendoGrip(PlayerEntity player) {
		if (player == null)
			return;
		if (player.getWorld().isClient)
			return;
		Hand hand = Hand.MAIN_HAND;
		ItemStack extendo = player.getMainHandStack();
		if (!AllItems.EXTENDO_GRIP.isIn(extendo)) {
			extendo = player.getOffHandStack();
			hand = Hand.OFF_HAND;
		}
		if (!AllItems.EXTENDO_GRIP.isIn(extendo))
			return;
		final Hand h = hand;
		if (!BacktankUtil.canAbsorbDamage(player, maxUses()))
			extendo.damage(1, player, p -> p.sendToolBreakStatus(h));
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return BacktankUtil.isBarVisible(stack, maxUses());
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return BacktankUtil.getBarWidth(stack, maxUses());
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return BacktankUtil.getBarColor(stack, maxUses());
	}

	private static int maxUses() {
		return AllConfigs.server().equipment.maxExtendoGripActions.get();
	}

	public static float bufferLivingAttackEvent(DamageSource damageSource, LivingEntity attacked, float amount) {
		// Workaround for removed patch to get the attacking entity.
		lastActiveDamageSource = damageSource;

		Entity trueSource = damageSource.getAttacker();
		if (trueSource instanceof PlayerEntity)
			findAndDamageExtendoGrip((PlayerEntity) trueSource);
		return amount;
	}

	public static double attacksByExtendoGripHaveMoreKnockback(double strength, PlayerEntity player) {
		if (!isHoldingExtendoGrip(player))
			return strength;
		return strength + 2;
	}

	public static boolean isHoldingExtendoGrip(PlayerEntity player) {
		boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getOffHandStack());
		boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getMainHandStack());
		boolean holdingGrip = inOff || inMain;
		return holdingGrip;
	}
}
