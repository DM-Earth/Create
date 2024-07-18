package com.simibubi.create.content.logistics.vault;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class ItemVaultBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory, SidedStorageBlockEntity {
	protected Storage<ItemVariant> itemCapability;

	protected ItemStackHandler inventory;
	protected BlockPos controller;
	protected BlockPos lastKnownPos;
	protected boolean updateConnectivity;
	protected int radius;
	protected int length;
	protected Axis axis;

	protected boolean recalculateComparatorsNextTick = false;

	public ItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

		inventory = new ItemStackHandler(AllConfigs.server().logistics.vaultCapacity.get()) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				recalculateComparatorsNextTick = true;
			}
		};

		itemCapability = null;
		radius = 1;
		length = 1;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	protected void updateConnectivity() {
		updateConnectivity = false;
		if (world.isClient())
			return;
		if (!isController())
			return;
		ConnectivityHandler.formMulti(this);
	}

	protected void updateComparators() {
		recalculateComparatorsNextTick = false;

		ItemVaultBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return;

		world.markDirty(controllerBE.pos);

		BlockPos pos = controllerBE.getPos();
		for (int y = 0; y < controllerBE.radius; y++) {
			for (int z = 0; z < (controllerBE.axis == Axis.X ? controllerBE.radius : controllerBE.length); z++) {
				for (int x = 0; x < (controllerBE.axis == Axis.Z ? controllerBE.radius : controllerBE.length); x++) {
					world.updateComparators(pos.add(x, y, z), getCachedState().getBlock());
				}
			}
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (lastKnownPos == null)
			lastKnownPos = getPos();
		else if (!lastKnownPos.equals(pos) && pos != null) {
			onPositionChanged();
			return;
		}

		if (updateConnectivity)
			updateConnectivity();

		if (recalculateComparatorsNextTick)
			updateComparators();
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public boolean isController() {
		return controller == null || pos.getX() == controller.getX()
			&& pos.getY() == controller.getY() && pos.getZ() == controller.getZ();
	}

	private void onPositionChanged() {
		removeController(true);
		lastKnownPos = pos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ItemVaultBlockEntity getControllerBE() {
		if (isController())
			return this;
		BlockEntity blockEntity = world.getBlockEntity(controller);
		if (blockEntity instanceof ItemVaultBlockEntity)
			return (ItemVaultBlockEntity) blockEntity;
		return null;
	}

	public void removeController(boolean keepContents) {
		if (world.isClient())
			return;
		updateConnectivity = true;
		controller = null;
		radius = 1;
		length = 1;

		BlockState state = getCachedState();
		if (ItemVaultBlock.isVault(state)) {
			state = state.with(ItemVaultBlock.LARGE, false);
			getWorld().setBlockState(pos, state, 22);
		}

		itemCapability = null;
		markDirty();
		sendData();
	}

	@Override
	public void setController(BlockPos controller) {
		if (world.isClient && !isVirtual())
			return;
		if (controller.equals(this.controller))
			return;
		this.controller = controller;
		itemCapability = null;
		markDirty();
		sendData();
	}

	@Override
	public BlockPos getController() {
		return isController() ? pos : controller;
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);

		BlockPos controllerBefore = controller;
		int prevSize = radius;
		int prevLength = length;

		updateConnectivity = compound.contains("Uninitialized");
		controller = null;
		lastKnownPos = null;

		if (compound.contains("LastKnownPos"))
			lastKnownPos = NbtHelper.toBlockPos(compound.getCompound("LastKnownPos"));
		if (compound.contains("Controller"))
			controller = NbtHelper.toBlockPos(compound.getCompound("Controller"));

		if (isController()) {
			radius = compound.getInt("Size");
			length = compound.getInt("Length");
		}

		if (!clientPacket) {
			inventory.deserializeNBT(compound.getCompound("Inventory"));
			return;
		}

		boolean changeOfController =
			controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
		if (hasWorld() && (changeOfController || prevSize != radius || prevLength != length))
			world.scheduleBlockRerenderIfNeeded(getPos(), Blocks.AIR.getDefaultState(), getCachedState());
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		if (updateConnectivity)
			compound.putBoolean("Uninitialized", true);
		if (lastKnownPos != null)
			compound.put("LastKnownPos", NbtHelper.fromBlockPos(lastKnownPos));
		if (!isController())
			compound.put("Controller", NbtHelper.fromBlockPos(controller));
		if (isController()) {
			compound.putInt("Size", radius);
			compound.putInt("Length", length);
		}

		super.write(compound, clientPacket);

		if (!clientPacket) {
			compound.putString("StorageType", "CombinedInv");
			compound.put("Inventory", inventory.serializeNBT());
		}
	}

	public ItemStackHandler getInventoryOfBlock() {
		return inventory;
	}

	public void applyInventoryToBlock(ItemStackHandler handler) {
		for (int i = 0; i < inventory.getSlotCount(); i++)
			inventory.setStackInSlot(i, i < handler.getSlotCount() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		initCapability();
		return itemCapability;
	}

	private void initCapability() {
		if (itemCapability != null)
			return;
		if (!isController()) {
			ItemVaultBlockEntity controllerBE = getControllerBE();
			if (controllerBE == null)
				return;
			controllerBE.initCapability();
			itemCapability = controllerBE.itemCapability;
			return;
		}

		boolean alongZ = ItemVaultBlock.getVaultBlockAxis(getCachedState()) == Axis.Z;
		ItemStackHandler[] invs = new ItemStackHandler[length * radius * radius];
		for (int yOffset = 0; yOffset < length; yOffset++) {
			for (int xOffset = 0; xOffset < radius; xOffset++) {
				for (int zOffset = 0; zOffset < radius; zOffset++) {
					BlockPos vaultPos = alongZ ? pos.add(xOffset, zOffset, yOffset)
						: pos.add(yOffset, xOffset, zOffset);
					ItemVaultBlockEntity vaultAt =
						ConnectivityHandler.partAt(AllBlockEntityTypes.ITEM_VAULT.get(), world, vaultPos);
					invs[yOffset * radius * radius + xOffset * radius + zOffset] =
						vaultAt != null ? vaultAt.inventory : new ItemStackHandler();
				}
			}
		}

		Storage<ItemVariant> combinedInvWrapper = new CombinedStorage<>(List.of(invs));
		combinedInvWrapper = new VersionedInventoryWrapper(combinedInvWrapper);
		itemCapability = combinedInvWrapper;
	}

	public static int getMaxLength(int radius) {
		return radius * 3;
	}

	@Override
	public void preventConnectivityUpdate() { updateConnectivity = false; }

	// fabric: see comment in FluidTankItem
	public void queueConnectivityUpdate() {
		updateConnectivity = true;
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = this.getCachedState();
		if (ItemVaultBlock.isVault(state)) { // safety
			world.setBlockState(getPos(), state.with(ItemVaultBlock.LARGE, radius > 2), 6);
		}
		itemCapability = null;
		markDirty();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() { return getMainAxisOf(this); }

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		if (longAxis == Direction.Axis.Y) return getMaxWidth();
		return getMaxLength(width);
	}

	@Override
	public int getMaxWidth() {
		return 3;
	}

	@Override
	public int getHeight() { return length; }

	@Override
	public int getWidth() { return radius; }

	@Override
	public void setHeight(int height) { this.length = height; }

	@Override
	public void setWidth(int width) { this.radius = width; }

	@Override
	public boolean hasInventory() { return true; }
}
