package com.simibubi.create.content.fluids;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class FluidBottleItemHook extends Item {

	private FluidBottleItemHook(Settings p) {
		super(p);
	}

	public static ActionResult preventWaterBottlesFromCreatesFluids(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isEmpty())
			return ActionResult.PASS;
		if (!(itemStack.getItem() instanceof GlassBottleItem))
			return ActionResult.PASS;

		if (player.isSpectator()) // forge checks this, fabric does not
			return ActionResult.PASS;

//		Level world = event.getWorld();
//		Player player = event.getPlayer();
		HitResult raytraceresult = raycast(world, player, RaycastContext.FluidHandling.SOURCE_ONLY);
		if (raytraceresult.getType() != HitResult.Type.BLOCK)
			return ActionResult.PASS;
		BlockPos blockpos = ((BlockHitResult) raytraceresult).getBlockPos();
		if (!world.canPlayerModifyAt(player, blockpos))
			return ActionResult.PASS;

		FluidState fluidState = world.getFluidState(blockpos);
		if (fluidState.isIn(FluidTags.WATER) && RegisteredObjects.getKeyOrThrow(fluidState.getFluid())
			.getNamespace()
			.equals(Create.ID)) {
			return ActionResult.FAIL;
		}

		return ActionResult.PASS;
	}

}
