package com.simibubi.create.foundation.ponder;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.ponder.PonderIndex;
import com.simibubi.create.infrastructure.ponder.SharedText;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PonderRegistry {

	public static final PonderTagRegistry TAGS = new PonderTagRegistry();
	public static final PonderChapterRegistry CHAPTERS = new PonderChapterRegistry();
	// Map from item IDs to storyboard entries
	public static final Map<Identifier, List<PonderStoryBoardEntry>> ALL = new HashMap<>();

	public static void addStoryBoard(PonderStoryBoardEntry entry) {
		synchronized (ALL) {
			List<PonderStoryBoardEntry> list = ALL.computeIfAbsent(entry.getComponent(), $ -> new ArrayList<>());
			synchronized (list) {
				list.add(entry);
			}
		}
	}

	public static List<PonderScene> compile(Identifier id) {
		List<PonderStoryBoardEntry> list = ALL.get(id);
		if (list == null) {
			return Collections.emptyList();
		}
		return compile(list);
	}

	public static List<PonderScene> compile(PonderChapter chapter) {
		List<PonderStoryBoardEntry> list = CHAPTERS.getStories(chapter);
		if (list == null) {
			return Collections.emptyList();
		}
		return compile(list);
	}

	public static List<PonderScene> compile(List<PonderStoryBoardEntry> entries) {
		if (PonderIndex.editingModeActive()) {
			PonderLocalization.SHARED.clear();
			SharedText.gatherText();
		}

		List<PonderScene> scenes = new ArrayList<>();

		for (int i = 0; i < entries.size(); i++) {
			PonderStoryBoardEntry sb = entries.get(i);
			Identifier id = sb.getSchematicLocation();
			StructureTemplate activeTemplate = loadSchematic(id);
			World level = EnvExecutor.unsafeRunForDist(
					() -> () -> MinecraftClient.getInstance().world,
					() -> () -> {
						throw new IllegalStateException("Cannot compile on a server");
					}
			);
			PonderWorld world = new PonderWorld(BlockPos.ORIGIN, level);
			StructurePlacementData settings = FabricPonderProcessing.makePlaceSettings(id);
			activeTemplate.place(world, BlockPos.ORIGIN, BlockPos.ORIGIN, settings, world.random, Block.NOTIFY_LISTENERS);
			world.createBackup();
			PonderScene scene = compileScene(i, sb, world);
			scene.begin();
			scenes.add(scene);
		}

		return scenes;
	}

	public static PonderScene compileScene(int i, PonderStoryBoardEntry sb, PonderWorld world) {
		PonderScene scene = new PonderScene(world, sb.getNamespace(), sb.getComponent(), sb.getTags());
		SceneBuilder builder = scene.builder();
		sb.getBoard()
				.program(builder, scene.getSceneBuildingUtil());
		return scene;
	}

	public static StructureTemplate loadSchematic(Identifier location) {
		return loadSchematic(MinecraftClient.getInstance().getResourceManager(), location);
	}

	public static StructureTemplate loadSchematic(ResourceManager resourceManager, Identifier location) {
		String namespace = location.getNamespace();
		String path = "ponder/" + location.getPath() + ".nbt";
		Identifier location1 = new Identifier(namespace, path);

		Optional<Resource> optionalResource = resourceManager.getResource(location1);
		if (optionalResource.isPresent()) {
			Resource resource = optionalResource.get();
			try (InputStream inputStream = resource.getInputStream()) {
				return loadSchematic(inputStream);
			} catch (IOException e) {
				Create.LOGGER.error("Failed to read ponder schematic: " + location1, e);
			}
		} else {
			Create.LOGGER.error("Ponder schematic missing: " + location1);
		}
		return new StructureTemplate();
	}

	public static StructureTemplate loadSchematic(InputStream resourceStream) throws IOException {
		StructureTemplate t = new StructureTemplate();
		DataInputStream stream =
				new DataInputStream(new BufferedInputStream(new GZIPInputStream(resourceStream)));
		NbtCompound nbt = NbtIo.read(stream, new NbtTagSizeTracker(0x20000000L));
		t.readNbt(MinecraftClient.getInstance().world.createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt);
		return t;
	}

}
