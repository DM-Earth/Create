package com.simibubi.create.content.contraptions.minecart.capability;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

public class MinecartControllerUpdatePacket extends SimplePacketBase {

	int entityID;
	NbtCompound nbt;

	public MinecartControllerUpdatePacket(MinecartController controller) {
		entityID = controller.cart()
			.getId();
		nbt = controller.serializeNBT();
	}

	public MinecartControllerUpdatePacket(PacketByteBuf buffer) {
		entityID = buffer.readInt();
		nbt = buffer.readNbt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
 		buffer.writeInt(entityID);
		buffer.writeNbt(nbt);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::handleCL));
		return true;
	}

	@Environment(EnvType.CLIENT)
	private void handleCL() {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null)
			return;
		Entity entityByID = world.getEntityById(entityID);
		if (entityByID == null)
			return;
		((AbstractMinecartEntity) entityByID).create$getController().deserializeNBT(nbt);
	}

}
