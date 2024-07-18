package com.simibubi.create.foundation.mixin.accessor;

import java.util.Map;
import net.minecraft.util.SystemDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SystemDetails.class)
public interface SystemReportAccessor {
	@Accessor
	static String getOPERATING_SYSTEM() {
		throw new AssertionError();
	}

	@Accessor
	static String getJAVA_VERSION() {
		throw new AssertionError();
	}

	@Accessor
	Map<String, String> getSections();
}
