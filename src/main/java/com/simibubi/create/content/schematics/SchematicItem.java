package com.simibubi.create.content.schematics;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.client.SchematicEditScreen;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SchematicItem extends Item {

	private static final Logger LOGGER = LogUtils.getLogger();

	public SchematicItem(Settings properties) {
		super(properties);
	}

	public static ItemStack create(RegistryEntryLookup<Block> lookup, String schematic, String owner) {
		ItemStack blueprint = AllItems.SCHEMATIC.asStack();

		NbtCompound tag = new NbtCompound();
		tag.putBoolean("Deployed", false);
		tag.putString("Owner", owner);
		tag.putString("File", schematic);
		tag.put("Anchor", NbtHelper.fromBlockPos(BlockPos.ORIGIN));
		tag.putString("Rotation", BlockRotation.NONE.name());
		tag.putString("Mirror", BlockMirror.NONE.name());
		blueprint.setNbt(tag);

		writeSize(lookup, blueprint);
		return blueprint;
	}

	@Override
	@Environment(value = EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World worldIn, List<Text> tooltip, TooltipContext flagIn) {
		if (stack.hasNbt()) {
			if (stack.getNbt()
				.contains("File"))
				tooltip.add(Components.literal(Formatting.GOLD + stack.getNbt()
					.getString("File")));
		} else {
			tooltip.add(Lang.translateDirect("schematic.invalid").formatted(Formatting.RED));
		}
		super.appendTooltip(stack, worldIn, tooltip, flagIn);
	}

	public static void writeSize(RegistryEntryLookup<Block> lookup, ItemStack blueprint) {
		NbtCompound tag = blueprint.getNbt();
		StructureTemplate t = loadSchematic(lookup, blueprint);
		tag.put("Bounds", NBTHelper.writeVec3i(t.getSize()));
		blueprint.setNbt(tag);
		SchematicInstances.clearHash(blueprint);
	}

	public static StructurePlacementData getSettings(ItemStack blueprint) {
		return getSettings(blueprint, true);
	}

	public static StructurePlacementData getSettings(ItemStack blueprint, boolean processNBT) {
		NbtCompound tag = blueprint.getNbt();
		StructurePlacementData settings = new StructurePlacementData();
		settings.setRotation(BlockRotation.valueOf(tag.getString("Rotation")));
		settings.setMirror(BlockMirror.valueOf(tag.getString("Mirror")));
		if (processNBT)
			settings.addProcessor(SchematicProcessor.INSTANCE);
		return settings;
	}

	public static StructureTemplate loadSchematic(RegistryEntryLookup<Block> lookup, ItemStack blueprint) {
		StructureTemplate t = new StructureTemplate();
		String owner = blueprint.getNbt()
			.getString("Owner");
		String schematic = blueprint.getNbt()
			.getString("File");

		if (!schematic.endsWith(".nbt"))
			return t;

		Path dir;
		Path file;

//		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
//			dir = Paths.get("schematics", "uploaded").toAbsolutePath();
//			file = Paths.get(owner, schematic);
//		} else {
//			dir = Paths.get("schematics").toAbsolutePath();
//			file = Paths.get(schematic);
//		}
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			dir = Paths.get("schematics", "uploaded").toAbsolutePath();
			file = Paths.get(owner, schematic);
		} else {
			dir = Paths.get("schematics").toAbsolutePath();
			file = Paths.get(schematic);
		}

		Path path = dir.resolve(file).normalize();
		if (!path.startsWith(dir))
			return t;

		try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
				new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
			NbtCompound nbt = NbtIo.read(stream, new NbtTagSizeTracker(0x20000000L));
			t.readNbt(lookup, nbt);
		} catch (IOException e) {
			LOGGER.warn("Failed to read schematic", e);
		}

		return t;
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getPlayer() != null && !onItemUse(context.getPlayer(), context.getHand()))
			return super.useOnBlock(context);
		return ActionResult.SUCCESS;
	}

	@Override
	public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
		if (!onItemUse(playerIn, handIn))
			return super.use(worldIn, playerIn, handIn);
		return new TypedActionResult<>(ActionResult.SUCCESS, playerIn.getStackInHand(handIn));
	}

	private boolean onItemUse(PlayerEntity player, Hand hand) {
		if (!player.isSneaking() || hand != Hand.MAIN_HAND)
			return false;
		if (!player.getStackInHand(hand)
			.hasNbt())
			return false;
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::displayBlueprintScreen);
		return true;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayBlueprintScreen() {
		ScreenOpener.open(new SchematicEditScreen());
	}

}
