package com.simibubi.create.infrastructure.gametest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.test.CustomTestProvider;
import net.minecraft.test.GameTest;
import net.minecraft.test.StructureTestUtil;
import net.minecraft.test.TestContext;
import net.minecraft.test.TestFunction;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * An extension to game tests implementing functionality for {@link CreateGameTestHelper} and {@link GameTestGroup}.
 * To use, create a {@link CustomTestProvider} that provides tests using {@link #getTestsFrom(Class[])}.
 */
public class CreateTestFunction extends TestFunction {
	// for structure blocks and /test runthis
	public static final Map<String, CreateTestFunction> NAMES_TO_FUNCTIONS = new HashMap<>();

	public final String fullName;
	public final String simpleName;

	protected CreateTestFunction(String fullName, String simpleName, String pBatchName, String pTestName,
								 String pStructureName, BlockRotation pRotation, int pMaxTicks, long pSetupTicks,
								 boolean pRequired, int pRequiredSuccesses, int pMaxAttempts, Consumer<TestContext> pFunction) {
		super(pBatchName, pTestName, pStructureName, pRotation, pMaxTicks, pSetupTicks, pRequired, pRequiredSuccesses, pMaxAttempts, pFunction);
		this.fullName = fullName;
		this.simpleName = simpleName;
		NAMES_TO_FUNCTIONS.put(fullName, this);
	}

	@Override
	public String getTemplatePath() {
		return simpleName;
	}

	/**
	 * Get all Create test functions from the given classes. This enables functionality
	 * of {@link CreateGameTestHelper} and {@link GameTestGroup}.
	 */
	public static Collection<TestFunction> getTestsFrom(Class<?>... classes) {
		return Stream.of(classes)
				.map(Class::getDeclaredMethods)
				.flatMap(Stream::of)
				.map(CreateTestFunction::of)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(TestFunction::getTemplatePath))
				.toList();
	}

	@Nullable
	public static TestFunction of(Method method) {
		GameTest gt = method.getAnnotation(GameTest.class);
		if (gt == null) // skip non-test methods
			return null;
		Class<?> owner = method.getDeclaringClass();
		GameTestGroup group = owner.getAnnotation(GameTestGroup.class);
		String simpleName = owner.getSimpleName() + '.' + method.getName();
		validateTestMethod(method, gt, owner, group, simpleName);

		String structure = "%s:gametest/%s/%s".formatted(group.namespace(), group.path(), gt.templateName());
		BlockRotation rotation = StructureTestUtil.getRotation(gt.rotation());

		String fullName = owner.getName() + "." + method.getName();
		return new CreateTestFunction(
				// use structure for test name since that's what MC fills structure blocks with for some reason
				fullName, simpleName, gt.batchId(), structure, structure, rotation, gt.tickLimit(), gt.duration(),
				gt.required(), gt.requiredSuccesses(), gt.maxAttempts(), asConsumer(method)
		);
	}

	private static void validateTestMethod(Method method, GameTest gt, Class<?> owner, GameTestGroup group, String simpleName) {
		if (gt.templateName().isEmpty())
			throw new IllegalArgumentException(simpleName + " must provide a template structure");

		if (!Modifier.isStatic(method.getModifiers()))
			throw new IllegalArgumentException(simpleName + " must be static");

		if (method.getReturnType() != void.class)
			throw new IllegalArgumentException(simpleName + " must return void");

		if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != CreateGameTestHelper.class)
			throw new IllegalArgumentException(simpleName + " must take 1 parameter of type CreateGameTestHelper");

		if (group == null)
			throw new IllegalArgumentException(owner.getName() + " must be annotated with @GameTestGroup");
	}

	private static Consumer<TestContext> asConsumer(Method method) {
		return (helper) -> {
			try {
				method.invoke(null, helper);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		};
	}

	@Override
	public void start(@NotNull TestContext helper) {
		// give structure block test info
		StructureBlockBlockEntity be = (StructureBlockBlockEntity) helper.getBlockEntity(BlockPos.ORIGIN);
		be.getCustomData().putString("CreateTestFunction", fullName);
		super.start(CreateGameTestHelper.of(helper));
	}
}
