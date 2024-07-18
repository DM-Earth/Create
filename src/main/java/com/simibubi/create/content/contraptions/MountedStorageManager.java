package com.simibubi.create.content.contraptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.simibubi.create.content.contraptions.Contraption.ContraptionInvWrapper;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MountedStorageManager {

	protected ContraptionInvWrapper inventory;
	protected ContraptionInvWrapper fuelInventory;
	protected CombinedTankWrapper fluidInventory;
	protected Map<BlockPos, MountedStorage> storage;
	protected Map<BlockPos, MountedFluidStorage> fluidStorage;

	public MountedStorageManager() {
		storage = new HashMap<>();
		fluidStorage = new HashMap<>();
	}

	public void entityTick(AbstractContraptionEntity entity) {
		fluidStorage.forEach((pos, mfs) -> mfs.tick(entity, pos, entity.getWorld().isClient));
	}

	public void createHandlers() {
		Collection<MountedStorage> itemHandlers = storage.values();

		inventory = wrapItems(itemHandlers.stream()
			.filter(MountedStorage::isValid)
			.map(MountedStorage::getItemHandler)
			.toList(), false);

		fuelInventory = wrapItems(itemHandlers.stream()
			.filter(MountedStorage::canUseForFuel)
			.map(MountedStorage::getItemHandler)
			.toList(), true);

		fluidInventory = wrapFluids(fluidStorage.values()
			.stream()
			.map(MountedFluidStorage::getFluidHandler)
			.collect(Collectors.toList()));
	}

	protected ContraptionInvWrapper wrapItems(Collection<? extends Storage<ItemVariant>> list, boolean fuel) {
		return new ContraptionInvWrapper(Arrays.copyOf(list.toArray(), list.size(), Storage[].class));
	}

	protected CombinedTankWrapper wrapFluids(Collection<? extends Storage<FluidVariant>> list) {
		return new CombinedTankWrapper(Arrays.copyOf(list.toArray(), list.size(), Storage[].class));
	}

	public void addBlock(BlockPos localPos, BlockEntity be) {
		if (be != null && MountedStorage.canUseAsStorage(be))
			storage.put(localPos, new MountedStorage(be));
		if (be != null && MountedFluidStorage.canUseAsStorage(be))
			fluidStorage.put(localPos, new MountedFluidStorage(be));
	}

	public void read(NbtCompound nbt, Map<BlockPos, BlockEntity> presentBlockEntities, boolean clientPacket) {
		storage.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Storage", NbtElement.COMPOUND_TYPE), c -> storage
			.put(NbtHelper.toBlockPos(c.getCompound("Pos")), MountedStorage.deserialize(c.getCompound("Data"))));

		fluidStorage.clear();
		NBTHelper.iterateCompoundList(nbt.getList("FluidStorage", NbtElement.COMPOUND_TYPE), c -> fluidStorage
			.put(NbtHelper.toBlockPos(c.getCompound("Pos")), MountedFluidStorage.deserialize(c.getCompound("Data"))));

		if (clientPacket && presentBlockEntities != null)
			bindTanks(presentBlockEntities);

		List<Storage<ItemVariant>> handlers = new ArrayList<>();
		List<Storage<ItemVariant>> fuelHandlers = new ArrayList<>();
		for (MountedStorage mountedStorage : storage.values()) {
			Storage<ItemVariant> itemHandler = mountedStorage.getItemHandler();
			handlers.add(itemHandler);
			if (mountedStorage.canUseForFuel())
				fuelHandlers.add(itemHandler);
		}

		inventory = wrapItems(handlers, false);
		fuelInventory = wrapItems(fuelHandlers, true);
		fluidInventory = wrapFluids(fluidStorage.values()
			.stream()
			.map(MountedFluidStorage::getFluidHandler)
			.map(tank -> (Storage<FluidVariant>) tank)
			.toList());
	}

	public void bindTanks(Map<BlockPos, BlockEntity> presentBlockEntities) {
		fluidStorage.forEach((pos, mfs) -> {
			BlockEntity blockEntity = presentBlockEntities.get(pos);
			if (!(blockEntity instanceof FluidTankBlockEntity))
				return;
			FluidTankBlockEntity tank = (FluidTankBlockEntity) blockEntity;
			FluidTank tankInventory = tank.getTankInventory();
			if (tankInventory instanceof FluidTank)
				((FluidTank) tankInventory).setFluid(mfs.tank.getFluid());
			tank.getFluidLevel()
				.startWithValue(tank.getFillState());
			mfs.assignBlockEntity(tank);
		});
	}

	public void write(NbtCompound nbt, boolean clientPacket) {
		NbtList storageNBT = new NbtList();
		if (!clientPacket)
			for (BlockPos pos : storage.keySet()) {
				NbtCompound c = new NbtCompound();
				MountedStorage mountedStorage = storage.get(pos);
				if (!mountedStorage.isValid())
					continue;
				c.put("Pos", NbtHelper.fromBlockPos(pos));
				c.put("Data", mountedStorage.serialize());
				storageNBT.add(c);
			}

		NbtList fluidStorageNBT = new NbtList();
		for (BlockPos pos : fluidStorage.keySet()) {
			NbtCompound c = new NbtCompound();
			MountedFluidStorage mountedStorage = fluidStorage.get(pos);
			if (!mountedStorage.isValid())
				continue;
			c.put("Pos", NbtHelper.fromBlockPos(pos));
			c.put("Data", mountedStorage.serialize());
			fluidStorageNBT.add(c);
		}

		nbt.put("Storage", storageNBT);
		nbt.put("FluidStorage", fluidStorageNBT);
	}

	public void removeStorageFromWorld() {
		storage.values()
			.forEach(MountedStorage::removeStorageFromWorld);
		fluidStorage.values()
			.forEach(MountedFluidStorage::removeStorageFromWorld);
	}

	public void addStorageToWorld(StructureBlockInfo block, BlockEntity blockEntity) {
		if (storage.containsKey(block.pos())) {
			MountedStorage mountedStorage = storage.get(block.pos());
			if (mountedStorage.isValid())
				mountedStorage.addStorageToWorld(blockEntity);
		}

		if (fluidStorage.containsKey(block.pos())) {
			MountedFluidStorage mountedStorage = fluidStorage.get(block.pos());
			if (mountedStorage.isValid())
				mountedStorage.addStorageToWorld(blockEntity);
		}
	}

	public void clear() {
		for (Storage<ItemVariant> storage : inventory.parts) {
			if (!(storage instanceof ContraptionInvWrapper wrapper) || !wrapper.isExternal) {
				TransferUtil.clearStorage(storage);
			}
		}
		TransferUtil.clearStorage(fluidInventory);
	}

	public void updateContainedFluid(BlockPos localPos, FluidStack containedFluid) {
		MountedFluidStorage mountedFluidStorage = fluidStorage.get(localPos);
		if (mountedFluidStorage != null)
			mountedFluidStorage.updateFluid(containedFluid);
	}

	public void attachExternal(Storage<ItemVariant> externalStorage) {
		inventory = new ContraptionInvWrapper(externalStorage, inventory);
		fuelInventory = new ContraptionInvWrapper(externalStorage, fuelInventory);
	}

	public ContraptionInvWrapper getItems() {
		return inventory;
	}

	public ContraptionInvWrapper getFuelItems() {
		return fuelInventory;
	}

	public CombinedTankWrapper getFluids() {
		return fluidInventory;
	}

	public boolean handlePlayerStorageInteraction(Contraption contraption, PlayerEntity player, BlockPos localPos) {
		if (player.getWorld().isClient()) {
			BlockEntity localBE = contraption.presentBlockEntities.get(localPos);
			return MountedStorage.canUseAsStorage(localBE);
		}

		MountedStorageManager storageManager = contraption.getStorageForSpawnPacket();
		MountedStorage storage = storageManager.storage.get(localPos);
		if (storage == null || storage.getItemHandler() == null)
			return false;
		ItemStackHandler primary = storage.getItemHandler();
		ItemStackHandler secondary = null;

		StructureBlockInfo info = contraption.getBlocks()
			.get(localPos);
		if (info != null && info.state().contains(ChestBlock.CHEST_TYPE)) {
			ChestType chestType = info.state().get(ChestBlock.CHEST_TYPE);
			Direction facing = info.state().getOrEmpty(ChestBlock.FACING)
				.orElse(Direction.SOUTH);
			Direction connectedDirection =
				chestType == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();

			if (chestType != ChestType.SINGLE) {
				MountedStorage storage2 = storageManager.storage.get(localPos.offset(connectedDirection));
				if (storage2 != null && storage2.getItemHandler() != null) {
					secondary = storage2.getItemHandler();
					if (chestType == ChestType.LEFT) {
						// switcheroo
						ItemStackHandler temp = primary;
						primary = secondary;
						secondary = temp;
					}
				}
			}
		}

		int slotCount = primary.getSlotCount() + (secondary == null ? 0 : secondary.getSlotCount());
		if (slotCount == 0)
			return false;
		if (slotCount % 9 != 0)
			return false;

		Supplier<Boolean> stillValid = () -> contraption.entity.isAlive()
			&& player.squaredDistanceTo(contraption.entity.toGlobalVector(Vec3d.ofCenter(localPos), 0)) < 64;
		Text name = info != null ? info.state().getBlock()
			.getName() : Components.literal("Container");
		player.openHandledScreen(MountedStorageInteraction.createMenuProvider(name, primary, secondary, slotCount, stillValid));

		Vec3d soundPos = contraption.entity.toGlobalVector(Vec3d.ofCenter(localPos), 0);
		player.getWorld().playSound(null, BlockPos.ofFloored(soundPos), SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.75f, 1f);
		return true;
	}

}
