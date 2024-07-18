package com.simibubi.create.content.contraptions;

import static com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.isExtensionPole;
import static com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.isPistonHead;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.HashMapPaletteAccessor;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllInteractionBehaviours;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.bearing.StabilizedContraption;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlock;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.chassis.ChassisBlockEntity;
import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock.MagnetBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock.RopeBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.content.contraptions.render.EmptyLighter;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.logistics.crate.CreativeCrateBlockEntity;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.utility.BBHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.ICoordinate;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.NBTProcessors;
import com.simibubi.create.foundation.utility.UniqueLinkedList;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.StickinessUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.PistonType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.BiMapPalette;
import net.minecraft.world.poi.PointOfInterestTypes;

public abstract class Contraption {

	public Optional<List<Box>> simplifiedEntityColliders;
	public AbstractContraptionEntity entity;

	public Box bounds;
	public BlockPos anchor;
	public boolean stalled;
	public boolean hasUniversalCreativeCrate;
	public boolean disassembled;

	protected Map<BlockPos, StructureBlockInfo> blocks;
	protected List<MutablePair<StructureBlockInfo, MovementContext>> actors;
	protected Map<BlockPos, MovingInteractionBehaviour> interactors;
	protected List<ItemStack> disabledActors;

	protected List<Box> superglue;
	protected List<BlockPos> seats;
	protected Map<UUID, Integer> seatMapping;
	protected Map<UUID, BlockFace> stabilizedSubContraptions;
	protected MountedStorageManager storage;

	private Set<SuperGlueEntity> glueToRemove;
	private Map<BlockPos, Entity> initialPassengers;
	private List<BlockFace> pendingSubContraptions;

	private CompletableFuture<Void> simplifiedEntityColliderProvider;

	// Client
	public Map<BlockPos, BlockEntity> presentBlockEntities;
	public List<BlockEntity> maybeInstancedBlockEntities;
	public List<BlockEntity> specialRenderedBlockEntities;

	protected ContraptionWorld world;
	public boolean deferInvalidate;

	public Contraption() {
		blocks = new HashMap<>();
		seats = new ArrayList<>();
		actors = new ArrayList<>();
		disabledActors = new ArrayList<>();
		interactors = new HashMap<>();
		superglue = new ArrayList<>();
		seatMapping = new HashMap<>();
		glueToRemove = new HashSet<>();
		initialPassengers = new HashMap<>();
		presentBlockEntities = new HashMap<>();
		maybeInstancedBlockEntities = new ArrayList<>();
		specialRenderedBlockEntities = new ArrayList<>();
		pendingSubContraptions = new ArrayList<>();
		stabilizedSubContraptions = new HashMap<>();
		simplifiedEntityColliders = Optional.empty();
		storage = new MountedStorageManager();
	}

	public ContraptionWorld getContraptionWorld() {
		if (world == null)
			world = new ContraptionWorld(entity.getWorld(), this);
		return world;
	}

	public abstract boolean assemble(World world, BlockPos pos) throws AssemblyException;

	public abstract boolean canBeStabilized(Direction facing, BlockPos localPos);

	public abstract ContraptionType getType();

	protected boolean customBlockPlacement(WorldAccess world, BlockPos pos, BlockState state) {
		return false;
	}

	protected boolean customBlockRemoval(WorldAccess world, BlockPos pos, BlockState state) {
		return false;
	}

	protected boolean addToInitialFrontier(World world, BlockPos pos, Direction forcedDirection,
										   Queue<BlockPos> frontier) throws AssemblyException {
		return true;
	}

	public static Contraption fromNBT(World world, NbtCompound nbt, boolean spawnData) {
		String type = nbt.getString("Type");
		Contraption contraption = ContraptionType.fromType(type);
		contraption.readNBT(world, nbt, spawnData);
		contraption.world = new ContraptionWorld(world, contraption);
		contraption.gatherBBsOffThread();
		return contraption;
	}

	public boolean searchMovedStructure(World world, BlockPos pos, @Nullable Direction forcedDirection)
			throws AssemblyException {
		initialPassengers.clear();
		Queue<BlockPos> frontier = new UniqueLinkedList<>();
		Set<BlockPos> visited = new HashSet<>();
		anchor = pos;

		if (bounds == null)
			bounds = new Box(BlockPos.ORIGIN);

		if (!BlockMovementChecks.isBrittle(world.getBlockState(pos)))
			frontier.add(pos);
		if (!addToInitialFrontier(world, pos, forcedDirection, frontier))
			return false;
		for (int limit = 100000; limit > 0; limit--) {
			if (frontier.isEmpty())
				return true;
			if (!moveBlock(world, forcedDirection, frontier, visited))
				return false;
		}
		throw AssemblyException.structureTooLarge();
	}

	public void onEntityCreated(AbstractContraptionEntity entity) {
		this.entity = entity;

		// Create subcontraptions
		for (BlockFace blockFace : pendingSubContraptions) {
			Direction face = blockFace.getFace();
			StabilizedContraption subContraption = new StabilizedContraption(face);
			World world = entity.getWorld();
			BlockPos pos = blockFace.getPos();
			try {
				if (!subContraption.assemble(world, pos))
					continue;
			} catch (AssemblyException e) {
				continue;
			}
			subContraption.removeBlocksFromWorld(world, BlockPos.ORIGIN);
			OrientedContraptionEntity movedContraption = OrientedContraptionEntity.create(world, subContraption, face);
			BlockPos anchor = blockFace.getConnectedPos();
			movedContraption.setPosition(anchor.getX() + .5f, anchor.getY(), anchor.getZ() + .5f);
			world.spawnEntity(movedContraption);
			stabilizedSubContraptions.put(movedContraption.getUuid(), new BlockFace(toLocalPos(pos), face));
		}

		storage.createHandlers();
		gatherBBsOffThread();
	}

	public void onEntityRemoved(AbstractContraptionEntity entity) {
		if (simplifiedEntityColliderProvider != null) {
			simplifiedEntityColliderProvider.cancel(false);
			simplifiedEntityColliderProvider = null;
		}
	}

	public void onEntityInitialize(World world, AbstractContraptionEntity contraptionEntity) {
		if (world.isClient)
			return;

		for (OrientedContraptionEntity orientedCE : world.getNonSpectatingEntities(OrientedContraptionEntity.class,
				contraptionEntity.getBoundingBox()
						.expand(1)))
			if (stabilizedSubContraptions.containsKey(orientedCE.getUuid()))
				orientedCE.startRiding(contraptionEntity);

		for (BlockPos seatPos : getSeats()) {
			Entity passenger = initialPassengers.get(seatPos);
			if (passenger == null)
				continue;
			int seatIndex = getSeats().indexOf(seatPos);
			if (seatIndex == -1)
				continue;
			contraptionEntity.addSittingPassenger(passenger, seatIndex);
		}
	}

	/**
	 * move the first block in frontier queue
	 */
	protected boolean moveBlock(World world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier,
								Set<BlockPos> visited) throws AssemblyException {
		BlockPos pos = frontier.poll();
		if (pos == null)
			return false;
		visited.add(pos);

		if (world.isOutOfHeightLimit(pos))
			return true;
		if (!world.canSetBlock(pos))
			throw AssemblyException.unloadedChunk(pos);
		if (isAnchoringBlockAt(pos))
			return true;
		BlockState state = world.getBlockState(pos);
		if (!BlockMovementChecks.isMovementNecessary(state, world, pos))
			return true;
		if (!movementAllowed(state, world, pos))
			throw AssemblyException.unmovableBlock(pos, state);
		if (state.getBlock() instanceof AbstractChassisBlock
				&& !moveChassis(world, pos, forcedDirection, frontier, visited))
			return false;

		if (AllBlocks.BELT.has(state))
			moveBelt(pos, frontier, visited, state);

		if (AllBlocks.WINDMILL_BEARING.has(state) && world.getBlockEntity(pos) instanceof WindmillBearingBlockEntity wbbe)
			wbbe.disassembleForMovement();

		if (AllBlocks.GANTRY_CARRIAGE.has(state))
			moveGantryPinion(world, pos, frontier, visited, state);

		if (AllBlocks.GANTRY_SHAFT.has(state))
			moveGantryShaft(world, pos, frontier, visited, state);

		if (AllBlocks.STICKER.has(state) && state.get(StickerBlock.EXTENDED)) {
			Direction offset = state.get(StickerBlock.FACING);
			BlockPos attached = pos.offset(offset);
			if (!visited.contains(attached)
					&& !BlockMovementChecks.isNotSupportive(world.getBlockState(attached), offset.getOpposite()))
				frontier.add(attached);
		}

		// Double Chest halves stick together
		if (state.contains(ChestBlock.CHEST_TYPE) && state.contains(ChestBlock.FACING)
				&& state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
			Direction offset = ChestBlock.getFacing(state);
			BlockPos attached = pos.offset(offset);
			if (!visited.contains(attached))
				frontier.add(attached);
		}

		// Bogeys tend to have sticky sides
		if (state.getBlock() instanceof AbstractBogeyBlock<?> bogey)
			for (Direction d : bogey.getStickySurfaces(world, pos, state))
				if (!visited.contains(pos.offset(d)))
					frontier.add(pos.offset(d));

		// Bearings potentially create stabilized sub-contraptions
		if (AllBlocks.MECHANICAL_BEARING.has(state))
			moveBearing(pos, frontier, visited, state);

		// WM Bearings attach their structure when moved
		if (AllBlocks.WINDMILL_BEARING.has(state))
			moveWindmillBearing(pos, frontier, visited, state);

		// Seats transfer their passenger to the contraption
		if (state.getBlock() instanceof SeatBlock)
			moveSeat(world, pos);

		// Pulleys drag their rope and their attached structure
		if (state.getBlock() instanceof PulleyBlock)
			movePulley(world, pos, frontier, visited);

		// Pistons drag their attaches poles and extension
		if (state.getBlock() instanceof MechanicalPistonBlock)
			if (!moveMechanicalPiston(world, pos, frontier, visited, state))
				return false;
		if (isExtensionPole(state))
			movePistonPole(world, pos, frontier, visited, state);
		if (isPistonHead(state))
			movePistonHead(world, pos, frontier, visited, state);

		// Cart assemblers attach themselves
		BlockPos posDown = pos.down();
		BlockState stateBelow = world.getBlockState(posDown);
		if (!visited.contains(posDown) && AllBlocks.CART_ASSEMBLER.has(stateBelow))
			frontier.add(posDown);

		// Slime blocks and super glue drag adjacent blocks if possible
		for (Direction offset : Iterate.directions) {
			BlockPos offsetPos = pos.offset(offset);
			BlockState blockState = world.getBlockState(offsetPos);
			if (isAnchoringBlockAt(offsetPos))
				continue;
			if (!movementAllowed(blockState, world, offsetPos)) {
				if (offset == forcedDirection)
					throw AssemblyException.unmovableBlock(pos, state);
				continue;
			}

			boolean wasVisited = visited.contains(offsetPos);
			boolean faceHasGlue = SuperGlueEntity.isGlued(world, pos, offset, glueToRemove);
			boolean blockAttachedTowardsFace =
					BlockMovementChecks.isBlockAttachedTowards(blockState, world, offsetPos, offset.getOpposite());
			boolean brittle = BlockMovementChecks.isBrittle(blockState);
			boolean canStick = !brittle && StickinessUtil.canStickTo(state, blockState) && StickinessUtil.canStickTo(blockState, state);
			if (canStick) {
				if (state.getPistonBehavior() == PistonBehavior.PUSH_ONLY
						|| blockState.getPistonBehavior() == PistonBehavior.PUSH_ONLY) {
					canStick = false;
				}
				if (BlockMovementChecks.isNotSupportive(state, offset)) {
					canStick = false;
				}
				if (BlockMovementChecks.isNotSupportive(blockState, offset.getOpposite())) {
					canStick = false;
				}
			}

			if (!wasVisited && (canStick || blockAttachedTowardsFace || faceHasGlue
					|| (offset == forcedDirection && !BlockMovementChecks.isNotSupportive(state, forcedDirection))))
				frontier.add(offsetPos);
		}

		addBlock(pos, capture(world, pos));
		if (blocks.size() <= AllConfigs.server().kinetics.maxBlocksMoved.get())
			return true;
		else
			throw AssemblyException.structureTooLarge();
	}

	protected void movePistonHead(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
								  BlockState state) {
		Direction direction = state.get(MechanicalPistonHeadBlock.FACING);
		BlockPos offset = pos.offset(direction.getOpposite());
		if (!visited.contains(offset)) {
			BlockState blockState = world.getBlockState(offset);
			if (isExtensionPole(blockState) && blockState.get(PistonExtensionPoleBlock.FACING)
					.getAxis() == direction.getAxis())
				frontier.add(offset);
			if (blockState.getBlock() instanceof MechanicalPistonBlock) {
				Direction pistonFacing = blockState.get(MechanicalPistonBlock.FACING);
				if (pistonFacing == direction
						&& blockState.get(MechanicalPistonBlock.STATE) == PistonState.EXTENDED)
					frontier.add(offset);
			}
		}
		if (state.get(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY) {
			BlockPos attached = pos.offset(direction);
			if (!visited.contains(attached))
				frontier.add(attached);
		}
	}

	protected void movePistonPole(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
								  BlockState state) {
		for (Direction d : Iterate.directionsInAxis(state.get(PistonExtensionPoleBlock.FACING)
				.getAxis())) {
			BlockPos offset = pos.offset(d);
			if (!visited.contains(offset)) {
				BlockState blockState = world.getBlockState(offset);
				if (isExtensionPole(blockState) && blockState.get(PistonExtensionPoleBlock.FACING)
						.getAxis() == d.getAxis())
					frontier.add(offset);
				if (isPistonHead(blockState) && blockState.get(MechanicalPistonHeadBlock.FACING)
						.getAxis() == d.getAxis())
					frontier.add(offset);
				if (blockState.getBlock() instanceof MechanicalPistonBlock) {
					Direction pistonFacing = blockState.get(MechanicalPistonBlock.FACING);
					if (pistonFacing == d || pistonFacing == d.getOpposite()
							&& blockState.get(MechanicalPistonBlock.STATE) == PistonState.EXTENDED)
						frontier.add(offset);
				}
			}
		}
	}

	protected void moveGantryPinion(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
									BlockState state) {
		BlockPos offset = pos.offset(state.get(GantryCarriageBlock.FACING));
		if (!visited.contains(offset))
			frontier.add(offset);
		Axis rotationAxis = ((IRotate) state.getBlock()).getRotationAxis(state);
		for (Direction d : Iterate.directionsInAxis(rotationAxis)) {
			offset = pos.offset(d);
			BlockState offsetState = world.getBlockState(offset);
			if (AllBlocks.GANTRY_SHAFT.has(offsetState) && offsetState.get(GantryShaftBlock.FACING)
					.getAxis() == d.getAxis())
				if (!visited.contains(offset))
					frontier.add(offset);
		}
	}

	protected void moveGantryShaft(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
								   BlockState state) {
		for (Direction d : Iterate.directions) {
			BlockPos offset = pos.offset(d);
			if (!visited.contains(offset)) {
				BlockState offsetState = world.getBlockState(offset);
				Direction facing = state.get(GantryShaftBlock.FACING);
				if (d.getAxis() == facing.getAxis() && AllBlocks.GANTRY_SHAFT.has(offsetState)
						&& offsetState.get(GantryShaftBlock.FACING) == facing)
					frontier.add(offset);
				else if (AllBlocks.GANTRY_CARRIAGE.has(offsetState)
						&& offsetState.get(GantryCarriageBlock.FACING) == d)
					frontier.add(offset);
			}
		}
	}

	private void moveWindmillBearing(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
		Direction facing = state.get(WindmillBearingBlock.FACING);
		BlockPos offset = pos.offset(facing);
		if (!visited.contains(offset))
			frontier.add(offset);
	}

	private void moveBearing(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
		Direction facing = state.get(MechanicalBearingBlock.FACING);
		if (!canBeStabilized(facing, pos.subtract(anchor))) {
			BlockPos offset = pos.offset(facing);
			if (!visited.contains(offset))
				frontier.add(offset);
			return;
		}
		pendingSubContraptions.add(new BlockFace(pos, facing));
	}

	private void moveBelt(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
		BlockPos nextPos = BeltBlock.nextSegmentPosition(state, pos, true);
		BlockPos prevPos = BeltBlock.nextSegmentPosition(state, pos, false);
		if (nextPos != null && !visited.contains(nextPos))
			frontier.add(nextPos);
		if (prevPos != null && !visited.contains(prevPos))
			frontier.add(prevPos);
	}

	private void moveSeat(World world, BlockPos pos) {
		BlockPos local = toLocalPos(pos);
		getSeats().add(local);
		List<SeatEntity> seatsEntities = world.getNonSpectatingEntities(SeatEntity.class, new Box(pos));
		if (!seatsEntities.isEmpty()) {
			SeatEntity seat = seatsEntities.get(0);
			List<Entity> passengers = seat.getPassengerList();
			if (!passengers.isEmpty())
				initialPassengers.put(local, passengers.get(0));
		}
	}

	private void movePulley(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited) {
		int limit = AllConfigs.server().kinetics.maxRopeLength.get();
		BlockPos ropePos = pos;
		while (limit-- >= 0) {
			ropePos = ropePos.down();
			if (!world.canSetBlock(ropePos))
				break;
			BlockState ropeState = world.getBlockState(ropePos);
			Block block = ropeState.getBlock();
			if (!(block instanceof RopeBlock) && !(block instanceof MagnetBlock)) {
				if (!visited.contains(ropePos))
					frontier.add(ropePos);
				break;
			}
			addBlock(ropePos, capture(world, ropePos));
		}
	}

	private boolean moveMechanicalPiston(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
										 BlockState state) throws AssemblyException {
		Direction direction = state.get(MechanicalPistonBlock.FACING);
		PistonState pistonState = state.get(MechanicalPistonBlock.STATE);
		if (pistonState == PistonState.MOVING)
			return false;

		BlockPos offset = pos.offset(direction.getOpposite());
		if (!visited.contains(offset)) {
			BlockState poleState = world.getBlockState(offset);
			if (AllBlocks.PISTON_EXTENSION_POLE.has(poleState) && poleState.get(PistonExtensionPoleBlock.FACING)
					.getAxis() == direction.getAxis())
				frontier.add(offset);
		}

		if (pistonState == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state)) {
			offset = pos.offset(direction);
			if (!visited.contains(offset))
				frontier.add(offset);
		}

		return true;
	}

	private boolean moveChassis(World world, BlockPos pos, Direction movementDirection, Queue<BlockPos> frontier,
								Set<BlockPos> visited) {
		BlockEntity be = world.getBlockEntity(pos);
		if (!(be instanceof ChassisBlockEntity))
			return false;
		ChassisBlockEntity chassis = (ChassisBlockEntity) be;
		chassis.addAttachedChasses(frontier, visited);
		List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(movementDirection, false);
		if (includedBlockPositions == null)
			return false;
		for (BlockPos blockPos : includedBlockPositions)
			if (!visited.contains(blockPos))
				frontier.add(blockPos);
		return true;
	}

	protected Pair<StructureBlockInfo, BlockEntity> capture(World world, BlockPos pos) {
		BlockState blockstate = world.getBlockState(pos);
		if (AllBlocks.REDSTONE_CONTACT.has(blockstate))
			blockstate = blockstate.with(RedstoneContactBlock.POWERED, true);
		if (AllBlocks.POWERED_SHAFT.has(blockstate))
			blockstate = BlockHelper.copyProperties(blockstate, AllBlocks.SHAFT.getDefaultState());
		if (blockstate.getBlock() instanceof ControlsBlock && getType() == ContraptionType.CARRIAGE)
			blockstate = blockstate.with(ControlsBlock.OPEN, true);
		if (blockstate.contains(SlidingDoorBlock.VISIBLE))
			blockstate = blockstate.with(SlidingDoorBlock.VISIBLE, false);
		if (blockstate.getBlock() instanceof ButtonBlock) {
			blockstate = blockstate.with(ButtonBlock.POWERED, false);
			world.scheduleBlockTick(pos, blockstate.getBlock(), -1);
		}
		if (blockstate.getBlock() instanceof PressurePlateBlock) {
			blockstate = blockstate.with(PressurePlateBlock.POWERED, false);
			world.scheduleBlockTick(pos, blockstate.getBlock(), -1);
		}
		NbtCompound compoundnbt = getBlockEntityNBT(world, pos);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof PoweredShaftBlockEntity)
			blockEntity = AllBlockEntityTypes.BRACKETED_KINETIC.create(pos, blockstate);
		return Pair.of(new StructureBlockInfo(pos, blockstate, compoundnbt), blockEntity);
	}

	protected void addBlock(BlockPos pos, Pair<StructureBlockInfo, BlockEntity> pair) {
		StructureBlockInfo captured = pair.getKey();
		BlockPos localPos = pos.subtract(anchor);
		StructureBlockInfo structureBlockInfo = new StructureBlockInfo(localPos, captured.state(), captured.nbt());

		if (blocks.put(localPos, structureBlockInfo) != null)
			return;
		bounds = bounds.union(new Box(localPos));

		BlockEntity be = pair.getValue();
		storage.addBlock(localPos, be);

		if (AllMovementBehaviours.getBehaviour(captured.state()) != null)
			actors.add(MutablePair.of(structureBlockInfo, null));

		MovingInteractionBehaviour interactionBehaviour = AllInteractionBehaviours.getBehaviour(captured.state());
		if (interactionBehaviour != null)
			interactors.put(localPos, interactionBehaviour);

		if (be instanceof CreativeCrateBlockEntity
				&& ((CreativeCrateBlockEntity) be).getBehaviour(FilteringBehaviour.TYPE)
				.getFilter()
				.isEmpty())
			hasUniversalCreativeCrate = true;
	}

	@Nullable
	protected NbtCompound getBlockEntityNBT(World world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null)
			return null;
		NbtCompound nbt = blockEntity.createNbtWithIdentifyingData();
		nbt.remove("x");
		nbt.remove("y");
		nbt.remove("z");

		if ((blockEntity instanceof FluidTankBlockEntity || blockEntity instanceof ItemVaultBlockEntity)
				&& nbt.contains("Controller"))
			nbt.put("Controller",
					NbtHelper.fromBlockPos(toLocalPos(NbtHelper.toBlockPos(nbt.getCompound("Controller")))));

		return nbt;
	}

	protected BlockPos toLocalPos(BlockPos globalPos) {
		return globalPos.subtract(anchor);
	}

	protected boolean movementAllowed(BlockState state, World world, BlockPos pos) {
		return BlockMovementChecks.isMovementAllowed(state, world, pos);
	}

	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return pos.equals(anchor);
	}

	public void readNBT(World world, NbtCompound nbt, boolean spawnData) {
		blocks.clear();
		presentBlockEntities.clear();
		specialRenderedBlockEntities.clear();

		NbtElement blocks = nbt.get("Blocks");
		// used to differentiate between the 'old' and the paletted serialization
		boolean usePalettedDeserialization =
				blocks != null && blocks.getType() == 10 && ((NbtCompound) blocks).contains("Palette");
		readBlocksCompound(blocks, world, usePalettedDeserialization);

		actors.clear();
		nbt.getList("Actors", 10)
				.forEach(c -> {
					NbtCompound comp = (NbtCompound) c;
					StructureBlockInfo info = this.blocks.get(NbtHelper.toBlockPos(comp.getCompound("Pos")));
					if (info == null)
						return;
					MovementContext context = MovementContext.readNBT(world, info, comp, this);
					getActors().add(MutablePair.of(info, context));
				});

		disabledActors = NBTHelper.readItemList(nbt.getList("DisabledActors", NbtElement.COMPOUND_TYPE));
		for (ItemStack stack : disabledActors)
			setActorsActive(stack, false);

		superglue.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Superglue", NbtElement.COMPOUND_TYPE),
				c -> superglue.add(SuperGlueEntity.readBoundingBox(c)));

		seats.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Seats", NbtElement.COMPOUND_TYPE), c -> seats.add(NbtHelper.toBlockPos(c)));

		seatMapping.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Passengers", NbtElement.COMPOUND_TYPE),
				c -> seatMapping.put(NbtHelper.toUuid(NBTHelper.getINBT(c, "Id")), c.getInt("Seat")));

		stabilizedSubContraptions.clear();
		NBTHelper.iterateCompoundList(nbt.getList("SubContraptions", NbtElement.COMPOUND_TYPE),
				c -> stabilizedSubContraptions.put(c.getUuid("Id"), BlockFace.fromNBT(c.getCompound("Location"))));

		interactors.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Interactors", NbtElement.COMPOUND_TYPE), c -> {
			BlockPos pos = NbtHelper.toBlockPos(c.getCompound("Pos"));
			StructureBlockInfo structureBlockInfo = getBlocks().get(pos);
			if (structureBlockInfo == null)
				return;
			MovingInteractionBehaviour behaviour = AllInteractionBehaviours.getBehaviour(structureBlockInfo.state());
			if (behaviour != null)
				interactors.put(pos, behaviour);
		});

		storage.read(nbt, presentBlockEntities, spawnData);

		if (nbt.contains("BoundsFront"))
			bounds = NBTHelper.readAABB(nbt.getList("BoundsFront", 5));

		stalled = nbt.getBoolean("Stalled");
		hasUniversalCreativeCrate = nbt.getBoolean("BottomlessSupply");
		anchor = NbtHelper.toBlockPos(nbt.getCompound("Anchor"));
	}

	public NbtCompound writeNBT(boolean spawnPacket) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("Type", getType().id);

		NbtCompound blocksNBT = writeBlocksCompound();

		NbtList actorsNBT = new NbtList();
		for (MutablePair<StructureBlockInfo, MovementContext> actor : getActors()) {
			MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(actor.left.state());
			if (behaviour == null)
				continue;
			NbtCompound compound = new NbtCompound();
			compound.put("Pos", NbtHelper.fromBlockPos(actor.left.pos()));
			behaviour
					.writeExtraData(actor.right);
			actor.right.writeToNBT(compound);
			actorsNBT.add(compound);
		}

		NbtList disabledActorsNBT = NBTHelper.writeItemList(disabledActors);

		NbtList superglueNBT = new NbtList();
		if (!spawnPacket) {
			for (Box glueEntry : superglue) {
				NbtCompound c = new NbtCompound();
				SuperGlueEntity.writeBoundingBox(c, glueEntry);
				superglueNBT.add(c);
			}
		}

		(spawnPacket ? getStorageForSpawnPacket() : storage).write(nbt, spawnPacket);

		NbtList interactorNBT = new NbtList();
		for (BlockPos pos : interactors.keySet()) {
			NbtCompound c = new NbtCompound();
			c.put("Pos", NbtHelper.fromBlockPos(pos));
			interactorNBT.add(c);
		}

		nbt.put("Seats", NBTHelper.writeCompoundList(getSeats(), NbtHelper::fromBlockPos));
		nbt.put("Passengers", NBTHelper.writeCompoundList(getSeatMapping().entrySet(), e -> {
			NbtCompound tag = new NbtCompound();
			tag.put("Id", NbtHelper.fromUuid(e.getKey()));
			tag.putInt("Seat", e.getValue());
			return tag;
		}));

		nbt.put("SubContraptions", NBTHelper.writeCompoundList(stabilizedSubContraptions.entrySet(), e -> {
			NbtCompound tag = new NbtCompound();
			tag.putUuid("Id", e.getKey());
			tag.put("Location", e.getValue()
					.serializeNBT());
			return tag;
		}));

		nbt.put("Blocks", blocksNBT);
		nbt.put("Actors", actorsNBT);
		nbt.put("DisabledActors", disabledActorsNBT);
		nbt.put("Interactors", interactorNBT);
		nbt.put("Superglue", superglueNBT);
		nbt.put("Anchor", NbtHelper.fromBlockPos(anchor));
		nbt.putBoolean("Stalled", stalled);
		nbt.putBoolean("BottomlessSupply", hasUniversalCreativeCrate);

		if (bounds != null) {
			NbtList bb = NBTHelper.writeAABB(bounds);
			nbt.put("BoundsFront", bb);
		}

		return nbt;
	}

	protected MountedStorageManager getStorageForSpawnPacket() {
		return storage;
	}

	private NbtCompound writeBlocksCompound() {
		NbtCompound compound = new NbtCompound();
		BiMapPalette<BlockState> palette = new BiMapPalette<>(new IdList<>(), 16, (i, s) -> {
			throw new IllegalStateException("Palette Map index exceeded maximum");
		});
		NbtList blockList = new NbtList();

		for (StructureBlockInfo block : this.blocks.values()) {
			int id = palette.index(block.state());
			NbtCompound c = new NbtCompound();
			c.putLong("Pos", block.pos().asLong());
			c.putInt("State", id);
			if (block.nbt() != null)
				c.put("Data", block.nbt());
			blockList.add(c);
		}

		NbtList paletteNBT = new NbtList();
		for (int i = 0; i < palette.getSize(); ++i)
			paletteNBT.add(NbtHelper.fromBlockState(((HashMapPaletteAccessor<BlockState>) palette).port_lib$getValues().get(i)));
		compound.put("Palette", paletteNBT);
		compound.put("BlockList", blockList);

		return compound;
	}

	private void readBlocksCompound(NbtElement compound, World world, boolean usePalettedDeserialization) {
		RegistryEntryLookup<Block> holderGetter = world.createCommandRegistryWrapper(RegistryKeys.BLOCK);
		BiMapPalette<BlockState> palette = null;
		NbtList blockList;
		if (usePalettedDeserialization) {
			NbtCompound c = ((NbtCompound) compound);
			palette = new BiMapPalette<>(new IdList<>(), 16, (i, s) -> {
				throw new IllegalStateException("Palette Map index exceeded maximum");
			});

			NbtList list = c.getList("Palette", 10);
			((HashMapPaletteAccessor) palette).port_lib$getValues().clear();
			for (int i = 0; i < list.size(); ++i)
				((HashMapPaletteAccessor) palette).port_lib$getValues().add(NbtHelper.toBlockState(holderGetter, list.getCompound(i)));

			blockList = c.getList("BlockList", 10);
		} else {
			blockList = (NbtList) compound;
		}

		BiMapPalette<BlockState> finalPalette = palette;
		blockList.forEach(e -> {
			NbtCompound c = (NbtCompound) e;

			StructureBlockInfo info =
					usePalettedDeserialization ? readStructureBlockInfo(c, finalPalette) : legacyReadStructureBlockInfo(c, holderGetter);

			this.blocks.put(info.pos(), info);

			if (!world.isClient)
				return;

			NbtCompound tag = info.nbt();
			if (tag == null)
				return;

			tag.putInt("x", info.pos().getX());
			tag.putInt("y", info.pos().getY());
			tag.putInt("z", info.pos().getZ());

			BlockEntity be = BlockEntity.createFromNbt(info.pos(), info.state(), tag);
			if (be == null)
				return;
			be.setWorld(world);
			if (be instanceof KineticBlockEntity kbe)
				kbe.setSpeed(0);
			be.getCachedState();

			MovementBehaviour movementBehaviour = AllMovementBehaviours.getBehaviour(info.state());
			if (movementBehaviour == null || !movementBehaviour.hasSpecialInstancedRendering())
				maybeInstancedBlockEntities.add(be);

			if (movementBehaviour != null && !movementBehaviour.renderAsNormalBlockEntity())
				return;

			presentBlockEntities.put(info.pos(), be);
			specialRenderedBlockEntities.add(be);
		});
	}

	private static StructureBlockInfo readStructureBlockInfo(NbtCompound blockListEntry,
															 BiMapPalette<BlockState> palette) {
		return new StructureBlockInfo(BlockPos.fromLong(blockListEntry.getLong("Pos")),
				Objects.requireNonNull(palette.get(blockListEntry.getInt("State"))),
				blockListEntry.contains("Data") ? blockListEntry.getCompound("Data") : null);
	}

	private static StructureBlockInfo legacyReadStructureBlockInfo(NbtCompound blockListEntry, RegistryEntryLookup<Block> holderGetter) {
		return new StructureBlockInfo(NbtHelper.toBlockPos(blockListEntry.getCompound("Pos")),
				NbtHelper.toBlockState(holderGetter, blockListEntry.getCompound("Block")),
				blockListEntry.contains("Data") ? blockListEntry.getCompound("Data") : null);
	}

	public void removeBlocksFromWorld(World world, BlockPos offset) {
		storage.removeStorageFromWorld();

		glueToRemove.forEach(glue -> {
			superglue.add(glue.getBoundingBox()
					.offset(Vec3d.of(offset.add(anchor))
							.multiply(-1)));
			glue.discard();
		});

		List<BlockBox> minimisedGlue = new ArrayList<>();
		for (int i = 0; i < superglue.size(); i++)
			minimisedGlue.add(null);

		for (boolean brittles : Iterate.trueAndFalse) {
			for (Iterator<StructureBlockInfo> iterator = blocks.values()
					.iterator(); iterator.hasNext(); ) {
				StructureBlockInfo block = iterator.next();
				if (brittles != BlockMovementChecks.isBrittle(block.state()))
					continue;

				for (int i = 0; i < superglue.size(); i++) {
					Box aabb = superglue.get(i);
					if (aabb == null
							|| !aabb.contains(block.pos().getX() + .5, block.pos().getY() + .5, block.pos().getZ() + .5))
						continue;
					if (minimisedGlue.get(i) == null)
						minimisedGlue.set(i, new BlockBox(block.pos()));
					else
						minimisedGlue.set(i, BBHelper.encapsulate(minimisedGlue.get(i), block.pos()));
				}

				BlockPos add = block.pos().add(anchor)
						.add(offset);
				if (customBlockRemoval(world, add, block.state()))
					continue;
				BlockState oldState = world.getBlockState(add);
				Block blockIn = oldState.getBlock();
				boolean blockMismatch = block.state().getBlock() != blockIn;
				blockMismatch &= !AllBlocks.POWERED_SHAFT.is(blockIn) || !AllBlocks.SHAFT.has(block.state());
				if (blockMismatch)
					iterator.remove();
				world.removeBlockEntity(add);
				int flags = Block.MOVED | Block.SKIP_DROPS | Block.FORCE_STATE
						| Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD;
				if (blockIn instanceof Waterloggable && oldState.contains(Properties.WATERLOGGED)
						&& oldState.get(Properties.WATERLOGGED)) {
					world.setBlockState(add, Blocks.WATER.getDefaultState(), flags);
					continue;
				}
				world.setBlockState(add, Blocks.AIR.getDefaultState(), flags);
			}
		}

		superglue.clear();
		for (BlockBox box : minimisedGlue) {
			if (box == null)
				continue;
			Box bb = new Box(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX() + 1, box.getMaxY() + 1, box.getMaxZ() + 1);
			if (bb.getAverageSideLength() > 1.01)
				superglue.add(bb);
		}

		for (StructureBlockInfo block : blocks.values()) {
			BlockPos add = block.pos().add(anchor)
					.add(offset);
//			if (!shouldUpdateAfterMovement(block))
//				continue;

			int flags = Block.MOVED | Block.NOTIFY_ALL;
			world.updateListeners(add, block.state(), Blocks.AIR.getDefaultState(), flags);

			// when the blockstate is set to air, the block's POI data is removed, but
			// markAndNotifyBlock tries to
			// remove it again, so to prevent an error from being logged by double-removal
			// we add the POI data back now
			// (code copied from ServerWorld.onBlockStateChange)
			ServerWorld serverWorld = (ServerWorld) world;
			PointOfInterestTypes.getTypeForState(block.state())
				.ifPresent(poiType -> {
					world.getServer()
						.execute(() -> {
							serverWorld.getPointOfInterestStorage()
								.add(add, poiType);
							DebugInfoSender.sendPoiAddition(serverWorld, add);
						});
				});

			world.markAndNotifyBlock(add, world.getWorldChunk(add), block.state(), Blocks.AIR.getDefaultState(), flags,
					512);
			block.state().prepare(world, add, flags & -2);
		}
	}

	public void addBlocksToWorld(World world, StructureTransform transform) {
		if (disassembled)
			return;
		disassembled = true;

		for (boolean nonBrittles : Iterate.trueAndFalse) {
			for (StructureBlockInfo block : blocks.values()) {
				if (nonBrittles == BlockMovementChecks.isBrittle(block.state()))
					continue;

				BlockPos targetPos = transform.apply(block.pos());
				BlockState state = transform.apply(block.state());

				if (customBlockPlacement(world, targetPos, state))
					continue;

				if (nonBrittles)
					for (Direction face : Iterate.directions)
						state = state.getStateForNeighborUpdate(face, world.getBlockState(targetPos.offset(face)), world, targetPos,
								targetPos.offset(face));

				BlockState blockState = world.getBlockState(targetPos);
				if (blockState.getHardness(world, targetPos) == -1 || (state.getCollisionShape(world, targetPos)
						.isEmpty()
						&& !blockState.getCollisionShape(world, targetPos)
						.isEmpty())) {
					if (targetPos.getY() == world.getBottomY())
						targetPos = targetPos.up();
					world.syncWorldEvent(2001, targetPos, Block.getRawIdFromState(state));
					Block.dropStacks(state, world, targetPos, null);
					continue;
				}
				if (state.getBlock() instanceof Waterloggable
						&& state.contains(Properties.WATERLOGGED)) {
					FluidState FluidState = world.getFluidState(targetPos);
					state = state.with(Properties.WATERLOGGED, FluidState.getFluid() == Fluids.WATER);
				}

				world.breakBlock(targetPos, true);

				if (AllBlocks.SHAFT.has(state))
					state = ShaftBlock.pickCorrectShaftType(state, world, targetPos);
				if (state.contains(SlidingDoorBlock.VISIBLE))
					state = state.with(SlidingDoorBlock.VISIBLE, !state.get(SlidingDoorBlock.OPEN))
							.with(SlidingDoorBlock.POWERED, false);
				// Stop Sculk shriekers from getting "stuck" if moved mid-shriek.
				if(state.isOf(Blocks.SCULK_SHRIEKER)){
					state = Blocks.SCULK_SHRIEKER.getDefaultState();
				}

				world.setBlockState(targetPos, state, Block.MOVED | Block.NOTIFY_ALL);

				boolean verticalRotation = transform.rotationAxis == null || transform.rotationAxis.isHorizontal();
				verticalRotation = verticalRotation && transform.rotation != BlockRotation.NONE;
				if (verticalRotation) {
					if (state.getBlock() instanceof RopeBlock || state.getBlock() instanceof MagnetBlock
						|| state.getBlock() instanceof DoorBlock)
						world.breakBlock(targetPos, true);
				}

				BlockEntity blockEntity = world.getBlockEntity(targetPos);

				NbtCompound tag = block.nbt();

				// Temporary fix: Calling load(CompoundTag tag) on a Sculk sensor causes it to not react to vibrations.
				if(state.isOf(Blocks.SCULK_SENSOR) || state.isOf(Blocks.SCULK_SHRIEKER))
					tag = null;

				if (blockEntity != null)
					tag = NBTProcessors.process(blockEntity, tag, false);
				if (blockEntity != null && tag != null) {
					tag.putInt("x", targetPos.getX());
					tag.putInt("y", targetPos.getY());
					tag.putInt("z", targetPos.getZ());

					if (verticalRotation && blockEntity instanceof PulleyBlockEntity) {
						tag.remove("Offset");
						tag.remove("InitialOffset");
					}

					if (blockEntity instanceof IMultiBlockEntityContainer && tag.contains("LastKnownPos"))
						tag.put("LastKnownPos", NbtHelper.fromBlockPos(BlockPos.ORIGIN.down(Integer.MAX_VALUE - 1)));

					blockEntity.readNbt(tag);
					storage.addStorageToWorld(block, blockEntity);
				}

				transform.apply(blockEntity);
			}
		}

		for (StructureBlockInfo block : blocks.values()) {
			if (!shouldUpdateAfterMovement(block))
				continue;
			BlockPos targetPos = transform.apply(block.pos());
			world.markAndNotifyBlock(targetPos, world.getWorldChunk(targetPos), block.state(), block.state(),
					Block.MOVED | Block.NOTIFY_ALL, 512);
		}

		for (Box box : superglue) {
			box = new Box(transform.apply(new Vec3d(box.minX, box.minY, box.minZ)),
					transform.apply(new Vec3d(box.maxX, box.maxY, box.maxZ)));
			if (!world.isClient)
				world.spawnEntity(new SuperGlueEntity(world, box));
		}

		storage.clear();
	}

	public void addPassengersToWorld(World world, StructureTransform transform, List<Entity> seatedEntities) {
		for (Entity seatedEntity : seatedEntities) {
			if (getSeatMapping().isEmpty())
				continue;
			Integer seatIndex = getSeatMapping().get(seatedEntity.getUuid());
			if (seatIndex == null)
				continue;
			BlockPos seatPos = getSeats().get(seatIndex);
			seatPos = transform.apply(seatPos);
			if (!(world.getBlockState(seatPos)
					.getBlock() instanceof SeatBlock))
				continue;
			if (SeatBlock.isSeatOccupied(world, seatPos))
				continue;
			SeatBlock.sitDown(world, seatPos, seatedEntity);
		}
	}

	public void startMoving(World world) {
		disabledActors.clear();

		for (MutablePair<StructureBlockInfo, MovementContext> pair : actors) {
			MovementContext context = new MovementContext(world, pair.left, this);
			MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(pair.left.state());
					if (behaviour != null)
				behaviour.startMoving(context);
			pair.setRight(context);
			if (behaviour instanceof ContraptionControlsMovement)
				disableActorOnStart(context);
		}

		for (ItemStack stack : disabledActors)
			setActorsActive(stack, false);
	}

	protected void disableActorOnStart(MovementContext context) {
		if (!ContraptionControlsMovement.isDisabledInitially(context))
			return;
		ItemStack filter = ContraptionControlsMovement.getFilter(context);
		if (filter == null)
			return;
		if (isActorTypeDisabled(filter))
			return;
		disabledActors.add(filter);
	}

	public boolean isActorTypeDisabled(ItemStack filter) {
		return disabledActors.stream()
			.anyMatch(i -> ContraptionControlsMovement.isSameFilter(i, filter));
	}

	public void setActorsActive(ItemStack referenceStack, boolean enable) {
		for (MutablePair<StructureBlockInfo, MovementContext> pair : actors) {
			MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(pair.left.state());
			if (behaviour == null)
				continue;
			ItemStack behaviourStack = behaviour.canBeDisabledVia(pair.right);
			if (behaviourStack == null)
				continue;
			if (!referenceStack.isEmpty() && !ContraptionControlsMovement.isSameFilter(referenceStack, behaviourStack))
				continue;
			pair.right.disabled = !enable;
			if (!enable)
				behaviour.onDisabledByControls(pair.right);
		}
	}

	public List<ItemStack> getDisabledActors() {
		return disabledActors;
	}

	public void stop(World world) {
		forEachActor(world, (behaviour, ctx) -> {
			behaviour.stopMoving(ctx);
			ctx.position = null;
			ctx.motion = Vec3d.ZERO;
			ctx.relativeMotion = Vec3d.ZERO;
			ctx.rotation = v -> v;
		});
	}

	public void forEachActor(World world, BiConsumer<MovementBehaviour, MovementContext> callBack) {
		for (MutablePair<StructureBlockInfo, MovementContext> pair : actors) {
			MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(pair.getLeft().state());
			if (behaviour == null)
				continue;
			callBack.accept(behaviour, pair.getRight());
		}
	}

	protected boolean shouldUpdateAfterMovement(StructureBlockInfo info) {
		if (PointOfInterestTypes.getTypeForState(info.state())
			.isPresent())
			return false;
		if (info.state().getBlock() instanceof SlidingDoorBlock)
			return false;
		return true;
	}

	public void expandBoundsAroundAxis(Axis axis) {
		Set<BlockPos> blocks = getBlocks().keySet();

		int radius = (int) (Math.ceil(Math.sqrt(getRadius(blocks, axis))));

		int maxX = radius + 2;
		int maxY = radius + 2;
		int maxZ = radius + 2;
		int minX = -radius - 1;
		int minY = -radius - 1;
		int minZ = -radius - 1;

		if (axis == Direction.Axis.X) {
			maxX = (int) bounds.maxX;
			minX = (int) bounds.minX;
		} else if (axis == Direction.Axis.Y) {
			maxY = (int) bounds.maxY;
			minY = (int) bounds.minY;
		} else if (axis == Direction.Axis.Z) {
			maxZ = (int) bounds.maxZ;
			minZ = (int) bounds.minZ;
		}

		bounds = new Box(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public Map<UUID, Integer> getSeatMapping() {
		return seatMapping;
	}

	public BlockPos getSeatOf(UUID entityId) {
		if (!getSeatMapping().containsKey(entityId))
			return null;
		int seatIndex = getSeatMapping().get(entityId);
		if (seatIndex >= getSeats().size())
			return null;
		return getSeats().get(seatIndex);
	}

	public BlockPos getBearingPosOf(UUID subContraptionEntityId) {
		if (stabilizedSubContraptions.containsKey(subContraptionEntityId))
			return stabilizedSubContraptions.get(subContraptionEntityId)
					.getConnectedPos();
		return null;
	}

	public void setSeatMapping(Map<UUID, Integer> seatMapping) {
		this.seatMapping = seatMapping;
	}

	public List<BlockPos> getSeats() {
		return seats;
	}

	public Map<BlockPos, StructureBlockInfo> getBlocks() {
		return blocks;
	}

	public List<MutablePair<StructureBlockInfo, MovementContext>> getActors() {
		return actors;
	}

	@Nullable
	public MutablePair<StructureBlockInfo, MovementContext> getActorAt(BlockPos localPos) {
		for (MutablePair<StructureBlockInfo, MovementContext> pair : actors)
			if (localPos.equals(pair.left.pos()))
				return pair;
		return null;
	}

	public Map<BlockPos, MovingInteractionBehaviour> getInteractors() {
		return interactors;
	}

	@Environment(EnvType.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		// TODO: move lighters to registry
		return new EmptyLighter(this);
	}

	public void invalidateColliders() {
		simplifiedEntityColliders = Optional.empty();
		gatherBBsOffThread();
	}

	private void gatherBBsOffThread() {
		getContraptionWorld();
		simplifiedEntityColliderProvider = CompletableFuture.supplyAsync(() -> {
					VoxelShape combinedShape = VoxelShapes.empty();
					for (Entry<BlockPos, StructureBlockInfo> entry : blocks.entrySet()) {
						StructureBlockInfo info = entry.getValue();
						BlockPos localPos = entry.getKey();
						VoxelShape collisionShape = info.state().getCollisionShape(world, localPos, ShapeContext.absent());
						if (collisionShape.isEmpty())
							continue;
						combinedShape = VoxelShapes.combine(combinedShape,
								collisionShape.offset(localPos.getX(), localPos.getY(), localPos.getZ()), BooleanBiFunction.OR);
					}
					return combinedShape.simplify()
							.getBoundingBoxes();
				})
				.thenAccept(r -> {
					simplifiedEntityColliders = Optional.of(r);
					simplifiedEntityColliderProvider = null;
				});
	}

	public static float getRadius(Set<BlockPos> blocks, Direction.Axis axis) {
		switch (axis) {
			case X:
				return getMaxDistSqr(blocks, BlockPos::getY, BlockPos::getZ);
			case Y:
				return getMaxDistSqr(blocks, BlockPos::getX, BlockPos::getZ);
			case Z:
				return getMaxDistSqr(blocks, BlockPos::getX, BlockPos::getY);
		}

		throw new IllegalStateException("Impossible axis");
	}

	public static float getMaxDistSqr(Set<BlockPos> blocks, ICoordinate one, ICoordinate other) {
		float maxDistSq = -1;
		for (BlockPos pos : blocks) {
			float a = one.get(pos);
			float b = other.get(pos);

			float distSq = a * a + b * b;

			if (distSq > maxDistSq)
				maxDistSq = distSq;
		}

		return maxDistSq;
	}

	public ContraptionInvWrapper getSharedInventory() {
		return storage.getItems();
	}

	public ContraptionInvWrapper getSharedFuelInventory() {
		return storage.getFuelItems();
	}

	public CombinedTankWrapper getSharedFluidTanks() {
		return storage.getFluids();
	}

	public Collection<StructureBlockInfo> getRenderedBlocks() {
		return blocks.values();
	}

	public Collection<BlockEntity> getSpecialRenderedBEs() {
		return specialRenderedBlockEntities;
	}

	public boolean isHiddenInPortal(BlockPos localPos) {
		return false;
	}

	public Optional<List<Box>> getSimplifiedEntityColliders() {
		return simplifiedEntityColliders;
	}

	public void handleContraptionFluidPacket(BlockPos localPos, FluidStack containedFluid) {
		storage.updateContainedFluid(localPos, containedFluid);
	}

	public static class ContraptionInvWrapper extends CombinedStorage<ItemVariant, Storage<ItemVariant>> {
		protected final boolean isExternal;

		public ContraptionInvWrapper(boolean isExternal, Storage<ItemVariant>... itemHandler) {
			super(List.of(itemHandler));
			this.isExternal = isExternal;
		}

		public ContraptionInvWrapper(Storage<ItemVariant>... itemHandler) {
			this(false, itemHandler);
		}
	}

	public void tickStorage(AbstractContraptionEntity entity) {
		storage.entityTick(entity);
	}

	public boolean containsBlockBreakers() {
		for (MutablePair<StructureBlockInfo, MovementContext> pair : actors) {
			MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(pair.getLeft().state());
			if (behaviour instanceof BlockBreakingMovementBehaviour || behaviour instanceof HarvesterMovementBehaviour)
				return true;
		}
		return false;
	}

}
