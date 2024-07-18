package com.simibubi.create.content.kinetics.saw;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import com.simibubi.create.foundation.utility.TreeCutter;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.ChorusPlantBlock;
import net.minecraft.block.GourdBlock;
import net.minecraft.block.KelpBlock;
import net.minecraft.block.KelpPlantBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SawBlockEntity extends BlockBreakingKineticBlockEntity implements SidedStorageBlockEntity {

	private static final Object cuttingRecipesKey = new Object();
	public static final Supplier<RecipeType<?>> woodcuttingRecipeType =
		Suppliers.memoize(() -> Registries.RECIPE_TYPE.get(new Identifier("druidcraft", "woodcutting")));

	public ProcessingInventory inventory;
	private int recipeIndex;
	private FilteringBehaviour filtering;

	private ItemStack playEvent;

	public SawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new ProcessingInventory(this::start).withSlotLimit(!AllConfigs.server().recipes.bulkCutting.get());
		inventory.remainingTime = -1;
		recipeIndex = 0;
		playEvent = ItemStack.EMPTY;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		filtering = new FilteringBehaviour(this, new SawFilterSlot()).forRecipes();
		behaviours.add(filtering);
		behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnelsWhen(this::canProcess));
		registerAwardables(behaviours, AllAdvancements.SAW_PROCESSING);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.put("Inventory", inventory.serializeNBT());
		compound.putInt("RecipeIndex", recipeIndex);
		super.write(compound, clientPacket);

		if (!clientPacket || playEvent.isEmpty())
			return;
		compound.put("PlayEvent", NBTSerializer.serializeNBT(playEvent));
		playEvent = ItemStack.EMPTY;
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		inventory.deserializeNBT(compound.getCompound("Inventory"));
		recipeIndex = compound.getInt("RecipeIndex");
		if (compound.contains("PlayEvent"))
			playEvent = ItemStack.fromNbt(compound.getCompound("PlayEvent"));
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(.125f);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void tickAudio() {
		super.tickAudio();
		if (getSpeed() == 0)
			return;

		if (!playEvent.isEmpty()) {
			boolean isWood = false;
			Item item = playEvent.getItem();
			if (item instanceof BlockItem) {
				Block block = ((BlockItem) item).getBlock();
				isWood = block.getSoundGroup(block.getDefaultState()) == BlockSoundGroup.WOOD;
			}
			spawnEventParticles(playEvent);
			playEvent = ItemStack.EMPTY;
			if (!isWood)
				AllSoundEvents.SAW_ACTIVATE_STONE.playAt(world, pos, 3, 1, true);
			else
				AllSoundEvents.SAW_ACTIVATE_WOOD.playAt(world, pos, 3, 1, true);
			return;
		}
	}

	@Override
	public void tick() {
		if (shouldRun() && ticksUntilNextProgress < 0)
			destroyNextTick();
		super.tick();

		if (!canProcess())
			return;
		if (getSpeed() == 0)
			return;
		if (inventory.remainingTime == -1) {
			if (!inventory.isEmpty() && !inventory.appliedRecipe)
				start(inventory.getStackInSlot(0));
			return;
		}

		float processingSpeed = MathHelper.clamp(Math.abs(getSpeed()) / 24, 1, 128);
		inventory.remainingTime -= processingSpeed;

		if (inventory.remainingTime > 0)
			spawnParticles(inventory.getStackInSlot(0));

		if (inventory.remainingTime < 5 && !inventory.appliedRecipe) {
			if (world.isClient && !isVirtual())
				return;
			playEvent = inventory.getStackInSlot(0);
			applyRecipe();
			inventory.appliedRecipe = true;
			inventory.recipeDuration = 20;
			inventory.remainingTime = 20;
			sendData();
			return;
		}

		Vec3d itemMovement = getItemMovementVec();
		Direction itemMovementFacing = Direction.getFacing(itemMovement.x, itemMovement.y, itemMovement.z);
		if (inventory.remainingTime > 0)
			return;
		inventory.remainingTime = 0;

		for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack tryExportingToBeltFunnel = getBehaviour(DirectBeltInputBehaviour.TYPE)
				.tryExportingToBeltFunnel(stack, itemMovementFacing.getOpposite(), false);
			if (tryExportingToBeltFunnel != null) {
				if (tryExportingToBeltFunnel.getCount() != stack.getCount()) {
					inventory.setStackInSlot(slot, tryExportingToBeltFunnel);
					notifyUpdate();
					return;
				}
				if (!tryExportingToBeltFunnel.isEmpty())
					return;
			}
		}

		BlockPos nextPos = pos.add(BlockPos.ofFloored(itemMovement));
		DirectBeltInputBehaviour behaviour = BlockEntityBehaviour.get(world, nextPos, DirectBeltInputBehaviour.TYPE);
		if (behaviour != null) {
			boolean changed = false;
			if (!behaviour.canInsertFromSide(itemMovementFacing))
				return;
			if (world.isClient && !isVirtual())
				return;
			for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.isEmpty())
					continue;
				ItemStack remainder = behaviour.handleInsertion(stack, itemMovementFacing, false);
				if (ItemStack.areEqual(remainder, stack))
					continue;
				inventory.setStackInSlot(slot, remainder);
				changed = true;
			}
			if (changed) {
				markDirty();
				sendData();
			}
			return;
		}

		// Eject Items
		Vec3d outPos = VecHelper.getCenterOf(pos)
			.add(itemMovement.multiply(.5f)
				.add(0, .5, 0));
		Vec3d outMotion = itemMovement.multiply(.0625)
			.add(0, .125, 0);
		for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemEntity entityIn = new ItemEntity(world, outPos.x, outPos.y, outPos.z, stack);
			entityIn.setVelocity(outMotion);
			world.spawnEntity(entityIn);
		}
		inventory.clear();
		world.updateComparators(pos, getCachedState().getBlock());
		inventory.remainingTime = -1;
		sendData();
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(world, pos, inventory);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return face == Direction.DOWN ? null : inventory;
	}

	protected void spawnEventParticles(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		ParticleEffect particleData = null;
		if (stack.getItem() instanceof BlockItem)
			particleData = new BlockStateParticleEffect(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock()
				.getDefaultState());
		else
			particleData = new ItemStackParticleEffect(ParticleTypes.ITEM, stack);

		Random r = world.random;
		Vec3d v = VecHelper.getCenterOf(this.pos)
			.add(0, 5 / 16f, 0);
		for (int i = 0; i < 10; i++) {
			Vec3d m = VecHelper.offsetRandomly(new Vec3d(0, 0.25f, 0), r, .125f);
			world.addParticle(particleData, v.x, v.y, v.z, m.x, m.y, m.y);
		}
	}

	protected void spawnParticles(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		ParticleEffect particleData = null;
		float speed = 1;
		if (stack.getItem() instanceof BlockItem)
			particleData = new BlockStateParticleEffect(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock()
				.getDefaultState());
		else {
			particleData = new ItemStackParticleEffect(ParticleTypes.ITEM, stack);
			speed = .125f;
		}

		Random r = world.random;
		Vec3d vec = getItemMovementVec();
		Vec3d pos = VecHelper.getCenterOf(this.pos);
		float offset = inventory.recipeDuration != 0 ? (float) (inventory.remainingTime) / inventory.recipeDuration : 0;
		offset /= 2;
		if (inventory.appliedRecipe)
			offset -= .5f;
		world.addParticle(particleData, pos.getX() + -vec.x * offset, pos.getY() + .45f, pos.getZ() + -vec.z * offset,
			-vec.x * speed, r.nextFloat() * speed, -vec.z * speed);
	}

	public Vec3d getItemMovementVec() {
		boolean alongX = !getCachedState().get(SawBlock.AXIS_ALONG_FIRST_COORDINATE);
		int offset = getSpeed() < 0 ? -1 : 1;
		return new Vec3d(offset * (alongX ? 1 : 0), 0, offset * (alongX ? 0 : -1));
	}

	private void applyRecipe() {
		List<? extends Recipe<?>> recipes = getRecipes();
		if (recipes.isEmpty())
			return;
		if (recipeIndex >= recipes.size())
			recipeIndex = 0;

		Recipe<?> recipe = recipes.get(recipeIndex);

		int rolls = inventory.getStackInSlot(0)
			.getCount();
		inventory.clear();

		List<ItemStack> list = new ArrayList<>();
		for (int roll = 0; roll < rolls; roll++) {
			List<ItemStack> results = new LinkedList<ItemStack>();
			if (recipe instanceof CuttingRecipe)
				results = ((CuttingRecipe) recipe).rollResults();
			else if (recipe instanceof StonecuttingRecipe || recipe.getType() == woodcuttingRecipeType.get())
				results.add(recipe.getOutput(world.getRegistryManager())
					.copy());

			for (int i = 0; i < results.size(); i++) {
				ItemStack stack = results.get(i);
				ItemHelper.addToList(stack, list);
			}
		}

		for (int slot = 0; slot < list.size() && slot + 1 < inventory.getSlotCount(); slot++)
			inventory.setStackInSlot(slot + 1, list.get(slot));

		award(AllAdvancements.SAW_PROCESSING);
	}

	private List<? extends Recipe<?>> getRecipes() {
		Optional<CuttingRecipe> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(world, inventory.getStackInSlot(0),
			AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
		if (assemblyRecipe.isPresent() && filtering.test(assemblyRecipe.get()
			.getOutput(world.getRegistryManager())))
			return ImmutableList.of(assemblyRecipe.get());

		Predicate<Recipe<?>> types = RecipeConditions.isOfType(AllRecipeTypes.CUTTING.getType(),
			AllConfigs.server().recipes.allowStonecuttingOnSaw.get() ? RecipeType.STONECUTTING : null,
			AllConfigs.server().recipes.allowWoodcuttingOnSaw.get() ? woodcuttingRecipeType.get() : null);

		List<Recipe<?>> startedSearch = RecipeFinder.get(cuttingRecipesKey, world, types);
		return startedSearch.stream()
			.filter(RecipeConditions.outputMatchesFilter(filtering))
			.filter(RecipeConditions.firstIngredientMatches(inventory.getStackInSlot(0)))
			.filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r))
			.collect(Collectors.toList());
	}

	public void insertItem(ItemEntity entity) {
		if (!canProcess())
			return;
		if (!inventory.isEmpty())
			return;
		if (!entity.isAlive())
			return;
		if (world.isClient)
			return;

		inventory.clear();
		try (Transaction t = TransferUtil.getTransaction()) {
			ItemStack contained = entity.getStack();
			long inserted = inventory.insert(ItemVariant.of(contained), contained.getCount(), t);
			if (contained.getCount() == inserted)
				entity.discard();
			else
				entity.setStack(ItemHandlerHelper.copyStackWithSize(contained, (int) (contained.getCount() - inserted)));
			t.commit();
		}
	}

	public void start(ItemStack inserted) {
		if (!canProcess())
			return;
		if (inventory.isEmpty())
			return;
		if (world.isClient && !isVirtual())
			return;

		List<? extends Recipe<?>> recipes = getRecipes();
		boolean valid = !recipes.isEmpty();
		int time = 50;

		if (recipes.isEmpty()) {
			inventory.remainingTime = inventory.recipeDuration = 10;
			inventory.appliedRecipe = false;
			sendData();
			return;
		}

		if (valid) {
			recipeIndex++;
			if (recipeIndex >= recipes.size())
				recipeIndex = 0;
		}

		Recipe<?> recipe = recipes.get(recipeIndex);
		if (recipe instanceof CuttingRecipe) {
			time = ((CuttingRecipe) recipe).getProcessingDuration();
		}

		inventory.remainingTime = time * Math.max(1, (inserted.getCount() / 5));
		inventory.recipeDuration = inventory.remainingTime;
		inventory.appliedRecipe = false;
		sendData();
	}

	protected boolean canProcess() {
		return getCachedState().get(SawBlock.FACING) == Direction.UP;
	}

	// Block Breaker

	@Override
	protected boolean shouldRun() {
		return getCachedState().get(SawBlock.FACING)
			.getAxis()
			.isHorizontal();
	}

	@Override
	protected BlockPos getBreakingPos() {
		return getPos().offset(getCachedState().get(SawBlock.FACING));
	}

	@Override
	public void onBlockBroken(BlockState stateToBreak) {
		Optional<AbstractBlockBreakQueue> dynamicTree =
			TreeCutter.findDynamicTree(stateToBreak.getBlock(), breakingPos);
		if (dynamicTree.isPresent()) {
			dynamicTree.get()
				.destroyBlocks(world, null, this::dropItemFromCutTree);
			return;
		}

		super.onBlockBroken(stateToBreak);
		TreeCutter.findTree(world, breakingPos)
			.destroyBlocks(world, null, this::dropItemFromCutTree);
	}

	public void dropItemFromCutTree(BlockPos pos, ItemStack stack) {
		float distance = (float) Math.sqrt(pos.getSquaredDistance(breakingPos));
		Vec3d dropPos = VecHelper.getCenterOf(pos);
		ItemEntity entity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack);
		entity.setVelocity(Vec3d.of(breakingPos.subtract(this.pos))
			.multiply(distance / 20f));
		world.spawnEntity(entity);
	}

	@Override
	public boolean canBreak(BlockState stateToBreak, float blockHardness) {
		boolean sawable = isSawable(stateToBreak);
		return super.canBreak(stateToBreak, blockHardness) && sawable;
	}

	public static boolean isSawable(BlockState stateToBreak) {
		if (stateToBreak.isIn(BlockTags.SAPLINGS))
			return false;
		if (TreeCutter.isLog(stateToBreak) || (stateToBreak.isIn(BlockTags.LEAVES)))
			return true;
		if (TreeCutter.isRoot(stateToBreak))
			return true;
		Block block = stateToBreak.getBlock();
		if (block instanceof BambooBlock)
			return true;
		if (block instanceof GourdBlock)
			return true;
		if (block instanceof CactusBlock)
			return true;
		if (block instanceof SugarCaneBlock)
			return true;
		if (block instanceof KelpPlantBlock)
			return true;
		if (block instanceof KelpBlock)
			return true;
		if (block instanceof ChorusPlantBlock)
			return true;
		if (TreeCutter.canDynamicTreeCutFrom(block))
			return true;
		return false;
	}

}
