package com.simibubi.create.content.kinetics.fan.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class FanProcessing {
	public static boolean canProcess(ItemEntity entity, FanProcessingType type) {
		if (entity.getCustomData()
			.contains("CreateData")) {
			NbtCompound compound = entity.getCustomData()
				.getCompound("CreateData");
			if (compound.contains("Processing")) {
				NbtCompound processing = compound.getCompound("Processing");

				if (AllFanProcessingTypes.parseLegacy(processing.getString("Type")) != type)
					return type.canProcess(entity.getStack(), entity.getWorld());
				else if (processing.getInt("Time") >= 0)
					return true;
				else if (processing.getInt("Time") == -1)
					return false;
			}
		}
		return type.canProcess(entity.getStack(), entity.getWorld());
	}

	public static boolean applyProcessing(ItemEntity entity, FanProcessingType type) {
		if (decrementProcessingTime(entity, type) != 0)
			return false;
		List<ItemStack> stacks = type.process(entity.getStack(), entity.getWorld());
		if (stacks == null)
			return false;
		if (stacks.isEmpty()) {
			entity.discard();
			return false;
		}
		entity.setStack(stacks.remove(0));
		for (ItemStack additional : stacks) {
			ItemEntity entityIn = new ItemEntity(entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), additional);
			entityIn.setVelocity(entity.getVelocity());
			entity.getWorld().spawnEntity(entityIn);
		}
		return true;
	}

	public static TransportedResult applyProcessing(TransportedItemStack transported, World world, FanProcessingType type) {
		TransportedResult ignore = TransportedResult.doNothing();
		if (transported.processedBy != type) {
			transported.processedBy = type;
			int timeModifierForStackSize = ((transported.stack.getCount() - 1) / 16) + 1;
			int processingTime =
				(int) (AllConfigs.server().kinetics.fanProcessingTime.get() * timeModifierForStackSize) + 1;
			transported.processingTime = processingTime;
			if (!type.canProcess(transported.stack, world))
				transported.processingTime = -1;
			return ignore;
		}
		if (transported.processingTime == -1)
			return ignore;
		if (transported.processingTime-- > 0)
			return ignore;

		List<ItemStack> stacks = type.process(transported.stack, world);
		if (stacks == null)
			return ignore;

		List<TransportedItemStack> transportedStacks = new ArrayList<>();
		for (ItemStack additional : stacks) {
			TransportedItemStack newTransported = transported.getSimilar();
			newTransported.stack = additional.copy();
			transportedStacks.add(newTransported);
		}
		return TransportedResult.convertTo(transportedStacks);
	}

	private static int decrementProcessingTime(ItemEntity entity, FanProcessingType type) {
		NbtCompound nbt = entity.getCustomData();

		if (!nbt.contains("CreateData"))
			nbt.put("CreateData", new NbtCompound());
		NbtCompound createData = nbt.getCompound("CreateData");

		if (!createData.contains("Processing"))
			createData.put("Processing", new NbtCompound());
		NbtCompound processing = createData.getCompound("Processing");

		if (!processing.contains("Type") || AllFanProcessingTypes.parseLegacy(processing.getString("Type")) != type) {
			processing.putString("Type", FanProcessingTypeRegistry.getIdOrThrow(type).toString());
			int timeModifierForStackSize = ((entity.getStack()
				.getCount() - 1) / 16) + 1;
			int processingTime =
				(int) (AllConfigs.server().kinetics.fanProcessingTime.get() * timeModifierForStackSize) + 1;
			processing.putInt("Time", processingTime);
		}

		int value = processing.getInt("Time") - 1;
		processing.putInt("Time", value);
		return value;
	}
}
