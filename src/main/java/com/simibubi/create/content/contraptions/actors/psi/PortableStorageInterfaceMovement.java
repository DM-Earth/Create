package com.simibubi.create.content.contraptions.actors.psi;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PortableStorageInterfaceMovement implements MovementBehaviour {

	static final String _workingPos_ = "WorkingPos";
	static final String _clientPrevPos_ = "ClientPrevPos";

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(PortableStorageInterfaceBlock.FACING)
			.getVector())
			.multiply(1.85f);
	}

	@Override
	public boolean hasSpecialInstancedRendering() {
		return true;
	}

	@Nullable
	@Override
	public ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld,
		MovementContext context) {
		return new PSIActorInstance(materialManager, simulationWorld, context);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffer) {
		if (!ContraptionRenderDispatcher.canInstance())
			PortableStorageInterfaceRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		boolean onCarriage = context.contraption instanceof CarriageContraption;
		if (onCarriage && context.motion.length() > 1 / 4f)
			return;
		if (!findInterface(context, pos))
			context.data.remove(_workingPos_);
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClient)
			getAnimation(context).tickChaser();

		boolean onCarriage = context.contraption instanceof CarriageContraption;
		if (onCarriage && context.motion.length() > 1 / 4f)
			return;

		if (context.world.isClient) {
			BlockPos pos = BlockPos.ofFloored(context.position);
			if (!findInterface(context, pos))
				reset(context);
			return;
		}

		if (!context.data.contains(_workingPos_))
			return;

		BlockPos pos = NbtHelper.toBlockPos(context.data.getCompound(_workingPos_));
		Vec3d target = VecHelper.getCenterOf(pos);

		if (!context.stall && !onCarriage
			&& context.position.isInRange(target, target.distanceTo(context.position.add(context.motion))))
			context.stall = true;

		Optional<Direction> currentFacingIfValid = getCurrentFacingIfValid(context);
		if (!currentFacingIfValid.isPresent())
			return;

		PortableStorageInterfaceBlockEntity stationaryInterface =
			getStationaryInterfaceAt(context.world, pos, context.state, currentFacingIfValid.get());
		if (stationaryInterface == null) {
			reset(context);
			return;
		}

		if (stationaryInterface.connectedEntity == null)
			stationaryInterface.startTransferringTo(context.contraption, stationaryInterface.distance);

		boolean timerBelow = stationaryInterface.transferTimer <= PortableStorageInterfaceBlockEntity.ANIMATION;
		stationaryInterface.keepAlive = 2;
		if (context.stall && timerBelow) {
			context.stall = false;
		}
	}

	protected boolean findInterface(MovementContext context, BlockPos pos) {
		if (context.contraption instanceof CarriageContraption cc && !cc.notInPortal())
			return false;
		Optional<Direction> currentFacingIfValid = getCurrentFacingIfValid(context);
		if (!currentFacingIfValid.isPresent())
			return false;

		Direction currentFacing = currentFacingIfValid.get();
		PortableStorageInterfaceBlockEntity psi =
			findStationaryInterface(context.world, pos, context.state, currentFacing);

		if (psi == null)
			return false;
		if (psi.isPowered())
			return false;

		context.data.put(_workingPos_, NbtHelper.fromBlockPos(psi.getPos()));
		if (!context.world.isClient) {
			Vec3d diff = VecHelper.getCenterOf(psi.getPos())
				.subtract(context.position);
			diff = VecHelper.project(diff, Vec3d.of(currentFacing.getVector()));
			float distance = (float) (diff.length() + 1.85f - 1);
			psi.startTransferringTo(context.contraption, distance);
		} else {
			context.data.put(_clientPrevPos_, NbtHelper.fromBlockPos(pos));
			if (context.contraption instanceof CarriageContraption || context.contraption.entity.isStalled()
				|| context.motion.lengthSquared() == 0)
				getAnimation(context).chase(psi.getConnectionDistance() / 2, 0.25f, Chaser.LINEAR);
		}

		return true;
	}

	@Override
	public void stopMoving(MovementContext context) {
//		reset(context);
	}

	@Override
	public void cancelStall(MovementContext context) {
		reset(context);
	}

	public void reset(MovementContext context) {
		context.data.remove(_clientPrevPos_);
		context.data.remove(_workingPos_);
		context.stall = false;
		getAnimation(context).chase(0, 0.25f, Chaser.LINEAR);
	}

	private PortableStorageInterfaceBlockEntity findStationaryInterface(World world, BlockPos pos, BlockState state,
		Direction facing) {
		for (int i = 0; i < 2; i++) {
			PortableStorageInterfaceBlockEntity interfaceAt =
				getStationaryInterfaceAt(world, pos.offset(facing, i), state, facing);
			if (interfaceAt == null)
				continue;
			return interfaceAt;
		}
		return null;
	}

	private PortableStorageInterfaceBlockEntity getStationaryInterfaceAt(World world, BlockPos pos, BlockState state,
		Direction facing) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof PortableStorageInterfaceBlockEntity psi))
			return null;
		BlockState blockState = world.getBlockState(pos);
		if (blockState.getBlock() != state.getBlock())
			return null;
		if (blockState.get(PortableStorageInterfaceBlock.FACING) != facing.getOpposite())
			return null;
		if (psi.isPowered())
			return null;
		return psi;
	}

	private Optional<Direction> getCurrentFacingIfValid(MovementContext context) {
		Vec3d directionVec = Vec3d.of(context.state.get(PortableStorageInterfaceBlock.FACING)
			.getVector());
		directionVec = context.rotation.apply(directionVec);
		Direction facingFromVector = Direction.getFacing(directionVec.x, directionVec.y, directionVec.z);
		if (directionVec.distanceTo(Vec3d.of(facingFromVector.getVector())) > 1 / 2f)
			return Optional.empty();
		return Optional.of(facingFromVector);
	}

	public static LerpedFloat getAnimation(MovementContext context) {
		if (!(context.temporaryData instanceof LerpedFloat lf)) {
			LerpedFloat nlf = LerpedFloat.linear();
			context.temporaryData = nlf;
			return nlf;
		}
		return lf;
	}

}
