package com.simibubi.create.content.contraptions.behaviour;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public abstract class MovingInteractionBehaviour {

	protected void setContraptionActorData(AbstractContraptionEntity contraptionEntity, int index,
		StructureBlockInfo info, MovementContext ctx) {
		contraptionEntity.getContraption().getActors().remove(index);
		contraptionEntity.getContraption().getActors().add(index, MutablePair.of(info, ctx));
		if (contraptionEntity.getWorld().isClient)
			contraptionEntity.getContraption().deferInvalidate = true;
	}

	protected void setContraptionBlockData(AbstractContraptionEntity contraptionEntity, BlockPos pos,
		StructureBlockInfo info) {
		if (contraptionEntity.getWorld().isClient())
			return;
		contraptionEntity.setBlock(pos, info);
	}

	public boolean handlePlayerInteraction(PlayerEntity player, Hand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		return true;
	}

	public void handleEntityCollision(Entity entity, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {}

}
