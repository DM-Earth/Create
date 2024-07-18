package com.simibubi.create.content.trains.bogey;

import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;

public class BackupBogeyRenderer extends BogeyRenderer.CommonRenderer {
	public static BackupBogeyRenderer INSTANCE = new BackupBogeyRenderer();

	@Override
	public void render(NbtCompound bogeyData, float wheelAngle, MatrixStack ms, int light, VertexConsumer vb, boolean inContraption) {

	}

	@Override
	public void initialiseContraptionModelData(MaterialManager materialManager, CarriageBogey carriageBogey) {

	}
}
