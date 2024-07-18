package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;

public class LargeWaterWheelBlockItem extends BlockItem {

	public LargeWaterWheelBlockItem(Block pBlock, Settings pProperties) {
		super(pBlock, pProperties);
	}

	@Override
	public ActionResult place(ItemPlacementContext ctx) {
		ActionResult result = super.place(ctx);
		if (result != ActionResult.FAIL)
			return result;
		Direction clickedFace = ctx.getSide();
		if (clickedFace.getAxis() != ((LargeWaterWheelBlock) getBlock()).getAxisForPlacement(ctx))
			result = super.place(ItemPlacementContext.offset(ctx, ctx.getBlockPos()
				.offset(clickedFace), clickedFace));
		if (result == ActionResult.FAIL && ctx.getWorld()
			.isClient())
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> showBounds(ctx));
		return result;
	}

	@Environment(EnvType.CLIENT)
	public void showBounds(ItemPlacementContext context) {
		BlockPos pos = context.getBlockPos();
		Axis axis = ((LargeWaterWheelBlock) getBlock()).getAxisForPlacement(context);
		Vec3d contract = Vec3d.of(Direction.get(AxisDirection.POSITIVE, axis)
			.getVector());
		if (!(context.getPlayer()instanceof ClientPlayerEntity localPlayer))
			return;
		CreateClient.OUTLINER.showAABB(Pair.of("waterwheel", pos), new Box(pos).expand(1)
			.contract(contract.x, contract.y, contract.z))
			.colored(0xFF_ff5d6c);
		Lang.translate("large_water_wheel.not_enough_space")
			.color(0xFF_ff5d6c)
			.sendStatus(localPlayer);
	}

}
