package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;

public class DeathCounterDisplaySource extends StatTrackingDisplaySource {

	@Override
	protected int updatedScoreOf(ServerPlayerEntity player) {
		return player.getStatHandler()
			.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS));
	}

	@Override
	protected String getTranslationKey() {
		return "player_deaths";
	}

	@Override
	protected String getObjectiveName() {
		return "deaths";
	}

	@Override
	protected Text getObjectiveDisplayName() {
		return Lang.translateDirect("display_source.scoreboard.objective.deaths");
	}

}
