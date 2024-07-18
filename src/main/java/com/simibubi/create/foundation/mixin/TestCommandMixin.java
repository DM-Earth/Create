package com.simibubi.create.foundation.mixin;

import javax.annotation.Nullable;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.TestCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestFunction;
import net.minecraft.test.TestFunctions;
import net.minecraft.test.TestSet;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.infrastructure.gametest.CreateTestFunction;

@Mixin(TestCommand.class)
public class TestCommandMixin {
	@Redirect(
			method = "run(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/test/TestSet;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/test/TestFunctions;getTestFunctionOrThrow(Ljava/lang/String;)Lnet/minecraft/test/TestFunction;"
			),
			require = 0 // don't crash if this fails. non-critical
	)
	private static TestFunction create$getCorrectTestFunction(String testName,
															  ServerWorld level, BlockPos pos, @Nullable TestSet tracker) {
		StructureBlockBlockEntity be = (StructureBlockBlockEntity) level.getBlockEntity(pos);
		NbtCompound data = be.getCustomData();
		if (!data.contains("CreateTestFunction", NbtElement.STRING_TYPE))
			return TestFunctions.getTestFunctionOrThrow(testName);
		String name = data.getString("CreateTestFunction");
		CreateTestFunction function = CreateTestFunction.NAMES_TO_FUNCTIONS.get(name);
		if (function == null)
			throw new IllegalStateException("Structure block has CreateTestFunction attached, but test [" + name + "] doesn't exist");
		return function;
	}
}
