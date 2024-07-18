package com.simibubi.create.content.contraptions;

import java.lang.ref.WeakReference;
import java.util.Collection;

import javax.annotation.Nullable;
import org.apache.commons.lang3.mutable.MutableObject;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.sync.ContraptionInteractionPacket;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class ContraptionHandlerClient {

	@Environment(EnvType.CLIENT)
	public static void preventRemotePlayersWalkingAnimations(PlayerEntity player) {
//		if (event.phase == Phase.START)
//			return;
		if (!(player instanceof OtherClientPlayerEntity))
			return;
		OtherClientPlayerEntity remotePlayer = (OtherClientPlayerEntity) player;
		NbtCompound data = remotePlayer.getCustomData();
		if (!data.contains("LastOverrideLimbSwingUpdate"))
			return;

		int lastOverride = data.getInt("LastOverrideLimbSwingUpdate");
		data.putInt("LastOverrideLimbSwingUpdate", lastOverride + 1);
		if (lastOverride > 5) {
			data.remove("LastOverrideLimbSwingUpdate");
			data.remove("OverrideLimbSwing");
			return;
		}

		float limbSwing = data.getFloat("OverrideLimbSwing");
		remotePlayer.prevX = remotePlayer.getX() - (limbSwing / 4);
		remotePlayer.prevZ = remotePlayer.getZ();
	}

	@Environment(EnvType.CLIENT)
	public static ActionResult rightClickingOnContraptionsGetsHandledLocally(MinecraftClient mc, HitResult result, Hand hand) {
		if (MinecraftClient.getInstance().currentScreen != null) // this is the only input event that doesn't check this?
			return ActionResult.PASS;

		ClientPlayerEntity player = mc.player;

		if (player == null)
			return ActionResult.PASS;
		if (player.isSpectator())
			return ActionResult.PASS;
		if (mc.world == null)
			return ActionResult.PASS;
		if (mc.interactionManager == null)
			return ActionResult.PASS;
//		if (!event.isUseItem())
//			return InteractionResult.PASS;

		Couple<Vec3d> rayInputs = getRayInputs(player);
		Vec3d origin = rayInputs.getFirst();
		Vec3d target = rayInputs.getSecond();
		Box aabb = new Box(origin, target).expand(16);

		Collection<WeakReference<AbstractContraptionEntity>> contraptions =
			ContraptionHandler.loadedContraptions.get(mc.world)
				.values();

		for (WeakReference<AbstractContraptionEntity> ref : contraptions) {
			AbstractContraptionEntity contraptionEntity = ref.get();
			if (contraptionEntity == null)
				continue;
			if (!contraptionEntity.getBoundingBox()
				.intersects(aabb))
				continue;

			BlockHitResult rayTraceResult = rayTraceContraption(origin, target, contraptionEntity);
			if (rayTraceResult == null)
				continue;

			Direction face = rayTraceResult.getSide();
			BlockPos pos = rayTraceResult.getBlockPos();

			if (contraptionEntity.handlePlayerInteraction(player, pos, face, hand)) {
				AllPackets.getChannel().sendToServer(new ContraptionInteractionPacket(contraptionEntity, hand, pos, face));
			} else if (handleSpecialInteractions(contraptionEntity, player, pos, face, hand)) {
			} else
				continue;

			return ActionResult.FAIL;
		}
		return ActionResult.PASS;
	}

	private static boolean handleSpecialInteractions(AbstractContraptionEntity contraptionEntity, PlayerEntity player,
		BlockPos localPos, Direction side, Hand interactionHand) {
		if (AllItems.WRENCH.isIn(player.getStackInHand(interactionHand))
			&& contraptionEntity instanceof CarriageContraptionEntity car)
			return TrainRelocator.carriageWrenched(car.toGlobalVector(VecHelper.getCenterOf(localPos), 1), car);
		return false;
	}

	@Environment(EnvType.CLIENT)
	public static Couple<Vec3d> getRayInputs(ClientPlayerEntity player) {
		MinecraftClient mc = MinecraftClient.getInstance();
		Vec3d origin = RaycastHelper.getTraceOrigin(player);
		double reach = ReachEntityAttributes.getReachDistance(player, mc.interactionManager.getReachDistance());
		if (mc.crosshairTarget != null && mc.crosshairTarget.getPos() != null)
			reach = Math.min(mc.crosshairTarget.getPos()
				.distanceTo(origin), reach);
		Vec3d target = RaycastHelper.getTraceTarget(player, reach, origin);
		return Couple.create(origin, target);
	}

	@Nullable
	public static BlockHitResult rayTraceContraption(Vec3d origin, Vec3d target,
		AbstractContraptionEntity contraptionEntity) {
		Vec3d localOrigin = contraptionEntity.toLocalVector(origin, 1);
		Vec3d localTarget = contraptionEntity.toLocalVector(target, 1);
		Contraption contraption = contraptionEntity.getContraption();

		MutableObject<BlockHitResult> mutableResult = new MutableObject<>();
		PredicateTraceResult predicateResult = RaycastHelper.rayTraceUntil(localOrigin, localTarget, p -> {
			for (Direction d : Iterate.directions) {
				if (d == Direction.UP)
					continue;
				BlockPos pos = d == Direction.DOWN ? p : p.offset(d);
				StructureBlockInfo blockInfo = contraption.getBlocks()
					.get(pos);
				if (blockInfo == null)
					continue;
				BlockState state = blockInfo.state();
				VoxelShape raytraceShape = state.getOutlineShape(contraption.getContraptionWorld(), BlockPos.ORIGIN.down());
				if (raytraceShape.isEmpty())
					continue;
				if (contraption.isHiddenInPortal(pos))
					continue;
				BlockHitResult rayTrace = raytraceShape.raycast(localOrigin, localTarget, pos);
				if (rayTrace != null) {
					mutableResult.setValue(rayTrace);
					return true;
				}
			}
			return false;
		});

		if (predicateResult == null || predicateResult.missed())
			return null;

		BlockHitResult rayTraceResult = mutableResult.getValue();
		return rayTraceResult;
	}

}
