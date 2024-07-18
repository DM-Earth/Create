package com.simibubi.create.content.kinetics.gauge;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllPackets;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

import io.github.fabricators_of_create.porting_lib.util.LazyOptional;

public class StressGaugeBlockEntity extends GaugeBlockEntity {

	public AbstractComputerBehaviour computerBehaviour;

	static BlockPos lastSent;

	public StressGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
		registerAwardables(behaviours, AllAdvancements.STRESSOMETER, AllAdvancements.STRESSOMETER_MAXED);
	}

	@Override
	public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
		super.updateFromNetwork(maxStress, currentStress, networkSize);

		if (!StressImpact.isEnabled())
			dialTarget = 0;
		else if (isOverStressed())
			dialTarget = 1.125f;
		else if (maxStress == 0)
			dialTarget = 0;
		else
			dialTarget = currentStress / maxStress;

		if (dialTarget > 0) {
			if (dialTarget < .5f)
				color = Color.mixColors(0x00FF00, 0xFFFF00, dialTarget * 2);
			else if (dialTarget < 1)
				color = Color.mixColors(0xFFFF00, 0xFF0000, (dialTarget) * 2 - 1);
			else
				color = 0xFF0000;
		}

		sendData();
		markDirty();
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		if (getSpeed() == 0) {
			dialTarget = 0;
			markDirty();
			return;
		}

		updateFromNetwork(capacity, stress, getOrCreateNetwork().getSize());
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		if (!StressImpact.isEnabled())
			return false;

		super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		double capacity = getNetworkCapacity();
		double stressFraction = getNetworkStress() / (capacity == 0 ? 1 : capacity);

		Lang.translate("gui.stressometer.title")
			.style(Formatting.GRAY)
			.forGoggles(tooltip);

		if (getTheoreticalSpeed() == 0)
			Lang.text(TooltipHelper.makeProgressBar(3, 0))
				.translate("gui.stressometer.no_rotation")
				.style(Formatting.DARK_GRAY)
				.forGoggles(tooltip);
		else {
			StressImpact.getFormattedStressText(stressFraction)
				.forGoggles(tooltip);
			Lang.translate("gui.stressometer.capacity")
				.style(Formatting.GRAY)
				.forGoggles(tooltip);

			double remainingCapacity = capacity - getNetworkStress();

			LangBuilder su = Lang.translate("generic.unit.stress");
			LangBuilder stressTip = Lang.number(remainingCapacity)
				.add(su)
				.style(StressImpact.of(stressFraction)
					.getRelativeColor());

			if (remainingCapacity != capacity)
				stressTip.text(Formatting.GRAY, " / ")
					.add(Lang.number(capacity)
						.add(su)
						.style(Formatting.DARK_GRAY));

			stressTip.forGoggles(tooltip, 1);
		}

		if (!pos.equals(lastSent))
			AllPackets.getChannel().sendToServer(new GaugeObservedPacket(lastSent = pos));

		return true;
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (clientPacket && pos != null && pos.equals(lastSent))
			lastSent = null;
	}

	public float getNetworkStress() {
		return stress;
	}

	public float getNetworkCapacity() {
		return capacity;
	}

	public void onObserved() {
		award(AllAdvancements.STRESSOMETER);
		if (MathHelper.approximatelyEquals(dialTarget, 1))
			award(AllAdvancements.STRESSOMETER_MAXED);
	}
}
