package com.simibubi.create.content.kinetics.belt;

import static com.simibubi.create.content.kinetics.belt.BeltPart.MIDDLE;
import static com.simibubi.create.content.kinetics.belt.BeltSlope.HORIZONTAL;
import static net.minecraft.util.math.Direction.AxisDirection.NEGATIVE;
import static net.minecraft.util.math.Direction.AxisDirection.POSITIVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.light.LightListener;
import com.jozufozu.flywheel.light.LightUpdater;
import com.jozufozu.flywheel.util.box.GridAlignedBB;
import com.jozufozu.flywheel.util.box.ImmutableBox;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler;
import com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.LightType;

public class BeltBlockEntity extends KineticBlockEntity implements SidedStorageBlockEntity, RenderAttachmentBlockEntity {

	public Map<Entity, TransportedEntityInfo> passengers;
	public Optional<DyeColor> color;
	public int beltLength;
	public int index;
	public Direction lastInsert;
	public CasingType casing;
	public boolean covered;

	protected BlockPos controller;
	protected BeltInventory inventory;
	protected Storage<ItemVariant> itemHandler;

	public NbtCompound trackerUpdateTag;

	@Environment(EnvType.CLIENT)
	public BeltLighter lighter;

	public static enum CasingType {
		NONE, ANDESITE, BRASS;
	}

	public BeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		controller = BlockPos.ORIGIN;
		itemHandler = null;
		casing = CasingType.NONE;
		color = Optional.empty();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::canInsertFrom)
			.setInsertionHandler(this::tryInsertingFromSide).considerOccupiedWhen(this::isOccupied));
		behaviours.add(new TransportedItemStackHandlerBehaviour(this, this::applyToAllItems)
			.withStackPlacement(this::getWorldPositionOf));
	}

	@Override
	public void tick() {
		// Init belt
		if (beltLength == 0)
			BeltBlock.initBelt(world, pos);

		super.tick();

		if (!AllBlocks.BELT.has(world.getBlockState(pos)))
			return;

		initializeItemHandler();

		// Move Items
		if (!isController())
			return;

		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			if (beltLength > 0 && lighter == null) {
				lighter = new BeltLighter();
			}
		});
		invalidateRenderBoundingBox();

		getInventory().tick();

		if (getSpeed() == 0)
			return;

		// Move Entities
		if (passengers == null)
			passengers = new HashMap<>();

		List<Entity> toRemove = new ArrayList<>();
		passengers.forEach((entity, info) -> {
			boolean canBeTransported = BeltMovementHandler.canBeTransported(entity);
			boolean leftTheBelt =
				info.getTicksSinceLastCollision() > ((getCachedState().get(BeltBlock.SLOPE) != HORIZONTAL) ? 3 : 1);
			if (!canBeTransported || leftTheBelt) {
				toRemove.add(entity);
				return;
			}

			info.tick();
			BeltMovementHandler.transportEntity(this, entity, info);
		});
		toRemove.forEach(passengers::remove);
	}

	@Override
	public float calculateStressApplied() {
		if (!isController())
			return 0;
		return super.calculateStressApplied();
	}

	@Override
	public Box createRenderBoundingBox() {
		if (!isController())
			return super.createRenderBoundingBox();
		else
			return super.createRenderBoundingBox().expand(beltLength + 1);
	}

	protected void initializeItemHandler() {
		if (world.isClient || itemHandler != null)
			return;
		if (beltLength == 0 || controller == null)
			return;
		if (!world.canSetBlock(controller))
			return;
		BlockEntity be = world.getBlockEntity(controller);
		if (be == null || !(be instanceof BeltBlockEntity))
			return;
		BeltInventory inventory = ((BeltBlockEntity) be).getInventory();
		if (inventory == null)
			return;
		itemHandler = new ItemHandlerBeltSegment(inventory, index);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction direction) {
		if (!isRemoved() && itemHandler == null)
			initializeItemHandler();
		return itemHandler;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (isController())
			getInventory().ejectAll();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		itemHandler = null;
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		if (controller != null)
			compound.put("Controller", NbtHelper.fromBlockPos(controller));
		compound.putBoolean("IsController", isController());
		compound.putInt("Length", beltLength);
		compound.putInt("Index", index);
		NBTHelper.writeEnum(compound, "Casing", casing);
		compound.putBoolean("Covered", covered);

		if (color.isPresent())
			NBTHelper.writeEnum(compound, "Dye", color.get());

		if (isController())
			compound.put("Inventory", getInventory().write());
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		int prevBeltLength = beltLength;
		super.read(compound, clientPacket);

		if (compound.getBoolean("IsController"))
			controller = pos;

		color = compound.contains("Dye") ? Optional.of(NBTHelper.readEnum(compound, "Dye", DyeColor.class))
			: Optional.empty();

		if (!wasMoved) {
			if (!isController())
				controller = NbtHelper.toBlockPos(compound.getCompound("Controller"));
			trackerUpdateTag = compound;
			index = compound.getInt("Index");
			beltLength = compound.getInt("Length");
			if (prevBeltLength != beltLength) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					if (lighter != null) {
						lighter.initializeLight();
					}
				});
			}
		}

		if (isController())
			getInventory().read(compound.getCompound("Inventory"));

		CasingType casingBefore = casing;
		boolean coverBefore = covered;
		casing = NBTHelper.readEnum(compound, "Casing", CasingType.class);
		covered = compound.getBoolean("Covered");

		if (!clientPacket)
			return;

		if (casingBefore == casing && coverBefore == covered)
			return;
		if (!isVirtual())
			requestModelDataUpdate();
		if (hasWorld())
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 16);
	}

	@Override
	public void clearKineticInformation() {
		super.clearKineticInformation();
		beltLength = 0;
		index = 0;
		controller = null;
		trackerUpdateTag = new NbtCompound();
	}

	public boolean applyColor(DyeColor colorIn) {
		if (colorIn == null) {
			if (!color.isPresent())
				return false;
		} else if (color.isPresent() && color.get() == colorIn)
			return false;
		if (world.isClient())
			return true;

		for (BlockPos blockPos : BeltBlock.getBeltChain(world, getController())) {
			BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
			if (belt == null)
				continue;
			belt.color = Optional.ofNullable(colorIn);
			belt.markDirty();
			belt.sendData();
		}

		return true;
	}

	public BeltBlockEntity getControllerBE() {
		if (controller == null)
			return null;
		if (!world.canSetBlock(controller))
			return null;
		BlockEntity be = world.getBlockEntity(controller);
		if (be == null || !(be instanceof BeltBlockEntity))
			return null;
		return (BeltBlockEntity) be;
	}

	public void setController(BlockPos controller) {
		this.controller = controller;
	}

	public BlockPos getController() {
		return controller == null ? pos : controller;
	}

	public boolean isController() {
		return controller != null && pos.getX() == controller.getX()
			&& pos.getY() == controller.getY() && pos.getZ() == controller.getZ();
	}

	public float getBeltMovementSpeed() {
		return getSpeed() / 480f;
	}

	public float getDirectionAwareBeltMovementSpeed() {
		int offset = getBeltFacing().getDirection()
			.offset();
		if (getBeltFacing().getAxis() == Axis.X)
			offset *= -1;
		return getBeltMovementSpeed() * offset;
	}

	public boolean hasPulley() {
		if (!AllBlocks.BELT.has(getCachedState()))
			return false;
		return getCachedState().get(BeltBlock.PART) != MIDDLE;
	}

	protected boolean isLastBelt() {
		if (getSpeed() == 0)
			return false;

		Direction direction = getBeltFacing();
		if (getCachedState().get(BeltBlock.SLOPE) == BeltSlope.VERTICAL)
			return false;

		BeltPart part = getCachedState().get(BeltBlock.PART);
		if (part == MIDDLE)
			return false;

		boolean movingPositively = (getSpeed() > 0 == (direction.getDirection()
			.offset() == 1)) ^ direction.getAxis() == Axis.X;
		return part == BeltPart.START ^ movingPositively;
	}

	public Vec3i getMovementDirection(boolean firstHalf) {
		return this.getMovementDirection(firstHalf, false);
	}

	public Vec3i getBeltChainDirection() {
		return this.getMovementDirection(true, true);
	}

	protected Vec3i getMovementDirection(boolean firstHalf, boolean ignoreHalves) {
		if (getSpeed() == 0)
			return BlockPos.ORIGIN;

		final BlockState blockState = getCachedState();
		final Direction beltFacing = blockState.get(Properties.HORIZONTAL_FACING);
		final BeltSlope slope = blockState.get(BeltBlock.SLOPE);
		final BeltPart part = blockState.get(BeltBlock.PART);
		final Axis axis = beltFacing.getAxis();

		Direction movementFacing = Direction.get(axis == Axis.X ? NEGATIVE : POSITIVE, axis);
		boolean notHorizontal = blockState.get(BeltBlock.SLOPE) != HORIZONTAL;
		if (getSpeed() < 0)
			movementFacing = movementFacing.getOpposite();
		Vec3i movement = movementFacing.getVector();

		boolean slopeBeforeHalf = (part == BeltPart.END) == (beltFacing.getDirection() == POSITIVE);
		boolean onSlope = notHorizontal && (part == MIDDLE || slopeBeforeHalf == firstHalf || ignoreHalves);
		boolean movingUp = onSlope && slope == (movementFacing == beltFacing ? BeltSlope.UPWARD : BeltSlope.DOWNWARD);

		if (!onSlope)
			return movement;

		return new Vec3i(movement.getX(), movingUp ? 1 : -1, movement.getZ());
	}

	public Direction getMovementFacing() {
		Axis axis = getBeltFacing().getAxis();
		return Direction.from(axis, getBeltMovementSpeed() < 0 ^ axis == Axis.X ? NEGATIVE : POSITIVE);
	}

	protected Direction getBeltFacing() {
		return getCachedState().get(Properties.HORIZONTAL_FACING);
	}

	public BeltInventory getInventory() {
		if (!isController()) {
			BeltBlockEntity controllerBE = getControllerBE();
			if (controllerBE != null)
				return controllerBE.getInventory();
			return null;
		}
		if (inventory == null) {
			inventory = new BeltInventory(this);
		}
		return inventory;
	}

	private void applyToAllItems(float maxDistanceFromCenter,
		Function<TransportedItemStack, TransportedResult> processFunction) {
		BeltBlockEntity controller = getControllerBE();
		if (controller == null)
			return;
		BeltInventory inventory = controller.getInventory();
		if (inventory != null)
			inventory.applyToEachWithin(index + .5f, maxDistanceFromCenter, processFunction);
	}

	private Vec3d getWorldPositionOf(TransportedItemStack transported) {
		BeltBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return Vec3d.ZERO;
		return BeltHelper.getVectorForOffset(controllerBE, transported.beltPosition);
	}

	public void setCasingType(CasingType type) {
		if (casing == type)
			return;

		BlockState blockState = getCachedState();
		boolean shouldBlockHaveCasing = type != CasingType.NONE;

		if (world.isClient) {
			casing = type;
			world.setBlockState(pos, blockState.with(BeltBlock.CASING, shouldBlockHaveCasing), 0);
			requestModelDataUpdate();
			world.updateListeners(pos, getCachedState(), getCachedState(), 16);
			return;
		}

		if (casing != CasingType.NONE)
			world.syncWorldEvent(2001, pos,
				Block.getRawIdFromState(casing == CasingType.ANDESITE ? AllBlocks.ANDESITE_CASING.getDefaultState()
					: AllBlocks.BRASS_CASING.getDefaultState()));
		if (blockState.get(BeltBlock.CASING) != shouldBlockHaveCasing)
			KineticBlockEntity.switchToBlockState(world, pos,
				blockState.with(BeltBlock.CASING, shouldBlockHaveCasing));
		casing = type;
		markDirty();
		sendData();
	}

	private boolean canInsertFrom(Direction side) {
		if (getSpeed() == 0)
			return false;
		BlockState state = getCachedState();
		if (state.contains(BeltBlock.SLOPE) && (state.get(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS
			|| state.get(BeltBlock.SLOPE) == BeltSlope.VERTICAL))
			return false;
		return getMovementFacing() != side.getOpposite();
	}
	
	private boolean isOccupied(Direction side) {
		BeltBlockEntity nextBeltController = getControllerBE();
		if (nextBeltController == null)
			return true;
		BeltInventory nextInventory = nextBeltController.getInventory();
		if (nextInventory == null)
			return true;
		if (getSpeed() == 0)
			return true;
		if (getMovementFacing() == side.getOpposite())
			return true;
		if (!nextInventory.canInsertAtFromSide(index, side))
			return true;
		return false;
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
		BeltBlockEntity nextBeltController = getControllerBE();
		ItemStack inserted = transportedStack.stack;
		ItemStack empty = ItemStack.EMPTY;

		if (nextBeltController == null)
			return inserted;
		BeltInventory nextInventory = nextBeltController.getInventory();
		if (nextInventory == null)
			return inserted;

		BlockEntity teAbove = world.getBlockEntity(pos.up());
		if (teAbove instanceof BrassTunnelBlockEntity) {
			BrassTunnelBlockEntity tunnelBE = (BrassTunnelBlockEntity) teAbove;
			if (tunnelBE.hasDistributionBehaviour()) {
				if (!tunnelBE.getStackToDistribute()
					.isEmpty())
					return inserted;
				if (!tunnelBE.testFlapFilter(side.getOpposite(), inserted))
					return inserted;
				if (!simulate) {
					BeltTunnelInteractionHandler.flapTunnel(nextInventory, index, side.getOpposite(), true);
					tunnelBE.setStackToDistribute(inserted, side.getOpposite(), null);
				}
				return empty;
			}
		}

		if (isOccupied(side))
			return inserted;
		if (simulate)
			return empty;

		transportedStack = transportedStack.copy();
		transportedStack.beltPosition = index + .5f - Math.signum(getDirectionAwareBeltMovementSpeed()) / 16f;

		Direction movementFacing = getMovementFacing();
		if (!side.getAxis()
			.isVertical()) {
			if (movementFacing != side) {
				transportedStack.sideOffset = side.getDirection()
					.offset() * .35f;
				if (side.getAxis() == Axis.X)
					transportedStack.sideOffset *= -1;
			} else
				transportedStack.beltPosition = getDirectionAwareBeltMovementSpeed() > 0 ? index : index + 1;
		}

		transportedStack.prevSideOffset = transportedStack.sideOffset;
		transportedStack.insertedAt = index;
		transportedStack.insertedFrom = side;
		transportedStack.prevBeltPosition = transportedStack.beltPosition;

		BeltTunnelInteractionHandler.flapTunnel(nextInventory, index, side.getOpposite(), true);

		nextInventory.addItem(transportedStack);
		nextBeltController.markDirty();
		nextBeltController.sendData();
		return empty;
	}

	@Override
	public RenderData getRenderAttachmentData() {
		return new RenderData(casing, covered);
	}

	@Override
	protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
		return state.contains(BeltBlock.SLOPE) && (state.get(BeltBlock.SLOPE) == BeltSlope.UPWARD
			|| state.get(BeltBlock.SLOPE) == BeltSlope.DOWNWARD);
	}

	@Override
	public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
		boolean connectedViaAxes, boolean connectedViaCogs) {
		if (target instanceof BeltBlockEntity && !connectedViaAxes)
			return getController().equals(((BeltBlockEntity) target).getController()) ? 1 : 0;
		return 0;
	}

	public void invalidateItemHandler() {
		itemHandler = null;
	}

	public boolean shouldRenderNormally() {
		if (world == null)
			return isController();
		BlockState state = getCachedState();
		return state != null && state.contains(BeltBlock.PART) && state.get(BeltBlock.PART) == BeltPart.START;
	}

	/**
	 * Hide this behavior in an inner class to avoid loading LightListener on servers.
	 */
	@Environment(EnvType.CLIENT)
	class BeltLighter implements LightListener {
		private byte[] light;

		public BeltLighter() {
			initializeLight();
			LightUpdater.get(world)
					.addListener(this);
		}

		/**
		 * Get the number of belt segments represented by the lighter.
		 * @return The number of segments.
		 */
		public int lightSegments() {
			return light == null ? 0 : light.length / 2;
		}

		/**
		 * Get the light value for a given segment.
		 * @param segment The segment to get the light value for.
		 * @return The light value.
		 */
		public int getPackedLight(int segment) {
			return light == null ? 0 : LightmapTextureManager.pack(light[segment * 2], light[segment * 2 + 1]);
		}

		@Override
		public GridAlignedBB getVolume() {
			BlockPos endPos = BeltHelper.getPositionForOffset(BeltBlockEntity.this, beltLength - 1);
			GridAlignedBB bb = GridAlignedBB.from(pos, endPos);
			bb.fixMinMax();
			return bb;
		}

		@Override
		public boolean isListenerInvalid() {
			return removed;
		}

		@Override
		public void onLightUpdate(LightType type, ImmutableBox changed) {
			if (removed)
				return;
			if (world == null)
				return;

			GridAlignedBB beltVolume = getVolume();

			if (beltVolume.intersects(changed)) {
				if (type == LightType.BLOCK)
					updateBlockLight();

				if (type == LightType.SKY)
					updateSkyLight();
			}
		}

		private void initializeLight() {
			light = new byte[beltLength * 2];

			Vec3i vec = getBeltFacing().getVector();
			BeltSlope slope = getCachedState().get(BeltBlock.SLOPE);
			int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;

			Mutable pos = new Mutable(controller.getX(), controller.getY(), controller.getZ());
			for (int i = 0; i < beltLength * 2; i += 2) {
				light[i] = (byte) world.getLightLevel(LightType.BLOCK, pos);
				light[i + 1] = (byte) world.getLightLevel(LightType.SKY, pos);
				pos.move(vec.getX(), verticality, vec.getZ());
			}
		}

		private void updateBlockLight() {
			Vec3i vec = getBeltFacing().getVector();
			BeltSlope slope = getCachedState().get(BeltBlock.SLOPE);
			int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;

			Mutable pos = new Mutable(controller.getX(), controller.getY(), controller.getZ());
			for (int i = 0; i < beltLength * 2; i += 2) {
				light[i] = (byte) world.getLightLevel(LightType.BLOCK, pos);

				pos.move(vec.getX(), verticality, vec.getZ());
			}
		}

		private void updateSkyLight() {
			Vec3i vec = getBeltFacing().getVector();
			BeltSlope slope = getCachedState().get(BeltBlock.SLOPE);
			int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;

			Mutable pos = new Mutable(controller.getX(), controller.getY(), controller.getZ());
			for (int i = 1; i < beltLength * 2; i += 2) {
				light[i] = (byte) world.getLightLevel(LightType.SKY, pos);

				pos.move(vec.getX(), verticality, vec.getZ());
			}
		}
	}

	public void setCovered(boolean blockCoveringBelt) {
		if (blockCoveringBelt == covered)
			return;
		covered = blockCoveringBelt;
		notifyUpdate();
	}

	public record RenderData(CasingType casingType, boolean covered) {
	}
}
