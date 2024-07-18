package com.simibubi.create.foundation.mixin.client;

import java.util.Set;
import java.util.SortedSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.collect.Sets;
import com.simibubi.create.foundation.block.render.BlockDestructionProgressExtension;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

@Mixin(WorldRenderer.class)
public class LevelRendererMixin {
	@Shadow
	private ClientWorld world;

	@Shadow
	@Final
	private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;

	@Inject(method = "setBlockBreakingInfo(ILnet/minecraft/util/math/BlockPos;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BlockBreakingInfo;setLastUpdateTick(I)V", shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
	private void create$onDestroyBlockProgress(int breakerId, BlockPos pos, int progress, CallbackInfo ci, BlockBreakingInfo progressObj) {
		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof MultiPosDestructionHandler handler) {
			Set<BlockPos> extraPositions = handler.getExtraPositions(world, pos, state, progress);
			if (extraPositions != null) {
				extraPositions.remove(pos);
				((BlockDestructionProgressExtension) progressObj).setExtraPositions(extraPositions);
				for (BlockPos extraPos : extraPositions) {
					blockBreakingProgressions.computeIfAbsent(extraPos.asLong(), l -> Sets.newTreeSet()).add(progressObj);
				}
			}
		}
	}

	@Inject(method = "removeBlockBreakingInfo(Lnet/minecraft/client/render/BlockBreakingInfo;)V", at = @At("RETURN"))
	private void create$onRemoveProgress(BlockBreakingInfo progress, CallbackInfo ci) {
		Set<BlockPos> extraPositions = ((BlockDestructionProgressExtension) progress).getExtraPositions();
		if (extraPositions != null) {
			for (BlockPos extraPos : extraPositions) {
				long l = extraPos.asLong();
				Set<BlockBreakingInfo> set = blockBreakingProgressions.get(l);
				if (set != null) {
					set.remove(progress);
					if (set.isEmpty()) {
						blockBreakingProgressions.remove(l);
					}
				}
			}
		}
	}
}
