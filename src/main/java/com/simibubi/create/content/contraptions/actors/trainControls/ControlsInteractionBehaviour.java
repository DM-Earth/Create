package com.simibubi.create.content.contraptions.actors.trainControls;

import java.util.UUID;

import com.google.common.base.Objects;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;

import com.simibubi.create.foundation.utility.AdventureUtil;
import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class ControlsInteractionBehaviour extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(PlayerEntity player, Hand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		if (AdventureUtil.isAdventure(player))
			return false;
		if (AllItems.WRENCH.isIn(player.getStackInHand(activeHand)))
			return false;

		UUID currentlyControlling = contraptionEntity.getControllingPlayer()
			.orElse(null);

		if (currentlyControlling != null) {
			contraptionEntity.stopControlling(localPos);
			if (Objects.equal(currentlyControlling, player.getUuid()))
				return true;
		}

		if (!contraptionEntity.startControlling(localPos, player))
			return false;

		contraptionEntity.setControllingPlayer(player.getUuid());
		if (player.getWorld().isClient)
			EnvExecutor.runWhenOn(EnvType.CLIENT,
				() -> () -> ControlsHandler.startControlling(contraptionEntity, localPos));
		return true;
	}

}
