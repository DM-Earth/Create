package com.simibubi.create.content.equipment.potatoCannon;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.mixin.accessor.FallingBlockEntityAccessor;
import com.simibubi.create.foundation.utility.WorldAttached;

import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.common.util.IPlantable;
import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.FoodComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class BuiltinPotatoProjectileTypes {

	private static final GameProfile ZOMBIE_CONVERTER_NAME =
		new GameProfile(UUID.fromString("be12d3dc-27d3-4992-8c97-66be53fd49c5"), "Converter");
	private static final WorldAttached<FakePlayer> ZOMBIE_CONVERTERS =
		new WorldAttached<>(w -> new ConverterFakePlayer((ServerWorld) w, ZOMBIE_CONVERTER_NAME){});

	public static class ConverterFakePlayer extends FakePlayer {
		public ConverterFakePlayer(ServerWorld world, GameProfile profile) {
			super(world, profile);
		}
	}

	public static final PotatoCannonProjectileType

	FALLBACK = create("fallback").damage(0)
		.register(),

		POTATO = create("potato").damage(5)
			.reloadTicks(15)
			.velocity(1.25f)
			.knockback(1.5f)
			.renderTumbling()
			.onBlockHit(plantCrop(Blocks.POTATOES))
			.registerAndAssign(Items.POTATO),

		BAKED_POTATO = create("baked_potato").damage(5)
			.reloadTicks(15)
			.velocity(1.25f)
			.knockback(0.5f)
			.renderTumbling()
			.preEntityHit(setFire(3))
			.registerAndAssign(Items.BAKED_POTATO),

		CARROT = create("carrot").damage(4)
			.reloadTicks(12)
			.velocity(1.45f)
			.knockback(0.3f)
			.renderTowardMotion(140, 1)
			.soundPitch(1.5f)
			.onBlockHit(plantCrop(Blocks.CARROTS))
			.registerAndAssign(Items.CARROT),

		GOLDEN_CARROT = create("golden_carrot").damage(12)
			.reloadTicks(15)
			.velocity(1.45f)
			.knockback(0.5f)
			.renderTowardMotion(140, 2)
			.soundPitch(1.5f)
			.registerAndAssign(Items.GOLDEN_CARROT),

		SWEET_BERRIES = create("sweet_berry").damage(3)
			.reloadTicks(10)
			.knockback(0.1f)
			.velocity(1.05f)
			.renderTumbling()
			.splitInto(3)
			.soundPitch(1.25f)
			.registerAndAssign(Items.SWEET_BERRIES),

		GLOW_BERRIES = create("glow_berry").damage(2)
			.reloadTicks(10)
			.knockback(0.05f)
			.velocity(1.05f)
			.renderTumbling()
			.splitInto(2)
			.soundPitch(1.2f)
			.onEntityHit(potion(StatusEffects.GLOWING, 1, 200, false))
			.registerAndAssign(Items.GLOW_BERRIES),

		CHOCOLATE_BERRIES = create("chocolate_berry").damage(4)
			.reloadTicks(10)
			.knockback(0.2f)
			.velocity(1.05f)
			.renderTumbling()
			.splitInto(3)
			.soundPitch(1.25f)
			.registerAndAssign(AllItems.CHOCOLATE_BERRIES.get()),

		POISON_POTATO = create("poison_potato").damage(5)
			.reloadTicks(15)
			.knockback(0.05f)
			.velocity(1.25f)
			.renderTumbling()
			.onEntityHit(potion(StatusEffects.POISON, 1, 160, true))
			.registerAndAssign(Items.POISONOUS_POTATO),

		CHORUS_FRUIT = create("chorus_fruit").damage(3)
			.reloadTicks(15)
			.velocity(1.20f)
			.knockback(0.05f)
			.renderTumbling()
			.onEntityHit(chorusTeleport(20))
			.registerAndAssign(Items.CHORUS_FRUIT),

		APPLE = create("apple").damage(5)
			.reloadTicks(10)
			.velocity(1.45f)
			.knockback(0.5f)
			.renderTumbling()
			.soundPitch(1.1f)
			.registerAndAssign(Items.APPLE),

		HONEYED_APPLE = create("honeyed_apple").damage(6)
			.reloadTicks(15)
			.velocity(1.35f)
			.knockback(0.1f)
			.renderTumbling()
			.soundPitch(1.1f)
			.onEntityHit(potion(StatusEffects.SLOWNESS, 2, 160, true))
			.registerAndAssign(AllItems.HONEYED_APPLE.get()),

		GOLDEN_APPLE = create("golden_apple").damage(1)
			.reloadTicks(100)
			.velocity(1.45f)
			.knockback(0.05f)
			.renderTumbling()
			.soundPitch(1.1f)
			.onEntityHit(ray -> {
				if (!canModifyWorld())
					return false;
				Entity entity = ray.getEntity();
				World world = entity.getWorld();

				if (!(entity instanceof ZombieVillagerEntity) || !((ZombieVillagerEntity) entity).hasStatusEffect(StatusEffects.WEAKNESS))
					return foodEffects(FoodComponents.GOLDEN_APPLE, false).test(ray);
				if (world.isClient)
					return false;

				FakePlayer dummy = ZOMBIE_CONVERTERS.get(world);
				dummy.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 1));
				((ZombieVillagerEntity) entity).interactMob(dummy, Hand.MAIN_HAND);
				return true;
			})
			.registerAndAssign(Items.GOLDEN_APPLE),

		ENCHANTED_GOLDEN_APPLE = create("enchanted_golden_apple").damage(1)
			.reloadTicks(100)
			.velocity(1.45f)
			.knockback(0.05f)
			.renderTumbling()
			.soundPitch(1.1f)
			.onEntityHit(foodEffects(FoodComponents.ENCHANTED_GOLDEN_APPLE, false))
			.registerAndAssign(Items.ENCHANTED_GOLDEN_APPLE),

		BEETROOT = create("beetroot").damage(2)
			.reloadTicks(5)
			.velocity(1.6f)
			.knockback(0.1f)
			.renderTowardMotion(140, 2)
			.soundPitch(1.6f)
			.registerAndAssign(Items.BEETROOT),

		MELON_SLICE = create("melon_slice").damage(3)
			.reloadTicks(8)
			.knockback(0.1f)
			.velocity(1.45f)
			.renderTumbling()
			.soundPitch(1.5f)
			.registerAndAssign(Items.MELON_SLICE),

		GLISTERING_MELON = create("glistering_melon").damage(5)
			.reloadTicks(8)
			.knockback(0.1f)
			.velocity(1.45f)
			.renderTumbling()
			.soundPitch(1.5f)
			.onEntityHit(potion(StatusEffects.GLOWING, 1, 100, true))
			.registerAndAssign(Items.GLISTERING_MELON_SLICE),

		MELON_BLOCK = create("melon_block").damage(8)
			.reloadTicks(20)
			.knockback(2.0f)
			.velocity(0.95f)
			.renderTumbling()
			.soundPitch(0.9f)
			.onBlockHit(placeBlockOnGround(Blocks.MELON))
			.registerAndAssign(Blocks.MELON),

		PUMPKIN_BLOCK = create("pumpkin_block").damage(6)
			.reloadTicks(15)
			.knockback(2.0f)
			.velocity(0.95f)
			.renderTumbling()
			.soundPitch(0.9f)
			.onBlockHit(placeBlockOnGround(Blocks.PUMPKIN))
			.registerAndAssign(Blocks.PUMPKIN),

		PUMPKIN_PIE = create("pumpkin_pie").damage(7)
			.reloadTicks(15)
			.knockback(0.05f)
			.velocity(1.1f)
			.renderTumbling()
			.sticky()
			.soundPitch(1.1f)
			.registerAndAssign(Items.PUMPKIN_PIE),

		CAKE = create("cake").damage(8)
			.reloadTicks(15)
			.knockback(0.1f)
			.velocity(1.1f)
			.renderTumbling()
			.sticky()
			.soundPitch(1.0f)
			.registerAndAssign(Items.CAKE),

		BLAZE_CAKE = create("blaze_cake").damage(15)
			.reloadTicks(20)
			.knockback(0.3f)
			.velocity(1.1f)
			.renderTumbling()
			.sticky()
			.preEntityHit(setFire(12))
			.soundPitch(1.0f)
			.registerAndAssign(AllItems.BLAZE_CAKE.get())

	;

	private static PotatoCannonProjectileType.Builder create(String name) {
		return new PotatoCannonProjectileType.Builder(Create.asResource(name));
	}

	private static Predicate<EntityHitResult> setFire(int seconds) {
		return ray -> {
			ray.getEntity()
				.setOnFireFor(seconds);
			return false;
		};
	}

	private static Predicate<EntityHitResult> potion(StatusEffect effect, int level, int ticks, boolean recoverable) {
		return ray -> {
			Entity entity = ray.getEntity();
			if (entity.getWorld().isClient)
				return true;
			if (entity instanceof LivingEntity)
				applyEffect((LivingEntity) entity, new StatusEffectInstance(effect, ticks, level - 1));
			return !recoverable;
		};
	}

	private static Predicate<EntityHitResult> foodEffects(FoodComponent food, boolean recoverable) {
		return ray -> {
			Entity entity = ray.getEntity();
			if (entity.getWorld().isClient)
				return true;

			if (entity instanceof LivingEntity) {
				for (Pair<StatusEffectInstance, Float> effect : food.getStatusEffects()) {
					if (Create.RANDOM.nextFloat() < effect.getSecond())
						applyEffect((LivingEntity) entity, new StatusEffectInstance(effect.getFirst()));
				}
			}
			return !recoverable;
		};
	}

	private static void applyEffect(LivingEntity entity, StatusEffectInstance effect) {
		if (effect.getEffectType()
			.isInstant())
			effect.getEffectType()
				.applyInstantEffect(null, null, entity, effect.getDuration(), 1.0);
		else
			entity.addStatusEffect(effect);
	}

	private static BiPredicate<WorldAccess, BlockHitResult> plantCrop(Supplier<? extends Block> cropBlock) {
		return (world, ray) -> {
			if (world.isClient())
				return true;
			if (!canModifyWorld())
				return false;

			BlockPos hitPos = ray.getBlockPos();
			if (world instanceof World l && !l.canSetBlock(hitPos))
				return true;
			Direction face = ray.getSide();
			if (face != Direction.UP)
				return false;
			BlockPos placePos = hitPos.offset(face);
			if (!world.getBlockState(placePos)
				.isReplaceable())
				return false;
			if (!(cropBlock.get() instanceof IPlantable))
				return false;
			BlockState blockState = world.getBlockState(hitPos);
			if (!(blockState.getBlock() instanceof FarmlandBlock))
				return false;
			world.setBlockState(placePos, cropBlock.get().getDefaultState(), 3);
			return true;
		};
	}

	private static BiPredicate<WorldAccess, BlockHitResult> plantCrop(Block cropBlock) {
		return plantCrop(() -> cropBlock);
	}

	private static BiPredicate<WorldAccess, BlockHitResult> placeBlockOnGround(
		Supplier<? extends Block> block) {
		return (world, ray) -> {
			if (world.isClient())
				return true;
			if (!canModifyWorld())
				return false;

			BlockPos hitPos = ray.getBlockPos();
			if (world instanceof World l && !l.canSetBlock(hitPos))
				return true;
			Direction face = ray.getSide();
			BlockPos placePos = hitPos.offset(face);
			if (!world.getBlockState(placePos)
				.isReplaceable())
				return false;

			if (face == Direction.UP) {
				world.setBlockState(placePos, block.get().getDefaultState(), 3);
			} else if (world instanceof World level) {
				double y = ray.getPos().y - 0.5;
				if (!world.isAir(placePos.up()))
					y = Math.min(y, placePos.getY());
				if (!world.isAir(placePos.down()))
					y = Math.max(y, placePos.getY());

				FallingBlockEntity falling = FallingBlockEntityAccessor.create$callInit(level, placePos.getX() + 0.5, y,
					placePos.getZ() + 0.5, block.get().getDefaultState());
				falling.timeFalling = 1;
				world.spawnEntity(falling);
			}

			return true;
		};
	}

	private static BiPredicate<WorldAccess, BlockHitResult> placeBlockOnGround(Block block) {
		return placeBlockOnGround(() -> block);
	}

	private static Predicate<EntityHitResult> chorusTeleport(double teleportDiameter) {
		return ray -> {
			Entity entity = ray.getEntity();
			World world = entity.getEntityWorld();
			if (world.isClient)
				return true;
			if (!canModifyWorld())
				return false;
			if (!(entity instanceof LivingEntity))
				return false;
			LivingEntity livingEntity = (LivingEntity) entity;

			double entityX = livingEntity.getX();
			double entityY = livingEntity.getY();
			double entityZ = livingEntity.getZ();

			for (int teleportTry = 0; teleportTry < 16; ++teleportTry) {
				double teleportX = entityX + (livingEntity.getRandom()
					.nextDouble() - 0.5D) * teleportDiameter;
				double teleportY = MathHelper.clamp(entityY + (livingEntity.getRandom()
					.nextInt((int) teleportDiameter) - (int) (teleportDiameter / 2)), 0.0D, world.getHeight() - 1);
				double teleportZ = entityZ + (livingEntity.getRandom()
					.nextDouble() - 0.5D) * teleportDiameter;

				EntityEvents.Teleport.EntityTeleportEvent event = new EntityEvents.Teleport.EntityTeleportEvent(livingEntity, teleportX, teleportY, teleportZ);
				EntityEvents.TELEPORT.invoker().onTeleport(event);
				if (event.isCanceled())
					return false;
				if (livingEntity.teleport(teleportX, teleportY, teleportZ, true)) {
					if (livingEntity.hasVehicle())
						livingEntity.stopRiding();

					SoundEvent soundevent =
						livingEntity instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
					world.playSound(null, entityX, entityY, entityZ, soundevent, SoundCategory.PLAYERS, 1.0F, 1.0F);
					livingEntity.playSound(soundevent, 1.0F, 1.0F);
					livingEntity.setVelocity(Vec3d.ZERO);
					return true;
				}
			}

			return false;
		};
	}

	public static void register() {}

	private static boolean canModifyWorld() {
		return AllConfigs.server().equipment.potatoCannonWorldModification.get();
	}

}
