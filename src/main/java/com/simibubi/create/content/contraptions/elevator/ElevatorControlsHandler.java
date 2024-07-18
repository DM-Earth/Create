package com.simibubi.create.content.contraptions.elevator;

import java.lang.ref.WeakReference;
import java.util.Collection;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity.ControlsSlot;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.Couple;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElevatorControlsHandler {

	private static ControlsSlot slot = new ElevatorControlsSlot();

	private static class ElevatorControlsSlot extends ContraptionControlsBlockEntity.ControlsSlot {

		@Override
		public boolean testHit(BlockState state, Vec3d localHit) {
			Vec3d offset = getLocalOffset(state);
			if (offset == null)
				return false;
			return localHit.distanceTo(offset) < scale * .85;
		}

	}

	@Environment(EnvType.CLIENT)
	public static boolean onScroll(double delta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;

		if (player == null)
			return false;
		if (player.isSpectator())
			return false;
		if (mc.world == null)
			return false;

		Couple<Vec3d> rayInputs = ContraptionHandlerClient.getRayInputs(player);
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

			Contraption contraption = contraptionEntity.getContraption();
			if (!(contraption instanceof ElevatorContraption ec))
				continue;

			if (!contraptionEntity.getBoundingBox()
				.intersects(aabb))
				continue;

			BlockHitResult rayTraceResult =
				ContraptionHandlerClient.rayTraceContraption(origin, target, contraptionEntity);
			if (rayTraceResult == null)
				continue;

			BlockPos pos = rayTraceResult.getBlockPos();
			StructureBlockInfo info = contraption.getBlocks()
				.get(pos);

			if (info == null)
				continue;
			if (!AllBlocks.CONTRAPTION_CONTROLS.has(info.state()))
				continue;

			if (!slot.testHit(info.state(), rayTraceResult.getPos()
				.subtract(Vec3d.of(pos))))
				continue;

			MovementContext ctx = null;
			for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
				if (info.equals(pair.left)) {
					ctx = pair.right;
					break;
				}
			}

			if (!(ctx.temporaryData instanceof ElevatorFloorSelection))
				ctx.temporaryData = new ElevatorFloorSelection();

			ElevatorFloorSelection efs = (ElevatorFloorSelection) ctx.temporaryData;
			int prev = efs.currentIndex;
			efs.currentIndex += delta;
			ContraptionControlsMovement.tickFloorSelection(efs, ec);

			if (prev != efs.currentIndex && !ec.namesList.isEmpty()) {
				float pitch = (efs.currentIndex) / (float) (ec.namesList.size());
				pitch = MathHelper.lerp(pitch, 1f, 1.5f);
				AllSoundEvents.SCROLL_VALUE.play(mc.player.getWorld(), mc.player,
					BlockPos.ofFloored(contraptionEntity.toGlobalVector(rayTraceResult.getPos(), 1)), 1, pitch);
			}

			return true;
		}

		return false;
	}

}
