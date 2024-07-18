package com.simibubi.create.content.equipment.bell;

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.block.BellBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Attachment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class BellRenderer<BE extends AbstractBellBlockEntity> extends SafeBlockEntityRenderer<BE> {

	public BellRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	protected void renderSafe(BE be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
		BlockState state = be.getCachedState();
		Direction facing = state.get(BellBlock.FACING);
		Attachment attachment = state.get(BellBlock.ATTACHMENT);

		SuperByteBuffer bell = CachedBufferer.partial(be.getBellModel(), state);

		if (be.isRinging)
			bell.rotateCentered(be.ringDirection.rotateYCounterclockwise(), getSwingAngle(be.ringingTicks + partialTicks));

		float rY = AngleHelper.horizontalAngle(facing);
		if (attachment == Attachment.SINGLE_WALL || attachment == Attachment.DOUBLE_WALL)
			rY += 90;
		bell.rotateCentered(Direction.UP, AngleHelper.rad(rY));

		bell.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getCutout()));
	}

	public static float getSwingAngle(float time) {
		float t = time / 1.5f;
		return 1.2f * MathHelper.sin(t / (float) Math.PI) / (2.5f + t / 3.0f);
	}

}
