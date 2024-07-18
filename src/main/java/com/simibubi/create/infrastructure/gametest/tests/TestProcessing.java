package com.simibubi.create.infrastructure.gametest.tests;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.util.math.BlockPos;

@GameTestGroup(path = "processing")
public class TestProcessing {
	@GameTest(templateName = "brass_mixing", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void brassMixing(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 3, 2);
		BlockPos chest = new BlockPos(7, 3, 1);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> helper.expectContainerWith(chest, AllItems.BRASS_INGOT.get()));
	}

	@GameTest(templateName = "brass_mixing_2", tickLimit = CreateGameTestHelper.TWENTY_SECONDS)
	public static void brassMixing2(CreateGameTestHelper helper) {
		BlockPos basinLever = new BlockPos(3, 3, 1);
		BlockPos armLever = new BlockPos(3, 3, 5);
		BlockPos output = new BlockPos(1, 2, 3);
		helper.toggleLever(armLever);
		helper.whenSecondsPassed(7, () -> helper.toggleLever(armLever));
		helper.whenSecondsPassed(10, () -> helper.toggleLever(basinLever));
		helper.addInstantFinalTask(() -> helper.expectContainerWith(output, AllItems.BRASS_INGOT.get()));
	}

	@GameTest(templateName = "crushing_wheel_crafting", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void crushingWheelCrafting(CreateGameTestHelper helper) {
		BlockPos chest = new BlockPos(1, 4, 3);
		List<BlockPos> levers = List.of(
				new BlockPos(2, 3, 2),
				new BlockPos(6, 3, 2),
				new BlockPos(3, 7, 3)
		);
		levers.forEach(helper::toggleLever);
		ItemStack expected = new ItemStack(AllBlocks.CRUSHING_WHEEL.get(), 2);
		helper.addInstantFinalTask(() -> helper.assertContainerContains(chest, expected));
	}

	@GameTest(templateName = "precision_mechanism_crafting", tickLimit = CreateGameTestHelper.TWENTY_SECONDS)
	public static void precisionMechanismCrafting(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(6, 3, 6);
		BlockPos output = new BlockPos(11, 3, 1);
		helper.toggleLever(lever);

		SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) helper.getWorld().getRecipeManager()
				.get(Create.asResource("sequenced_assembly/precision_mechanism"))
				.orElseThrow(() -> new GameTestException("Precision Mechanism recipe not found"));
		Item result = recipe.getOutput(helper.getWorld().getRegistryManager()).getItem();
		Item[] possibleResults = recipe.resultPool.stream()
				.map(ProcessingOutput::getStack)
				.map(ItemStack::getItem)
				.filter(item -> item != result)
				.toArray(Item[]::new);

		helper.addInstantFinalTask(() -> {
			helper.expectContainerWith(output, result);
			helper.assertAnyContained(output, possibleResults);
		});
	}

	@GameTest(templateName = "sand_washing", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void sandWashing(CreateGameTestHelper helper) {
		BlockPos leverPos = new BlockPos(5, 3, 1);
		helper.toggleLever(leverPos);
		BlockPos chestPos = new BlockPos(8, 3, 2);
		helper.addInstantFinalTask(() -> helper.expectContainerWith(chestPos, Items.CLAY_BALL));
	}

	@GameTest(templateName = "stone_cobble_sand_crushing", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void stoneCobbleSandCrushing(CreateGameTestHelper helper) {
		BlockPos chest = new BlockPos(1, 6, 2);
		BlockPos lever = new BlockPos(2, 3, 1);
		helper.toggleLever(lever);
		ItemStack expected = new ItemStack(Items.SAND, 5);
		helper.addInstantFinalTask(() -> helper.assertContainerContains(chest, expected));
	}

	@GameTest(templateName = "track_crafting", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void trackCrafting(CreateGameTestHelper helper) {
		BlockPos output = new BlockPos(7, 3, 2);
		BlockPos lever = new BlockPos(2, 3, 1);
		helper.toggleLever(lever);
		ItemStack expected = new ItemStack(AllBlocks.TRACK.get(), 6);
		helper.addInstantFinalTask(() -> {
			helper.assertContainerContains(output, expected);
			Storage<ItemVariant> storage = helper.itemStorageAt(output);
			ItemHelper.extract(storage, ItemHelper.sameItemPredicate(expected), 6, false);
			helper.expectEmptyContainer(output);
		});
	}

	@GameTest(templateName = "water_filling_bottle")
	public static void waterFillingBottle(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(3, 3, 3);
		BlockPos output = new BlockPos(2, 2, 4);
		ItemStack expected = PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> helper.assertContainerContains(output, expected));
	}

	@GameTest(templateName = "wheat_milling")
	public static void wheatMilling(CreateGameTestHelper helper) {
		BlockPos output = new BlockPos(1, 2, 1);
		BlockPos lever = new BlockPos(1, 7, 1);
		helper.toggleLever(lever);
		ItemStack expected = new ItemStack(AllItems.WHEAT_FLOUR.get(), 3);
		helper.addInstantFinalTask(() -> helper.assertContainerContains(output, expected));
	}
}
