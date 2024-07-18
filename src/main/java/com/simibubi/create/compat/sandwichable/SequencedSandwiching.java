package com.simibubi.create.compat.sandwichable;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;

import io.github.foundationgames.sandwichable.Sandwichable;
import io.github.foundationgames.sandwichable.items.ItemsRegistry;
import io.github.foundationgames.sandwichable.util.Sandwich;

public class SequencedSandwiching {
	/**
	 * Only allow sequenced sandwiching on sandwiches (or bread, becoming sandwiches),
	 * AND if the item-to-add can be put on a sandwich.
	 */
	public static boolean shouldSandwich(ItemStack handling, ItemStack held, World level) {
		boolean eligible = Sandwich.canAdd(held) && (Sandwichable.isBread(handling) || handling.isOf(ItemsRegistry.SANDWICH));
		int max = level.getGameRules().getInt(Sandwichable.SANDWICH_SIZE_RULE);
		Sandwich sandwich = sandwichFromStack(handling);
		if (sandwich != null && max > 0) {
			return eligible && sandwich.getFoodList().size() < max;
		}
		return eligible;
	}

	/**
	 * Actually assemble a sandwich.
	 * @param transported the sandwich stack, passing below the deployer
	 * @param handler the deployer's handler
	 * @param deployer the block entity of the deployer
	 */
	public static void activateSandwich(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler,
										DeployerBlockEntity deployer) {

		TransportedItemStack transportedRemainder = transported.copy();
		DeployerFakePlayer player = deployer.getPlayer();
		transportedRemainder.stack.decrement(1);
		ItemStack heldItem = player.getMainHandStack();

		ItemStack newSandwich = stackOnSandwich(transported.stack, heldItem, deployer);
		if (newSandwich.isEmpty())
			return;

		TransportedItemStack output = transported.copy();
		boolean centered = BeltHelper.isItemUpright(newSandwich);
		output.stack = newSandwich;
		output.angle = centered ? 180 : Create.RANDOM.nextInt(360);

		handler.handleProcessingOnItem(transported, TransportedResult
				.convertToAndLeaveHeld(ImmutableList.of(output), transportedRemainder));

		heldItem.decrement(1);

		BlockPos pos = deployer.getPos();
		World world = deployer.getWorld();
		if (heldItem.isEmpty())
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, .25f, 1);
		world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, .25f, .75f);

		deployer.sendData();
	}

	/**
	 * Stacks 'toAdd' on top of the given sandwich.
	 * If the item to add is a spread (and has a remainder) it is added to the deployer's overflow items.
	 * @return the new sandwich, with the item stacked on top, or EMPTY if nothing stacked
	 */
	public static ItemStack stackOnSandwich(ItemStack sandwich, ItemStack toAdd, DeployerBlockEntity deployer) {
		World level = deployer.getWorld();
		Sandwich s = sandwichFromStack(sandwich);
		if (s != null) {
			// null - not added
			// empty - do not return an item
			// non-empty - return item
			ItemStack result = s.tryAddTopFoodFrom(level, toAdd.copy());
			if (result == null) {
				return sandwich;
			} else if (!result.isEmpty()) {
				deployer.getOverflowItems().add(result.copy());
			}
			NbtCompound newTag = s.writeToNbt(new NbtCompound());
			ItemStack newSandwich = sandwich.copy();
			newSandwich.getOrCreateNbt().put("BlockEntityTag", newTag);
			return newSandwich;
		} else if (Sandwichable.isBread(sandwich)) {
			s = new Sandwich();
			s.addTopFoodFrom(sandwich.copy());
			ItemStack result = s.tryAddTopFoodFrom(level, toAdd.copy());
			if (result == null) {
				return sandwich;
			} else if (!result.isEmpty()) {
				deployer.getOverflowItems().add(result.copy());
			}
			NbtCompound newTag = s.writeToNbt(new NbtCompound());
			ItemStack freshSandwich = ItemsRegistry.SANDWICH.getDefaultStack();
			freshSandwich.getOrCreateNbt().put("BlockEntityTag", newTag);
			return freshSandwich;
		}
		return ItemStack.EMPTY;
	}

	@Nullable
	public static Sandwich sandwichFromStack(ItemStack stack) {
		if (stack.isOf(ItemsRegistry.SANDWICH)) {
			NbtCompound tag = stack.getNbt();
			if (tag != null && tag.contains("BlockEntityTag")) {
				tag = tag.getCompound("BlockEntityTag");
				Sandwich s = new Sandwich();
				s.addFromNbt(tag);
				return s;
			}
		}
		return null;
	}
}
