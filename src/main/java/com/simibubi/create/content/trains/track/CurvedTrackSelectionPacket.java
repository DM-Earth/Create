package com.simibubi.create.content.trains.track;

import org.apache.commons.lang3.mutable.MutableObject;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem.OverlapResult;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class CurvedTrackSelectionPacket extends BlockEntityConfigurationPacket<TrackBlockEntity> {

	private BlockPos targetPos;
	private boolean front;
	private int segment;
	private int slot;

	public CurvedTrackSelectionPacket(BlockPos pos, BlockPos targetPos, int segment, boolean front, int slot) {
		super(pos);
		this.targetPos = targetPos;
		this.segment = segment;
		this.front = front;
		this.slot = slot;
	}

	public CurvedTrackSelectionPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeBlockPos(targetPos);
		buffer.writeVarInt(segment);
		buffer.writeBoolean(front);
		buffer.writeVarInt(slot);
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		targetPos = buffer.readBlockPos();
		segment = buffer.readVarInt();
		front = buffer.readBoolean();
		slot = buffer.readVarInt();
	}

	@Override
	protected void applySettings(ServerPlayerEntity player, TrackBlockEntity be) {
		if (player.getInventory().selectedSlot != slot)
			return;
		ItemStack stack = player.getInventory()
			.getStack(slot);
		if (!(stack.getItem() instanceof TrackTargetingBlockItem))
			return;
		if (player.isSneaking() && stack.hasNbt()) {
			player.sendMessage(Lang.translateDirect("track_target.clear"), true);
			stack.setNbt(null);
			AllSoundEvents.CONTROLLER_CLICK.play(player.getWorld(), null, pos, 1, .5f);
			return;
		}

		EdgePointType<?> type = AllBlocks.TRACK_SIGNAL.isIn(stack) ? EdgePointType.SIGNAL : EdgePointType.STATION;
		MutableObject<OverlapResult> result = new MutableObject<>(null);
		TrackTargetingBlockItem.withGraphLocation(player.getWorld(), pos, front,
			new BezierTrackPointLocation(targetPos, segment), type, (overlap, location) -> result.setValue(overlap));

		if (result.getValue().feedback != null) {
			player.sendMessage(Lang.translateDirect(result.getValue().feedback)
				.formatted(Formatting.RED), true);
			AllSoundEvents.DENY.play(player.getWorld(), null, pos, .5f, 1);
			return;
		}

		NbtCompound stackTag = stack.getOrCreateNbt();
		stackTag.put("SelectedPos", NbtHelper.fromBlockPos(pos));
		stackTag.putBoolean("SelectedDirection", front);

		NbtCompound bezierNbt = new NbtCompound();
		bezierNbt.putInt("Segment", segment);
		bezierNbt.put("Key", NbtHelper.fromBlockPos(targetPos));
		bezierNbt.putBoolean("FromStack", true);
		stackTag.put("Bezier", bezierNbt);

		player.sendMessage(Lang.translateDirect("track_target.set"), true);
		stack.setNbt(stackTag);
		AllSoundEvents.CONTROLLER_CLICK.play(player.getWorld(), null, pos, 1, 1);
	}

	@Override
	protected int maxRange() {
		return 64;
	}

	@Override
	protected void applySettings(TrackBlockEntity be) {}

}
