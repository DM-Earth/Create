package com.simibubi.create.foundation.mixin.fabric;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.StructureTestUtil;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StructureTestUtil.class, priority = 900) // apply before FAPI, run earlier
public class StructureUtilsMixin {
	/**
	 * this is what vanilla and forge do, but FAPI forces a different system
	 * @see StructureTestUtilMixin
 	 */
	@Inject(method = "createStructureTemplate(Ljava/lang/String;Lnet/minecraft/server/world/ServerWorld;)Lnet/minecraft/structure/StructureTemplate;", at = @At("HEAD"), cancellable = true)
	private static void useStructureManager(String name, ServerWorld level, CallbackInfoReturnable<StructureTemplate> cir) {
		Identifier id = new Identifier(name);
		level.getStructureTemplateManager().getTemplate(id).ifPresent(cir::setReturnValue);
	}
}
