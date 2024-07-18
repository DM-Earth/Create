package com.simibubi.create.content.logistics.funnel;

import java.lang.ref.WeakReference;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock.Shape;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class FunnelBlockEntity extends SmartBlockEntity implements IHaveHoveringInformation {

	private FilteringBehaviour filtering;
	private InvManipulationBehaviour invManipulation;
	private VersionedInventoryTrackerBehaviour invVersionTracker;
	private int extractionCooldown;

	private WeakReference<ItemEntity> lastObserved; // In-world Extractors only

	LerpedFloat flap;

	static enum Mode {
		INVALID, PAUSED, COLLECT, PUSHING_TO_BELT, TAKING_FROM_BELT, EXTRACT
	}

	public FunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		extractionCooldown = 0;
		flap = createChasingFlap();
	}

	public Mode determineCurrentMode() {
		BlockState state = getCachedState();
		if (!FunnelBlock.isFunnel(state))
			return Mode.INVALID;
		if (state.getOrEmpty(Properties.POWERED)
			.orElse(false))
			return Mode.PAUSED;
		if (state.getBlock() instanceof BeltFunnelBlock) {
			Shape shape = state.get(BeltFunnelBlock.SHAPE);
			if (shape == Shape.PULLING)
				return Mode.TAKING_FROM_BELT;
			if (shape == Shape.PUSHING)
				return Mode.PUSHING_TO_BELT;

			BeltBlockEntity belt = BeltHelper.getSegmentBE(world, pos.down());
			if (belt != null)
				return belt.getMovementFacing() == state.get(BeltFunnelBlock.HORIZONTAL_FACING)
					? Mode.PUSHING_TO_BELT
					: Mode.TAKING_FROM_BELT;
			return Mode.INVALID;
		}
		if (state.getBlock() instanceof FunnelBlock)
			return state.get(FunnelBlock.EXTRACTING) ? Mode.EXTRACT : Mode.COLLECT;

		return Mode.INVALID;
	}

	@Override
	public void tick() {
		super.tick();
		flap.tickChaser();
		Mode mode = determineCurrentMode();
		if (world.isClient)
			return;

		// Redstone resets the extraction cooldown
		if (mode == Mode.PAUSED)
			extractionCooldown = 0;
		if (mode == Mode.TAKING_FROM_BELT)
			return;

		if (extractionCooldown > 0) {
			extractionCooldown--;
			return;
		}

		if (mode == Mode.PUSHING_TO_BELT)
			activateExtractingBeltFunnel();
		if (mode == Mode.EXTRACT)
			activateExtractor();
	}

	private void activateExtractor() {
		if (invVersionTracker.stillWaiting(invManipulation))
			return;
		
		BlockState blockState = getCachedState();
		Direction facing = AbstractFunnelBlock.getFunnelFacing(blockState);

		if (facing == null)
			return;

		boolean trackingEntityPresent = true;
		Box area = getEntityOverflowScanningArea();

		// Check if last item is still blocking the extractor
		if (lastObserved == null) {
			trackingEntityPresent = false;
		} else {
			ItemEntity lastEntity = lastObserved.get();
			if (lastEntity == null || !lastEntity.isAlive() || !lastEntity.getBoundingBox()
				.intersects(area)) {
				trackingEntityPresent = false;
				lastObserved = null;
			}
		}

		if (trackingEntityPresent)
			return;

		// Find other entities blocking the extract (only if necessary)
		int amountToExtract = getAmountToExtract();
		ExtractionCountMode mode = getModeToExtract();
		ItemStack stack = invManipulation.simulate()
			.extract(mode, amountToExtract);
		if (stack.isEmpty()) {
			invVersionTracker.awaitNewVersion(invManipulation);
			return;
		}
		for (ItemEntity itemEntity : world.getNonSpectatingEntities(ItemEntity.class, area)) {
			lastObserved = new WeakReference<>(itemEntity);
			return;
		}

		// Extract
		stack = invManipulation.extract(mode, amountToExtract);
		if (stack.isEmpty())
			return;

		flap(false);
		onTransfer(stack);

		Vec3d outputPos = VecHelper.getCenterOf(pos);
		boolean vertical = facing.getAxis()
			.isVertical();
		boolean up = facing == Direction.UP;

		outputPos = outputPos.add(Vec3d.of(facing.getVector())
			.multiply(vertical ? up ? .15f : .5f : .25f));
		if (!vertical)
			outputPos = outputPos.subtract(0, .45f, 0);

		Vec3d motion = Vec3d.ZERO;
		if (up)
			motion = new Vec3d(0, 4 / 16f, 0);

		ItemEntity item = new ItemEntity(world, outputPos.x, outputPos.y, outputPos.z, stack.copy());
		item.setToDefaultPickupDelay();
		item.setVelocity(motion);
		world.spawnEntity(item);
		lastObserved = new WeakReference<>(item);

		startCooldown();
	}

	static final Box coreBB = new Box(VecHelper.CENTER_OF_ORIGIN, VecHelper.CENTER_OF_ORIGIN).expand(.75f);

	private Box getEntityOverflowScanningArea() {
		Direction facing = AbstractFunnelBlock.getFunnelFacing(getCachedState());
		Box bb = coreBB.offset(pos);
		if (facing == null || facing == Direction.UP)
			return bb;
		return bb.stretch(0, -1, 0);
	}

	private void activateExtractingBeltFunnel() {
		if (invVersionTracker.stillWaiting(invManipulation))
			return;

		BlockState blockState = getCachedState();
		Direction facing = blockState.get(BeltFunnelBlock.HORIZONTAL_FACING);
		DirectBeltInputBehaviour inputBehaviour =
			BlockEntityBehaviour.get(world, pos.down(), DirectBeltInputBehaviour.TYPE);

		if (inputBehaviour == null)
			return;
		if (!inputBehaviour.canInsertFromSide(facing))
			return;
		if (inputBehaviour.isOccupied(facing))
			return;

		int amountToExtract = getAmountToExtract();
		ExtractionCountMode mode = getModeToExtract();
		MutableBoolean deniedByInsertion = new MutableBoolean(false);
		ItemStack stack = invManipulation.extract(mode, amountToExtract, s -> {
			ItemStack handleInsertion = inputBehaviour.handleInsertion(s, facing, true);
			if (handleInsertion.isEmpty())
				return true;
			deniedByInsertion.setTrue();
			return false;
		});
		if (stack.isEmpty()) {
			if (deniedByInsertion.isFalse())
				invVersionTracker.awaitNewVersion(invManipulation.getInventory());
			return;
		}
		flap(false);
		onTransfer(stack);
		inputBehaviour.handleInsertion(stack, facing, false);
		startCooldown();
	}

	public int getAmountToExtract() {
		if (!supportsAmountOnFilter())
			return 64;
		int amountToExtract = invManipulation.getAmountFromFilter();
		if (!filtering.isActive())
			amountToExtract = 1;
		return amountToExtract;
	}

	public ExtractionCountMode getModeToExtract() {
		if (!supportsAmountOnFilter() || !filtering.isActive())
			return ExtractionCountMode.UPTO;
		return invManipulation.getModeFromFilter();
	}

	private int startCooldown() {
		return extractionCooldown = AllConfigs.server().logistics.defaultExtractionTimer.get();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		invManipulation =
			new InvManipulationBehaviour(this, (w, p, s) -> new BlockFace(p, AbstractFunnelBlock.getFunnelFacing(s)
				.getOpposite()));
		behaviours.add(invManipulation);
		
		behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));

		filtering = new FilteringBehaviour(this, new FunnelFilterSlotPositioning());
		filtering.showCountWhen(this::supportsAmountOnFilter);
		filtering.onlyActiveWhen(this::supportsFiltering);
		filtering.withCallback($ -> invVersionTracker.reset());
		behaviours.add(filtering);
		
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::supportsDirectBeltInput)
			.setInsertionHandler(this::handleDirectBeltInput));
		registerAwardables(behaviours, AllAdvancements.FUNNEL);
	}

	private boolean supportsAmountOnFilter() {
		BlockState blockState = getCachedState();
		boolean beltFunnelsupportsAmount = false;
		if (blockState.getBlock() instanceof BeltFunnelBlock) {
			Shape shape = blockState.get(BeltFunnelBlock.SHAPE);
			if (shape == Shape.PUSHING)
				beltFunnelsupportsAmount = true;
			else
				beltFunnelsupportsAmount = BeltHelper.getSegmentBE(world, pos.down()) != null;
		}
		boolean extractor = blockState.getBlock() instanceof FunnelBlock && blockState.get(FunnelBlock.EXTRACTING);
		return beltFunnelsupportsAmount || extractor;
	}

	private boolean supportsDirectBeltInput(Direction side) {
		BlockState blockState = getCachedState();
		if (blockState == null)
			return false;
		if (!(blockState.getBlock() instanceof FunnelBlock))
			return false;
		if (blockState.get(FunnelBlock.EXTRACTING))
			return false;
		return FunnelBlock.getFunnelFacing(blockState) == Direction.UP;
	}

	private boolean supportsFiltering() {
		BlockState blockState = getCachedState();
		return AllBlocks.BRASS_BELT_FUNNEL.has(blockState) || AllBlocks.BRASS_FUNNEL.has(blockState);
	}

	private ItemStack handleDirectBeltInput(TransportedItemStack stack, Direction side, boolean simulate) {
		ItemStack inserted = stack.stack;
		if (!filtering.test(inserted))
			return inserted;
		if (determineCurrentMode() == Mode.PAUSED)
			return inserted;
		if (simulate)
			invManipulation.simulate();
		if (!simulate)
			onTransfer(inserted);
		return invManipulation.insert(inserted);
	}

	public void flap(boolean inward) {
		if (!world.isClient) {
			AllPackets.getChannel()
				.sendToClientsTracking(new FunnelFlapPacket(this, inward), this);
		} else {
			flap.setValue(inward ? 1 : -1);
			AllSoundEvents.FUNNEL_FLAP.playAt(world, pos, 1, 1, true);
		}
	}

	public boolean hasFlap() {
		BlockState blockState = getCachedState();
		if (!AbstractFunnelBlock.getFunnelFacing(blockState)
			.getAxis()
			.isHorizontal())
			return false;
		return true;
	}

	public float getFlapOffset() {
		BlockState blockState = getCachedState();
		if (!(blockState.getBlock() instanceof BeltFunnelBlock))
			return -1 / 16f;
		switch (blockState.get(BeltFunnelBlock.SHAPE)) {
		default:
		case RETRACTED:
			return 0;
		case EXTENDED:
			return 8 / 16f;
		case PULLING:
		case PUSHING:
			return -2 / 16f;
		}
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putInt("TransferCooldown", extractionCooldown);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		extractionCooldown = compound.getInt("TransferCooldown");

		if (clientPacket)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> InstancedRenderDispatcher.enqueueUpdate(this));
	}

	public void onTransfer(ItemStack stack) {
		AllBlocks.SMART_OBSERVER.get()
			.onFunnelTransfer(world, pos, stack);
		award(AllAdvancements.FUNNEL);
	}

	private LerpedFloat createChasingFlap() {
		return LerpedFloat.linear()
			.startWithValue(.25f)
			.chase(0, .05f, Chaser.EXP);
	}

}