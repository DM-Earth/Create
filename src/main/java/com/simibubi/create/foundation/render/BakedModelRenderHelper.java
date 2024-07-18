package com.simibubi.create.foundation.render;

import com.jozufozu.flywheel.core.model.ModelUtil;
import com.jozufozu.flywheel.core.model.ShadeSeparatedBufferedData;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;

public class BakedModelRenderHelper {

	public static SuperByteBuffer standardBlockRender(BlockState renderedState) {
		BlockRenderManager dispatcher = MinecraftClient.getInstance()
				.getBlockRenderManager();
		return standardModelRender(dispatcher.getModel(renderedState), renderedState);
	}

	public static SuperByteBuffer standardModelRender(BakedModel model, BlockState referenceState) {
		return standardModelRender(model, referenceState, new MatrixStack());
	}

	public static SuperByteBuffer standardModelRender(BakedModel model, BlockState referenceState, MatrixStack ms) {
		ShadeSeparatedBufferedData data = ModelUtil.getBufferedData(model, referenceState, ms);
		SuperByteBuffer sbb = new SuperByteBuffer(data);
		data.release();
		return sbb;
	}

}
