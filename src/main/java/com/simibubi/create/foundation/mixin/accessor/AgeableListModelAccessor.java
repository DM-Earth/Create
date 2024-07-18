package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AnimalModel.class)
public interface AgeableListModelAccessor {
	@Invoker("getHeadParts")
	Iterable<ModelPart> create$callGetHeadParts();

	@Invoker("getBodyParts")
	Iterable<ModelPart> create$callGetBodyParts();
}
