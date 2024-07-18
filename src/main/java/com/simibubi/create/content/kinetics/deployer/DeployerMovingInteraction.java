package com.simibubi.create.content.kinetics.deployer;

import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import com.simibubi.create.foundation.utility.AdventureUtil;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

public class DeployerMovingInteraction extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(PlayerEntity player, Hand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		if (AdventureUtil.isAdventure(player))
			return false;
		MutablePair<StructureBlockInfo, MovementContext> actor = contraptionEntity.getContraption()
			.getActorAt(localPos);
		if (actor == null || actor.right == null)
			return false;

		MovementContext ctx = actor.right;
		ItemStack heldStack = player.getStackInHand(activeHand);
		if (heldStack.getItem()
				.equals(AllItems.WRENCH.get())) {
			DeployerBlockEntity.Mode mode = NBTHelper.readEnum(ctx.blockEntityData, "Mode", DeployerBlockEntity.Mode.class);
			NBTHelper.writeEnum(ctx.blockEntityData, "Mode",
				mode == DeployerBlockEntity.Mode.PUNCH ? DeployerBlockEntity.Mode.USE : DeployerBlockEntity.Mode.PUNCH);

		} else {
			if (ctx.world.isClient)
				return true; // we'll try again on the server side
			DeployerFakePlayer fake = null;

			if (!(ctx.temporaryData instanceof DeployerFakePlayer) && ctx.world instanceof ServerWorld) {
				UUID owner = ctx.blockEntityData.contains("Owner") ? ctx.blockEntityData.getUuid("Owner") : null;
				DeployerFakePlayer deployerFakePlayer = new DeployerFakePlayer((ServerWorld) ctx.world, owner);
				deployerFakePlayer.onMinecartContraption = ctx.contraption instanceof MountedContraption;
				deployerFakePlayer.getInventory()
					.readNbt(ctx.blockEntityData.getList("Inventory", NbtElement.COMPOUND_TYPE));
				ctx.temporaryData = fake = deployerFakePlayer;
				ctx.blockEntityData.remove("Inventory");
			} else
				fake = (DeployerFakePlayer) ctx.temporaryData;

			if (fake == null)
				return false;

			ItemStack deployerItem = fake.getMainHandStack();
			player.setStackInHand(activeHand, deployerItem.copy());
			fake.setStackInHand(Hand.MAIN_HAND, heldStack.copy());
			ctx.blockEntityData.put("HeldItem", NBTSerializer.serializeNBT(heldStack));
			ctx.data.put("HeldItem", NBTSerializer.serializeNBT(heldStack));
		}
//		if (index >= 0)
//			setContraptionActorData(contraptionEntity, index, info, ctx);
		return true;
	}
}
