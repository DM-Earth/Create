package com.simibubi.create.content.equipment.toolbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ResetableLazy;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ToolboxBlockEntity extends SmartBlockEntity implements NamedScreenHandlerFactory, Nameable, SidedStorageBlockEntity {

	public LerpedFloat lid = LerpedFloat.linear()
		.startWithValue(0);

	public LerpedFloat drawers = LerpedFloat.linear()
		.startWithValue(0);

	UUID uniqueId;
	ToolboxInventory inventory;
	ResetableLazy<DyeColor> colorProvider;
	protected int openCount;

	Map<Integer, WeakHashMap<PlayerEntity, Integer>> connectedPlayers;

	private Text customName;

	public ToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		connectedPlayers = new HashMap<>();
		inventory = new ToolboxInventory(this);
		colorProvider = ResetableLazy.of(() -> {
			BlockState blockState = getCachedState();
			if (blockState != null && blockState.getBlock() instanceof ToolboxBlock)
				return ((ToolboxBlock) blockState.getBlock()).getColor();
			return DyeColor.BROWN;
		});
		setLazyTickRate(10);
	}

	public DyeColor getColor() {
		return colorProvider.get();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void initialize() {
		super.initialize();
		ToolboxHandler.onLoad(this);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		ToolboxHandler.onUnload(this);
	}

	@Override
	public void tick() {
		super.tick();

		if (world.isClient)
			tickAudio();
		if (!world.isClient)
			tickPlayers();

		lid.chase(openCount > 0 ? 1 : 0, 0.2f, Chaser.LINEAR);
		drawers.chase(openCount > 0 ? 1 : 0, 0.2f, Chaser.EXP);
		lid.tickChaser();
		drawers.tickChaser();
	}

	private void tickPlayers() {
		boolean update = false;

		for (Iterator<Entry<Integer, WeakHashMap<PlayerEntity, Integer>>> toolboxSlots = connectedPlayers.entrySet()
			.iterator(); toolboxSlots.hasNext();) {

			Entry<Integer, WeakHashMap<PlayerEntity, Integer>> toolboxSlotEntry = toolboxSlots.next();
			WeakHashMap<PlayerEntity, Integer> set = toolboxSlotEntry.getValue();
			int slot = toolboxSlotEntry.getKey();

			ItemStack referenceItem = inventory.filters.get(slot);
			boolean clear = referenceItem.isEmpty();

			for (Iterator<Entry<PlayerEntity, Integer>> playerEntries = set.entrySet()
				.iterator(); playerEntries.hasNext();) {
				Entry<PlayerEntity, Integer> playerEntry = playerEntries.next();

				PlayerEntity player = playerEntry.getKey();
				int hotbarSlot = playerEntry.getValue();

				if (!clear && !ToolboxHandler.withinRange(player, this))
					continue;

				PlayerInventory playerInv = player.getInventory();
				ItemStack playerStack = playerInv.getStack(hotbarSlot);

				if (clear || !playerStack.isEmpty()
					&& !ToolboxInventory.canItemsShareCompartment(playerStack, referenceItem)) {
					player.getCustomData()
						.getCompound("CreateToolboxData")
						.remove(String.valueOf(hotbarSlot));
					playerEntries.remove();
					if (player instanceof ServerPlayerEntity)
						ToolboxHandler.syncData(player);
					continue;
				}

				int count = playerStack.getCount();
				int targetAmount = (referenceItem.getMaxCount() + 1) / 2;

				if (count < targetAmount) {
					int amountToReplenish = targetAmount - count;

					if (isOpenInContainer(player)) {
						try (Transaction t = TransferUtil.getTransaction()) {
							ItemStack extracted = inventory.takeFromCompartment(amountToReplenish, slot, t);
							if (!extracted.isEmpty()) {
								ToolboxHandler.unequip(player, hotbarSlot, false);
								ToolboxHandler.syncData(player);
								continue;
							}
						}
					}

					try (Transaction t = TransferUtil.getTransaction()) {
						ItemStack extracted = inventory.takeFromCompartment(amountToReplenish, slot, t);
						if (!extracted.isEmpty()) {
							update = true;
							ItemStack template = playerStack.isEmpty() ? extracted : playerStack;
							playerInv.setStack(hotbarSlot,
									ItemHandlerHelper.copyStackWithSize(template, count + extracted.getCount()));
							t.commit();
						}
					}
				}

				if (count > targetAmount) {
					int amountToDeposit = count - targetAmount;
					ItemStack toDistribute = ItemHandlerHelper.copyStackWithSize(playerStack, amountToDeposit);

					if (isOpenInContainer(player)) {
						try (Transaction t = TransferUtil.getTransaction()) {
							int deposited = amountToDeposit - inventory.distributeToCompartment(toDistribute, slot, t)
									.getCount();
							if (deposited > 0) {
								ToolboxHandler.unequip(player, hotbarSlot, true);
								ToolboxHandler.syncData(player);
								continue;
							}
						}

					}

					try (Transaction t = TransferUtil.getTransaction()) {
						int deposited = amountToDeposit - inventory.distributeToCompartment(toDistribute, slot, t)
								.getCount();
						if (deposited > 0) {
							update = true;
							playerInv.setStack(hotbarSlot,
									ItemHandlerHelper.copyStackWithSize(playerStack, count - deposited));
							t.commit();
						}
					}
				}
			}

			if (clear)
				toolboxSlots.remove();
		}

		if (update)

			sendData();

	}

	private boolean isOpenInContainer(PlayerEntity player) {
		return player.currentScreenHandler instanceof ToolboxMenu
			&& ((ToolboxMenu) player.currentScreenHandler).contentHolder == this;
	}

	public void unequipTracked() {
		if (world.isClient)
			return;

		Set<ServerPlayerEntity> affected = new HashSet<>();

		for (Iterator<Entry<Integer, WeakHashMap<PlayerEntity, Integer>>> toolboxSlots = connectedPlayers.entrySet()
			.iterator(); toolboxSlots.hasNext();) {

			Entry<Integer, WeakHashMap<PlayerEntity, Integer>> toolboxSlotEntry = toolboxSlots.next();
			WeakHashMap<PlayerEntity, Integer> set = toolboxSlotEntry.getValue();

			for (Iterator<Entry<PlayerEntity, Integer>> playerEntries = set.entrySet()
				.iterator(); playerEntries.hasNext();) {
				Entry<PlayerEntity, Integer> playerEntry = playerEntries.next();

				PlayerEntity player = playerEntry.getKey();
				int hotbarSlot = playerEntry.getValue();

				ToolboxHandler.unequip(player, hotbarSlot, false);
				if (player instanceof ServerPlayerEntity)
					affected.add((ServerPlayerEntity) player);
			}
		}

		for (ServerPlayerEntity player : affected)
			ToolboxHandler.syncData(player);
		connectedPlayers.clear();
	}

	public void unequip(int slot, PlayerEntity player, int hotbarSlot, boolean keepItems) {
		if (!connectedPlayers.containsKey(slot))
			return;
		connectedPlayers.get(slot)
			.remove(player);
		if (keepItems)
			return;

		PlayerInventoryStorage playerInv = PlayerInventoryStorage.of(player);
		SingleSlotStorage<ItemVariant> storage = playerInv.getSlot(hotbarSlot);
		if (storage.isResourceBlank())
			return;
		ItemVariant resource = storage.getResource();
		int amount = (int) storage.getAmount();
		ItemStack toInsert = ToolboxInventory.cleanItemNBT(resource.toStack(amount));
		try (Transaction t = TransferUtil.getTransaction()) {
			ItemStack remainder = inventory.distributeToCompartment(toInsert, slot, t);
			int inserted = amount - remainder.getCount();
			storage.extract(resource, inserted, t);
			t.commit();
		}
	}

	private void tickAudio() {
		Vec3d vec = VecHelper.getCenterOf(pos);
		if (lid.settled()) {
			if (openCount > 0 && lid.getChaseTarget() == 0) {
				world.playSound(vec.x, vec.y, vec.z, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.25F,
					world.random.nextFloat() * 0.1F + 1.2F, true);
				world.playSound(vec.x, vec.y, vec.z, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.1F,
					world.random.nextFloat() * 0.1F + 1.1F, true);
			}
			if (openCount == 0 && lid.getChaseTarget() == 1)
				world.playSound(vec.x, vec.y, vec.z, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.1F,
					world.random.nextFloat() * 0.1F + 1.1F, true);

		} else if (openCount == 0 && lid.getChaseTarget() == 0 && lid.getValue(0) > 1 / 16f
			&& lid.getValue(1) < 1 / 16f)
			world.playSound(vec.x, vec.y, vec.z, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.25F,
				world.random.nextFloat() * 0.1F + 1.2F, true);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return inventory;
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		inventory.deserializeNBT(compound.getCompound("Inventory"));
		super.read(compound, clientPacket);
		if (compound.contains("UniqueId", 11))
			this.uniqueId = compound.getUuid("UniqueId");
		if (compound.contains("CustomName", 8))
			this.customName = Text.Serializer.fromJson(compound.getString("CustomName"));
		if (clientPacket)
			openCount = compound.getInt("OpenCount");
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		if (uniqueId == null)
			uniqueId = UUID.randomUUID();

		compound.put("Inventory", inventory.serializeNBT());
		compound.putUuid("UniqueId", uniqueId);

		if (customName != null)
			compound.putString("CustomName", Text.Serializer.toJson(customName));
		super.write(compound, clientPacket);
		if (clientPacket)
			compound.putInt("OpenCount", openCount);
	}

	@Override
	public ScreenHandler createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return ToolboxMenu.create(id, inv, this);
	}

	@Override
	public void lazyTick() {
		updateOpenCount();
		// keep re-advertising active TEs
		ToolboxHandler.onLoad(this);
		super.lazyTick();
	}

	void updateOpenCount() {
		if (world.isClient)
			return;
		if (openCount == 0)
			return;

		int prevOpenCount = openCount;
		openCount = 0;

		for (PlayerEntity playerentity : world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8)))
			if (playerentity.currentScreenHandler instanceof ToolboxMenu
				&& ((ToolboxMenu) playerentity.currentScreenHandler).contentHolder == this)
				openCount++;

		if (prevOpenCount != openCount)
			sendData();
	}

	public void startOpen(PlayerEntity player) {
		if (player.isSpectator())
			return;
		if (openCount < 0)
			openCount = 0;
		openCount++;
		sendData();
	}

	public void stopOpen(PlayerEntity player) {
		if (player.isSpectator())
			return;
		openCount--;
		sendData();
	}

	public void connectPlayer(int slot, PlayerEntity player, int hotbarSlot) {
		if (world.isClient)
			return;
		WeakHashMap<PlayerEntity, Integer> map = connectedPlayers.computeIfAbsent(slot, WeakHashMap::new);
		Integer previous = map.get(player);
		if (previous != null) {
			if (previous == hotbarSlot)
				return;
			ToolboxHandler.unequip(player, previous, false);
		}
		map.put(player, hotbarSlot);
	}

	public void readInventory(NbtCompound compound) {
		inventory.deserializeNBT(compound);
	}

	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public boolean isFullyInitialized() {
		// returns true when uniqueId has been initialized
		return uniqueId != null;
	}

	public void setCustomName(Text customName) {
		this.customName = customName;
	}

	@Override
	public Text getDisplayName() {
		return customName != null ? customName
			: AllBlocks.TOOLBOXES.get(getColor())
				.get()
				.getName();
	}

	@Override
	public Text getCustomName() {
		return customName;
	}

	@Override
	public boolean hasCustomName() {
		return customName != null;
	}

	@Override
	public Text getName() {
		return customName;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setCachedState(BlockState state) {
		super.setCachedState(state);
		colorProvider.reset();
	}

}
