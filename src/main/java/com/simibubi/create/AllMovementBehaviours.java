package com.simibubi.create;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.simibubi.create.content.contraptions.behaviour.BellMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.CampfireMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.dispenser.DispenserMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.dispenser.DropperMovementBehaviour;
import com.simibubi.create.foundation.utility.AttachedRegistry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

public class AllMovementBehaviours {
	private static final AttachedRegistry<Block, MovementBehaviour> BLOCK_BEHAVIOURS = new AttachedRegistry<>(Registries.BLOCK);
	private static final List<BehaviourProvider> GLOBAL_BEHAVIOURS = new ArrayList<>();

	public static void registerBehaviour(Identifier block, MovementBehaviour behaviour) {
		BLOCK_BEHAVIOURS.register(block, behaviour);
	}

	public static void registerBehaviour(Block block, MovementBehaviour behaviour) {
		BLOCK_BEHAVIOURS.register(block, behaviour);
	}

	public static void registerBehaviourProvider(BehaviourProvider provider) {
		GLOBAL_BEHAVIOURS.add(provider);
	}

	@Nullable
	public static MovementBehaviour getBehaviour(BlockState state) {
		MovementBehaviour behaviour = BLOCK_BEHAVIOURS.get(state.getBlock());
		if (behaviour != null) {
			return behaviour;
		}

		for (BehaviourProvider provider : GLOBAL_BEHAVIOURS) {
			behaviour = provider.getBehaviour(state);
			if (behaviour != null) {
				return behaviour;
			}
		}

		return null;
	}

	public static <B extends Block> NonNullConsumer<? super B> movementBehaviour(
		MovementBehaviour behaviour) {
		return b -> registerBehaviour(b, behaviour);
	}

	static void registerDefaults() {
		registerBehaviour(Blocks.BELL, new BellMovementBehaviour());
		registerBehaviour(Blocks.CAMPFIRE, new CampfireMovementBehaviour());
		registerBehaviour(Blocks.SOUL_CAMPFIRE, new CampfireMovementBehaviour());

		DispenserMovementBehaviour.gatherMovedDispenseItemBehaviours();
		registerBehaviour(Blocks.DISPENSER, new DispenserMovementBehaviour());
		registerBehaviour(Blocks.DROPPER, new DropperMovementBehaviour());
	}

	public interface BehaviourProvider {
		@Nullable
		MovementBehaviour getBehaviour(BlockState state);
	}
}
