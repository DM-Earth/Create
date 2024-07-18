package com.simibubi.create.foundation.utility;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class Components {
	private static final Text IMMUTABLE_EMPTY = Text.empty();

	public static Text immutableEmpty() {
		return IMMUTABLE_EMPTY;
	}

	/** Use {@link #immutableEmpty()} when possible to prevent creating an extra object. */
	public static MutableText empty() {
		return Text.empty();
	}

	public static MutableText literal(String str) {
		return Text.literal(str);
	}

	public static MutableText translatable(String key) {
		return Text.translatable(key);
	}

	public static MutableText translatable(String key, Object... args) {
		return Text.translatable(key, args);
	}

	public static MutableText keybind(String name) {
		return Text.keybind(name);
	}
}
