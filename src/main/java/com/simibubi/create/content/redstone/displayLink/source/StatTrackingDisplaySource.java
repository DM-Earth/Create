package com.simibubi.create.content.redstone.displayLink.source;

import java.util.stream.Stream;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.LongAttached;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardCriterion.RenderType;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public abstract class StatTrackingDisplaySource extends ScoreboardDisplaySource {

	@Override
	protected Stream<LongAttached<MutableText>> provideEntries(DisplayLinkContext context, int maxRows) {
		World level = context.blockEntity()
			.getWorld();
		if (!(level instanceof ServerWorld sLevel))
			return Stream.empty();

		String name = "create_auto_" + getObjectiveName();
		Scoreboard scoreboard = level.getScoreboard();
		if (!scoreboard.containsObjective(name))
			scoreboard.addObjective(name, ScoreboardCriterion.DUMMY, getObjectiveDisplayName(), RenderType.INTEGER);
		ScoreboardObjective objective = scoreboard.getNullableObjective(name);

		sLevel.getServer().getPlayerManager().getPlayerList()
			.forEach(s -> scoreboard.getPlayerScore(s.getEntityName(), objective)
				.setScore(updatedScoreOf(s)));

		return showScoreboard(sLevel, name, maxRows);
	}

	protected abstract String getObjectiveName();

	protected abstract Text getObjectiveDisplayName();

	protected abstract int updatedScoreOf(ServerPlayerEntity player);

	@Override
	protected boolean valueFirst() {
		return false;
	}

	@Override
	protected boolean shortenNumbers(DisplayLinkContext context) {
		return false;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {}

}
