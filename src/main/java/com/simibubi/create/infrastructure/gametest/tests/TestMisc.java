package com.simibubi.create.infrastructure.gametest.tests;

import static com.simibubi.create.infrastructure.gametest.CreateGameTestHelper.FIFTEEN_SECONDS;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity.State;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.test.GameTest;
import net.minecraft.util.math.BlockPos;

@GameTestGroup(path = "misc")
public class TestMisc {
	@GameTest(templateName = "schematicannon", tickLimit = FIFTEEN_SECONDS)
	public static void schematicannon(CreateGameTestHelper helper) {
		// load the structure
		BlockPos whiteEndBottom = helper.getAbsolutePos(new BlockPos(5, 2, 1));
		BlockPos redEndTop = helper.getAbsolutePos(new BlockPos(5, 4, 7));
		ServerWorld level = helper.getWorld();
		SchematicExport.saveSchematic(
				SchematicExport.SCHEMATICS.resolve("uploaded/Deployer"), "schematicannon_gametest", true,
				level, whiteEndBottom, redEndTop
		);
		ItemStack schematic =
			SchematicItem.create(level.createCommandRegistryWrapper(RegistryKeys.BLOCK), "schematicannon_gametest.nbt", "Deployer");
		// deploy to pos
		BlockPos anchor = helper.getAbsolutePos(new BlockPos(1, 2, 1));
		schematic.getOrCreateNbt().putBoolean("Deployed", true);
		schematic.getOrCreateNbt().put("Anchor", NbtHelper.fromBlockPos(anchor));
		// setup cannon
		BlockPos cannonPos = new BlockPos(3, 2, 6);
		SchematicannonBlockEntity cannon = helper.getBlockEntity(AllBlockEntityTypes.SCHEMATICANNON.get(), cannonPos);
		cannon.inventory.setStackInSlot(0, schematic);
		// run
		cannon.state = State.RUNNING;
		cannon.statusMsg = "running";
		helper.addInstantFinalTask(() -> {
			if (cannon.state != State.STOPPED) {
				helper.throwGameTestException("Schematicannon not done");
			}
			BlockPos lastBlock = new BlockPos(1, 4, 7);
			helper.expectBlock(Blocks.RED_WOOL, lastBlock);
		});
	}

	@GameTest(templateName = "shearing")
	public static void shearing(CreateGameTestHelper helper) {
		BlockPos sheepPos = new BlockPos(2, 1, 2);
		SheepEntity sheep = helper.getFirstEntity(EntityType.SHEEP, sheepPos);
		sheep.sheared(SoundCategory.NEUTRAL);
		helper.addInstantFinalTask(() -> {
			helper.expectItemAt(Items.WHITE_WOOL, sheepPos, 2);
		});
	}

	@GameTest(templateName = "smart_observer_blocks")
	public static void smartObserverBlocks(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 2, 1);
		BlockPos leftLamp = new BlockPos(3, 4, 3);
		BlockPos rightLamp = new BlockPos(1, 4, 3);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.expectBlockProperty(leftLamp, RedstoneLampBlock.LIT, true);
			helper.expectBlockProperty(rightLamp, RedstoneLampBlock.LIT, false);
		});
	}

	@GameTest(templateName = "threshold_switch_pulley")
	public static void thresholdSwitchPulley(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(3, 7, 1);
		BlockPos switchPos = new BlockPos(1, 6, 1);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			ThresholdSwitchBlockEntity switchBe = helper.getBlockEntity(AllBlockEntityTypes.THRESHOLD_SWITCH.get(), switchPos);
			float level = switchBe.getStockLevel();
			if (level < 0 || level > 1)
				helper.throwGameTestException("Invalid level: " + level);
		});
	}

	@GameTest(templateName = "netherite_backtank", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void netheriteBacktank(CreateGameTestHelper helper) {
		BlockPos lava = new BlockPos(2, 2, 3);
		BlockPos zombieSpawn = lava.up(2);
		BlockPos armorStandPos = new BlockPos(2, 2, 1);
		helper.runAtTick(5, () -> {
			ZombieEntity zombie = helper.spawnEntity(EntityType.ZOMBIE, zombieSpawn);
			ArmorStandEntity armorStand = helper.getFirstEntity(EntityType.ARMOR_STAND, armorStandPos);
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				zombie.equipStack(slot, armorStand.getEquippedStack(slot).copy());
			}
		});
		helper.addInstantFinalTask(() -> {
			helper.assertSecondsPassed(9);
			helper.expectEntityAt(EntityType.ZOMBIE, lava);
		});
	}
}
