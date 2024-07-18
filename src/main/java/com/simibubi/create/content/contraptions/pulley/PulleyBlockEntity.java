package com.simibubi.create.content.contraptions.pulley;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class PulleyBlockEntity extends LinearActuatorBlockEntity implements ThresholdSwitchObservable {

	protected int initialOffset;
	private float prevAnimatedOffset;

	protected BlockPos mirrorParent;
	protected List<BlockPos> mirrorChildren;
	public WeakReference<AbstractContraptionEntity> sharedMirrorContraption;

	public PulleyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected Box createRenderBoundingBox() {
		double expandY = -offset;
		if (sharedMirrorContraption != null) {
			AbstractContraptionEntity ace = sharedMirrorContraption.get();
			if (ace != null)
				expandY = ace.getY() - pos.getY();
		}
		return super.createRenderBoundingBox().stretch(0, expandY, 0);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.PULLEY_MAXED);
	}

	@Override
	public void tick() {
		float prevOffset = offset;
		super.tick();

		if (world.isClient() && mirrorParent != null)
			if (sharedMirrorContraption == null || sharedMirrorContraption.get() == null
				|| !sharedMirrorContraption.get()
					.isAlive()) {
				sharedMirrorContraption = null;
				if (world.getBlockEntity(mirrorParent)instanceof PulleyBlockEntity pte && pte.movedContraption != null)
					sharedMirrorContraption = new WeakReference<>(pte.movedContraption);
			}

		if (isVirtual())
			prevAnimatedOffset = offset;
		invalidateRenderBoundingBox();

		if (prevOffset < 200 && offset >= 200)
			award(AllAdvancements.PULLEY_MAXED);
	}

	@Override
	protected boolean isPassive() {
		return mirrorParent != null;
	}

	@Nullable
	public AbstractContraptionEntity getAttachedContraption() {
		return mirrorParent != null && sharedMirrorContraption != null ? sharedMirrorContraption.get()
			: movedContraption;
	}

	@Override
	protected void assemble() throws AssemblyException {
		if (!(world.getBlockState(pos)
			.getBlock() instanceof PulleyBlock))
			return;
		if (speed == 0 && mirrorParent == null)
			return;
		int maxLength = AllConfigs.server().kinetics.maxRopeLength.get();
		int i = 1;
		while (i <= maxLength) {
			BlockPos ropePos = pos.down(i);
			BlockState ropeState = world.getBlockState(ropePos);
			if (!AllBlocks.ROPE.has(ropeState) && !AllBlocks.PULLEY_MAGNET.has(ropeState)) {
				break;
			}
			++i;
		}
		offset = i - 1;
		if (offset >= getExtensionRange() && getSpeed() > 0)
			return;
		if (offset <= 0 && getSpeed() < 0)
			return;

		// Collect Construct
		if (!world.isClient && mirrorParent == null) {
			needsContraption = false;
			BlockPos anchor = pos.down(MathHelper.floor(offset + 1));
			initialOffset = MathHelper.floor(offset);
			PulleyContraption contraption = new PulleyContraption(initialOffset);
			boolean canAssembleStructure = contraption.assemble(world, anchor);

			if (canAssembleStructure) {
				Direction movementDirection = getSpeed() > 0 ? Direction.DOWN : Direction.UP;
				if (ContraptionCollider.isCollidingWithWorld(world, contraption, anchor.offset(movementDirection),
					movementDirection))
					canAssembleStructure = false;
			}

			if (!canAssembleStructure && getSpeed() > 0)
				return;

			removeRopes();

			if (!contraption.getBlocks()
				.isEmpty()) {
				contraption.removeBlocksFromWorld(world, BlockPos.ORIGIN);
				movedContraption = ControlledContraptionEntity.create(world, this, contraption);
				movedContraption.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
				world.spawnEntity(movedContraption);
				forceMove = true;
				needsContraption = true;

				if (contraption.containsBlockBreakers())
					award(AllAdvancements.CONTRAPTION_ACTORS);

				for (BlockPos pos : contraption.createColliders(world, Direction.UP)) {
					if (pos.getY() != 0)
						continue;
					pos = pos.add(anchor);
					if (world.getBlockEntity(
						new BlockPos(pos.getX(), pos.getY(), pos.getZ())) instanceof PulleyBlockEntity pbe)
						pbe.startMirroringOther(pos);
				}
			}
		}

		if (mirrorParent != null)
			removeRopes();

		clientOffsetDiff = 0;
		running = true;
		sendData();
	}

	private void removeRopes() {
		for (int i = ((int) offset); i > 0; i--) {
			BlockPos offset = pos.down(i);
			BlockState oldState = world.getBlockState(offset);
			world.setBlockState(offset, oldState.getFluidState()
				.getBlockState(), 66);
		}
	}

	@Override
	public void disassemble() {
		if (!running && movedContraption == null && mirrorParent == null)
			return;
		offset = getGridOffset(offset);
		if (movedContraption != null)
			resetContraptionToOffset();

		if (!world.isClient) {
			if (shouldCreateRopes()) {
				if (offset > 0) {
					BlockPos magnetPos = pos.down((int) offset);
					FluidState ifluidstate = world.getFluidState(magnetPos);
					world.breakBlock(magnetPos, world.getBlockState(magnetPos)
						.getCollisionShape(world, magnetPos)
						.isEmpty());
					world.setBlockState(magnetPos, AllBlocks.PULLEY_MAGNET.getDefaultState()
						.with(Properties.WATERLOGGED,
							Boolean.valueOf(ifluidstate.getFluid() == Fluids.WATER)),
						66);
				}

				boolean[] waterlog = new boolean[(int) offset];

				for (int i = 1; i <= ((int) offset) - 1; i++) {
					BlockPos ropePos = pos.down(i);
					FluidState ifluidstate = world.getFluidState(ropePos);
					waterlog[i] = ifluidstate.getFluid() == Fluids.WATER;
					world.breakBlock(ropePos, world.getBlockState(ropePos)
						.getCollisionShape(world, ropePos)
						.isEmpty());
				}
				for (int i = 1; i <= ((int) offset) - 1; i++)
					world.setBlockState(pos.down(i), AllBlocks.ROPE.getDefaultState()
						.with(Properties.WATERLOGGED, waterlog[i]), 66);
			}

			if (movedContraption != null && mirrorParent == null)
				movedContraption.disassemble();
			notifyMirrorsOfDisassembly();
		}

		if (movedContraption != null)
			movedContraption.discard();

		movedContraption = null;
		initialOffset = 0;
		running = false;
		sendData();
	}

	protected boolean shouldCreateRopes() {
		return !removed;
	}

	@Override
	protected Vec3d toPosition(float offset) {
		if (movedContraption.getContraption() instanceof PulleyContraption) {
			PulleyContraption contraption = (PulleyContraption) movedContraption.getContraption();
			return Vec3d.of(contraption.anchor)
				.add(0, contraption.getInitialOffset() - offset, 0);

		}
		return Vec3d.ZERO;
	}

	@Override
	protected void visitNewPosition() {
		super.visitNewPosition();
		if (world.isClient)
			return;
		if (movedContraption != null)
			return;
		if (getSpeed() <= 0)
			return;

		BlockPos posBelow = pos.down((int) (offset + getMovementSpeed()) + 1);
		BlockState state = world.getBlockState(posBelow);
		if (!BlockMovementChecks.isMovementNecessary(state, world, posBelow))
			return;
		if (BlockMovementChecks.isBrittle(state))
			return;

		disassemble();
		assembleNextTick = true;
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		initialOffset = compound.getInt("InitialOffset");
		needsContraption = compound.getBoolean("NeedsContraption");
		super.read(compound, clientPacket);

		BlockPos prevMirrorParent = mirrorParent;
		mirrorParent = null;
		mirrorChildren = null;

		if (compound.contains("MirrorParent")) {
			mirrorParent = NbtHelper.toBlockPos(compound.getCompound("MirrorParent"));
			offset = 0;
			if (prevMirrorParent == null || !prevMirrorParent.equals(mirrorParent))
				sharedMirrorContraption = null;
		}

		if (compound.contains("MirrorChildren"))
			mirrorChildren = NBTHelper.readCompoundList(compound.getList("MirrorChildren", NbtElement.COMPOUND_TYPE),
				NbtHelper::toBlockPos);

		if (mirrorParent == null)
			sharedMirrorContraption = null;
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		compound.putInt("InitialOffset", initialOffset);
		super.write(compound, clientPacket);

		if (mirrorParent != null)
			compound.put("MirrorParent", NbtHelper.fromBlockPos(mirrorParent));
		if (mirrorChildren != null)
			compound.put("MirrorChildren", NBTHelper.writeCompoundList(mirrorChildren, NbtHelper::fromBlockPos));
	}

	public void startMirroringOther(BlockPos parent) {
		if (parent.equals(pos))
			return;
		if (!(world.getBlockEntity(parent) instanceof PulleyBlockEntity pbe))
			return;
		if (pbe.getType() != getType())
			return;
		if (pbe.mirrorChildren == null)
			pbe.mirrorChildren = new ArrayList<>();
		pbe.mirrorChildren.add(pos);
		pbe.notifyUpdate();

		mirrorParent = parent;
		try {
			assemble();
		} catch (AssemblyException e) {
		}
		notifyUpdate();
	}

	public void notifyMirrorsOfDisassembly() {
		if (mirrorChildren == null)
			return;
		for (BlockPos blockPos : mirrorChildren) {
			if (!(world.getBlockEntity(blockPos) instanceof PulleyBlockEntity pbe))
				continue;
			pbe.offset = offset;
			pbe.disassemble();
			pbe.mirrorParent = null;
			pbe.notifyUpdate();
		}
		mirrorChildren.clear();
		notifyUpdate();
	}

	@Override
	protected int getExtensionRange() {
		return Math.max(0, Math.min(AllConfigs.server().kinetics.maxRopeLength.get(),
			(pos.getY() - 1) - world.getBottomY()));
	}

	@Override
	protected int getInitialOffset() {
		return initialOffset;
	}

	@Override
	protected Vec3d toMotionVector(float speed) {
		return new Vec3d(0, -speed, 0);
	}

	@Override
	protected ValueBoxTransform getMovementModeSlot() {
		return new CenteredSideValueBoxTransform((state, d) -> d == Direction.UP);
	}

	@Override
	public float getInterpolatedOffset(float partialTicks) {
		if (isVirtual())
			return MathHelper.lerp(partialTicks, prevAnimatedOffset, offset);
		boolean moving = running && (movedContraption == null || !movedContraption.isStalled());
		return super.getInterpolatedOffset(moving ? partialTicks : 0.5f);
	}

	public void animateOffset(float forcedOffset) {
		offset = forcedOffset;
	}

	@Override
	public float getPercent() {
		int distance = pos.getY() - world.getBottomY();
		if (distance <= 0)
			return 100;
		return 100 * getInterpolatedOffset(.5f) / distance;
	}

	public BlockPos getMirrorParent() {
		return mirrorParent;
	}
}
