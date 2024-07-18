package com.simibubi.create.content.contraptions.sync;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class ContraptionFluidPacket extends SimplePacketBase {

	private int entityId;
	private BlockPos localPos;
	private FluidStack containedFluid;

	public ContraptionFluidPacket(int entityId, BlockPos localPos, FluidStack containedFluid) {
		this.entityId = entityId;
		this.localPos = localPos;
		this.containedFluid = containedFluid;
	}

	public ContraptionFluidPacket(PacketByteBuf buffer) {
		entityId = buffer.readInt();
		localPos = buffer.readBlockPos();
		containedFluid = FluidStack.readFromPacket(buffer);
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeBlockPos(localPos);
		containedFluid.writeToPacket(buffer);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Entity entityByID = MinecraftClient.getInstance().world.getEntityById(entityId);
			if (!(entityByID instanceof AbstractContraptionEntity))
				return;
			AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) entityByID;
			contraptionEntity.getContraption().handleContraptionFluidPacket(localPos, containedFluid);
		});
		return true;
	}
}
