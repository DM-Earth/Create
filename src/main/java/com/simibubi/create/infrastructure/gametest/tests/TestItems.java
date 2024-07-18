package com.simibubi.create.infrastructure.gametest.tests;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity.SelectionMode;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.text.MutableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

@GameTestGroup(path = "items")
public class TestItems {
	@GameTest(templateName = "andesite_tunnel_split")
	public static void andesiteTunnelSplit(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 6, 2);
		helper.toggleLever(lever);
		Map<BlockPos, ItemStack> outputs = Map.of(
				new BlockPos(2, 2, 1), new ItemStack(AllItems.BRASS_INGOT.get(), 1),
				new BlockPos(3, 2, 1), new ItemStack(AllItems.BRASS_INGOT.get(), 1),
				new BlockPos(4, 2, 2), new ItemStack(AllItems.BRASS_INGOT.get(), 3)
		);
		helper.addInstantFinalTask(() -> outputs.forEach(helper::assertContainerContains));
	}

	@GameTest(templateName = "arm_purgatory", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void armPurgatory(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 3, 2);
		BlockPos depot1Pos = new BlockPos(3, 2, 1);
		DepotBlockEntity depot1 = helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), depot1Pos);
		BlockPos depot2Pos = new BlockPos(1, 2, 1);
		DepotBlockEntity depot2 = helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), depot2Pos);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.assertSecondsPassed(5);
			ItemStack held1 = depot1.getHeldItem();
			boolean held1Empty = held1.isEmpty();
			int held1Count = held1.getCount();
			ItemStack held2 = depot2.getHeldItem();
			boolean held2Empty = held2.isEmpty();
			int held2Count = held2.getCount();
			if (held1Empty && held2Empty)
				helper.throwGameTestException("No item present");
			if (!held1Empty && held1Count != 1)
				helper.throwGameTestException("Unexpected count on depot 1: " + held1Count);
			if (!held2Empty && held2Count != 1)
				helper.throwGameTestException("Unexpected count on depot 2: " + held2Count);
		});
	}

	@GameTest(templateName = "attribute_filters", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void attributeFilters(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 3, 1);
		BlockPos end = new BlockPos(11, 2, 2);
		Map<BlockPos, ItemStack> outputs = Map.of(
				new BlockPos(3, 2, 1), new ItemStack(AllBlocks.BRASS_BLOCK.get()),
				new BlockPos(4, 2, 1), new ItemStack(Items.APPLE),
				new BlockPos(5, 2, 1), new ItemStack(Items.WATER_BUCKET),
				new BlockPos(6, 2, 1), EnchantedBookItem.forEnchantment(
						new EnchantmentLevelEntry(Enchantments.PROTECTION, 1)
				),
				new BlockPos(7, 2, 1), Util.make(
						new ItemStack(Items.NETHERITE_SWORD),
						s -> s.setDamage(1)
				),
				new BlockPos(8, 2, 1), new ItemStack(Items.IRON_HELMET),
				new BlockPos(9, 2, 1), new ItemStack(Items.COAL),
				new BlockPos(10, 2, 1), new ItemStack(Items.POTATO)
		);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			outputs.forEach(helper::assertContainerContains);
			helper.expectEmptyContainer(end);
		});
	}

	@GameTest(templateName = "belt_coaster", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void beltCoaster(CreateGameTestHelper helper) {
		BlockPos input = new BlockPos(1, 5, 6);
		BlockPos output = new BlockPos(3, 8, 6);
		BlockPos lever = new BlockPos(1, 5, 5);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			long outputItems = helper.getTotalItems(output);
			if (outputItems != 27)
				helper.throwGameTestException("Expected 27 items, got " + outputItems);
			long remainingItems = helper.getTotalItems(input);
			if (remainingItems != 2)
				helper.throwGameTestException("Expected 2 items remaining, got " + remainingItems);
		});
	}

	@GameTest(templateName = "brass_tunnel_filtering")
	public static void brassTunnelFiltering(CreateGameTestHelper helper) {
		Map<BlockPos, ItemStack> outputs = Map.of(
				new BlockPos(3, 2, 2), new ItemStack(Items.COPPER_INGOT, 13),
				new BlockPos(4, 2, 3), new ItemStack(AllItems.ZINC_INGOT.get(), 4),
				new BlockPos(4, 2, 4), new ItemStack(Items.IRON_INGOT, 2),
				new BlockPos(4, 2, 5), new ItemStack(Items.GOLD_INGOT, 24),
				new BlockPos(3, 2, 6), new ItemStack(Items.DIAMOND, 17)
		);
		BlockPos lever = new BlockPos(2, 3, 2);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> outputs.forEach(helper::assertContainerContains));
	}

	@GameTest(templateName = "brass_tunnel_prefer_nearest", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void brassTunnelPreferNearest(CreateGameTestHelper helper) {
		List<BlockPos> tunnels = List.of(
				new BlockPos(3, 3, 1),
				new BlockPos(3, 3, 2),
				new BlockPos(3, 3, 3)
		);
		List<BlockPos> out = List.of(
				new BlockPos(5, 2, 1),
				new BlockPos(5, 2, 2),
				new BlockPos(5, 2, 3)
		);
		BlockPos lever = new BlockPos(2, 3, 2);
		helper.toggleLever(lever);
		// tunnels reconnect and lose their modes
		tunnels.forEach(tunnel -> helper.setTunnelMode(tunnel, SelectionMode.PREFER_NEAREST));
		helper.addInstantFinalTask(() ->
				out.forEach(pos ->
						helper.assertContainerContains(pos, AllBlocks.BRASS_CASING.get())
				)
		);
	}

	@GameTest(templateName = "brass_tunnel_round_robin", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void brassTunnelRoundRobin(CreateGameTestHelper helper) {
		List<BlockPos> outputs = List.of(
				new BlockPos(7, 3, 1),
				new BlockPos(7, 3, 2),
				new BlockPos(7, 3, 3)
		);
		brassTunnelModeTest(helper, SelectionMode.ROUND_ROBIN, outputs);
	}

	@GameTest(templateName = "brass_tunnel_split")
	public static void brassTunnelSplit(CreateGameTestHelper helper) {
		List<BlockPos> outputs = List.of(
				new BlockPos(7, 2, 1),
				new BlockPos(7, 2, 2),
				new BlockPos(7, 2, 3)
		);
		brassTunnelModeTest(helper, SelectionMode.SPLIT, outputs);
	}

	private static void brassTunnelModeTest(CreateGameTestHelper helper, SelectionMode mode, List<BlockPos> outputs) {
		BlockPos lever = new BlockPos(2, 3, 2);
		List<BlockPos> tunnels = List.of(
				new BlockPos(3, 3, 1),
				new BlockPos(3, 3, 2),
				new BlockPos(3, 3, 3)
		);
		helper.toggleLever(lever);
		tunnels.forEach(tunnel -> helper.setTunnelMode(tunnel, mode));
		helper.addInstantFinalTask(() -> {
			long items = 0;
			for (BlockPos out : outputs) {
				helper.assertContainerContains(out, AllBlocks.BRASS_CASING.get());
				items += helper.getTotalItems(out);
			}
			if (items != 10)
				helper.throwGameTestException("expected 10 items, got " + items);
		});
	}

	@GameTest(templateName = "brass_tunnel_sync_input", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void brassTunnelSyncInput(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(1, 3, 2);
		List<BlockPos> redstoneBlocks = List.of(
				new BlockPos(3, 4, 1),
				new BlockPos(3, 4, 2),
				new BlockPos(3, 4, 3)
		);
		List<BlockPos> tunnels = List.of(
				new BlockPos(5, 3, 1),
				new BlockPos(5, 3, 2),
				new BlockPos(5, 3, 3)
		);
		List<BlockPos> outputs = List.of(
				new BlockPos(7, 2, 1),
				new BlockPos(7, 2, 2),
				new BlockPos(7, 2, 3)
		);
		helper.toggleLever(lever);
		tunnels.forEach(tunnel -> helper.setTunnelMode(tunnel, SelectionMode.SYNCHRONIZE));
		helper.addInstantFinalTask(() -> {
			if (helper.secondsPassed() < 9) {
				helper.setBlockState(redstoneBlocks.get(0), Blocks.AIR);
				helper.assertSecondsPassed(3);
				outputs.forEach(helper::expectEmptyContainer);
				helper.setBlockState(redstoneBlocks.get(1), Blocks.AIR);
				helper.assertSecondsPassed(6);
				outputs.forEach(helper::expectEmptyContainer);
				helper.setBlockState(redstoneBlocks.get(2), Blocks.AIR);
				helper.assertSecondsPassed(9);
			} else {
				outputs.forEach(out -> helper.assertContainerContains(out, AllBlocks.BRASS_CASING.get()));
			}
		});
	}

	@GameTest(templateName = "smart_observer_belt_and_funnel", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void smartObserverBeltAndFunnel(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(6, 3, 2);
		List<BlockPos> targets = List.of(
				new BlockPos(5, 2, 1), // belt
				new BlockPos(2, 4, 6) // funnel
		);
		List<BlockPos> overflows = List.of(
				new BlockPos(6, 2, 1), // belt
				new BlockPos(1, 3, 6) // funnel
		);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.assertSecondsPassed(9);
			targets.forEach(pos -> helper.expectBlock(Blocks.DIAMOND_BLOCK, pos));
			overflows.forEach(pos -> helper.expectBlock(Blocks.AIR, pos));
		});
	}

	@GameTest(templateName = "smart_observer_chutes")
	public static void smartObserverChutes(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(1, 5, 2);
		BlockPos output = new BlockPos(1, 5, 3);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> helper.expectBlock(Blocks.DIAMOND_BLOCK, output));
	}

	@GameTest(templateName = "smart_observer_counting")
	public static void smartObserverCounting(CreateGameTestHelper helper) {
		BlockPos chest = new BlockPos(3, 2, 1);
		long totalChestItems = helper.getTotalItems(chest);
		BlockPos chestNixiePos = new BlockPos(2, 3, 1);
		NixieTubeBlockEntity chestNixie = helper.getBlockEntity(AllBlockEntityTypes.NIXIE_TUBE.get(), chestNixiePos);

		BlockPos doubleChest = new BlockPos(2, 2, 3);
		long totalDoubleChestItems = helper.getTotalItems(doubleChest);
		BlockPos doubleChestNixiePos = new BlockPos(1, 3, 3);
		NixieTubeBlockEntity doubleChestNixie = helper.getBlockEntity(AllBlockEntityTypes.NIXIE_TUBE.get(), doubleChestNixiePos);

		helper.addInstantFinalTask(() -> {
			String chestNixieText = chestNixie.getFullText().getString();
			long chestNixieReading = Long.parseLong(chestNixieText);
			if (chestNixieReading != totalChestItems)
				helper.throwGameTestException("Chest nixie detected %s, expected %s".formatted(chestNixieReading, totalChestItems));
			String doubleChestNixieText = doubleChestNixie.getFullText().getString();
			long doubleChestNixieReading = Long.parseLong(doubleChestNixieText);
			if (doubleChestNixieReading != totalDoubleChestItems)
				helper.throwGameTestException("Double chest nixie detected %s, expected %s".formatted(doubleChestNixieReading, totalDoubleChestItems));
		});
	}

	@GameTest(templateName = "smart_observer_filtered_storage")
	public static void smartObserverFilteredStorage(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 3, 1);
		BlockPos leftLamp = new BlockPos(3, 2, 3);
		BlockPos rightLamp = new BlockPos(1, 2, 3);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.expectBlockProperty(leftLamp, RedstoneLampBlock.LIT, true);
			helper.expectBlockProperty(rightLamp, RedstoneLampBlock.LIT, false);
		});
	}

	@GameTest(templateName = "smart_observer_storage")
	public static void smartObserverStorage(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(1, 3, 2);
		BlockPos lamp = new BlockPos(1, 2, 3);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> helper.expectBlockProperty(lamp, RedstoneLampBlock.LIT, true));
	}

	@GameTest(templateName = "depot_display", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void depotDisplay(CreateGameTestHelper helper) {
		BlockPos displayPos = new BlockPos(5, 3, 1);
		List<DepotBlockEntity> depots = Stream.of(
				new BlockPos(2, 2, 1),
				new BlockPos(1, 2, 1)
		).map(pos -> helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), pos)).toList();
		List<BlockPos> levers = List.of(
				new BlockPos(2, 5, 0),
				new BlockPos(1, 5, 0)
		);
		levers.forEach(helper::toggleLever);
		FlapDisplayBlockEntity display = helper.getBlockEntity(AllBlockEntityTypes.FLAP_DISPLAY.get(), displayPos).getController();
		helper.addInstantFinalTask(() -> {
			for (int i = 0; i < 2; i++) {
				FlapDisplayLayout line = display.getLines().get(i);
				MutableText textComponent = Components.empty();
				line.getSections().stream().map(FlapDisplaySection::getText).forEach(textComponent::append);
				String text = textComponent.getString().toLowerCase(Locale.ROOT).trim();

				DepotBlockEntity depot = depots.get(i);
				ItemStack item = depot.getHeldItem();
				String name = Registries.ITEM.getId(item.getItem()).getPath();

				if (!name.equals(text))
					helper.throwGameTestException("Text mismatch: wanted [" + name + "], got: " + text);
			}
		});
	}

	@GameTest(templateName = "threshold_switch")
	public static void thresholdSwitch(CreateGameTestHelper helper) {
		BlockPos chest = new BlockPos(1, 2, 1);
		BlockPos lamp = new BlockPos(2, 3, 1);
		helper.expectBlockProperty(lamp, RedstoneLampBlock.LIT, false);
		Storage<ItemVariant> chestStorage = helper.itemStorageAt(chest);
		ItemStack diamondStack = new ItemStack(Items.DIAMOND, 64);
		for (int i = 0; i < 18; i++) { // insert 18 stacks
			TransferUtil.insertItem(chestStorage, diamondStack);
		}
		helper.addInstantFinalTask(() -> helper.expectBlockProperty(lamp, RedstoneLampBlock.LIT, true));
	}

	@GameTest(templateName = "storages", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void storages(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(12, 3, 2);
		BlockPos startChest = new BlockPos(13, 3, 1);
		Object2LongMap<Item> originalContent = helper.getItemContent(startChest);
		BlockPos endShulker = new BlockPos(1, 3, 1);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> helper.assertContentPresent(originalContent, endShulker));
	}

	@GameTest(templateName = "vault_comparator_output")
	public static void vaultComparatorOutput(CreateGameTestHelper helper) {
		BlockPos smallInput = new BlockPos(1, 4, 1);
		BlockPos smallNixie = new BlockPos(3, 2, 1);
		helper.assertNixiePower(smallNixie, 0);
		helper.whenSecondsPassed(1, () -> helper.spawnItems(smallInput, Items.BREAD, 64 * 9));

		BlockPos medInput = new BlockPos(1, 5, 4);
		BlockPos medNixie = new BlockPos(4, 2, 4);
		helper.assertNixiePower(medNixie, 0);
		helper.whenSecondsPassed(2, () -> helper.spawnItems(medInput, Items.BREAD, 64 * 77));

		BlockPos bigInput = new BlockPos(1, 6, 8);
		BlockPos bigNixie = new BlockPos(5, 2, 7);
		helper.assertNixiePower(bigNixie, 0);
		helper.whenSecondsPassed(3, () -> helper.spawnItems(bigInput, Items.BREAD, 64 * 240));

		helper.addInstantFinalTask(() -> {
			helper.assertNixiePower(smallNixie, 7);
			helper.assertNixiePower(medNixie, 7);
			helper.assertNixiePower(bigNixie, 7);
		});
	}
}
