package com.simibubi.create.content.contraptions;

import java.util.Collection;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import com.simibubi.create.foundation.utility.AttachedRegistry;
import com.simibubi.create.infrastructure.config.AllConfigs;

public enum ContraptionMovementSetting {
	MOVABLE, NO_PICKUP, UNMOVABLE;

	private static final AttachedRegistry<Block, Supplier<ContraptionMovementSetting>> SETTING_SUPPLIERS = new AttachedRegistry<>(Registries.BLOCK);

	public static void register(Identifier block, Supplier<ContraptionMovementSetting> settingSupplier) {
		SETTING_SUPPLIERS.register(block, settingSupplier);
	}

	public static void register(Block block, Supplier<ContraptionMovementSetting> settingSupplier) {
		SETTING_SUPPLIERS.register(block, settingSupplier);
	}

	@Nullable
	public static ContraptionMovementSetting get(Block block) {
		if (block instanceof IMovementSettingProvider provider)
			return provider.getContraptionMovementSetting();
		Supplier<ContraptionMovementSetting> supplier = SETTING_SUPPLIERS.get(block);
		if (supplier == null)
			return null;
		return supplier.get();
	}

	public static boolean allAre(Collection<StructureTemplate.StructureBlockInfo> blocks, ContraptionMovementSetting are) {
		return blocks.stream().anyMatch(b -> get(b.state().getBlock()) == are);
	}

	public static boolean isNoPickup(Collection<StructureTemplate.StructureBlockInfo> blocks) {
		return allAre(blocks, ContraptionMovementSetting.NO_PICKUP);
	}

	public static void registerDefaults() {
		register(Blocks.SPAWNER, () -> AllConfigs.server().kinetics.spawnerMovement.get());
		register(Blocks.BUDDING_AMETHYST, () -> AllConfigs.server().kinetics.amethystMovement.get());
		register(Blocks.OBSIDIAN, () -> AllConfigs.server().kinetics.obsidianMovement.get());
		register(Blocks.CRYING_OBSIDIAN, () -> AllConfigs.server().kinetics.obsidianMovement.get());
		register(Blocks.RESPAWN_ANCHOR, () -> AllConfigs.server().kinetics.obsidianMovement.get());
		register(Blocks.REINFORCED_DEEPSLATE, () -> AllConfigs.server().kinetics.reinforcedDeepslateMovement.get());
	}

	public interface IMovementSettingProvider /* extends Block */ {
		ContraptionMovementSetting getContraptionMovementSetting();
	}
}
