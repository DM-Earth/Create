package com.simibubi.create.content.schematics.table;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import com.simibubi.create.foundation.utility.Lang;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;


public class SchematicTableBlockEntity extends SmartBlockEntity implements NamedScreenHandlerFactory, IInteractionChecker {

	public SchematicTableInventory inventory;
	public boolean isUploading;
	public String uploadingSchematic;
	public float uploadingProgress;
	public boolean sendUpdate;

	public class SchematicTableInventory extends ItemStackHandler {
		public SchematicTableInventory() {
			super(2);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			markDirty();
		}
	}

	public SchematicTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new SchematicTableInventory();
		uploadingSchematic = null;
		uploadingProgress = 0;
	}

	public void sendToMenu(PacketByteBuf buffer) {
		buffer.writeBlockPos(getPos());
		buffer.writeNbt(toInitialChunkDataNbt());
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		inventory.deserializeNBT(compound.getCompound("Inventory"));
		super.read(compound, clientPacket);
		if (!clientPacket)
			return;
		if (compound.contains("Uploading")) {
			isUploading = true;
			uploadingSchematic = compound.getString("Schematic");
			uploadingProgress = compound.getFloat("Progress");
		} else {
			isUploading = false;
			uploadingSchematic = null;
			uploadingProgress = 0;
		}
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		compound.put("Inventory", inventory.serializeNBT());
		super.write(compound, clientPacket);

		if (clientPacket && isUploading) {
			compound.putBoolean("Uploading", true);
			compound.putString("Schematic", uploadingSchematic);
			compound.putFloat("Progress", uploadingProgress);
		}
	}

	@Override
	public void tick() {
		// Update Client block entity
		if (sendUpdate) {
			sendUpdate = false;
			world.updateListeners(pos, getCachedState(), getCachedState(), 6);
		}
	}

	public void startUpload(String schematic) {
		isUploading = true;
		uploadingProgress = 0;
		uploadingSchematic = schematic;
		sendUpdate = true;
		inventory.setStackInSlot(0, ItemStack.EMPTY);
	}

	public void finishUpload() {
		isUploading = false;
		uploadingProgress = 0;
		uploadingSchematic = null;
		sendUpdate = true;
	}

	@Override
	public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return SchematicTableMenu.create(id, inv, this);
	}

	@Override
	public Text getDisplayName() {
		return Lang.translateDirect("gui.schematicTable.title");
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		if (world == null || world.getBlockEntity(pos) != this) {
			return false;
		}
		return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D,
			pos.getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

}
