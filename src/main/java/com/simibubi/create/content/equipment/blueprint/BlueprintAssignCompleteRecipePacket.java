package com.simibubi.create.content.equipment.blueprint;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class BlueprintAssignCompleteRecipePacket extends SimplePacketBase {

	private Identifier recipeID;

	public BlueprintAssignCompleteRecipePacket(Identifier recipeID) {
		this.recipeID = recipeID;
	}

	public BlueprintAssignCompleteRecipePacket(PacketByteBuf buffer) {
		recipeID = buffer.readIdentifier();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeIdentifier(recipeID);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;
			if (player.currentScreenHandler instanceof BlueprintMenu) {
				BlueprintMenu c = (BlueprintMenu) player.currentScreenHandler;
				player.getWorld()
						.getRecipeManager()
						.get(recipeID)
						.ifPresent(r -> BlueprintItem.assignCompleteRecipe(c.player.getWorld(), c.ghostInventory, r));
			}
		});
		return true;
	}

}
