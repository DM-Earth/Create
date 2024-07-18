package com.simibubi.create.foundation.mixin.fabric;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface GuiAccessor {
	@Accessor
	int getHeldItemTooltipFade();
}
