package com.simibubi.create.content.schematics.packet;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class SchematicPlacePacket extends SimplePacketBase {

	public ItemStack stack;

	public SchematicPlacePacket(ItemStack stack) {
		this.stack = stack;
	}

	public SchematicPlacePacket(PacketByteBuf buffer) {
		stack = buffer.readItemStack();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeItemStack(stack);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;
			if (!player.isCreative())
				return;

			World world = player.getWorld();
			SchematicPrinter printer = new SchematicPrinter();
			printer.loadSchematic(stack, world, !player.isCreativeLevelTwoOp());
			if (!printer.isLoaded() || printer.isErrored())
				return;

			boolean includeAir = AllConfigs.server().schematics.creativePrintIncludesAir.get();

			while (printer.advanceCurrentPos()) {
				if (!printer.shouldPlaceCurrent(world))
					continue;

				printer.handleCurrentTarget((pos, state, blockEntity) -> {
					boolean placingAir = state.isAir();
					if (placingAir && !includeAir)
						return;

					NbtCompound data = BlockHelper.prepareBlockEntityData(state, blockEntity);
					BlockHelper.placeSchematicBlock(world, state, pos, null, data);
				}, (pos, entity) -> {
					world.spawnEntity(entity);
				});
			}
		});
		return true;
	}

}
