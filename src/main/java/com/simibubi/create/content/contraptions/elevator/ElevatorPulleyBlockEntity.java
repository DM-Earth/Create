package com.simibubi.create.content.contraptions.elevator;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class ElevatorPulleyBlockEntity extends PulleyBlockEntity {

	private float prevSpeed;
	private boolean arrived;
	private int clientOffsetTarget;
	private boolean initialOffsetReceived;

	public ElevatorPulleyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		prevSpeed = 0;
		arrived = true;
		initialOffsetReceived = false;
	}

	private int getTargetOffset() {
		if (world.isClient)
			return clientOffsetTarget;
		if (movedContraption == null || !(movedContraption.getContraption()instanceof ElevatorContraption ec))
			return (int) offset;

		Integer target = ec.getCurrentTargetY(world);
		if (target == null)
			return (int) offset;

		return pos.getY() - target + ec.contactYOffset - 1;
	}

	@Override
	public void attach(ControlledContraptionEntity contraption) {
		super.attach(contraption);
		if (offset >= 0)
			resetContraptionToOffset();
		if (world.isClient) {
			AllPackets.getChannel().sendToServer(new ElevatorFloorListPacket.RequestFloorList(contraption));
			return;
		}

		if (contraption.getContraption()instanceof ElevatorContraption ec)
			ElevatorColumn.getOrCreate(world, ec.getGlobalColumn())
				.setActive(true);
	}

	@Override
	public void tick() {
		boolean wasArrived = arrived;
		super.tick();

		if (movedContraption == null)
			return;
		if (!(movedContraption.getContraption()instanceof ElevatorContraption ec))
			return;
		if (world.isClient())
			ec.setClientYTarget(pos.getY() - clientOffsetTarget + ec.contactYOffset - 1);

		waitingForSpeedChange = false;
		ec.arrived = wasArrived;

		if (!arrived)
			return;

		double y = movedContraption.getY();
		int targetLevel = MathHelper.floor(0.5f + y) + ec.contactYOffset;

		Integer ecCurrentTargetY = ec.getCurrentTargetY(world);
		if (ecCurrentTargetY != null)
			targetLevel = ecCurrentTargetY;
		if (world.isClient())
			targetLevel = ec.clientYTarget;
		if (!wasArrived && !world.isClient()) {
			triggerContact(ec, targetLevel - ec.contactYOffset);
			AllSoundEvents.CONTRAPTION_DISASSEMBLE.play(world, null, pos.down((int) offset), 0.75f, 0.8f);
		}

		double diff = targetLevel - y - ec.contactYOffset;
		if (Math.abs(diff) > 1f / 128)
			diff *= 0.25f;
		movedContraption.setPosition(movedContraption.getPos()
			.add(0, diff, 0));
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (world.isClient() || !arrived)
			return;
		if (movedContraption == null || !movedContraption.isAlive())
			return;
		if (!(movedContraption.getContraption()instanceof ElevatorContraption ec))
			return;
		if (getTargetOffset() != (int) offset)
			return;

		double y = movedContraption.getY();
		int targetLevel = MathHelper.floor(0.5f + y);
		triggerContact(ec, targetLevel);
	}

	private void triggerContact(ElevatorContraption ec, int targetLevel) {
		ColumnCoords coords = ec.getGlobalColumn();
		ElevatorColumn column = ElevatorColumn.get(world, coords);
		if (column == null)
			return;

		BlockPos contactPos = column.contactAt(targetLevel + ec.contactYOffset);
		if (!world.canSetBlock(contactPos))
			return;
		BlockState contactState = world.getBlockState(contactPos);
		if (!AllBlocks.ELEVATOR_CONTACT.has(contactState))
			return;
		if (contactState.get(ElevatorContactBlock.POWERING))
			return;

		ElevatorContactBlock ecb = AllBlocks.ELEVATOR_CONTACT.get();
		ecb.withBlockEntityDo(world, contactPos, be -> be.activateBlock = true);
		ecb.scheduleActivation(world, contactPos);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		if (clientPacket)
			compound.putInt("ClientTarget", clientOffsetTarget);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (!clientPacket)
			return;

		clientOffsetTarget = compound.getInt("ClientTarget");
		if (initialOffsetReceived)
			return;

		offset = compound.getFloat("Offset");
		initialOffsetReceived = true;
		resetContraptionToOffset();
	}

	@Override
	public float getMovementSpeed() {
		int currentTarget = getTargetOffset();

		if (!world.isClient() && currentTarget != clientOffsetTarget) {
			clientOffsetTarget = currentTarget;
			sendData();
		}

		float diff = currentTarget - offset;
		float movementSpeed = MathHelper.clamp(convertToLinear(getSpeed() * 2), -1.99f, 1.99f);
		float rpmLimit = Math.abs(movementSpeed);

		float configacc = MathHelper.lerp(Math.abs(movementSpeed), 0.0075f, 0.0175f);
		float decelleration = (float) Math.sqrt(2 * Math.abs(diff) * configacc);

		float speed = diff;
		speed = MathHelper.clamp(speed, -rpmLimit, rpmLimit);
		speed = MathHelper.clamp(speed, prevSpeed - configacc, prevSpeed + configacc);
		speed = MathHelper.clamp(speed, -decelleration, decelleration);

		arrived = Math.abs(diff) < 0.5f;

		if (speed > 1 / 1024f && !world.isClient())
			markDirty();

		return prevSpeed = speed;
	}

	@Override
	protected boolean shouldCreateRopes() {
		return false;
	}

	@Override
	public void disassemble() {
		if (movedContraption != null && movedContraption.getContraption()instanceof ElevatorContraption ec) {
			ElevatorColumn column = ElevatorColumn.get(world, ec.getGlobalColumn());
			if (column != null)
				column.setActive(false);
		}

		super.disassemble();
		offset = -1;
		sendData();
	}

	public void clicked() {
		if (isPassive() && world.getBlockEntity(mirrorParent)instanceof ElevatorPulleyBlockEntity parent) {
			parent.clicked();
			return;
		}

		if (running)
			disassemble();
		else
			assembleNextTick = true;
	}

	@Override
	protected boolean moveAndCollideContraption() {
		if (arrived)
			return false;
		super.moveAndCollideContraption();
		return false;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		registerAwardables(behaviours, AllAdvancements.CONTRAPTION_ACTORS);
	}

	@Override
	protected void assemble() throws AssemblyException {
		if (!(world.getBlockState(pos)
			.getBlock() instanceof ElevatorPulleyBlock))
			return;
		if (getSpeed() == 0)
			return;

		int maxLength = AllConfigs.server().kinetics.maxRopeLength.get();
		int i = 1;
		while (i <= maxLength) {
			BlockPos ropePos = pos.down(i);
			BlockState ropeState = world.getBlockState(ropePos);
			if (!ropeState.getCollisionShape(world, ropePos)
				.isEmpty()
				&& !ropeState.isReplaceable()) {
				break;
			}
			++i;
		}

		offset = i - 1;
		forceMove = true;

		// Collect Construct
		if (!world.isClient && mirrorParent == null) {
			needsContraption = false;
			BlockPos anchor = pos.down(MathHelper.floor(offset + 1));
			offset = MathHelper.floor(offset);
			ElevatorContraption contraption = new ElevatorContraption((int) offset);

			float offsetOnSucess = offset;
			offset = 0;

			boolean canAssembleStructure = contraption.assemble(world, anchor);
			if (!canAssembleStructure && getSpeed() > 0)
				return;

			if (!contraption.getBlocks()
				.isEmpty()) {
				offset = offsetOnSucess;
				contraption.removeBlocksFromWorld(world, BlockPos.ORIGIN);
				movedContraption = ControlledContraptionEntity.create(world, this, contraption);
				movedContraption.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
				contraption.maxContactY = pos.getY() + contraption.contactYOffset - 1;
				contraption.minContactY = contraption.maxContactY - maxLength;
				world.spawnEntity(movedContraption);
				forceMove = true;
				needsContraption = true;

				if (contraption.containsBlockBreakers())
					award(AllAdvancements.CONTRAPTION_ACTORS);

				for (BlockPos pos : contraption.createColliders(world, Direction.UP)) {
					if (pos.getY() != 0)
						continue;
					pos = pos.add(anchor);
					if (world.getBlockEntity(new BlockPos(pos.getX(), pos.getY(),
						pos.getZ())) instanceof ElevatorPulleyBlockEntity pbe)
						pbe.startMirroringOther(pos);
				}

				ElevatorColumn column = ElevatorColumn.getOrCreate(world, contraption.getGlobalColumn());
				int target = (int) (pos.getY() + contraption.contactYOffset - 1 - offset);
				column.target(target);
				column.gatherAll();
				column.setActive(true);
				column.markDirty();

				contraption.broadcastFloorData(world, column.contactAt(target));
				clientOffsetTarget = column.getTargetedYLevel();
				arrived = true;
			}
		}

		clientOffsetDiff = 0;
		running = true;
		sendData();
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		markDirty();
	}

	@Override
	protected MovementMode getMovementMode() {
		return MovementMode.MOVE_NEVER_PLACE;
	}

}
