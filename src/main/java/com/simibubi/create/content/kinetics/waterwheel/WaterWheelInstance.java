package com.simibubi.create.content.kinetics.waterwheel;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.model.BlockModel;
import com.simibubi.create.content.kinetics.base.CutoutRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;

public class WaterWheelInstance<T extends WaterWheelBlockEntity> extends CutoutRotatingInstance<T> {
	protected final boolean large;
	protected final WaterWheelModelKey key;

	public WaterWheelInstance(MaterialManager materialManager, T blockEntity, boolean large) {
		super(materialManager, blockEntity);
		this.large = large;
		key = new WaterWheelModelKey(large, getRenderedBlockState(), blockEntity.material);
	}

	public static <T extends WaterWheelBlockEntity> WaterWheelInstance<T> standard(MaterialManager materialManager, T blockEntity) {
		return new WaterWheelInstance<>(materialManager, blockEntity, false);
	}

	public static <T extends WaterWheelBlockEntity> WaterWheelInstance<T> large(MaterialManager materialManager, T blockEntity) {
		return new WaterWheelInstance<>(materialManager, blockEntity, true);
	}

	@Override
	public boolean shouldReset() {
		return super.shouldReset() || key.material() != blockEntity.material;
	}

	@Override
	protected Instancer<RotatingData> getModel() {
		return getRotatingMaterial().model(key, () -> {
			BakedModel model = WaterWheelRenderer.generateModel(key);
			BlockState state = key.state();
			Direction dir;
			if (key.large()) {
				dir = Direction.from(state.get(LargeWaterWheelBlock.AXIS), AxisDirection.POSITIVE);
			} else {
				dir = state.get(WaterWheelBlock.FACING);
			}
			MatrixStack transform = CachedBufferer.rotateToFaceVertical(dir).get();
			return BlockModel.of(model, Blocks.AIR.getDefaultState(), transform);
		});
	}
}
