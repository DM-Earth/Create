package com.simibubi.create.content.kinetics.gauge;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

public class SpeedGaugeBlockEntity extends GaugeBlockEntity {

	public AbstractComputerBehaviour computerBehaviour;

	public SpeedGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		float speed = Math.abs(getSpeed());

		dialTarget = getDialTarget(speed);
		color = Color.mixColors(SpeedLevel.of(speed)
			.getColor(), 0xffffff, .25f);

		markDirty();
	}

	public static float getDialTarget(float speed) {
		speed = Math.abs(speed);
		float medium = AllConfigs.server().kinetics.mediumSpeed.get()
			.floatValue();
		float fast = AllConfigs.server().kinetics.fastSpeed.get()
			.floatValue();
		float max = AllConfigs.server().kinetics.maxRotationSpeed.get()
			.floatValue();
		float target = 0;
		if (speed == 0)
			target = 0;
		else if (speed < medium)
			target = MathHelper.lerp(speed / medium, 0, .45f);
		else if (speed < fast)
			target = MathHelper.lerp((speed - medium) / (fast - medium), .45f, .75f);
		else
			target = MathHelper.lerp((speed - fast) / (max - fast), .75f, 1.125f);
		return target;
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		Lang.translate("gui.speedometer.title")
			.style(Formatting.GRAY)
			.forGoggles(tooltip);
		SpeedLevel.getFormattedSpeedText(speed, isOverStressed())
			.forGoggles(tooltip);
		return true;
	}
}
