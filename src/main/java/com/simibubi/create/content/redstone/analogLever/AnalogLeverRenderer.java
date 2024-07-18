package com.simibubi.create.content.redstone.analogLever;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class AnalogLeverRenderer extends SafeBlockEntityRenderer<AnalogLeverBlockEntity> {

	public AnalogLeverRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	protected void renderSafe(AnalogLeverBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {

		if (Backend.canUseInstancing(be.getWorld())) return;

		BlockState leverState = be.getCachedState();
		float state = be.clientState.getValue(partialTicks);

		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());

		// Handle
		SuperByteBuffer handle = CachedBufferer.partial(AllPartialModels.ANALOG_LEVER_HANDLE, leverState);
		float angle = (float) ((state / 15) * 90 / 180 * Math.PI);
		transform(handle, leverState).translate(1 / 2f, 1 / 16f, 1 / 2f)
				.rotate(Direction.EAST, angle)
				.translate(-1 / 2f, -1 / 16f, -1 / 2f);
		handle.light(light)
				.renderInto(ms, vb);

		// Indicator
		int color = Color.mixColors(0x2C0300, 0xCD0000, state / 15f);
		SuperByteBuffer indicator = transform(CachedBufferer.partial(AllPartialModels.ANALOG_LEVER_INDICATOR, leverState), leverState);
		indicator.light(light)
				.color(color)
				.renderInto(ms, vb);
	}

	private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState leverState) {
		WallMountLocation face = leverState.get(AnalogLeverBlock.FACE);
		float rX = face == WallMountLocation.FLOOR ? 0 : face == WallMountLocation.WALL ? 90 : 180;
		float rY = AngleHelper.horizontalAngle(leverState.get(AnalogLeverBlock.FACING));
		buffer.rotateCentered(Direction.UP, (float) (rY / 180 * Math.PI));
		buffer.rotateCentered(Direction.EAST, (float) (rX / 180 * Math.PI));
		return buffer;
	}

}
