package com.simibubi.create.content.kinetics.deployer;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.sandwichable.SequencedSandwiching;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.Mode;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.State;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

public class BeltDeployerCallbacks {

	public static ProcessingResult onItemReceived(TransportedItemStack s, TransportedItemStackHandlerBehaviour i,
		DeployerBlockEntity blockEntity) {

		if (blockEntity.getSpeed() == 0)
			return ProcessingResult.PASS;
		if (blockEntity.mode == Mode.PUNCH)
			return ProcessingResult.PASS;
		BlockState blockState = blockEntity.getCachedState();
		if (!blockState.contains(FACING) || blockState.get(FACING) != Direction.DOWN)
			return ProcessingResult.PASS;
		if (blockEntity.state != State.WAITING)
			return ProcessingResult.HOLD;
		if (blockEntity.redstoneLocked)
			return ProcessingResult.PASS;

		DeployerFakePlayer player = blockEntity.getPlayer();
		ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandStack();

		if (held.isEmpty())
			return ProcessingResult.HOLD;
		if (blockEntity.getRecipe(s.stack) == null) {
			if (Mods.SANDWICHABLE.isLoaded()) {
				if (!SequencedSandwiching.shouldSandwich(s.stack, held, blockEntity.getWorld()))
					return ProcessingResult.PASS;
			} else {
				return ProcessingResult.PASS;
			}
		}

		blockEntity.start();
		return ProcessingResult.HOLD;
	}

	public static ProcessingResult whenItemHeld(TransportedItemStack s, TransportedItemStackHandlerBehaviour i,
		DeployerBlockEntity blockEntity) {

		if (blockEntity.getSpeed() == 0)
			return ProcessingResult.PASS;
		BlockState blockState = blockEntity.getCachedState();
		if (!blockState.contains(FACING) || blockState.get(FACING) != Direction.DOWN)
			return ProcessingResult.PASS;

		DeployerFakePlayer player = blockEntity.getPlayer();
		ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandStack();
		if (held.isEmpty())
			return ProcessingResult.HOLD;

		Recipe<?> recipe = blockEntity.getRecipe(s.stack);
		boolean shouldSandwich = Mods.SANDWICHABLE.isLoaded() && SequencedSandwiching.shouldSandwich(s.stack, held, blockEntity.getWorld());
		if (recipe == null && !shouldSandwich) {
			return ProcessingResult.PASS;
		}

		if (blockEntity.state == State.RETRACTING && blockEntity.timer == 1000) {
			if (recipe != null)
				activate(s, i, blockEntity, recipe);
			else
				SequencedSandwiching.activateSandwich(s, i, blockEntity);
			return ProcessingResult.HOLD;
		}

		if (blockEntity.state == State.WAITING) {
			if (blockEntity.redstoneLocked)
				return ProcessingResult.PASS;
			blockEntity.start();
		}

		return ProcessingResult.HOLD;
	}

	public static void activate(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler,
		DeployerBlockEntity blockEntity, Recipe<?> recipe) {
		List<TransportedItemStack> collect =
			RecipeApplier.applyRecipeOn(blockEntity.getWorld(), ItemHandlerHelper.copyStackWithSize(transported.stack, 1), recipe)
				.stream()
				.map(stack -> {
					TransportedItemStack copy = transported.copy();
					boolean centered = BeltHelper.isItemUpright(stack);
					copy.stack = stack;
					copy.locked = true;
					copy.angle = centered ? 180 : Create.RANDOM.nextInt(360);
					return copy;
				})
				.map(t -> {
					t.locked = false;
					return t;
				})
				.collect(Collectors.toList());

		blockEntity.award(AllAdvancements.DEPLOYER);
		TransportedItemStack left = transported.copy();
		blockEntity.player.spawnedItemEffects = transported.stack.copy();
		left.stack.decrement(1);
		ItemStack resultItem = null;

		if (collect.isEmpty()) {
			resultItem = left.stack.copy();
			handler.handleProcessingOnItem(transported, TransportedResult.convertTo(left));
		} else {
			resultItem = collect.get(0).stack.copy();
			handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(collect, left));
		}

		ItemStack heldItem = blockEntity.player.getMainHandStack();
		boolean unbreakable = heldItem.hasNbt() && heldItem.getNbt()
			.getBoolean("Unbreakable");
		boolean keepHeld =
			recipe instanceof ItemApplicationRecipe && ((ItemApplicationRecipe) recipe).shouldKeepHeldItem();

		if (!unbreakable && !keepHeld) {
			if (heldItem.isDamageable())
				heldItem.damage(1, blockEntity.player,
					s -> s.sendToolBreakStatus(Hand.MAIN_HAND));
			else
				heldItem.decrement(1);
		}

		if (resultItem != null && !resultItem.isEmpty())
			awardAdvancements(blockEntity, resultItem);

		BlockPos pos = blockEntity.getPos();
		World world = blockEntity.getWorld();
		if (heldItem.isEmpty())
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, .25f, 1);
		world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, .25f, .75f);
		if (recipe instanceof SandPaperPolishingRecipe)
			AllSoundEvents.SANDING_SHORT.playOnServer(world, pos, .35f, 1f);

		blockEntity.sendData();
	}

	private static void awardAdvancements(DeployerBlockEntity blockEntity, ItemStack created) {
		CreateAdvancement advancement = null;

		if (AllBlocks.ANDESITE_CASING.isIn(created))
			advancement = AllAdvancements.ANDESITE_CASING;
		else if (AllBlocks.BRASS_CASING.isIn(created))
			advancement = AllAdvancements.BRASS_CASING;
		else if (AllBlocks.COPPER_CASING.isIn(created))
			advancement = AllAdvancements.COPPER_CASING;
		else if (AllBlocks.RAILWAY_CASING.isIn(created))
			advancement = AllAdvancements.TRAIN_CASING;
		else
			return;

		blockEntity.award(advancement);
	}

}
