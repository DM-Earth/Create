package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelStructuralBlock;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockView;

@Mixin(FlowableFluid.class)
public class WaterWheelFluidSpreadMixin {
	@Inject(method = "canFlowThrough(Lnet/minecraft/world/BlockView;Lnet/minecraft/fluid/Fluid;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z", at = @At("HEAD"), cancellable = true)
	protected void create$canPassThroughOnWaterWheel(BlockView pLevel, Fluid pFluid, BlockPos pFromPos, BlockState p_75967_,
		Direction pDirection, BlockPos p_75969_, BlockState p_75970_, FluidState p_75971_,
		CallbackInfoReturnable<Boolean> cir) {

		if (pDirection.getAxis() == Axis.Y)
			return;

		BlockPos belowPos = pFromPos.down();
		BlockState belowState = pLevel.getBlockState(belowPos);

		if (AllBlocks.WATER_WHEEL_STRUCTURAL.has(belowState)) {
			if (AllBlocks.WATER_WHEEL_STRUCTURAL.get()
				.stillValid(pLevel, belowPos, belowState, false))
				belowState = pLevel.getBlockState(WaterWheelStructuralBlock.getMaster(pLevel, belowPos, belowState));
		} else if (!AllBlocks.WATER_WHEEL.has(belowState))
			return;

		if (belowState.getBlock() instanceof IRotate irotate
			&& irotate.getRotationAxis(belowState) == pDirection.getAxis())
			cir.setReturnValue(false);
	}
}
