package com.simibubi.create.content.redstone.displayLink.source;

import java.util.stream.Stream;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.LongAttached;
import com.simibubi.create.foundation.utility.Lang;

import com.simibubi.create.foundation.utility.LongAttached;

public class ScoreboardDisplaySource extends ValueListDisplaySource {

	@Override
	protected Stream<LongAttached<MutableText>> provideEntries(DisplayLinkContext context, int maxRows) {
		World level = context.blockEntity()
			.getWorld();
		if (!(level instanceof ServerWorld sLevel))
			return Stream.empty();

		String name = context.sourceConfig()
			.getString("Objective");

		return showScoreboard(sLevel, name, maxRows);
	}

	protected Stream<LongAttached<MutableText>> showScoreboard(ServerWorld sLevel, String objectiveName,
		int maxRows) {
		ScoreboardObjective objective = sLevel.getScoreboard()
			.getNullableObjective(objectiveName);
		if (objective == null)
			return notFound(objectiveName).stream();

		return sLevel.getScoreboard()
			.getAllPlayerScores(objective)
			.stream()
			.map(score -> LongAttached.with(score.getScore(), Components.literal(score.getPlayerName())
				.copy()))
			.sorted(LongAttached.comparator())
			.limit(maxRows);
	}

	private ImmutableList<LongAttached<MutableText>> notFound(String objective) {
		return ImmutableList
			.of(LongAttached.with(404, Lang.translateDirect("display_source.scoreboard.objective_not_found", objective)));
	}

	@Override
	protected String getTranslationKey() {
		return "scoreboard";
	}

	@Override
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
		if (isFirstLine)
			builder.addTextInput(0, 137, (e, t) -> {
				e.setText("");
				t.withTooltip(ImmutableList.of(Lang.translateDirect("display_source.scoreboard.objective")
					.styled(s -> s.withColor(0x5391E1)),
					Lang.translateDirect("gui.schedule.lmb_edit")
						.formatted(Formatting.DARK_GRAY, Formatting.ITALIC)));
			}, "Objective");
		else
			addFullNumberConfig(builder);
	}

	@Override
	protected boolean valueFirst() {
		return false;
	}

}
