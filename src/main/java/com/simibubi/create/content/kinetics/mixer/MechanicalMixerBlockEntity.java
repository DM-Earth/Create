package com.simibubi.create.content.kinetics.mixer;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.fluids.potion.PotionMixingRecipes;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MechanicalMixerBlockEntity extends BasinOperatingBlockEntity {

	private static final Object shapelessOrMixingRecipesKey = new Object();

	public int runningTicks;
	public int processingTicks;
	public boolean running;

	public MechanicalMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public float getRenderedHeadOffset(float partialTicks) {
		int localTick;
		float offset = 0;
		if (running) {
			if (runningTicks < 20) {
				localTick = runningTicks;
				float num = (localTick + partialTicks) / 20f;
				num = ((2 - MathHelper.cos((float) (num * Math.PI))) / 2);
				offset = num - .5f;
			} else if (runningTicks <= 20) {
				offset = 1;
			} else {
				localTick = 40 - runningTicks;
				float num = (localTick - partialTicks) / 20f;
				num = ((2 - MathHelper.cos((float) (num * Math.PI))) / 2);
				offset = num - .5f;
			}
		}
		return offset + 7 / 16f;
	}

	public float getRenderedHeadRotationSpeed(float partialTicks) {
		float speed = getSpeed();
		if (running) {
			if (runningTicks < 15) {
				return speed;
			}
			if (runningTicks <= 20) {
				return speed * 2;
			}
			return speed;
		}
		return speed / 2;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.MIXER);
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).stretch(0, -1.5, 0);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		running = compound.getBoolean("Running");
		runningTicks = compound.getInt("Ticks");
		super.read(compound, clientPacket);

		if (clientPacket && hasWorld())
			getBasin().ifPresent(bte -> bte.setAreFluidsMoving(running && runningTicks <= 20));
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putBoolean("Running", running);
		compound.putInt("Ticks", runningTicks);
		super.write(compound, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		if (runningTicks >= 40) {
			running = false;
			runningTicks = 0;
			basinChecker.scheduleUpdate();
			return;
		}

		float speed = Math.abs(getSpeed());
		if (running && world != null) {
			if (world.isClient && runningTicks == 20)
				renderParticles();

			if ((!world.isClient || isVirtual()) && runningTicks == 20) {
				if (processingTicks < 0) {
					float recipeSpeed = 1;
					if (currentRecipe instanceof ProcessingRecipe) {
						int t = ((ProcessingRecipe<?>) currentRecipe).getProcessingDuration();
						if (t != 0)
							recipeSpeed = t / 100f;
					}

					processingTicks = MathHelper.clamp((MathHelper.floorLog2((int) (512 / speed))) * MathHelper.ceil(recipeSpeed * 15) + 1, 1, 512);

					Optional<BasinBlockEntity> basin = getBasin();
					if (basin.isPresent()) {
						Couple<SmartFluidTankBehaviour> tanks = basin.get()
							.getTanks();
						if (!tanks.getFirst()
							.isEmpty()
							|| !tanks.getSecond()
								.isEmpty())
							world.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
								SoundCategory.BLOCKS, .75f, speed < 65 ? .75f : 1.5f);
					}

				} else {
					processingTicks--;
					if (processingTicks == 0) {
						runningTicks++;
						processingTicks = -1;
						applyBasinRecipe();
						sendData();
					}
				}
			}

			if (runningTicks != 20)
				runningTicks++;
		}
	}

	public void renderParticles() {
		Optional<BasinBlockEntity> basin = getBasin();
		if (!basin.isPresent() || world == null)
			return;

		for (SmartInventory inv : basin.get()
			.getInvs()) {
			for (int slot = 0; slot < inv.getSlotCount(); slot++) {
				ItemStack stackInSlot = inv.getStack(slot);
				if (stackInSlot.isEmpty())
					continue;
				ItemStackParticleEffect data = new ItemStackParticleEffect(ParticleTypes.ITEM, stackInSlot);
				spillParticle(data);
			}
		}

		for (SmartFluidTankBehaviour behaviour : basin.get()
			.getTanks()) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks()) {
				if (tankSegment.isEmpty(0))
					continue;
				spillParticle(FluidFX.getFluidParticle(tankSegment.getRenderedFluid()));
			}
		}
	}

	protected void spillParticle(ParticleEffect data) {
		float angle = world.random.nextFloat() * 360;
		Vec3d offset = new Vec3d(0, 0, 0.25f);
		offset = VecHelper.rotate(offset, angle, Axis.Y);
		Vec3d target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y)
			.add(0, .25f, 0);
		Vec3d center = offset.add(VecHelper.getCenterOf(pos));
		target = VecHelper.offsetRandomly(target.subtract(offset), world.random, 1 / 128f);
		world.addParticle(data, center.x, center.y - 1.75f, center.z, target.x, target.y, target.z);
	}

	@Override
	protected List<Recipe<?>> getMatchingRecipes() {
		List<Recipe<?>> matchingRecipes = super.getMatchingRecipes();

		if (!AllConfigs.server().recipes.allowBrewingInMixer.get())
			return matchingRecipes;

		Optional<BasinBlockEntity> basin = getBasin();
		if (!basin.isPresent())
			return matchingRecipes;

		BasinBlockEntity basinBlockEntity = basin.get();
		if (basin.isEmpty())
			return matchingRecipes;

		Storage<ItemVariant> availableItems = basinBlockEntity
			.getItemStorage(null);
		if (availableItems == null)
			return matchingRecipes;

		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : availableItems.nonEmptyViews()) {
				List<MixingRecipe> list = PotionMixingRecipes.BY_ITEM.get(view.getResource().getItem());
				if (list == null)
					continue;
				for (MixingRecipe mixingRecipe : list)
					if (matchBasinRecipe(mixingRecipe))
						matchingRecipes.add(mixingRecipe);
			}
		}

		return matchingRecipes;
	}

	@Override
	protected <C extends Inventory> boolean matchStaticFilters(Recipe<C> r) {
		return ((r instanceof CraftingRecipe && !(r instanceof ShapedRecipe)
				 && AllConfigs.server().recipes.allowShapelessInMixer.get() && r.getIngredients()
				.size() > 1
				 && !MechanicalPressBlockEntity.canCompress(r)) && !AllRecipeTypes.shouldIgnoreInAutomation(r)
			|| r.getType() == AllRecipeTypes.MIXING.getType());
	}

	@Override
	public void startProcessingBasin() {
		if (running && runningTicks <= 20)
			return;
		super.startProcessingBasin();
		running = true;
		runningTicks = 0;
	}

	@Override
	public boolean continueWithPreviousRecipe() {
		runningTicks = 20;
		return true;
	}

	@Override
	protected void onBasinRemoved() {
		if (!running)
			return;
		runningTicks = 40;
		running = false;
	}

	@Override
	protected Object getRecipeCacheKey() {
		return shapelessOrMixingRecipesKey;
	}

	@Override
	protected boolean isRunning() {
		return running;
	}

	@Override
	protected Optional<CreateAdvancement> getProcessedRecipeTrigger() {
		return Optional.of(AllAdvancements.MIXER);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void tickAudio() {
		super.tickAudio();

		// SoundEvents.BLOCK_STONE_BREAK
		boolean slow = Math.abs(getSpeed()) < 65;
		if (slow && AnimationTickHolder.getTicks() % 2 == 0)
			return;
		if (runningTicks == 20)
			AllSoundEvents.MIXING.playAt(world, pos, .75f, 1, true);
	}

}
