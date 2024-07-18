package com.simibubi.create.content.fluids.drain;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ItemDrainBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, SidedStorageBlockEntity {

	public static final int FILLING_TIME = 20;

	SmartFluidTankBehaviour internalTank;
	TransportedItemStack heldItem;
	protected int processingTicks;
	Map<Direction, ItemDrainItemHandler> itemHandlers;

	SnapshotParticipant<TransportedItemStack> snapshotParticipant = new SnapshotParticipant<>() {
		@Override
		protected TransportedItemStack createSnapshot() {
			return heldItem == null ? TransportedItemStack.EMPTY : heldItem.fullCopy();
		}

		@Override
		protected void readSnapshot(TransportedItemStack snapshot) {
			heldItem = snapshot == TransportedItemStack.EMPTY ? null : snapshot;
		}

		@Override
		protected void onFinalCommit() {
			notifyUpdate();
		}
	};

	public ItemDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		itemHandlers = new IdentityHashMap<>();
		for (Direction d : Iterate.horizontalDirections) {
			ItemDrainItemHandler itemDrainItemHandler = new ItemDrainItemHandler(this, d);
			itemHandlers.put(d, itemDrainItemHandler);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnels()
			.setInsertionHandler(this::tryInsertingFromSide));
		behaviours.add(internalTank = SmartFluidTankBehaviour.single(this, (long) (FluidConstants.BUCKET * 1.5))
			.allowExtraction()
			.forbidInsertion());
		registerAwardables(behaviours, AllAdvancements.DRAIN, AllAdvancements.CHAINED_DRAIN);
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
		ItemStack inserted = transportedStack.stack;
		ItemStack returned = ItemStack.EMPTY;

		if (!getHeldItemStack().isEmpty())
			return inserted;

		if (inserted.getCount() > 1 && GenericItemEmptying.canItemBeEmptied(world, inserted)) {
			returned = ItemHandlerHelper.copyStackWithSize(inserted, inserted.getCount() - 1);
			inserted = ItemHandlerHelper.copyStackWithSize(inserted, 1);
		}

		if (simulate)
			return returned;

		transportedStack = transportedStack.copy();
		transportedStack.stack = inserted.copy();
		transportedStack.beltPosition = side.getAxis()
			.isVertical() ? .5f : 0;
		transportedStack.prevSideOffset = transportedStack.sideOffset;
		transportedStack.prevBeltPosition = transportedStack.beltPosition;
		setHeldItem(transportedStack, side);
		markDirty();
		sendData();

		return returned;
	}

	public ItemStack getHeldItemStack() {
		return heldItem == null ? ItemStack.EMPTY : heldItem.stack;
	}

	@Override
	public void tick() {
		super.tick();

		if (heldItem == null) {
			processingTicks = 0;
			return;
		}

		boolean onClient = world.isClient && !isVirtual();

		if (processingTicks > 0) {
			heldItem.prevBeltPosition = .5f;
			boolean wasAtBeginning = processingTicks == FILLING_TIME;
			if (!onClient || processingTicks < FILLING_TIME)
				processingTicks--;
			if (!continueProcessing()) {
				processingTicks = 0;
				notifyUpdate();
				return;
			}
			if (wasAtBeginning != (processingTicks == FILLING_TIME))
				sendData();
			return;
		}

		heldItem.prevBeltPosition = heldItem.beltPosition;
		heldItem.prevSideOffset = heldItem.sideOffset;

		heldItem.beltPosition += itemMovementPerTick();
		if (heldItem.beltPosition > 1) {
			heldItem.beltPosition = 1;

			if (onClient)
				return;

			Direction side = heldItem.insertedFrom;

			ItemStack tryExportingToBeltFunnel = getBehaviour(DirectBeltInputBehaviour.TYPE)
				.tryExportingToBeltFunnel(heldItem.stack, side.getOpposite(), false);
			if (tryExportingToBeltFunnel != null) {
				if (tryExportingToBeltFunnel.getCount() != heldItem.stack.getCount()) {
					if (tryExportingToBeltFunnel.isEmpty())
						heldItem = null;
					else
						heldItem.stack = tryExportingToBeltFunnel;
					notifyUpdate();
					return;
				}
				if (!tryExportingToBeltFunnel.isEmpty())
					return;
			}

			BlockPos nextPosition = pos.offset(side);
			DirectBeltInputBehaviour directBeltInputBehaviour =
				BlockEntityBehaviour.get(world, nextPosition, DirectBeltInputBehaviour.TYPE);
			if (directBeltInputBehaviour == null) {
				if (!BlockHelper.hasBlockSolidSide(world.getBlockState(nextPosition), world, nextPosition,
					side.getOpposite())) {
					ItemStack ejected = heldItem.stack;
					Vec3d outPos = VecHelper.getCenterOf(pos)
						.add(Vec3d.of(side.getVector())
							.multiply(.75));
					float movementSpeed = itemMovementPerTick();
					Vec3d outMotion = Vec3d.of(side.getVector())
						.multiply(movementSpeed)
						.add(0, 1 / 8f, 0);
					outPos.add(outMotion.normalize());
					ItemEntity entity = new ItemEntity(world, outPos.x, outPos.y + 6 / 16f, outPos.z, ejected);
					entity.setVelocity(outMotion);
					entity.setToDefaultPickupDelay();
					entity.velocityModified = true;
					world.spawnEntity(entity);

					heldItem = null;
					notifyUpdate();
				}
				return;
			}

			if (!directBeltInputBehaviour.canInsertFromSide(side))
				return;

			ItemStack returned = directBeltInputBehaviour.handleInsertion(heldItem.copy(), side, false);

			if (returned.isEmpty()) {
				if (world.getBlockEntity(nextPosition) instanceof ItemDrainBlockEntity)
					award(AllAdvancements.CHAINED_DRAIN);
				heldItem = null;
				notifyUpdate();
				return;
			}

			if (returned.getCount() != heldItem.stack.getCount()) {
				heldItem.stack = returned;
				notifyUpdate();
				return;
			}

			return;
		}

		if (heldItem.prevBeltPosition < .5f && heldItem.beltPosition >= .5f) {
			if (!GenericItemEmptying.canItemBeEmptied(world, heldItem.stack))
				return;
			heldItem.beltPosition = .5f;
			if (onClient)
				return;
			processingTicks = FILLING_TIME;
			sendData();
		}

	}

	protected boolean continueProcessing() {
		if (world.isClient && !isVirtual())
			return true;
		if (processingTicks < 5)
			return true;
		if (!GenericItemEmptying.canItemBeEmptied(world, heldItem.stack))
			return false;

		Pair<FluidStack, ItemStack> emptyItem = GenericItemEmptying.emptyItem(world, heldItem.stack, true);
		FluidStack fluidFromItem = emptyItem.getFirst();

		try (Transaction t = TransferUtil.getTransaction()) {
			if (processingTicks > 5) {
				internalTank.allowInsertion();
				try (Transaction nested = t.openNested()) {
					if (!fluidFromItem.isEmpty()) {
						long inserted = internalTank.getPrimaryHandler().insert(fluidFromItem.getType(), fluidFromItem.getAmount(), nested);
						if (inserted != fluidFromItem.getAmount()) {
							internalTank.forbidInsertion();
							processingTicks = FILLING_TIME;
							return true;
						}
					}
				}
				internalTank.forbidInsertion();
				return true;
			}

			emptyItem = GenericItemEmptying.emptyItem(world, heldItem.stack.copy(), false);
			award(AllAdvancements.DRAIN);

			// Process finished
			ItemStack out = emptyItem.getSecond();
			if (!out.isEmpty())
				heldItem.stack = out;
			else
				heldItem = null;
			internalTank.allowInsertion();
			TransferUtil.insertFluid(internalTank.getPrimaryHandler(), fluidFromItem);
			t.commit();
			internalTank.forbidInsertion();
			notifyUpdate();
			return true;
		}
	}

	private float itemMovementPerTick() {
		return 1 / 8f;
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	public void setHeldItem(TransportedItemStack heldItem, Direction insertedFrom) {
		this.heldItem = heldItem;
		this.heldItem.insertedFrom = insertedFrom;
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		if (heldItem != null)
			compound.put("HeldItem", heldItem.serializeNBT());
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		heldItem = null;
		processingTicks = compound.getInt("ProcessingTicks");
		if (compound.contains("HeldItem"))
			heldItem = TransportedItemStack.read(compound.getCompound("HeldItem"));
		super.read(compound, clientPacket);
	}

	@Nullable
	@Override
	public Storage<FluidVariant> getFluidStorage(@Nullable Direction face) {
		if (face != Direction.UP) {
			return internalTank.getCapability();
		}
		return null;
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		if (face != null && face.getAxis().isHorizontal()) {
			return itemHandlers.get(face);
		}
		return null;
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		return containedFluidTooltip(tooltip, isPlayerSneaking, getFluidStorage(null));
	}
}