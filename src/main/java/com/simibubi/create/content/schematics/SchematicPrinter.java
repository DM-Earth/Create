package com.simibubi.create.content.schematics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import com.simibubi.create.foundation.utility.BBHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SchematicPrinter {

	public enum PrintStage {
		BLOCKS, DEFERRED_BLOCKS, ENTITIES
	}

	private boolean schematicLoaded;
	private boolean isErrored;
	private SchematicWorld blockReader;
	private BlockPos schematicAnchor;

	private BlockPos currentPos;
	private int printingEntityIndex;
	private PrintStage printStage;
	private List<BlockPos> deferredBlocks;

	public SchematicPrinter() {
		printingEntityIndex = -1;
		printStage = PrintStage.BLOCKS;
		deferredBlocks = new LinkedList<>();
	}

	public void fromTag(NbtCompound compound, boolean clientPacket) {
		if (compound.contains("CurrentPos"))
			currentPos = NbtHelper.toBlockPos(compound.getCompound("CurrentPos"));
		if (clientPacket) {
			schematicLoaded = false;
			if (compound.contains("Anchor")) {
				schematicAnchor = NbtHelper.toBlockPos(compound.getCompound("Anchor"));
				schematicLoaded = true;
			}
		}

		printingEntityIndex = compound.getInt("EntityProgress");
		printStage = PrintStage.valueOf(compound.getString("PrintStage"));
		compound.getList("DeferredBlocks", 10).stream()
				.map(p -> NbtHelper.toBlockPos((NbtCompound) p))
				.collect(Collectors.toCollection(() -> deferredBlocks));
	}

	public void write(NbtCompound compound) {
		if (currentPos != null)
			compound.put("CurrentPos", NbtHelper.fromBlockPos(currentPos));
		if (schematicAnchor != null)
			compound.put("Anchor", NbtHelper.fromBlockPos(schematicAnchor));

		compound.putInt("EntityProgress", printingEntityIndex);
		compound.putString("PrintStage", printStage.name());
		NbtList tagDeferredBlocks = new NbtList();
		for (BlockPos p : deferredBlocks)
			tagDeferredBlocks.add(NbtHelper.fromBlockPos(p));
		compound.put("DeferredBlocks", tagDeferredBlocks);
	}

	public void loadSchematic(ItemStack blueprint, World originalWorld, boolean processNBT) {
		if (!blueprint.hasNbt() || !blueprint.getNbt().getBoolean("Deployed"))
			return;

		StructureTemplate activeTemplate =
			SchematicItem.loadSchematic(originalWorld.createCommandRegistryWrapper(RegistryKeys.BLOCK), blueprint);
		StructurePlacementData settings = SchematicItem.getSettings(blueprint, processNBT);

		schematicAnchor = NbtHelper.toBlockPos(blueprint.getNbt()
				.getCompound("Anchor"));
		blockReader = new SchematicWorld(schematicAnchor, originalWorld);

		try {
			activeTemplate.place(blockReader, schematicAnchor, schematicAnchor, settings,
				blockReader.getRandom(), Block.NOTIFY_LISTENERS);
		} catch (Exception e) {
			Create.LOGGER.error("Failed to load Schematic for Printing", e);
			schematicLoaded = true;
			isErrored = true;
			return;
		}

		BlockPos extraBounds = StructureTemplate.transform(settings, new BlockPos(activeTemplate.getSize())
				.add(-1, -1, -1));
		blockReader.bounds = BBHelper.encapsulate(blockReader.bounds, extraBounds);

		StructureTransform transform = new StructureTransform(settings.getPosition(), Direction.Axis.Y,
				settings.getRotation(), settings.getMirror());
		for (BlockEntity be : blockReader.getBlockEntities())
			transform.apply(be);

		printingEntityIndex = -1;
		printStage = PrintStage.BLOCKS;
		deferredBlocks.clear();
		BlockBox bounds = blockReader.getBounds();
		currentPos = new BlockPos(bounds.getMinX() - 1, bounds.getMinY(), bounds.getMinZ());
		schematicLoaded = true;
	}

	public void resetSchematic() {
		schematicLoaded = false;
		schematicAnchor = null;
		isErrored = false;
		currentPos = null;
		blockReader = null;
		printingEntityIndex = -1;
		printStage = PrintStage.BLOCKS;
		deferredBlocks.clear();
	}

	public boolean isLoaded() {
		return schematicLoaded;
	}

	public boolean isErrored() {
		return isErrored;
	}

	public BlockPos getCurrentTarget() {
		if (!isLoaded() || isErrored())
			return null;
		return schematicAnchor.add(currentPos);
	}

	public PrintStage getPrintStage() {
		return printStage;
	}

	public BlockPos getAnchor() {
		return schematicAnchor;
	}

	public boolean isWorldEmpty() {
		return blockReader.getAllPositions().isEmpty();
		//return blockReader.getBounds().getLength().equals(new Vector3i(0,0,0));
	}

	@FunctionalInterface
	public interface BlockTargetHandler {
		void handle(BlockPos target, BlockState blockState, BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface EntityTargetHandler {
		void handle(BlockPos target, Entity entity);
	}

	public void handleCurrentTarget(BlockTargetHandler blockHandler, EntityTargetHandler entityHandler) {
		BlockPos target = getCurrentTarget();

		if (printStage == PrintStage.ENTITIES) {
			Entity entity = blockReader.getEntityStream()
					.collect(Collectors.toList())
					.get(printingEntityIndex);
			entityHandler.handle(target, entity);
		} else {
			BlockState blockState = BlockHelper.setZeroAge(blockReader.getBlockState(target));
			BlockEntity blockEntity = blockReader.getBlockEntity(target);
			blockHandler.handle(target, blockState, blockEntity);
		}
	}

	@FunctionalInterface
	public interface PlacementPredicate {
		boolean shouldPlace(BlockPos target, BlockState blockState, BlockEntity blockEntity,
							BlockState toReplace, BlockState toReplaceOther, boolean isNormalCube);
	}

	public boolean shouldPlaceCurrent(World world) {
		return shouldPlaceCurrent(world, (a, b, c, d, e, f) -> true);
	}

	public boolean shouldPlaceCurrent(World world, PlacementPredicate predicate) {
		if (world == null)
			return false;

		if (printStage == PrintStage.ENTITIES)
			return true;

		return shouldPlaceBlock(world, predicate, getCurrentTarget());
	}

	public boolean shouldPlaceBlock(World world, PlacementPredicate predicate, BlockPos pos) {
		BlockState state = BlockHelper.setZeroAge(blockReader.getBlockState(pos));
		BlockEntity blockEntity = blockReader.getBlockEntity(pos);

		BlockState toReplace = world.getBlockState(pos);
		BlockEntity toReplaceBE = world.getBlockEntity(pos);
		BlockState toReplaceOther = null;

		if (state.contains(Properties.BED_PART) && state.contains(Properties.HORIZONTAL_FACING)
				&& state.get(Properties.BED_PART) == BedPart.FOOT)
			toReplaceOther = world.getBlockState(pos.offset(state.get(Properties.HORIZONTAL_FACING)));
		if (state.contains(Properties.DOUBLE_BLOCK_HALF)
				&& state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)
			toReplaceOther = world.getBlockState(pos.up());

		boolean mergeTEs = blockEntity != null && toReplaceBE instanceof IMergeableBE mergeBE && toReplaceBE.getType()
				.equals(blockEntity.getType());

		if (!world.canSetBlock(pos))
			return false;
		if (!world.getWorldBorder().contains(pos))
			return false;
		if (toReplace == state && !mergeTEs)
			return false;
		if (toReplace.getHardness(world, pos) == -1
				|| (toReplaceOther != null && toReplaceOther.getHardness(world, pos) == -1))
			return false;

		boolean isNormalCube = state.isSolidBlock(blockReader, currentPos);
		return predicate.shouldPlace(pos, state, blockEntity, toReplace, toReplaceOther, isNormalCube);
	}

	public ItemRequirement getCurrentRequirement() {
		if (printStage == PrintStage.ENTITIES)
			return ItemRequirement.of(blockReader.getEntityStream()
					.collect(Collectors.toList())
					.get(printingEntityIndex));

		BlockPos target = getCurrentTarget();
		BlockState blockState = BlockHelper.setZeroAge(blockReader.getBlockState(target));
		BlockEntity blockEntity = blockReader.getBlockEntity(target);
		return ItemRequirement.of(blockState, blockEntity);
	}

	public int markAllBlockRequirements(MaterialChecklist checklist, World world, PlacementPredicate predicate) {
		int blocksToPlace = 0;
		for (BlockPos pos : blockReader.getAllPositions()) {
			BlockPos relPos = pos.add(schematicAnchor);
			BlockState required = blockReader.getBlockState(relPos);
			BlockEntity requiredBE = blockReader.getBlockEntity(relPos);

			if (!world.canSetBlock(pos.add(schematicAnchor))) {
				checklist.warnBlockNotLoaded();
				continue;
			}
			if (!shouldPlaceBlock(world, predicate, relPos))
				continue;
			ItemRequirement requirement = ItemRequirement.of(required, requiredBE);
			if (requirement.isEmpty())
				continue;
			if (requirement.isInvalid())
				continue;
			checklist.require(requirement);
			blocksToPlace++;
		}
		return blocksToPlace;
	}

	public void markAllEntityRequirements(MaterialChecklist checklist) {
		blockReader.getEntityStream()
				.forEach(entity -> {
					ItemRequirement requirement = ItemRequirement.of(entity);
					if (requirement.isEmpty())
						return;
					if (requirement.isInvalid())
						return;
					checklist.require(requirement);
				});
	}

	public boolean advanceCurrentPos() {
		List<Entity> entities = blockReader.getEntityStream().collect(Collectors.toList());

		do {
			if (printStage == PrintStage.BLOCKS) {
				while (tryAdvanceCurrentPos()) {
					deferredBlocks.add(currentPos);
				}
			}

			if (printStage == PrintStage.DEFERRED_BLOCKS) {
				if (deferredBlocks.isEmpty()) {
					printStage = PrintStage.ENTITIES;
				} else {
					currentPos = deferredBlocks.remove(0);
				}
			}

			if (printStage == PrintStage.ENTITIES) {
				if (printingEntityIndex + 1 < entities.size()) {
					printingEntityIndex++;
					currentPos = entities.get(printingEntityIndex).getBlockPos().subtract(schematicAnchor);
				} else {
					// Reached end of printing
					return false;
				}
			}
		} while (!blockReader.getBounds().contains(currentPos));

		// More things available to print
		return true;
	}

	public boolean tryAdvanceCurrentPos() {
		currentPos = currentPos.offset(Direction.EAST);
		BlockBox bounds = blockReader.getBounds();
		BlockPos posInBounds = currentPos.add(-bounds.getMinX(), -bounds.getMinY(), -bounds.getMinZ());

		if (posInBounds.getX() > bounds.getBlockCountX())
			currentPos = new BlockPos(bounds.getMinX(), currentPos.getY(), currentPos.getZ() + 1).west();
		if (posInBounds.getZ() > bounds.getBlockCountZ())
			currentPos = new BlockPos(currentPos.getX(), currentPos.getY() + 1, bounds.getMinZ()).west();

		// End of blocks reached
		if (currentPos.getY() > bounds.getBlockCountY()) {
			printStage = PrintStage.DEFERRED_BLOCKS;
			return false;
		}

		return shouldDeferBlock(blockReader.getBlockState(getCurrentTarget()));
	}

	public static boolean shouldDeferBlock(BlockState state) {
		return AllBlocks.GANTRY_CARRIAGE.has(state) || AllBlocks.MECHANICAL_ARM.has(state)
				|| BlockMovementChecks.isBrittle(state);
	}

}
