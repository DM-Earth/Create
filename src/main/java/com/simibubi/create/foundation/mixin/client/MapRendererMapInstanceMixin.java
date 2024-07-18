package com.simibubi.create.foundation.mixin.client;

import java.util.Iterator;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import com.google.common.collect.Iterators;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.simibubi.create.foundation.map.CustomRenderedMapDecoration;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// fabric: we have an AW for it, and compiler complains if specified by string
@Mixin(MapRenderer.MapTexture.class)
public class MapRendererMapInstanceMixin {
	@Shadow
	private MapState state;

	// fabric: completely redone

	@ModifyExpressionValue(
		method = "draw",
		at = @At(
				value = "INVOKE",
				target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;"
		)
	)
	private Iterator<MapIcon> wrapIterator(Iterator<MapIcon> original) {
		// skip rendering custom ones in the main loop
		return Iterators.filter(original, decoration -> !(decoration instanceof CustomRenderedMapDecoration));
	}

	@Inject(method = "draw", at = @At("TAIL"))
	private void renderCustomDecorations(MatrixStack poseStack, VertexConsumerProvider bufferSource, boolean active,
										 int packedLight, CallbackInfo ci, @Local(ordinal = 3) int index) { // ignore error, works
		// render custom ones in second loop
		for (MapIcon decoration : this.state.getIcons()) {
			if (decoration instanceof CustomRenderedMapDecoration renderer) {
				renderer.render(poseStack, bufferSource, active, packedLight, state, index);
			}
		}
	}
}
