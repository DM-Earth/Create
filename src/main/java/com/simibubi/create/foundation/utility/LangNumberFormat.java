package com.simibubi.create.foundation.utility;

import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class LangNumberFormat {

	private NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
	public static LangNumberFormat numberFormat = new LangNumberFormat();

	public NumberFormat get() {
		return format;
	}

	public void update() {
		format = NumberFormat.getInstance(MinecraftClient.getInstance()
				.getLanguageManager()
				.getSelectedJavaLocale());
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(0);
		format.setGroupingUsed(true);
	}

	public static String format(double d) {
		if (MathHelper.approximatelyEquals(d, 0))
			d = 0;
		return numberFormat.get()
			.format(d)
			.replace("\u00A0", " ");
	}

}
