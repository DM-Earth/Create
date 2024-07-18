package com.simibubi.create.content.logistics.tunnel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;

public class TunnelFlapPacket extends BlockEntityDataPacket<BeltTunnelBlockEntity> {

    private List<Pair<Direction, Boolean>> flaps;

    public TunnelFlapPacket(PacketByteBuf buffer) {
        super(buffer);

        byte size = buffer.readByte();

        this.flaps = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Direction direction = Direction.byId(buffer.readByte());
            boolean inwards = buffer.readBoolean();

            flaps.add(Pair.of(direction, inwards));
        }
    }

    public TunnelFlapPacket(BeltTunnelBlockEntity blockEntity, List<Pair<Direction, Boolean>> flaps) {
        super(blockEntity.getPos());

        this.flaps = new ArrayList<>(flaps);
    }

    @Override
    protected void writeData(PacketByteBuf buffer) {
        buffer.writeByte(flaps.size());

        for (Pair<Direction, Boolean> flap : flaps) {
            buffer.writeByte(flap.getLeft().getId());
            buffer.writeBoolean(flap.getRight());
        }
    }

    @Override
    protected void handlePacket(BeltTunnelBlockEntity blockEntity) {
        for (Pair<Direction, Boolean> flap : flaps) {
            blockEntity.flap(flap.getLeft(), flap.getRight());
        }
    }
}
