package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;

public class ItemThroughputDisplaySource extends AccumulatedItemCountDisplaySource {

	static final int POOL_SIZE = 10;

	@Override
	protected MutableText provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		NbtCompound conf = context.sourceConfig();
		if (conf.contains("Inactive"))
			return ZERO.copy();

		double interval = 20 * Math.pow(60, conf.getInt("Interval"));
		double rate = conf.getFloat("Rate") * interval;

		if (rate > 0) {
			long previousTime = conf.getLong("LastReceived");
			long gameTime = context.blockEntity()
				.getWorld()
				.getTime();
			int diff = (int) (gameTime - previousTime);
			if (diff > 0) {
				// Too long since last item
				int lastAmount = conf.getInt("LastReceivedAmount");
				double timeBetweenStacks = lastAmount / rate;
				if (diff > timeBetweenStacks * 2)
					conf.putBoolean("Inactive", true);
			}
		}

		return Lang.number(rate)
			.component();
	}

	public void itemReceived(DisplayLinkBlockEntity be, int amount) {
		if (be.getCachedState()
			.getOrEmpty(DisplayLinkBlock.POWERED)
			.orElse(true))
			return;

		NbtCompound conf = be.getSourceConfig();
		long gameTime = be.getWorld()
			.getTime();

		if (!conf.contains("LastReceived")) {
			conf.putLong("LastReceived", gameTime);
			return;
		}

		long previousTime = conf.getLong("LastReceived");
		NbtList rates = conf.getList("PrevRates", NbtElement.FLOAT_TYPE);

		if (rates.size() != POOL_SIZE) {
			rates = new NbtList();
			for (int i = 0; i < POOL_SIZE; i++)
				rates.add(NbtFloat.of(-1));
		}

		int poolIndex = conf.getInt("Index") % POOL_SIZE;
		rates.set(poolIndex, NbtFloat.of((float) (amount / (double) (gameTime - previousTime))));

		float rate = 0;
		int validIntervals = 0;
		for (int i = 0; i < POOL_SIZE; i++) {
			float pooledRate = rates.getFloat(i);
			if (pooledRate >= 0) {
				rate += pooledRate;
				validIntervals++;
			}
		}

		conf.remove("Rate");
		if (validIntervals > 0) {
			rate /= validIntervals;
			conf.putFloat("Rate", rate);
		}

		conf.remove("Inactive");
		conf.putInt("LastReceivedAmount", amount);
		conf.putLong("LastReceived", gameTime);
		conf.putInt("Index", poolIndex + 1);
		conf.put("PrevRates", rates);
		be.updateGatheredData();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder,
		boolean isFirstLine) {
		super.initConfigurationWidgets(context, builder, isFirstLine);
		if (isFirstLine)
			return;

		builder.addSelectionScrollInput(0, 80, (si, l) -> {
			si.forOptions(Lang.translatedOptions("display_source.item_throughput.interval", "second", "minute", "hour"))
				.titled(Lang.translateDirect("display_source.item_throughput.interval"));
		}, "Interval");
	}

	@Override
	protected String getTranslationKey() {
		return "item_throughput";
	}

}
