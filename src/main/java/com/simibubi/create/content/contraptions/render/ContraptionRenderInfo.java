package com.simibubi.create.content.contraptions.render;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ContraptionRenderInfo {
	public final Contraption contraption;
	public final VirtualRenderWorld renderWorld;

	private final ContraptionMatrices matrices = new ContraptionMatrices();
	private boolean visible;

	public ContraptionRenderInfo(Contraption contraption, VirtualRenderWorld renderWorld) {
		this.contraption = contraption;
		this.renderWorld = renderWorld;
	}

	public int getEntityId() {
		return contraption.entity.getId();
	}

	public boolean isDead() {
		return !contraption.entity.isAliveOrStale();
	}

	public void beginFrame(BeginFrameEvent event) {
		matrices.clear();

		AbstractContraptionEntity entity = contraption.entity;

		visible = event.getFrustum()
			.isVisible(entity.getVisibilityBoundingBox()
				.expand(2));
	}

	public boolean isVisible() {
		return visible && contraption.entity.isAliveOrStale() && contraption.entity.isReadyForRender();
	}

	/**
	 * Need to call this during RenderLayerEvent.
	 */
	public void setupMatrices(MatrixStack viewProjection, double camX, double camY, double camZ) {
		if (!matrices.isReady()) {
			AbstractContraptionEntity entity = contraption.entity;

			viewProjection.push();

			double x = MathHelper.lerp(AnimationTickHolder.getPartialTicks(), entity.lastRenderX, entity.getX()) - camX;
			double y = MathHelper.lerp(AnimationTickHolder.getPartialTicks(), entity.lastRenderY, entity.getY()) - camY;
			double z = MathHelper.lerp(AnimationTickHolder.getPartialTicks(), entity.lastRenderZ, entity.getZ()) - camZ;

			viewProjection.translate(x, y, z);

			matrices.setup(viewProjection, entity);

			viewProjection.pop();
		}
	}

	/**
	 * If #setupMatrices is called correctly, the returned matrices will be ready
	 */
	public ContraptionMatrices getMatrices() {
		return matrices;
	}

	public void invalidate() {

	}
}
