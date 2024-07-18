package com.simibubi.create.infrastructure.gametest;

import java.util.Collection;
import net.minecraft.test.CustomTestProvider;
import net.minecraft.test.TestFunction;
import com.simibubi.create.infrastructure.gametest.tests.TestContraptions;
import com.simibubi.create.infrastructure.gametest.tests.TestFluids;
import com.simibubi.create.infrastructure.gametest.tests.TestItems;
import com.simibubi.create.infrastructure.gametest.tests.TestMisc;
import com.simibubi.create.infrastructure.gametest.tests.TestProcessing;

public class CreateGameTests {
	private static final Class<?>[] testHolders = {
			TestContraptions.class,
			TestFluids.class,
			TestItems.class,
			TestMisc.class,
			TestProcessing.class
	};

	@CustomTestProvider
	public static Collection<TestFunction> generateTests() {
		return CreateTestFunction.getTestsFrom(testHolders);
	}
}
