package com.simibubi.create.foundation.mixin.fabric;

import net.minecraft.data.server.tag.TagProvider.ProvidedTagBuilder;
import net.minecraft.registry.tag.TagBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProvidedTagBuilder.class)
public interface TagAppenderAccessor {
	@Accessor
	TagBuilder getBuilder();
}
