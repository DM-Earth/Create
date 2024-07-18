package com.simibubi.create.content.equipment.armor;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class BacktankRenderer extends KineticBlockEntityRenderer<BacktankBlockEntity> {
	public BacktankRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BacktankBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		BlockState blockState = be.getCachedState();
		SuperByteBuffer cogs = CachedBufferer.partial(getCogsModel(blockState), blockState);
		cogs.centre()
			.rotateY(180 + AngleHelper.horizontalAngle(blockState.get(BacktankBlock.HORIZONTAL_FACING)))
			.unCentre()
			.translate(0, 6.5f / 16, 11f / 16)
			.rotate(Direction.EAST,
				AngleHelper.rad(be.getSpeed() / 4f * AnimationTickHolder.getRenderTime(be.getWorld()) % 360))
			.translate(0, -6.5f / 16, -11f / 16);
		cogs.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
	}

	@Override
	protected SuperByteBuffer getRotatedModel(BacktankBlockEntity be, BlockState state) {
		return CachedBufferer.partial(getShaftModel(state), state);
	}

	public static PartialModel getCogsModel(BlockState state) {
		if (AllBlocks.NETHERITE_BACKTANK.has(state)) {
			return AllPartialModels.NETHERITE_BACKTANK_COGS;
		}
		return AllPartialModels.COPPER_BACKTANK_COGS;
	}

	public static PartialModel getShaftModel(BlockState state) {
		if (AllBlocks.NETHERITE_BACKTANK.has(state)) {
			return AllPartialModels.NETHERITE_BACKTANK_SHAFT;
		}
		return AllPartialModels.COPPER_BACKTANK_SHAFT;
	}
}
