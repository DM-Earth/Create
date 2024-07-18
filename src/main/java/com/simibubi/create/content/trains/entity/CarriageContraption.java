package com.simibubi.create.content.trains.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.simibubi.create.foundation.fluid.CombinedTankWrapper;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.content.contraptions.render.NonStationaryLighter;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

public class CarriageContraption extends Contraption {

	private Direction assemblyDirection;
	private boolean forwardControls;
	private boolean backwardControls;

	public Couple<Boolean> blazeBurnerConductors;
	public Map<BlockPos, Couple<Boolean>> conductorSeats;
	public ArrivalSoundQueue soundQueue;

	protected MountedStorageManager storageProxy;

	// during assembly only
	private int bogeys;
	private boolean sidewaysControls;
	private BlockPos secondBogeyPos;
	private List<BlockPos> assembledBlazeBurners;

	// render
	public int portalCutoffMin;
	public int portalCutoffMax;

	static final ContraptionInvWrapper fallbackItems = new ContraptionInvWrapper();
	static final CombinedTankWrapper fallbackFluids = new CombinedTankWrapper();

	public CarriageContraption() {
		conductorSeats = new HashMap<>();
		assembledBlazeBurners = new ArrayList<>();
		blazeBurnerConductors = Couple.create(false, false);
		soundQueue = new ArrivalSoundQueue();
		portalCutoffMin = Integer.MIN_VALUE;
		portalCutoffMax = Integer.MAX_VALUE;
		storage = new TrainCargoManager();
	}

	public void setSoundQueueOffset(int offset) {
		soundQueue.offset = offset;
	}

	public CarriageContraption(Direction assemblyDirection) {
		this();
		this.assemblyDirection = assemblyDirection;
		this.bogeys = 0;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) throws AssemblyException {
		if (!searchMovedStructure(world, pos, null))
			return false;
		if (blocks.size() <= 1)
			return false;
		if (bogeys == 0)
			return false;
		if (bogeys > 2)
			throw new AssemblyException(Lang.translateDirect("train_assembly.too_many_bogeys", bogeys));
		if (sidewaysControls)
			throw new AssemblyException(Lang.translateDirect("train_assembly.sideways_controls"));

		for (BlockPos blazePos : assembledBlazeBurners)
			for (Direction direction : Iterate.directionsInAxis(assemblyDirection.getAxis()))
				if (inControl(blazePos, direction))
					blazeBurnerConductors.set(direction != assemblyDirection, true);
		for (BlockPos seatPos : getSeats())
			for (Direction direction : Iterate.directionsInAxis(assemblyDirection.getAxis()))
				if (inControl(seatPos, direction))
					conductorSeats.computeIfAbsent(seatPos, p -> Couple.create(false, false))
						.set(direction != assemblyDirection, true);

		return true;
	}

	public boolean inControl(BlockPos pos, Direction direction) {
		BlockPos controlsPos = pos.offset(direction);
		if (!blocks.containsKey(controlsPos))
			return false;
		StructureBlockInfo info = blocks.get(controlsPos);
		if (!AllBlocks.TRAIN_CONTROLS.has(info.state()))
			return false;
		return info.state().get(ControlsBlock.FACING) == direction.getOpposite();
	}

	public void swapStorageAfterAssembly(CarriageContraptionEntity cce) {
		// Ensure that the entity does not hold its inventory data, because the global
		// carriage manages it instead
		Carriage carriage = cce.getCarriage();
		if (carriage.storage == null) {
			carriage.storage = (TrainCargoManager) storage;
			storage = new MountedStorageManager();
		}
		storageProxy = carriage.storage;
	}

	public void returnStorageForDisassembly(MountedStorageManager storage) {
		this.storage = storage;
	}

	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return false;
	}

	@Override
	protected Pair<StructureBlockInfo, BlockEntity> capture(World world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);

		if (ArrivalSoundQueue.isPlayable(blockState)) {
			int anchorCoord = VecHelper.getCoordinate(anchor, assemblyDirection.getAxis());
			int posCoord = VecHelper.getCoordinate(pos, assemblyDirection.getAxis());
			soundQueue.add((posCoord - anchorCoord) * assemblyDirection.getDirection()
				.offset(), toLocalPos(pos));
		}

		if (blockState.getBlock() instanceof AbstractBogeyBlock<?> bogey) {
			boolean captureBE = bogey.captureBlockEntityForTrain();
			bogeys++;
			if (bogeys == 2)
				secondBogeyPos = pos;
			return Pair.of(new StructureBlockInfo(pos, blockState, captureBE ? getBlockEntityNBT(world, pos) : null),
				captureBE ? world.getBlockEntity(pos) : null);
		}

		if (AllBlocks.BLAZE_BURNER.has(blockState)
			&& blockState.get(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.NONE)
			assembledBlazeBurners.add(toLocalPos(pos));

		if (AllBlocks.TRAIN_CONTROLS.has(blockState)) {
			Direction facing = blockState.get(ControlsBlock.FACING);
			if (facing.getAxis() != assemblyDirection.getAxis())
				sidewaysControls = true;
			else {
				boolean forwards = facing == assemblyDirection;
				if (forwards)
					forwardControls = true;
				else
					backwardControls = true;
			}
		}

		return super.capture(world, pos);
	}

	@Override
	public NbtCompound writeNBT(boolean spawnPacket) {
		NbtCompound tag = super.writeNBT(spawnPacket);
		NBTHelper.writeEnum(tag, "AssemblyDirection", getAssemblyDirection());
		tag.putBoolean("FrontControls", forwardControls);
		tag.putBoolean("BackControls", backwardControls);
		tag.putBoolean("FrontBlazeConductor", blazeBurnerConductors.getFirst());
		tag.putBoolean("BackBlazeConductor", blazeBurnerConductors.getSecond());
		NbtList list = NBTHelper.writeCompoundList(conductorSeats.entrySet(), e -> {
			NbtCompound compoundTag = new NbtCompound();
			compoundTag.put("Pos", NbtHelper.fromBlockPos(e.getKey()));
			compoundTag.putBoolean("Forward", e.getValue()
				.getFirst());
			compoundTag.putBoolean("Backward", e.getValue()
				.getSecond());
			return compoundTag;
		});
		tag.put("ConductorSeats", list);
		soundQueue.serialize(tag);
		return tag;
	}

	@Override
	public void readNBT(World world, NbtCompound nbt, boolean spawnData) {
		assemblyDirection = NBTHelper.readEnum(nbt, "AssemblyDirection", Direction.class);
		forwardControls = nbt.getBoolean("FrontControls");
		backwardControls = nbt.getBoolean("BackControls");
		blazeBurnerConductors =
			Couple.create(nbt.getBoolean("FrontBlazeConductor"), nbt.getBoolean("BackBlazeConductor"));
		conductorSeats.clear();
		NBTHelper.iterateCompoundList(nbt.getList("ConductorSeats", NbtElement.COMPOUND_TYPE),
			c -> conductorSeats.put(NbtHelper.toBlockPos(c.getCompound("Pos")),
				Couple.create(c.getBoolean("Forward"), c.getBoolean("Backward"))));
		soundQueue.deserialize(nbt);
		super.readNBT(world, nbt, spawnData);
	}

	@Override
	public boolean canBeStabilized(Direction facing, BlockPos localPos) {
		return false;
	}

	@Override
	protected MountedStorageManager getStorageForSpawnPacket() {
		return storageProxy;
	}

	@Override
	public ContraptionType getType() {
		return ContraptionType.CARRIAGE;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		return new NonStationaryLighter<>(this);
	}

	public Direction getAssemblyDirection() {
		return assemblyDirection;
	}

	public boolean hasForwardControls() {
		return forwardControls;
	}

	public boolean hasBackwardControls() {
		return backwardControls;
	}

	public BlockPos getSecondBogeyPos() {
		return secondBogeyPos;
	}

	private Collection<BlockEntity> specialRenderedBEsOutsidePortal = new ArrayList<>();

	@Override
	public Collection<StructureBlockInfo> getRenderedBlocks() {
		if (notInPortal())
			return super.getRenderedBlocks();

		specialRenderedBEsOutsidePortal = new ArrayList<>();
		specialRenderedBlockEntities.stream()
			.filter(be -> !isHiddenInPortal(be.getPos()))
			.forEach(specialRenderedBEsOutsidePortal::add);

		Collection<StructureBlockInfo> values = new ArrayList<>();
		for (Entry<BlockPos, StructureBlockInfo> entry : blocks.entrySet()) {
			BlockPos pos = entry.getKey();
			if (withinVisible(pos))
				values.add(entry.getValue());
			else if (atSeam(pos))
				values.add(new StructureBlockInfo(pos, Blocks.PURPLE_STAINED_GLASS.getDefaultState(), null));
		}
		return values;
	}

	@Override
	public Collection<BlockEntity> getSpecialRenderedBEs() {
		if (notInPortal())
			return super.getSpecialRenderedBEs();
		return specialRenderedBEsOutsidePortal;
	}

	@Override
	public Optional<List<Box>> getSimplifiedEntityColliders() {
		if (notInPortal())
			return super.getSimplifiedEntityColliders();
		return Optional.empty();
	}

	@Override
	public boolean isHiddenInPortal(BlockPos localPos) {
		if (notInPortal())
			return super.isHiddenInPortal(localPos);
		return !withinVisible(localPos) || atSeam(localPos);
	}

	public boolean notInPortal() {
		return portalCutoffMin == Integer.MIN_VALUE && portalCutoffMax == Integer.MAX_VALUE;
	}

	public boolean atSeam(BlockPos localPos) {
		Direction facing = assemblyDirection;
		Axis axis = facing.rotateYClockwise()
			.getAxis();
		int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getDirection()
			.offset();
		return coord == portalCutoffMin || coord == portalCutoffMax;
	}

	public boolean withinVisible(BlockPos localPos) {
		Direction facing = assemblyDirection;
		Axis axis = facing.rotateYClockwise()
			.getAxis();
		int coord = axis.choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -facing.getDirection()
			.offset();
		return coord > portalCutoffMin && coord < portalCutoffMax;
	}

	@Override
	public ContraptionInvWrapper getSharedInventory() {
		return storageProxy == null ? fallbackItems : storageProxy.getItems();
	}

	@Override
	public CombinedTankWrapper getSharedFluidTanks() {
		return storageProxy == null ? fallbackFluids : storageProxy.getFluids();
	}

	public void handleContraptionFluidPacket(BlockPos localPos, FluidStack containedFluid) {
		storage.updateContainedFluid(localPos, containedFluid);
	}

	@Override
	public void tickStorage(AbstractContraptionEntity entity) {
		if (entity.getWorld().isClient)
			storage.entityTick(entity);
		else if (storageProxy != null)
			storageProxy.entityTick(entity);
	}

}
