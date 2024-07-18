package com.simibubi.create.foundation.blockEntity.behaviour;

import java.util.function.Function;
import net.minecraft.text.MutableText;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.Lang;

public class ValueSettingsFormatter {

	private Function<ValueSettings, MutableText> formatter;

	public ValueSettingsFormatter(Function<ValueSettings, MutableText> formatter) {
		this.formatter = formatter;
	}

	public MutableText format(ValueSettings valueSettings) {
		return formatter.apply(valueSettings);
	}

	public static class ScrollOptionSettingsFormatter extends ValueSettingsFormatter {

		private INamedIconOptions[] options;

		public ScrollOptionSettingsFormatter(INamedIconOptions[] options) {
			super(v -> Lang.translateDirect(options[v.value()].getTranslationKey()));
			this.options = options;
		}

		public AllIcons getIcon(ValueSettings valueSettings) {
			return options[valueSettings.value()].getIcon();
		}

	}

}
