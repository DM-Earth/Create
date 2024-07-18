package com.simibubi.create.content.contraptions.actors.contraptionControls;

import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.foundation.utility.AdventureUtil;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorTargetFloorPacket;

public class ContraptionControlsMovingInteraction extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(PlayerEntity player, Hand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		Contraption contraption = contraptionEntity.getContraption();

		MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(localPos);
		if (actor == null)
			return false;
		MovementContext ctx = actor.right;
		if (ctx == null)
			return false;
		if (contraption instanceof ElevatorContraption ec)
			return elevatorInteraction(localPos, contraptionEntity, ec, ctx);

		if (AdventureUtil.isAdventure(player))
			return false;

		if (contraptionEntity.getWorld().isClient()) {
			if (contraption.presentBlockEntities.get(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
				cbe.pressButton();
			return true;
		}

		ItemStack filter = ContraptionControlsMovement.getFilter(ctx);
		boolean disable = true;
		boolean invert = false;

		List<ItemStack> disabledActors = contraption.getDisabledActors();
		for (Iterator<ItemStack> iterator = disabledActors.iterator(); iterator.hasNext();) {
			ItemStack presentFilter = iterator.next();
			boolean sameFilter = ContraptionControlsMovement.isSameFilter(presentFilter, filter);
			if (presentFilter.isEmpty()) {
				iterator.remove();
				disable = false;
				if (!sameFilter)
					invert = true;
				continue;
			}
			if (!sameFilter)
				continue;
			iterator.remove();
			disable = false;
			break;
		}

		if (invert) {
			for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
				MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(pair.left.state());
				if (behaviour == null)
					continue;
				ItemStack behaviourStack = behaviour.canBeDisabledVia(pair.right);
				if (behaviourStack == null)
					continue;
				if (ContraptionControlsMovement.isSameFilter(behaviourStack, filter))
					continue;
				if (contraption.isActorTypeDisabled(behaviourStack))
					continue;
				disabledActors.add(behaviourStack);
				send(contraptionEntity, behaviourStack, true);
			}
		}

		if (filter.isEmpty())
			disabledActors.clear();
		if (disable)
			disabledActors.add(filter);

		contraption.setActorsActive(filter, !disable);
		ContraptionControlsBlockEntity.sendStatus(player, filter, !disable);
		send(contraptionEntity, filter, disable);

		AllSoundEvents.CONTROLLER_CLICK.play(player.getWorld(), null,
			BlockPos.ofFloored(contraptionEntity.toGlobalVector(Vec3d.ofCenter(localPos), 1)), 1, disable ? 0.8f : 1.5f);

		return true;
	}

	private void send(AbstractContraptionEntity contraptionEntity, ItemStack filter, boolean disable) {
		AllPackets.getChannel().sendToClientsTracking(
			new ContraptionDisableActorPacket(contraptionEntity.getId(), filter, !disable), contraptionEntity
		);
	}

	private boolean elevatorInteraction(BlockPos localPos, AbstractContraptionEntity contraptionEntity,
		ElevatorContraption contraption, MovementContext ctx) {
		World level = contraptionEntity.getWorld();
		if (!level.isClient()) {
			BlockPos pos = BlockPos.ofFloored(contraptionEntity.toGlobalVector(Vec3d.ofCenter(localPos), 1));
			AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, 1.5f);
			AllSoundEvents.CONTRAPTION_ASSEMBLE.play(level, null, pos, 0.75f, 0.8f);
			return true;
		}
		if (!(ctx.temporaryData instanceof ElevatorFloorSelection efs))
			return false;
		if (efs.currentTargetY == contraption.clientYTarget)
			return true;

		AllPackets.getChannel().sendToServer(new ElevatorTargetFloorPacket(contraptionEntity, efs.currentTargetY));
		if (contraption.presentBlockEntities.get(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
			cbe.pressButton();
		return true;
	}

}
