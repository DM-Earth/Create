package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class ToolboxRenderer extends SmartBlockEntityRenderer<ToolboxBlockEntity> {

	public ToolboxRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ToolboxBlockEntity blockEntity, float partialTicks, MatrixStack ms,
		VertexConsumerProvider buffer, int light, int overlay) {

		BlockState blockState = blockEntity.getCachedState();
		Direction facing = blockState.get(ToolboxBlock.FACING)
			.getOpposite();
		SuperByteBuffer lid =
			CachedBufferer.partial(AllPartialModels.TOOLBOX_LIDS.get(blockEntity.getColor()), blockState);
		SuperByteBuffer drawer = CachedBufferer.partial(AllPartialModels.TOOLBOX_DRAWER, blockState);

		float lidAngle = blockEntity.lid.getValue(partialTicks);
		float drawerOffset = blockEntity.drawers.getValue(partialTicks);

		VertexConsumer builder = buffer.getBuffer(RenderLayer.getCutoutMipped());
		lid.centre()
			.rotateY(-facing.asRotation())
			.unCentre()
			.translate(0, 6 / 16f, 12 / 16f)
			.rotateX(135 * lidAngle)
			.translate(0, -6 / 16f, -12 / 16f)
			.light(light)
			.renderInto(ms, builder);

		for (int offset : Iterate.zeroAndOne) {
			drawer.centre()
					.rotateY(-facing.asRotation())
					.unCentre()
					.translate(0, offset * 1 / 8f, -drawerOffset * .175f * (2 - offset))
					.light(light)
					.renderInto(ms, builder);
		}

	}

}
