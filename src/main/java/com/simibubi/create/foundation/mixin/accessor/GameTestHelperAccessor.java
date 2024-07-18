package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.test.GameTestState;
import net.minecraft.test.TestContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TestContext.class)
public interface GameTestHelperAccessor {
	@Accessor
	GameTestState getTest();
	@Accessor
	boolean getHasFinalClause();
	@Accessor
	void setHasFinalClause(boolean value);
}