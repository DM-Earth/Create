package com.simibubi.create.content.kinetics.fan.processing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.BlastingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.SmokingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.RecipeWrapper;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe.HauntingWrapper;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe.SplashingWrapper;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.VecHelper;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

public class AllFanProcessingTypes {
	public static final NoneType NONE = register("none", new NoneType());
	public static final BlastingType BLASTING = register("blasting", new BlastingType());
	public static final HauntingType HAUNTING = register("haunting", new HauntingType());
	public static final SmokingType SMOKING = register("smoking", new SmokingType());
	public static final SplashingType SPLASHING = register("splashing", new SplashingType());

	private static final Map<String, FanProcessingType> LEGACY_NAME_MAP;

	static {
		Object2ReferenceOpenHashMap<String, FanProcessingType> map = new Object2ReferenceOpenHashMap<>();
		map.put("NONE", NONE);
		map.put("BLASTING", BLASTING);
		map.put("HAUNTING", HAUNTING);
		map.put("SMOKING", SMOKING);
		map.put("SPLASHING", SPLASHING);
		map.trim();
		LEGACY_NAME_MAP = map;
	}

	private static <T extends FanProcessingType> T register(String id, T type) {
		FanProcessingTypeRegistry.register(Create.asResource(id), type);
		return type;
	}

	@Nullable
	public static FanProcessingType ofLegacyName(String name) {
		return LEGACY_NAME_MAP.get(name);
	}

	public static void register() {
	}

	public static FanProcessingType parseLegacy(String str) {
		FanProcessingType type = ofLegacyName(str);
		if (type != null) {
			return type;
		}
		return FanProcessingType.parse(str);
	}

	public static class NoneType implements FanProcessingType {
		@Override
		public boolean isValidAt(World level, BlockPos pos) {
			return true;
		}

		@Override
		public int getPriority() {
			return -1000000;
		}

		@Override
		public boolean canProcess(ItemStack stack, World level) {
			return false;
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, World level) {
			return null;
		}

		@Override
		public void spawnProcessingParticles(World level, Vec3d pos) {
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, Random random) {
		}

		@Override
		public void affectEntity(Entity entity, World level) {
		}
	}

	public static class BlastingType implements FanProcessingType {
		private static final RecipeWrapper RECIPE_WRAPPER = new RecipeWrapper(new ItemStackHandler(1));

		@Override
		public boolean isValidAt(World level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(fluidState)) {
				return true;
			}
			BlockState blockState = level.getBlockState(pos);
			if (AllBlockTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(blockState)) {
				if (blockState.contains(BlazeBurnerBlock.HEAT_LEVEL) && !blockState.get(BlazeBurnerBlock.HEAT_LEVEL).isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public int getPriority() {
			return 100;
		}

		@Override
		public boolean canProcess(ItemStack stack, World level) {
			RECIPE_WRAPPER.setStack(0, stack);
			Optional<SmeltingRecipe> smeltingRecipe = level.getRecipeManager()
				.getFirstMatch(RecipeType.SMELTING, RECIPE_WRAPPER, level);

			if (smeltingRecipe.isPresent())
				return true;

			RECIPE_WRAPPER.setStack(0, stack);
			Optional<BlastingRecipe> blastingRecipe = level.getRecipeManager()
				.getFirstMatch(RecipeType.BLASTING, RECIPE_WRAPPER, level);

			if (blastingRecipe.isPresent())
				return true;

			return !stack.getItem()
				.isFireproof();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, World level) {
			RECIPE_WRAPPER.setStack(0, stack);
			Optional<SmokingRecipe> smokingRecipe = level.getRecipeManager()
				.getFirstMatch(RecipeType.SMOKING, RECIPE_WRAPPER, level);

			RECIPE_WRAPPER.setStack(0, stack);
			Optional<? extends AbstractCookingRecipe> smeltingRecipe = level.getRecipeManager()
				.getFirstMatch(RecipeType.SMELTING, RECIPE_WRAPPER, level);
			if (!smeltingRecipe.isPresent()) {
				RECIPE_WRAPPER.setStack(0, stack);
				smeltingRecipe = level.getRecipeManager()
					.getFirstMatch(RecipeType.BLASTING, RECIPE_WRAPPER, level);
			}

			if (smeltingRecipe.isPresent()) {
				DynamicRegistryManager registryAccess = level.getRegistryManager();
				if (!smokingRecipe.isPresent() || !ItemStack.areItemsEqual(smokingRecipe.get()
					.getOutput(registryAccess),
					smeltingRecipe.get()
						.getOutput(registryAccess))) {
					return RecipeApplier.applyRecipeOn(level, stack, smeltingRecipe.get());
				}
			}

			return Collections.emptyList();
		}

		@Override
		public void spawnProcessingParticles(World level, Vec3d pos) {
			if (level.random.nextInt(8) != 0)
				return;
			level.addParticle(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + .25f, pos.z, 0, 1 / 16f, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, Random random) {
			particleAccess.setColor(Color.mixColors(0xFF4400, 0xFF8855, random.nextFloat()));
			particleAccess.setAlpha(.5f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.FLAME, .25f);
			if (random.nextFloat() < 1 / 16f)
				particleAccess.spawnExtraParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.LAVA.getDefaultState()), .25f);
		}

		@Override
		public void affectEntity(Entity entity, World level) {
			if (level.isClient)
				return;

			if (!entity.isFireImmune()) {
				entity.setOnFireFor(10);
				entity.damage(CreateDamageSources.fanLava(level), 4);
			}
		}
	}

	public static class HauntingType implements FanProcessingType {
		private static final HauntingWrapper HAUNTING_WRAPPER = new HauntingWrapper();

		@Override
		public boolean isValidAt(World level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(fluidState)) {
				return true;
			}
			BlockState blockState = level.getBlockState(pos);
			if (AllBlockTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(blockState)) {
				if (blockState.isIn(BlockTags.CAMPFIRES) && blockState.contains(CampfireBlock.LIT) && !blockState.get(CampfireBlock.LIT)) {
					return false;
				}
				if (blockState.contains(LitBlazeBurnerBlock.FLAME_TYPE) && blockState.get(LitBlazeBurnerBlock.FLAME_TYPE) != LitBlazeBurnerBlock.FlameType.SOUL) {
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public int getPriority() {
			return 300;
		}

		@Override
		public boolean canProcess(ItemStack stack, World level) {
			HAUNTING_WRAPPER.setStack(0, stack);
			Optional<HauntingRecipe> recipe = AllRecipeTypes.HAUNTING.find(HAUNTING_WRAPPER, level);
			return recipe.isPresent();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, World level) {
			HAUNTING_WRAPPER.setStack(0, stack);
			Optional<HauntingRecipe> recipe = AllRecipeTypes.HAUNTING.find(HAUNTING_WRAPPER, level);
			if (recipe.isPresent())
				return RecipeApplier.applyRecipeOn(level, stack, recipe.get());
			return null;
		}

		@Override
		public void spawnProcessingParticles(World level, Vec3d pos) {
			if (level.random.nextInt(8) != 0)
				return;
			pos = pos.add(VecHelper.offsetRandomly(Vec3d.ZERO, level.random, 1)
				.multiply(1, 0.05f, 1)
				.normalize()
				.multiply(0.15f));
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + .45f, pos.z, 0, 0, 0);
			if (level.random.nextInt(2) == 0)
				level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + .25f, pos.z, 0, 0, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, Random random) {
			particleAccess.setColor(Color.mixColors(0x0, 0x126568, random.nextFloat()));
			particleAccess.setAlpha(1f);
			if (random.nextFloat() < 1 / 128f)
				particleAccess.spawnExtraParticle(ParticleTypes.SOUL_FIRE_FLAME, .125f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, .125f);
		}

		@Override
		public void affectEntity(Entity entity, World level) {
			if (level.isClient) {
				if (entity instanceof HorseEntity) {
					Vec3d p = entity.getLerpedPos(0);
					Vec3d v = p.add(0, 0.5f, 0)
						.add(VecHelper.offsetRandomly(Vec3d.ZERO, level.random, 1)
							.multiply(1, 0.2f, 1)
							.normalize()
							.multiply(1f));
					level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v.x, v.y, v.z, 0, 0.1f, 0);
					if (level.random.nextInt(3) == 0)
						level.addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y + .5f, p.z,
							(level.random.nextFloat() - .5f) * .5f, 0.1f, (level.random.nextFloat() - .5f) * .5f);
				}
				return;
			}

			if (entity instanceof LivingEntity livingEntity) {
				livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0, false, false));
				livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1, false, false));
			}
			if (entity instanceof HorseEntity horse) {
				int progress = horse.getCustomData()
					.getInt("CreateHaunting");
				if (progress < 100) {
					if (progress % 10 == 0) {
						level.playSound(null, entity.getBlockPos(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.NEUTRAL,
							1f, 1.5f * progress / 100f);
					}
					horse.getCustomData()
						.putInt("CreateHaunting", progress + 1);
					return;
				}

				level.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE,
					SoundCategory.NEUTRAL, 1.25f, 0.65f);

				SkeletonHorseEntity skeletonHorse = EntityType.SKELETON_HORSE.create(level);
				NbtCompound serializeNBT = horse.writeNbt(new NbtCompound());
				serializeNBT.remove("UUID");
				if (!horse.getArmorType()
					.isEmpty())
					horse.dropStack(horse.getArmorType());

				NBTSerializer.deserializeNBT(skeletonHorse, serializeNBT);
				skeletonHorse.setPosition(horse.getLerpedPos(0));
				level.spawnEntity(skeletonHorse);
				horse.discard();
			}
		}
	}

	public static class SmokingType implements FanProcessingType {
		private static final RecipeWrapper RECIPE_WRAPPER = new RecipeWrapper(new ItemStackHandler(1));

		@Override
		public boolean isValidAt(World level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(fluidState)) {
				return true;
			}
			BlockState blockState = level.getBlockState(pos);
			if (AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(blockState)) {
				if (blockState.isIn(BlockTags.CAMPFIRES) && blockState.contains(CampfireBlock.LIT) && !blockState.get(CampfireBlock.LIT)) {
					return false;
				}
				if (blockState.contains(LitBlazeBurnerBlock.FLAME_TYPE) && blockState.get(LitBlazeBurnerBlock.FLAME_TYPE) != LitBlazeBurnerBlock.FlameType.REGULAR) {
					return false;
				}
				if (blockState.contains(BlazeBurnerBlock.HEAT_LEVEL) && blockState.get(BlazeBurnerBlock.HEAT_LEVEL) != BlazeBurnerBlock.HeatLevel.SMOULDERING) {
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public int getPriority() {
			return 200;
		}

		@Override
		public boolean canProcess(ItemStack stack, World level) {
			RECIPE_WRAPPER.setStack(0, stack);
			Optional<SmokingRecipe> recipe = level.getRecipeManager()
				.getFirstMatch(RecipeType.SMOKING, RECIPE_WRAPPER, level);
			return recipe.isPresent();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, World level) {
			RECIPE_WRAPPER.setStack(0, stack);
			Optional<SmokingRecipe> smokingRecipe = level.getRecipeManager()
				.getFirstMatch(RecipeType.SMOKING, RECIPE_WRAPPER, level);

			if (smokingRecipe.isPresent())
				return RecipeApplier.applyRecipeOn(level, stack, smokingRecipe.get());

			return null;
		}

		@Override
		public void spawnProcessingParticles(World level, Vec3d pos) {
			if (level.random.nextInt(8) != 0)
				return;
			level.addParticle(ParticleTypes.POOF, pos.x, pos.y + .25f, pos.z, 0, 1 / 16f, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, Random random) {
			particleAccess.setColor(Color.mixColors(0x0, 0x555555, random.nextFloat()));
			particleAccess.setAlpha(1f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, .125f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.LARGE_SMOKE, .125f);
		}

		@Override
		public void affectEntity(Entity entity, World level) {
			if (level.isClient)
				return;

			if (!entity.isFireImmune()) {
				entity.setOnFireFor(2);
				entity.damage(CreateDamageSources.fanFire(level), 2);
			}
		}
	}

	public static class SplashingType implements FanProcessingType {
		private static final SplashingWrapper SPLASHING_WRAPPER = new SplashingWrapper();

		@Override
		public boolean isValidAt(World level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(fluidState)) {
				return true;
			}
			BlockState blockState = level.getBlockState(pos);
			if (AllBlockTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(blockState)) {
				return true;
			}
			return false;
		}

		@Override
		public int getPriority() {
			return 400;
		}

		@Override
		public boolean canProcess(ItemStack stack, World level) {
			SPLASHING_WRAPPER.setStack(0, stack);
			Optional<SplashingRecipe> recipe = AllRecipeTypes.SPLASHING.find(SPLASHING_WRAPPER, level);
			return recipe.isPresent();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, World level) {
			SPLASHING_WRAPPER.setStack(0, stack);
			Optional<SplashingRecipe> recipe = AllRecipeTypes.SPLASHING.find(SPLASHING_WRAPPER, level);
			if (recipe.isPresent())
				return RecipeApplier.applyRecipeOn(level, stack, recipe.get());
			return null;
		}

		@Override
		public void spawnProcessingParticles(World level, Vec3d pos) {
			if (level.random.nextInt(8) != 0)
				return;
			Vector3f color = new Color(0x0055FF).asVectorF();
			level.addParticle(new DustParticleEffect(color, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
				pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
			level.addParticle(ParticleTypes.SPIT, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
				pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, Random random) {
			particleAccess.setColor(Color.mixColors(0x4499FF, 0x2277FF, random.nextFloat()));
			particleAccess.setAlpha(1f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE, .125f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE_POP, .125f);
		}

		@Override
		public void affectEntity(Entity entity, World level) {
			if (level.isClient)
				return;

			if (entity instanceof EndermanEntity || entity.getType() == EntityType.SNOW_GOLEM
				|| entity.getType() == EntityType.BLAZE) {
				entity.damage(entity.getDamageSources().drown(), 2);
			}
			if (entity.isOnFire()) {
				entity.extinguish();
				level.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE,
					SoundCategory.NEUTRAL, 0.7F, 1.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
			}
		}
	}
}
