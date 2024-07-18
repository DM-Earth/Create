package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class FluidSplashPacket extends SimplePacketBase {

	private BlockPos pos;
	private FluidStack fluid;

	public FluidSplashPacket(BlockPos pos, FluidStack fluid) {
		this.pos = pos;
		this.fluid = fluid;
	}

	public FluidSplashPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
		fluid =FluidStack.readFromPacket(buffer);
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		fluid.writeToPacket(buffer);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			if (MinecraftClient.getInstance().player.getPos()
				.distanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > 100)
				return;
			FluidFX.splash(pos, fluid);
		}));
		return true;
	}

}
