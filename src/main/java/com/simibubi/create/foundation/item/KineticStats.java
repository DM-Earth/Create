package com.simibubi.create.foundation.item;

import static net.minecraft.util.Formatting.DARK_GRAY;
import static net.minecraft.util.Formatting.GRAY;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CKinetics;

public class KineticStats implements TooltipModifier {
	protected final Block block;

	public KineticStats(Block block) {
		this.block = block;
	}

	@Nullable
	public static KineticStats create(Item item) {
		if (item instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			if (block instanceof IRotate || block instanceof SteamEngineBlock) {
				return new KineticStats(block);
			}
		}
		return null;
	}

	@Override
	public void modify(ItemStack stack, PlayerEntity player, TooltipContext flags, List<Text> tooltip) {
		List<Text> kineticStats = getKineticStats(block, player);
		if (!kineticStats.isEmpty()) {
			tooltip.add(Components.immutableEmpty());
			tooltip.addAll(kineticStats);
		}
	}

	public static List<Text> getKineticStats(Block block, PlayerEntity player) {
		List<Text> list = new ArrayList<>();

		CKinetics config = AllConfigs.server().kinetics;
		LangBuilder rpmUnit = Lang.translate("generic.unit.rpm");
		LangBuilder suUnit = Lang.translate("generic.unit.stress");

		boolean hasGoggles = GogglesItem.isWearingGoggles(player);

		boolean showStressImpact;
		if (block instanceof IRotate) {
			showStressImpact = !((IRotate) block).hideStressImpact();
		} else {
			showStressImpact = true;
		}

		if (block instanceof ValveHandleBlock)
			block = AllBlocks.COPPER_VALVE_HANDLE.get();

		boolean hasStressImpact =
			StressImpact.isEnabled() && showStressImpact && BlockStressValues.getImpact(block) > 0;
		boolean hasStressCapacity = StressImpact.isEnabled() && BlockStressValues.hasCapacity(block);

		if (hasStressImpact) {
			Lang.translate("tooltip.stressImpact")
				.style(GRAY)
				.addTo(list);

			double impact = BlockStressValues.getImpact(block);
			StressImpact impactId = impact >= config.highStressImpact.get() ? StressImpact.HIGH
				: (impact >= config.mediumStressImpact.get() ? StressImpact.MEDIUM : StressImpact.LOW);
			LangBuilder builder = Lang.builder()
				.add(Lang.text(TooltipHelper.makeProgressBar(3, impactId.ordinal() + 1))
					.style(impactId.getAbsoluteColor()));

			if (hasGoggles) {
				builder.add(Lang.number(impact))
					.text("x ")
					.add(rpmUnit)
					.addTo(list);
			} else
				builder.translate("tooltip.stressImpact." + Lang.asId(impactId.name()))
					.addTo(list);
		}

		if (hasStressCapacity) {
			Lang.translate("tooltip.capacityProvided")
				.style(GRAY)
				.addTo(list);

			double capacity = BlockStressValues.getCapacity(block);
			Couple<Integer> generatedRPM = BlockStressValues.getGeneratedRPM(block);

			StressImpact impactId = capacity >= config.highCapacity.get() ? StressImpact.HIGH
				: (capacity >= config.mediumCapacity.get() ? StressImpact.MEDIUM : StressImpact.LOW);
			StressImpact opposite = StressImpact.values()[StressImpact.values().length - 2 - impactId.ordinal()];
			LangBuilder builder = Lang.builder()
				.add(Lang.text(TooltipHelper.makeProgressBar(3, impactId.ordinal() + 1))
					.style(opposite.getAbsoluteColor()));

			if (hasGoggles) {
				builder.add(Lang.number(capacity))
					.text("x ")
					.add(rpmUnit)
					.addTo(list);

				if (generatedRPM != null) {
					LangBuilder amount = Lang.number(capacity * generatedRPM.getSecond())
						.add(suUnit);
					Lang.text(" -> ")
						.add(!generatedRPM.getFirst()
							.equals(generatedRPM.getSecond()) ? Lang.translate("tooltip.up_to", amount) : amount)
						.style(DARK_GRAY)
						.addTo(list);
				}
			} else
				builder.translate("tooltip.capacityProvided." + Lang.asId(impactId.name()))
					.addTo(list);
		}

		return list;
	}
}
