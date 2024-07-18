package com.simibubi.create.content.schematics;

import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllStructureProcessorTypes;
import com.simibubi.create.foundation.utility.NBTProcessors;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.StructureProcessorExtensions;

public class SchematicProcessor extends StructureProcessor implements StructureProcessorExtensions {
	public static final SchematicProcessor INSTANCE = new SchematicProcessor();
	public static final Codec<SchematicProcessor> CODEC = Codec.unit(() -> {
		return INSTANCE;
	});


	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos anotherPos, StructureTemplate.StructureBlockInfo rawInfo,
			StructureTemplate.StructureBlockInfo info, StructurePlacementData settings) {
		if (info.nbt() != null && info.state().hasBlockEntity()) {
			BlockEntity be = ((BlockEntityProvider) info.state().getBlock()).createBlockEntity(info.pos(), info.state());
			if (be != null) {
				NbtCompound nbt = NBTProcessors.process(be, info.nbt(), false);
				if (nbt != info.nbt())
					return new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), nbt);
			}
		}
		return info;
	}

	@Nullable
	@Override
	public StructureTemplate.StructureEntityInfo processEntity(WorldView world, BlockPos pos, StructureTemplate.StructureEntityInfo rawInfo,
			StructureTemplate.StructureEntityInfo info, StructurePlacementData settings, StructureTemplate template) {
		return EntityType.fromNbt(info.nbt).flatMap(type -> {
			if (world instanceof World) {
				Entity e = type.create((World) world);
				if (e != null && !e.entityDataRequiresOperator()) {
					return Optional.of(info);
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return AllStructureProcessorTypes.SCHEMATIC.get();
	}

}
