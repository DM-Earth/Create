package com.simibubi.create.content.contraptions.gantry;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

public class GantryCarriageBlockEntity extends KineticBlockEntity implements IDisplayAssemblyExceptions {

	boolean assembleNextTick;
	protected AssemblyException lastException;

	public GantryCarriageBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}
	
	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.CONTRAPTION_ACTORS);
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
	}

	public void checkValidGantryShaft() {
		if (shouldAssemble())
			queueAssembly();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!getCachedState().canPlaceAt(world, pos))
			world.breakBlock(pos, true);
	}

	public void queueAssembly() {
		assembleNextTick = true;
	}

	@Override
	public void tick() {
		super.tick();

		if (world.isClient)
			return;

		if (assembleNextTick) {
			tryAssemble();
			assembleNextTick = false;
		}
	}

	@Override
	public AssemblyException getLastAssemblyException() {
		return lastException;
	}

	private void tryAssemble() {
		BlockState blockState = getCachedState();
		if (!(blockState.getBlock() instanceof GantryCarriageBlock))
			return;

		Direction direction = blockState.get(GantryCarriageBlock.FACING);
		GantryContraption contraption = new GantryContraption(direction);

		BlockEntity blockEntity = world.getBlockEntity(pos.offset(direction.getOpposite()));
		if (!(blockEntity instanceof GantryShaftBlockEntity shaftBE))
			return;
		BlockState shaftState = shaftBE.getCachedState();
		if (!AllBlocks.GANTRY_SHAFT.has(shaftState))
			return;

		float pinionMovementSpeed = shaftBE.getPinionMovementSpeed();
		Direction shaftOrientation = shaftState.get(GantryShaftBlock.FACING);
		Direction movementDirection = shaftOrientation;
		if (pinionMovementSpeed < 0)
			movementDirection = movementDirection.getOpposite();

		try {
			lastException = null;
			if (!contraption.assemble(world, pos))
				return;

			sendData();
		} catch (AssemblyException e) {
			lastException = e;
			sendData();
			return;
		}
		if (ContraptionCollider.isCollidingWithWorld(world, contraption, pos.offset(movementDirection),
			movementDirection))
			return;
		
		if (contraption.containsBlockBreakers())
			award(AllAdvancements.CONTRAPTION_ACTORS);

		contraption.removeBlocksFromWorld(world, BlockPos.ORIGIN);
		GantryContraptionEntity movedContraption =
			GantryContraptionEntity.create(world, contraption, shaftOrientation);
		BlockPos anchor = pos;
		movedContraption.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
		AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(world, pos);
		world.spawnEntity(movedContraption);

		if (shaftBE.sequenceContext != null
			&& shaftBE.sequenceContext.instruction() == SequencerInstructions.TURN_DISTANCE)
			movedContraption.limitMovement(shaftBE.sequenceContext.getEffectiveValue(shaftBE.getTheoreticalSpeed()));
	}

	@Override
	protected void write(NbtCompound compound, boolean clientPacket) {
		AssemblyException.write(compound, lastException);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		lastException = AssemblyException.read(compound);
		super.read(compound, clientPacket);
	}

	@Override
	public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
		boolean connectedViaAxes, boolean connectedViaCogs) {
		float defaultModifier =
			super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);

		if (connectedViaAxes)
			return defaultModifier;
		if (!AllBlocks.GANTRY_SHAFT.has(stateTo))
			return defaultModifier;
		if (!stateTo.get(GantryShaftBlock.POWERED))
			return defaultModifier;

		Direction direction = Direction.getFacing(diff.getX(), diff.getY(), diff.getZ());
		if (stateFrom.get(GantryCarriageBlock.FACING) != direction.getOpposite())
			return defaultModifier;
		return getGantryPinionModifier(stateTo.get(GantryShaftBlock.FACING), stateFrom.get(GantryCarriageBlock.FACING));
	}

	public static float getGantryPinionModifier(Direction shaft, Direction pinionDirection) {
		Axis shaftAxis = shaft.getAxis();
		float directionModifier = shaft.getDirection()
			.offset();
		if (shaftAxis == Axis.Y)
			if (pinionDirection == Direction.NORTH || pinionDirection == Direction.EAST)
				return -directionModifier;
		if (shaftAxis == Axis.X)
			if (pinionDirection == Direction.DOWN || pinionDirection == Direction.SOUTH)
				return -directionModifier;
		if (shaftAxis == Axis.Z)
			if (pinionDirection == Direction.UP || pinionDirection == Direction.WEST)
				return -directionModifier;
		return directionModifier;
	}

	private boolean shouldAssemble() {
		BlockState blockState = getCachedState();
		if (!(blockState.getBlock() instanceof GantryCarriageBlock))
			return false;
		Direction facing = blockState.get(GantryCarriageBlock.FACING)
			.getOpposite();
		BlockState shaftState = world.getBlockState(pos.offset(facing));
		if (!(shaftState.getBlock() instanceof GantryShaftBlock))
			return false;
		if (shaftState.get(GantryShaftBlock.POWERED))
			return false;
		BlockEntity be = world.getBlockEntity(pos.offset(facing));
		return be instanceof GantryShaftBlockEntity && ((GantryShaftBlockEntity) be).canAssembleOn();
	}
}
