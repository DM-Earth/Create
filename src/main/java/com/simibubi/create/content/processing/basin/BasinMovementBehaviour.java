package com.simibubi.create.content.processing.basin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;

public class BasinMovementBehaviour implements MovementBehaviour {
	public Map<String, ItemStackHandler> getOrReadInventory(MovementContext context) {
		Map<String, ItemStackHandler> map = new HashMap<>();
		map.put("InputItems", new ItemStackHandler(9));
		map.put("OutputItems", new ItemStackHandler(8));
		map.forEach((s, h) -> h.deserializeNBT(context.blockEntityData.getCompound(s)));
		return map;
	}

	@Override
	public boolean renderAsNormalBlockEntity() {
		return true;
	}

	@Override
	public void tick(MovementContext context) {
		MovementBehaviour.super.tick(context);
		if (context.temporaryData == null || (boolean) context.temporaryData) {
			Vec3d facingVec = context.rotation.apply(Vec3d.of(Direction.UP.getVector()));
			facingVec.normalize();
			if (Direction.getFacing(facingVec.x, facingVec.y, facingVec.z) == Direction.DOWN)
				dump(context, facingVec);
		}
	}

	private void dump(MovementContext context, Vec3d facingVec) {
		getOrReadInventory(context).forEach((key, itemStackHandler) -> {
			for (int i = 0; i < itemStackHandler.getSlotCount(); i++) {
				if (itemStackHandler.getStackInSlot(i)
					.isEmpty())
					continue;
				ItemEntity itemEntity = new ItemEntity(context.world, context.position.x, context.position.y,
					context.position.z, itemStackHandler.getStackInSlot(i));
				itemEntity.setVelocity(facingVec.multiply(.05));
				context.world.spawnEntity(itemEntity);
				itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
			}
			context.blockEntityData.put(key, itemStackHandler.serializeNBT());
		});
		BlockEntity blockEntity = context.contraption.presentBlockEntities.get(context.localPos);
		if (blockEntity instanceof BasinBlockEntity)
			((BasinBlockEntity) blockEntity).readOnlyItems(context.blockEntityData);
		context.temporaryData = false; // did already dump, so can't any more
	}
}
