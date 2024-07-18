package com.simibubi.create.content.trains.bogey;

import java.util.Optional;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

public final class BogeyInstance {
	private final BogeySizes.BogeySize size;
	private final BogeyStyle style;

	public final CarriageBogey bogey;
	public final BogeyRenderer renderer;
	public final Optional<BogeyRenderer.CommonRenderer> commonRenderer;

	public BogeyInstance(CarriageBogey bogey, BogeyStyle style, BogeySizes.BogeySize size,
		MaterialManager materialManager) {
		this.bogey = bogey;
		this.size = size;
		this.style = style;

		this.renderer = this.style.createRendererInstance(this.size);
		this.commonRenderer = this.style.getNewCommonRenderInstance();

		commonRenderer.ifPresent(bogeyRenderer -> bogeyRenderer.initialiseContraptionModelData(materialManager, bogey));
		renderer.initialiseContraptionModelData(materialManager, bogey);
	}

	public void beginFrame(float wheelAngle, MatrixStack ms) {
		if (ms == null) {
			renderer.emptyTransforms();
			return;
		}

		commonRenderer.ifPresent(bogeyRenderer -> bogeyRenderer.render(bogey.bogeyData, wheelAngle, ms));
		renderer.render(bogey.bogeyData, wheelAngle, ms);
	}

	public void updateLight(BlockRenderView world, CarriageContraptionEntity entity) {
		var lightPos = BlockPos.ofFloored(getLightPos(entity));
		commonRenderer
			.ifPresent(bogeyRenderer -> bogeyRenderer.updateLight(world.getLightLevel(LightType.BLOCK, lightPos),
				world.getLightLevel(LightType.SKY, lightPos)));
		renderer.updateLight(world.getLightLevel(LightType.BLOCK, lightPos),
			world.getLightLevel(LightType.SKY, lightPos));
	}

	private Vec3d getLightPos(CarriageContraptionEntity entity) {
		return bogey.getAnchorPosition() != null ? bogey.getAnchorPosition()
			: entity.getClientCameraPosVec(AnimationTickHolder.getPartialTicks());
	}

	@FunctionalInterface
	interface BogeyInstanceFactory {
		BogeyInstance create(CarriageBogey bogey, BogeySizes.BogeySize size, MaterialManager materialManager);
	}
}
