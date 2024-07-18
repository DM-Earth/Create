package com.simibubi.create.foundation.ponder;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processing for Ponder schematics to allow using the same ones on Forge and Fabric.
 */
public class FabricPonderProcessing {
	public static final Codec<Processor> PROCESSOR_CODEC = Identifier.CODEC
			.fieldOf("structureId")
			.xmap(Processor::new, processor -> processor.structureId)
			.codec();

	public static final StructureProcessorType<Processor> PROCESSOR_TYPE = Registry.register(
			Registries.STRUCTURE_PROCESSOR,
			Create.asResource("fabric_ponder_processor"),
			() -> PROCESSOR_CODEC
	);

	/**
	 * A predicate that makes all processes apply to all schematics.
	 */
	public static final ProcessingPredicate ALWAYS = (id, process) -> true;

	private static final Map<String, ProcessingPredicate> predicates = new HashMap<>();

	/**
	 * Register a {@link ProcessingPredicate} for a mod.
	 * Only one predicate may be registered for each mod.
	 * The predicate determines which {@link Process}es will be applied to which schematics.
	 */
	public static ProcessingPredicate register(String modId, ProcessingPredicate predicate) {
		ProcessingPredicate existing = predicates.get(modId);
		if (existing != null) {
			throw new IllegalStateException(
					"Tried to register ProcessingPredicate [%s] for mod '%s', while one already exists: [%s]"
							.formatted(predicate, modId, existing)
			);
		}
	    predicates.put(modId, predicate);
		return predicate;
	}

	public static StructurePlacementData makePlaceSettings(Identifier structureId) {
		return new StructurePlacementData().addProcessor(new Processor(structureId));
	}

	@Internal
	public static void init() {
		register(Create.ID, ALWAYS);
	}

	public enum Process {
		FLUID_TANK_AMOUNTS
	}

	@FunctionalInterface
	public interface ProcessingPredicate {
		boolean shouldApplyProcess(Identifier schematicId, Process process);
	}

	public static class Processor extends StructureProcessor {
		public final Identifier structureId;

		public Processor(Identifier structureId) {
			this.structureId = structureId;
		}

		@Nullable
		@Override
		public StructureTemplate.StructureBlockInfo process(
				@NotNull WorldView level, @NotNull BlockPos pos, @NotNull BlockPos pivot,
				@NotNull StructureBlockInfo blockInfo, @NotNull StructureBlockInfo relativeBlockInfo,
				@NotNull StructurePlacementData settings) {
			ProcessingPredicate predicate = predicates.get(structureId.getNamespace());
			if (predicate == null) // do nothing
				return relativeBlockInfo;

			NbtCompound nbt = relativeBlockInfo.nbt();
			if (nbt != null
					&& AllBlocks.FLUID_TANK.has(relativeBlockInfo.state())
					&& nbt.contains("TankContent", NbtElement.COMPOUND_TYPE)
					&& predicate.shouldApplyProcess(structureId, Process.FLUID_TANK_AMOUNTS)) {

				FluidStack content = FluidStack.loadFluidStackFromNBT(nbt.getCompound("TankContent"));
				long amount = content.getAmount();
				float buckets = amount / 1000f;
				long fixedAmount = (long) (buckets * FluidConstants.BUCKET);
				content.setAmount(fixedAmount);

				NbtCompound newNbt = nbt.copy();
				newNbt.put("TankContent", content.writeToNBT(new NbtCompound()));
				return new StructureBlockInfo(relativeBlockInfo.pos(), relativeBlockInfo.state(), newNbt);
			}

			// no processes were applied
			return relativeBlockInfo;
		}

		@Override
		@NotNull
		protected StructureProcessorType<?> getType() {
			return PROCESSOR_TYPE;
		}
	}
}
