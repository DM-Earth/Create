package com.simibubi.create.content.contraptions.behaviour.dispenser;

import java.util.HashMap;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.mixin.accessor.DispenserBlockAccessor;

public class DispenserMovementBehaviour extends DropperMovementBehaviour {
	private static final HashMap<Item, IMovedDispenseItemBehaviour> MOVED_DISPENSE_ITEM_BEHAVIOURS = new HashMap<>();
	private static final HashMap<Item, IMovedDispenseItemBehaviour> MOVED_PROJECTILE_DISPENSE_BEHAVIOURS = new HashMap<>();
	private static boolean spawnEggsRegistered = false;

	public static void gatherMovedDispenseItemBehaviours() {
		IMovedDispenseItemBehaviour.init();
	}

	public static void registerMovedDispenseItemBehaviour(Item item,
		IMovedDispenseItemBehaviour movedDispenseItemBehaviour) {
		MOVED_DISPENSE_ITEM_BEHAVIOURS.put(item, movedDispenseItemBehaviour);
	}

	public static DispenserBehavior getDispenseMethod(ItemStack itemstack) {
		return ((DispenserBlockAccessor) Blocks.DISPENSER).create$callGetBehaviorForItem(itemstack);
	}

	@Override
	protected void activate(MovementContext context, BlockPos pos) {
		if (!spawnEggsRegistered) {
			spawnEggsRegistered = true;
			IMovedDispenseItemBehaviour.initSpawnEggs();
		}

		DispenseItemLocation location = getDispenseLocation(context);
		if (location.isEmpty()) {
			context.world.syncWorldEvent(1001, pos, 0);
		} else {
			ItemStack itemStack = getItemStackAt(location, context);
			// Special dispense item behaviour for moving contraptions
			if (MOVED_DISPENSE_ITEM_BEHAVIOURS.containsKey(itemStack.getItem())) {
				setItemStackAt(location, MOVED_DISPENSE_ITEM_BEHAVIOURS.get(itemStack.getItem()).dispense(itemStack, context, pos), context);
				return;
			}

			ItemStack backup = itemStack.copy();
			// If none is there, try vanilla registry
			try {
				if (MOVED_PROJECTILE_DISPENSE_BEHAVIOURS.containsKey(itemStack.getItem())) {
					setItemStackAt(location, MOVED_PROJECTILE_DISPENSE_BEHAVIOURS.get(itemStack.getItem()).dispense(itemStack, context, pos), context);
					return;
				}

				DispenserBehavior behavior = getDispenseMethod(itemStack);
				if (behavior instanceof ProjectileDispenserBehavior) { // Projectile behaviours can be converted most of the time
					IMovedDispenseItemBehaviour movedBehaviour = MovedProjectileDispenserBehaviour.of((ProjectileDispenserBehavior) behavior);
					setItemStackAt(location, movedBehaviour.dispense(itemStack, context, pos), context);
					MOVED_PROJECTILE_DISPENSE_BEHAVIOURS.put(itemStack.getItem(), movedBehaviour); // buffer conversion if successful
					return;
				}

				Vec3d facingVec = Vec3d.of(context.state.get(DispenserBlock.FACING).getVector());
				facingVec = context.rotation.apply(facingVec);
				facingVec.normalize();
				Direction clostestFacing = Direction.getFacing(facingVec.x, facingVec.y, facingVec.z);
				ContraptionBlockSource blockSource = new ContraptionBlockSource(context, pos, clostestFacing);

				if (behavior.getClass() != ItemDispenserBehavior.class) { // There is a dispense item behaviour registered for the vanilla dispenser
					setItemStackAt(location, behavior.dispense(blockSource, itemStack), context);
					return;
				}
			} catch (NullPointerException ignored) {
				itemStack = backup; // Something went wrong with the BE being null in ContraptionBlockSource, reset the stack
			}

			setItemStackAt(location, DEFAULT_BEHAVIOUR.dispense(itemStack, context, pos), context);  // the default: launch the item
		}
	}

}
