package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.WorldView;

public interface IRotate extends IWrenchable {

	enum SpeedLevel {
		NONE(Formatting.DARK_GRAY, 0x000000, 0),
		SLOW(Formatting.GREEN, 0x22FF22, 10),
		MEDIUM(Formatting.AQUA, 0x0084FF, 20),
		FAST(Formatting.LIGHT_PURPLE, 0xFF55FF, 30);

		private final Formatting textColor;
		private final int color;
		private final int particleSpeed;

		SpeedLevel(Formatting textColor, int color, int particleSpeed) {
			this.textColor = textColor;
			this.color = color;
			this.particleSpeed = particleSpeed;
		}

		public Formatting getTextColor() {
			return textColor;
		}

		public int getColor() {
			return color;
		}

		public int getParticleSpeed() {
			return particleSpeed;
		}

		public float getSpeedValue() {
			switch (this) {
			case FAST:
				return AllConfigs.server().kinetics.fastSpeed.get()
					.floatValue();
			case MEDIUM:
				return AllConfigs.server().kinetics.mediumSpeed.get()
					.floatValue();
			case SLOW:
				return 1;
			case NONE:
			default:
				return 0;
			}
		}

		public static SpeedLevel of(float speed) {
			speed = Math.abs(speed);

			if (speed >= AllConfigs.server().kinetics.fastSpeed.get())
				return FAST;
			if (speed >= AllConfigs.server().kinetics.mediumSpeed.get())
				return MEDIUM;
			if (speed >= 1)
				return SLOW;
			return NONE;
		}

		public static LangBuilder getFormattedSpeedText(float speed, boolean overstressed) {
			SpeedLevel speedLevel = of(speed);
			LangBuilder builder = Lang.text(TooltipHelper.makeProgressBar(3, speedLevel.ordinal()));

			builder.translate("tooltip.speedRequirement." + Lang.asId(speedLevel.name()))
				.space()
				.text("(")
				.add(Lang.number(Math.abs(speed)))
				.space()
				.translate("generic.unit.rpm")
				.text(")")
				.space();

			if (overstressed)
				builder.style(Formatting.DARK_GRAY)
					.style(Formatting.STRIKETHROUGH);
			else
				builder.style(speedLevel.getTextColor());

			return builder;
		}

	}

	enum StressImpact {
		LOW(Formatting.YELLOW, Formatting.GREEN),
		MEDIUM(Formatting.GOLD, Formatting.YELLOW),
		HIGH(Formatting.RED, Formatting.GOLD),
		OVERSTRESSED(Formatting.RED, Formatting.RED);

		private final Formatting absoluteColor;
		private final Formatting relativeColor;

		StressImpact(Formatting absoluteColor, Formatting relativeColor) {
			this.absoluteColor = absoluteColor;
			this.relativeColor = relativeColor;
		}

		public Formatting getAbsoluteColor() {
			return absoluteColor;
		}

		public Formatting getRelativeColor() {
			return relativeColor;
		}

		public static StressImpact of(double stressPercent) {
			if (stressPercent > 1)
				return StressImpact.OVERSTRESSED;
			if (stressPercent > .75d)
				return StressImpact.HIGH;
			if (stressPercent > .5d)
				return StressImpact.MEDIUM;
			return StressImpact.LOW;
		}

		public static boolean isEnabled() {
			return !AllConfigs.server().kinetics.disableStress.get();
		}

		public static LangBuilder getFormattedStressText(double stressPercent) {
			StressImpact stressLevel = of(stressPercent);
			return Lang.text(TooltipHelper.makeProgressBar(3, Math.min(stressLevel.ordinal() + 1, 3)))
				.translate("tooltip.stressImpact." + Lang.asId(stressLevel.name()))
				.text(String.format(" (%s%%) ", (int) (stressPercent * 100)))
				.style(stressLevel.getRelativeColor());
		}
	}

	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face);

	public Axis getRotationAxis(BlockState state);

	public default SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.NONE;
	}

	public default boolean hideStressImpact() {
		return false;
	}

	public default boolean showCapacityWithAnnotation() {
		return false;
	}

}
