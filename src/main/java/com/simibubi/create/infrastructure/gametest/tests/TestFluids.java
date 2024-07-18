package com.simibubi.create.infrastructure.gametest.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@GameTestGroup(path = "fluids")
public class TestFluids {
	@GameTest(templateName = "hose_pulley_transfer", tickLimit = CreateGameTestHelper.TWENTY_SECONDS)
	public static void hosePulleyTransfer(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(7, 7, 5);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.assertSecondsPassed(15);
			// check filled
			BlockPos filledLowerCorner = new BlockPos(2, 3, 2);
			BlockPos filledUpperCorner = new BlockPos(4, 5, 4);
			BlockPos.iterate(filledLowerCorner, filledUpperCorner)
					.forEach(pos -> helper.expectBlock(Blocks.WATER, pos));
			// check emptied
			BlockPos emptiedLowerCorner = new BlockPos(8, 3, 2);
			BlockPos emptiedUpperCorner = new BlockPos(10, 5, 4);
			BlockPos.iterate(emptiedLowerCorner, emptiedUpperCorner)
					.forEach(pos -> helper.expectBlock(Blocks.AIR, pos));
			// check nothing left in pulley
			BlockPos pulleyPos = new BlockPos(4, 7, 3);
			Storage<FluidVariant> storage = helper.fluidStorageAt(pulleyPos);
			if (storage instanceof HosePulleyFluidHandler hose) {
				SmartFluidTank internalTank = hose.getInternalTank();
				if (!internalTank.isEmpty())
					helper.throwGameTestException("Pulley not empty");
			} else {
				helper.throwGameTestException("Not a pulley");
			}
		});
	}

	@GameTest(templateName = "in_world_pumping_out")
	public static void inWorldPumpingOut(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(4, 3, 3);
		BlockPos basin = new BlockPos(5, 2, 2);
		BlockPos output = new BlockPos(2, 2, 2);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.expectBlock(Blocks.WATER, output);
			helper.assertTankEmpty(basin);
		});
	}

	@GameTest(templateName = "in_world_pumping_in")
	public static void inWorldPumpingIn(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(4, 3, 3);
		BlockPos basin = new BlockPos(5, 2, 2);
		BlockPos water = new BlockPos(2, 2, 2);
		FluidStack expectedResult = new FluidStack(Fluids.WATER, FluidConstants.BUCKET);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.expectBlock(Blocks.AIR, water);
			helper.assertFluidPresent(expectedResult, basin);
		});
	}

	@GameTest(templateName = "steam_engine")
	public static void steamEngine(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(4, 3, 3);
		helper.toggleLever(lever);
		BlockPos stressometer = new BlockPos(5, 2, 5);
		BlockPos speedometer = new BlockPos(4, 2, 5);
		helper.addInstantFinalTask(() -> {
			StressGaugeBlockEntity stress = helper.getBlockEntity(AllBlockEntityTypes.STRESSOMETER.get(), stressometer);
			SpeedGaugeBlockEntity speed = helper.getBlockEntity(AllBlockEntityTypes.SPEEDOMETER.get(), speedometer);
			float capacity = stress.getNetworkCapacity();
			helper.assertCloseEnoughTo(capacity, 2048);
			float rotationSpeed = MathHelper.abs(speed.getSpeed());
			helper.assertCloseEnoughTo(rotationSpeed, 16);
		});
	}

	@GameTest(templateName = "3_pipe_combine", tickLimit = CreateGameTestHelper.TWENTY_SECONDS)
	public static void threePipeCombine(CreateGameTestHelper helper) {
		BlockPos tank1Pos = new BlockPos(5, 2, 1);
		BlockPos tank2Pos = tank1Pos.south();
		BlockPos tank3Pos = tank2Pos.south();
		long initialContents = helper.getFluidInTanks(tank1Pos, tank2Pos, tank3Pos);

		BlockPos pumpPos = new BlockPos(2, 2, 2);
		helper.flipBlock(pumpPos);
		helper.addInstantFinalTask(() -> {
			helper.assertSecondsPassed(13);
			// make sure fully drained
			helper.assertTanksEmpty(tank1Pos, tank2Pos, tank3Pos);
			// and fully moved
			BlockPos outputTankPos = new BlockPos(1, 2, 2);
			long moved = helper.getFluidInTanks(outputTankPos);
			if (moved != initialContents)
				helper.throwGameTestException("Wrong amount of fluid amount. expected [%s], got [%s]".formatted(initialContents, moved));
			// verify nothing was duped or deleted
		});
	}

	@GameTest(templateName = "3_pipe_split", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void threePipeSplit(CreateGameTestHelper helper) {
		BlockPos pumpPos = new BlockPos(2, 2, 2);
		BlockPos tank1Pos = new BlockPos(5, 2, 1);
		BlockPos tank2Pos = tank1Pos.south();
		BlockPos tank3Pos = tank2Pos.south();
		BlockPos outputTankPos = new BlockPos(1, 2, 2);

		long totalContents = helper.getFluidInTanks(tank1Pos, tank2Pos, tank3Pos, outputTankPos);
		helper.flipBlock(pumpPos);

		helper.addInstantFinalTask(() -> {
			helper.assertSecondsPassed(7);
			FluidStack contents = helper.getTankContents(outputTankPos);
			if (!contents.isEmpty()) {
				helper.throwGameTestException("Tank not empty: " + contents.getAmount());
			}
			long newTotalContents = helper.getFluidInTanks(tank1Pos, tank2Pos, tank3Pos);
			if (newTotalContents != totalContents) {
				helper.throwGameTestException("Wrong total fluid amount. expected [%s], got [%s]".formatted(totalContents, newTotalContents));
			}
		});
	}

	@GameTest(templateName = "large_waterwheel", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void largeWaterwheel(CreateGameTestHelper helper) {
		BlockPos wheel = new BlockPos(4, 3, 2);
		BlockPos leftEnd = new BlockPos(6, 2, 2);
		BlockPos rightEnd = new BlockPos(2, 2, 2);
		List<BlockPos> edges = List.of(new BlockPos(4, 5, 1), new BlockPos(4, 5, 3));
		BlockPos openLever = new BlockPos(3, 8, 1);
		BlockPos leftLever = new BlockPos(5, 7, 1);
		waterwheel(helper, wheel, 4, 512, leftEnd, rightEnd, edges, openLever, leftLever);
	}

	@GameTest(templateName = "small_waterwheel", tickLimit = CreateGameTestHelper.TEN_SECONDS)
	public static void smallWaterwheel(CreateGameTestHelper helper) {
		BlockPos wheel = new BlockPos(3, 2, 2);
		BlockPos leftEnd = new BlockPos(4, 2, 2);
		BlockPos rightEnd = new BlockPos(2, 2, 2);
		List<BlockPos> edges = List.of(new BlockPos(3, 3, 1), new BlockPos(3, 3, 3));
		BlockPos openLever = new BlockPos(2, 6, 1);
		BlockPos leftLever = new BlockPos(4, 5, 1);
		waterwheel(helper, wheel, 8, 256, leftEnd, rightEnd, edges, openLever, leftLever);
	}

	private static void waterwheel(CreateGameTestHelper helper,
								   BlockPos wheel, float expectedRpm, float expectedSU,
								   BlockPos leftEnd, BlockPos rightEnd, List<BlockPos> edges,
								   BlockPos openLever, BlockPos leftLever) {
		BlockPos speedometer = wheel.north();
		BlockPos stressometer = wheel.south();
		helper.toggleLever(openLever);
		helper.addInstantFinalTask(() -> {
			// must always be true
			edges.forEach(pos -> helper.dontExpectBlock(Blocks.WATER, pos));
			helper.expectBlock(Blocks.WATER, rightEnd);
			// first step: expect water on left end while flow is allowed
			if (!helper.getBlockState(leftLever).get(LeverBlock.POWERED)) {
				helper.expectBlock(Blocks.WATER, leftEnd);
				// water is present. both sides should cancel.
				helper.assertSpeedometerSpeed(speedometer, 0);
				helper.assertStressometerCapacity(stressometer, 0);
				// success, pull the lever, enter step 2
				helper.powerLever(leftLever);
				helper.throwGameTestException("Entering step 2");
			} else {
				// lever is pulled, flow should stop
				helper.dontExpectBlock(Blocks.WATER, leftEnd);
				// 1-sided flow, should be spinning
				helper.assertSpeedometerSpeed(speedometer, expectedRpm);
				helper.assertStressometerCapacity(stressometer, expectedSU);
			}
		});
	}

	@GameTest(templateName = "waterwheel_materials", tickLimit = CreateGameTestHelper.FIFTEEN_SECONDS)
	public static void waterwheelMaterials(CreateGameTestHelper helper) {
		List<Item> planks = Registries.BLOCK.getOrCreateEntryList(BlockTags.PLANKS).stream()
				.map(RegistryEntry::value).map(ItemConvertible::asItem).collect(Collectors.toCollection(ArrayList::new));
		List<BlockPos> chests = List.of(new BlockPos(6, 4, 2), new BlockPos(6, 4, 3));
		List<BlockPos> deployers = chests.stream().map(pos -> pos.down(2)).toList();
		helper.waitAndRun(3, () -> chests.forEach(chest ->
				planks.forEach(plank -> TransferUtil.insertItem(helper.itemStorageAt(chest), new ItemStack(plank)))
		));

		BlockPos smallWheel = new BlockPos(4, 2, 2);
		BlockPos largeWheel = new BlockPos(3, 3, 3);
		BlockPos lever = new BlockPos(5, 3, 1);
		helper.toggleLever(lever);

		helper.addInstantFinalTask(() -> {
			Item plank = planks.get(0);
			if (!(plank instanceof BlockItem blockItem))
				throw new GameTestException(Registries.ITEM.getId(plank) + " is not a BlockItem");
			Block block = blockItem.getBlock();

			WaterWheelBlockEntity smallWheelBe = helper.getBlockEntity(AllBlockEntityTypes.WATER_WHEEL.get(), smallWheel);
			if (!smallWheelBe.material.isOf(block))
				helper.throwGameTestException("Small waterwheel has not consumed " + Registries.ITEM.getId(plank));

			WaterWheelBlockEntity largeWheelBe = helper.getBlockEntity(AllBlockEntityTypes.LARGE_WATER_WHEEL.get(), largeWheel);
			if (!largeWheelBe.material.isOf(block))
				helper.throwGameTestException("Large waterwheel has not consumed " + Registries.ITEM.getId(plank));

			// next item
			planks.remove(0);
			deployers.forEach(pos -> TransferUtil.clearStorage(helper.itemStorageAt(pos)));
			if (!planks.isEmpty())
				helper.throwGameTestException("Not all planks have been consumed");
		});
	}

	@GameTest(templateName = "smart_observer_pipes")
	public static void smartObserverPipes(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(3, 3, 1);
		BlockPos output = new BlockPos(3, 4, 4);
		BlockPos tankOutput = new BlockPos(1, 2, 4);
		FluidStack expected = new FluidStack(Fluids.WATER, 2 * FluidConstants.BUCKET);
		helper.toggleLever(lever);
		helper.addInstantFinalTask(() -> {
			helper.assertFluidPresent(expected, tankOutput);
			helper.expectBlock(Blocks.DIAMOND_BLOCK, output);
		});
	}

	@GameTest(templateName = "threshold_switch", tickLimit = CreateGameTestHelper.TWENTY_SECONDS)
	public static void thresholdSwitch(CreateGameTestHelper helper) {
		BlockPos leftHandle = new BlockPos(4, 2, 4);
		BlockPos leftValve = new BlockPos(4, 2, 3);
		BlockPos leftTank = new BlockPos(5, 2, 3);

		BlockPos rightHandle = new BlockPos(2, 2, 4);
		BlockPos rightValve = new BlockPos(2, 2, 3);
		BlockPos rightTank = new BlockPos(1, 2, 3);

		BlockPos drainHandle = new BlockPos(3, 3, 2);
		BlockPos drainValve = new BlockPos(3, 3, 1);
		BlockPos lamp = new BlockPos(1, 3, 1);
		BlockPos tank = new BlockPos(2, 2, 1);
		helper.addInstantFinalTask(() -> {
			if (!helper.getBlockState(leftValve).get(FluidValveBlock.ENABLED)) { // step 1
				helper.getBlockEntity(AllBlockEntityTypes.VALVE_HANDLE.get(), leftHandle)
						.activate(false); // open the valve, fill 4 buckets
				helper.throwGameTestException("Entering step 2");
			} else if (!helper.getBlockState(rightValve).get(FluidValveBlock.ENABLED)) { // step 2
				helper.assertFluidPresent(FluidStack.EMPTY, leftTank); // wait for left tank to drain
				helper.expectBlockProperty(lamp, RedstoneLampBlock.LIT, false); // should not be on yet
				helper.getBlockEntity(AllBlockEntityTypes.VALVE_HANDLE.get(), rightHandle)
						.activate(false); // fill another 4 buckets
				helper.throwGameTestException("Entering step 3");
			} else if (!helper.getBlockState(drainValve).get(FluidValveBlock.ENABLED)) { // step 3
				helper.assertFluidPresent(FluidStack.EMPTY, rightTank); // wait for right tank to drain
				// 16 buckets inserted. tank full, lamp on.
				helper.expectBlockProperty(lamp, RedstoneLampBlock.LIT, true);
				// drain what's filled so far
				helper.getBlockEntity(AllBlockEntityTypes.VALVE_HANDLE.get(), drainHandle)
						.activate(false); // drain all 8 buckets
				helper.throwGameTestException("Entering step 4");
			} else {
				helper.assertTankEmpty(tank); // wait for it to empty
				helper.expectBlockProperty(lamp, RedstoneLampBlock.LIT, false); // should be off now
			}
		});
	}
}
