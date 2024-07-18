package com.simibubi.create.foundation.gui;

import net.minecraft.client.render.DiffuseLighting;

public interface ILightingSettings {

	void applyLighting();

	static final ILightingSettings DEFAULT_3D = () -> DiffuseLighting.enableGuiDepthLighting();
	static final ILightingSettings DEFAULT_FLAT = () -> DiffuseLighting.disableGuiDepthLighting();

}
