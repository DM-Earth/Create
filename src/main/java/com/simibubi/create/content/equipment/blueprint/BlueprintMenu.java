package com.simibubi.create.content.equipment.blueprint;

import java.util.Optional;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.content.equipment.blueprint.BlueprintEntity.BlueprintSection;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class BlueprintMenu extends GhostItemMenu<BlueprintSection> {

	public BlueprintMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public BlueprintMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, BlueprintSection section) {
		super(type, id, inv, section);
	}

	public static BlueprintMenu create(int id, PlayerInventory inv, BlueprintSection section) {
		return new BlueprintMenu(AllMenuTypes.CRAFTING_BLUEPRINT.get(), id, inv, section);
	}

	@Override
	protected boolean allowRepeats() {
		return true;
	}

	@Override
	protected void addSlots() {
		addPlayerSlots(8, 131);

		int x = 29;
		int y = 21;
		int index = 0;
		for (int row = 0; row < 3; ++row)
			for (int col = 0; col < 3; ++col)
				this.addSlot(new BlueprintCraftSlot(ghostInventory, index++, x + col * 18, y + row * 18));

		addSlot(new BlueprintCraftSlot(ghostInventory, index++, 123, 40));
		addSlot(new SlotItemHandler(ghostInventory, index++, 135, 57));
	}

	public void onCraftMatrixChanged() {
		World level = contentHolder.getBlueprintWorld();
		if (level.isClient)
			return;

		ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) player;
		RecipeInputInventory craftingInventory = new BlueprintCraftingInventory(this, ghostInventory);
		Optional<CraftingRecipe> optional = player.getServer()
				.getRecipeManager()
				.getFirstMatch(RecipeType.CRAFTING, craftingInventory, player.getEntityWorld());

		if (!optional.isPresent()) {
			if (ghostInventory.getStackInSlot(9)
					.isEmpty())
				return;
			if (!contentHolder.inferredIcon)
				return;

			ghostInventory.setStackInSlot(9, ItemStack.EMPTY);
			serverplayerentity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), 36 + 9, ItemStack.EMPTY));
			contentHolder.inferredIcon = false;
			return;
		}

		CraftingRecipe icraftingrecipe = optional.get();
		ItemStack itemstack = icraftingrecipe.craft(craftingInventory, level.getRegistryManager());
		ghostInventory.setStackInSlot(9, itemstack);
		contentHolder.inferredIcon = true;
		ItemStack toSend = itemstack.copy();
		toSend.getOrCreateNbt()
				.putBoolean("InferredFromRecipe", true);
		serverplayerentity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), 36 + 9, toSend));
	}

	@Override
	public void setStackInSlot(int slotId, int stateId, ItemStack stack) {
		if (slotId == 36 + 9) {
			if (stack.hasNbt()) {
				contentHolder.inferredIcon = stack.getNbt()
						.getBoolean("InferredFromRecipe");
				stack.getNbt()
						.remove("InferredFromRecipe");
			} else
				contentHolder.inferredIcon = false;
		}
		super.setStackInSlot(slotId, stateId, stack);
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return contentHolder.getItems();
	}

	@Override
	protected void initAndReadInventory(BlueprintSection contentHolder) {
		super.initAndReadInventory(contentHolder);
	}

	@Override
	protected void saveData(BlueprintSection contentHolder) {
		contentHolder.save(ghostInventory);
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected BlueprintSection createOnClient(PacketByteBuf extraData) {
		int entityID = extraData.readVarInt();
		int section = extraData.readVarInt();
		Entity entityByID = MinecraftClient.getInstance().world.getEntityById(entityID);
		if (!(entityByID instanceof BlueprintEntity))
			return null;
		BlueprintEntity blueprintEntity = (BlueprintEntity) entityByID;
		BlueprintSection blueprintSection = blueprintEntity.getSection(section);
		return blueprintSection;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return contentHolder != null && contentHolder.canPlayerUse(player);
	}

	static class BlueprintCraftingInventory extends CraftingInventory {

		public BlueprintCraftingInventory(ScreenHandler menu, ItemStackHandler items) {
			super(menu, 3, 3);
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					ItemStack stack = items.getStackInSlot(y * 3 + x);
					setStack(y * 3 + x, stack == null ? ItemStack.EMPTY : stack.copy());
				}
			}
		}

	}

	public class BlueprintCraftSlot extends SlotItemHandler {

		public int index;

		public BlueprintCraftSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition) {
			super(itemHandler, index, xPosition, yPosition);
			this.id = index;
		}

		@Override
		public void markDirty() {
			super.markDirty();
			if (id == 9 && hasStack() && !contentHolder.getBlueprintWorld().isClient) {
				contentHolder.inferredIcon = false;
				ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) player;
				serverplayerentity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), 36 + 9, getStack()));
			}
			if (id < 9)
				onCraftMatrixChanged();
		}

	}

}
