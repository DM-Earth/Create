package com.simibubi.create.foundation.mixin.fabric;

import java.util.concurrent.CompletableFuture;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadExecutor.class)
public interface BlockableEventLoopAccessor {
	@Invoker
	CompletableFuture<Void> callSubmitAsync(Runnable task);
}
