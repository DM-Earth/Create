package com.simibubi.create.content.equipment.clipboard;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class ClipboardBlockEntity extends SmartBlockEntity {

	public ItemStack dataContainer;
	private UUID lastEdit;

	public ClipboardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		dataContainer = AllBlocks.CLIPBOARD.asStack();
	}

	@Override
	public void initialize() {
		super.initialize();
		updateWrittenState();
	}

	public void onEditedBy(PlayerEntity player) {
		lastEdit = player.getUuid();
		notifyUpdate();
		updateWrittenState();
	}

	public void updateWrittenState() {
		BlockState blockState = getCachedState();
		if (!AllBlocks.CLIPBOARD.has(blockState))
			return;
		if (world.isClient())
			return;
		boolean isWritten = blockState.get(ClipboardBlock.WRITTEN);
		boolean shouldBeWritten = dataContainer.getNbt() != null;
		if (isWritten == shouldBeWritten)
			return;
		world.setBlockState(pos, blockState.with(ClipboardBlock.WRITTEN, shouldBeWritten));
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Item", NBTSerializer.serializeNBT(dataContainer));
		if (clientPacket && lastEdit != null)
			tag.putUuid("LastEdit", lastEdit);
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		dataContainer = ItemStack.fromNbt(tag.getCompound("Item"));

		if (clientPacket)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> readClientSide(tag));
	}

	@Environment(EnvType.CLIENT)
	private void readClientSide(NbtCompound tag) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!(mc.currentScreen instanceof ClipboardScreen cs))
			return;
		if (tag.contains("LastEdit") && tag.getUuid("LastEdit")
			.equals(mc.player.getUuid()))
			return;
		if (!pos.equals(cs.targetedBlock))
			return;
		cs.reopenWith(dataContainer);
	}

}
