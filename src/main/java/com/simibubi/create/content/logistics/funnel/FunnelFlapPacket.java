package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import net.minecraft.network.PacketByteBuf;

public class FunnelFlapPacket extends BlockEntityDataPacket<FunnelBlockEntity> {

    private final boolean inwards;

    public FunnelFlapPacket(PacketByteBuf buffer) {
        super(buffer);

        inwards = buffer.readBoolean();
    }

    public FunnelFlapPacket(FunnelBlockEntity blockEntity, boolean inwards) {
        super(blockEntity.getPos());
        this.inwards = inwards;
    }

    @Override
    protected void writeData(PacketByteBuf buffer) {
        buffer.writeBoolean(inwards);
    }

    @Override
    protected void handlePacket(FunnelBlockEntity blockEntity) {
        blockEntity.flap(inwards);
    }
}
