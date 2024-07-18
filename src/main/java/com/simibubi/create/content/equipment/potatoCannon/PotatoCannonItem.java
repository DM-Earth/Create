package com.simibubi.create.content.equipment.potatoCannon;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingBehaviorItem;
import io.github.fabricators_of_create.porting_lib.item.EntitySwingListenerItem;
import io.github.fabricators_of_create.porting_lib.item.ReequipAnimationItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PotatoCannonItem extends RangedWeaponItem implements CustomArmPoseItem, EntitySwingListenerItem, ReequipAnimationItem, CustomEnchantingBehaviorItem {

	public static ItemStack CLIENT_CURRENT_AMMO = ItemStack.EMPTY;
	public static final int MAX_DAMAGE = 100;

	public PotatoCannonItem(Settings properties) {
		super(properties.maxDamageIfAbsent(MAX_DAMAGE));
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		return false;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		if (enchantment == Enchantments.POWER)
			return true;
		if (enchantment == Enchantments.PUNCH)
			return true;
		if (enchantment == Enchantments.FLAME)
			return true;
		if (enchantment == Enchantments.LOOTING)
			return true;
		if (enchantment == AllEnchantments.POTATO_RECOVERY.get())
			return true;
		return CustomEnchantingBehaviorItem.super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		return use(context.getWorld(), context.getPlayer(), context.getHand()).getResult();
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

	private int maxUses() {
		return AllConfigs.server().equipment.maxPotatoCannonShots.get();
	}

	public boolean isCannon(ItemStack stack) {
		return stack.getItem() instanceof PotatoCannonItem;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		return findAmmoInInventory(world, player, stack).map(itemStack -> {

			if (ShootableGadgetItemMethods.shouldSwap(player, stack, hand, this::isCannon))
				return TypedActionResult.fail(stack);

			if (world.isClient) {
				CreateClient.POTATO_CANNON_RENDER_HANDLER.dontAnimateItem(hand);
				return TypedActionResult.success(stack);
			}

			Vec3d barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(player, hand == Hand.MAIN_HAND,
				new Vec3d(.75f, -0.15f, 1.5f));
			Vec3d correction =
				ShootableGadgetItemMethods.getGunBarrelVec(player, hand == Hand.MAIN_HAND, new Vec3d(-.05f, 0, 0))
					.subtract(player.getPos()
						.add(0, player.getStandingEyeHeight(), 0));

			PotatoCannonProjectileType projectileType = PotatoProjectileTypeManager.getTypeForStack(itemStack)
				.orElse(BuiltinPotatoProjectileTypes.FALLBACK);
			Vec3d lookVec = player.getRotationVector();
			Vec3d motion = lookVec.add(correction)
				.normalize()
				.multiply(2)
				.multiply(projectileType.getVelocityMultiplier());

			float soundPitch = projectileType.getSoundPitch() + (Create.RANDOM.nextFloat() - .5f) / 4f;

			boolean spray = projectileType.getSplit() > 1;
			Vec3d sprayBase = VecHelper.rotate(new Vec3d(0, 0.1, 0), 360 * Create.RANDOM.nextFloat(), Axis.Z);
			float sprayChange = 360f / projectileType.getSplit();

			for (int i = 0; i < projectileType.getSplit(); i++) {
				PotatoProjectileEntity projectile = AllEntityTypes.POTATO_PROJECTILE.create(world);
				projectile.setItem(itemStack);
				projectile.setEnchantmentEffectsFromCannon(stack);

				Vec3d splitMotion = motion;
				if (spray) {
					float imperfection = 40 * (Create.RANDOM.nextFloat() - 0.5f);
					Vec3d sprayOffset = VecHelper.rotate(sprayBase, i * sprayChange + imperfection, Axis.Z);
					splitMotion = splitMotion.add(VecHelper.lookAt(sprayOffset, motion));
				}

				if (i != 0)
					projectile.recoveryChance = 0;

				projectile.setPosition(barrelPos.x, barrelPos.y, barrelPos.z);
				projectile.setVelocity(splitMotion);
				projectile.setOwner(player);
				world.spawnEntity(projectile);
			}

			if (!player.isCreative()) {
				itemStack.decrement(1);
				if (itemStack.isEmpty())
					player.getInventory().removeOne(itemStack);
			}

			if (!BacktankUtil.canAbsorbDamage(player, maxUses()))
				stack.damage(1, player, p -> p.sendToolBreakStatus(hand));

			Integer cooldown =
				findAmmoInInventory(world, player, stack).flatMap(PotatoProjectileTypeManager::getTypeForStack)
					.map(PotatoCannonProjectileType::getReloadTicks)
					.orElse(10);

			ShootableGadgetItemMethods.applyCooldown(player, stack, hand, this::isCannon, cooldown);
			ShootableGadgetItemMethods.sendPackets(player,
				b -> new PotatoCannonPacket(barrelPos, lookVec.normalize(), itemStack, hand, soundPitch, b));
			return TypedActionResult.success(stack);
		})
			.orElse(TypedActionResult.pass(stack));
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || newStack.getItem() != oldStack.getItem();
	}

	private Optional<ItemStack> findAmmoInInventory(World world, PlayerEntity player, ItemStack held) {
		ItemStack findAmmo = player.getProjectileType(held);
		return PotatoProjectileTypeManager.getTypeForStack(findAmmo)
			.map($ -> findAmmo);
	}

	@Environment(EnvType.CLIENT)
	public static Optional<ItemStack> getAmmoforPreview(ItemStack cannon) {
		if (AnimationTickHolder.getTicks() % 3 != 0)
			return Optional.of(CLIENT_CURRENT_AMMO)
				.filter(stack -> !stack.isEmpty());

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		CLIENT_CURRENT_AMMO = ItemStack.EMPTY;
		if (player == null)
			return Optional.empty();
		ItemStack findAmmo = player.getProjectileType(cannon);
		Optional<ItemStack> found = PotatoProjectileTypeManager.getTypeForStack(findAmmo)
			.map($ -> findAmmo);
		found.ifPresent(stack -> CLIENT_CURRENT_AMMO = stack);
		return found;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext flag) {
		int power = EnchantmentHelper.getLevel(Enchantments.POWER, stack);
		int punch = EnchantmentHelper.getLevel(Enchantments.PUNCH, stack);
		final float additionalDamageMult = 1 + power * .2f;
		final float additionalKnockback = punch * .5f;

		getAmmoforPreview(stack).ifPresent(ammo -> {
			String _attack = "potato_cannon.ammo.attack_damage";
			String _reload = "potato_cannon.ammo.reload_ticks";
			String _knockback = "potato_cannon.ammo.knockback";

			tooltip.add(Components.immutableEmpty());
			tooltip.add(Components.translatable(ammo.getTranslationKey()).append(Components.literal(":"))
				.formatted(Formatting.GRAY));
			PotatoCannonProjectileType type = PotatoProjectileTypeManager.getTypeForStack(ammo)
				.get();
			MutableText spacing = Components.literal(" ");
			Formatting green = Formatting.GREEN;
			Formatting darkGreen = Formatting.DARK_GREEN;

			float damageF = type.getDamage() * additionalDamageMult;
			MutableText damage = Components.literal(
				damageF == MathHelper.floor(damageF) ? "" + MathHelper.floor(damageF) : "" + damageF);
			MutableText reloadTicks = Components.literal("" + type.getReloadTicks());
			MutableText knockback =
				Components.literal("" + (type.getKnockback() + additionalKnockback));

			damage = damage.formatted(additionalDamageMult > 1 ? green : darkGreen);
			knockback = knockback.formatted(additionalKnockback > 0 ? green : darkGreen);
			reloadTicks = reloadTicks.formatted(darkGreen);

			tooltip.add(spacing.copyContentOnly()
				.append(Lang.translateDirect(_attack, damage)
					.formatted(darkGreen)));
			tooltip.add(spacing.copyContentOnly()
				.append(Lang.translateDirect(_reload, reloadTicks)
					.formatted(darkGreen)));
			tooltip.add(spacing.copyContentOnly()
				.append(Lang.translateDirect(_knockback, knockback)
					.formatted(darkGreen)));
		});
		super.appendTooltip(stack, world, tooltip, flag);
	}

	@Override
	public Predicate<ItemStack> getProjectiles() {
		return stack -> PotatoProjectileTypeManager.getTypeForStack(stack)
			.isPresent();
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		return true;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.NONE;
	}

	@Override
	@Nullable
	public ArmPose getArmPose(ItemStack stack, AbstractClientPlayerEntity player, Hand hand) {
		if (!player.handSwinging) {
			return ArmPose.CROSSBOW_HOLD;
		}
		return null;
	}

	@Override
	public int getRange() {
		return 15;
	}

//	@Override
//	@OnlyIn(Dist.CLIENT)
//	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new PotatoCannonItemRenderer()));
//	}

}
