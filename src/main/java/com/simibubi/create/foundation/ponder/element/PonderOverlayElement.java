package com.simibubi.create.foundation.ponder.element;

import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import net.minecraft.client.gui.DrawContext;

public abstract class PonderOverlayElement extends PonderElement {

	public void tick(PonderScene scene) {}

	public abstract void render(PonderScene scene, PonderUI screen, DrawContext graphics, float partialTicks);

}
