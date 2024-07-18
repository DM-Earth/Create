package com.simibubi.create.content.schematics;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.schematics.SchematicExport.SchematicExportResult;
import com.simibubi.create.content.schematics.table.SchematicTableBlockEntity;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CSchematics;

public class ServerSchematicLoader {

	private Map<String, SchematicUploadEntry> activeUploads;

	public class SchematicUploadEntry {
		public World world;
		public BlockPos tablePos;
		public OutputStream stream;
		public long bytesUploaded;
		public long totalBytes;
		public int idleTime;

		public SchematicUploadEntry(OutputStream stream, long totalBytes, World world, BlockPos tablePos) {
			this.stream = stream;
			this.totalBytes = totalBytes;
			this.tablePos = tablePos;
			this.world = world;
			this.bytesUploaded = 0;
			this.idleTime = 0;
		}
	}

	public ServerSchematicLoader() {
		activeUploads = new HashMap<>();
	}

	public String getSchematicPath() {
		return "schematics/uploaded";
	}

	public void tick() {
		// Detect Timed out Uploads
		Set<String> deadEntries = new HashSet<>();
		for (String upload : activeUploads.keySet()) {
			SchematicUploadEntry entry = activeUploads.get(upload);

			if (entry.idleTime++ > getConfig().schematicIdleTimeout.get()) {
				Create.LOGGER.warn("Schematic Upload timed out: " + upload);
				deadEntries.add(upload);
			}

		}

		// Remove Timed out Uploads
		deadEntries.forEach(this::cancelUpload);
	}

	public void shutdown() {
		// Close open streams
		new HashSet<>(activeUploads.keySet()).forEach(this::cancelUpload);
	}

	public void handleNewUpload(ServerPlayerEntity player, String schematic, long size, BlockPos pos) {
		String playerPath = getSchematicPath() + "/" + player.getGameProfile()
			.getName();
		String playerSchematicId = player.getGameProfile()
			.getName() + "/" + schematic;
		FilesHelper.createFolderIfMissing(playerPath);

		// Unsupported Format
		if (!schematic.endsWith(".nbt")) {
			Create.LOGGER.warn("Attempted Schematic Upload with non-supported Format: " + playerSchematicId);
			return;
		}

		Path playerSchematicsPath = Paths.get(getSchematicPath(), player.getGameProfile()
			.getName())
			.toAbsolutePath();

		Path uploadPath = playerSchematicsPath.resolve(schematic)
			.normalize();
		if (!uploadPath.startsWith(playerSchematicsPath)) {
			Create.LOGGER.warn("Attempted Schematic Upload with directory escape: {}", playerSchematicId);
			return;
		}

		// Too big
		if (!validateSchematicSizeOnServer(player, size))
			return;

		// Skip existing Uploads
		if (activeUploads.containsKey(playerSchematicId))
			return;

		try {
			// Validate Referenced Block
			SchematicTableBlockEntity table = getTable(player.getEntityWorld(), pos);
			if (table == null)
				return;

			// Delete schematic with same name
			Files.deleteIfExists(uploadPath);

			// Too many Schematics
			long count;
			try (Stream<Path> list = Files.list(Paths.get(playerPath))) {
				count = list.count();
			}

			if (count >= getConfig().maxSchematics.get()) {
				Stream<Path> list2 = Files.list(Paths.get(playerPath));
				Optional<Path> lastFilePath = list2.filter(f -> !Files.isDirectory(f))
					.min(Comparator.comparingLong(f -> f.toFile()
						.lastModified()));
				list2.close();
				if (lastFilePath.isPresent()) {
					Files.deleteIfExists(lastFilePath.get());
				}
			}

			// Open Stream
			OutputStream writer = Files.newOutputStream(uploadPath);
			activeUploads.put(playerSchematicId, new SchematicUploadEntry(writer, size, player.getWorld(), pos));

			// Notify Block Entity
			table.startUpload(schematic);

		} catch (IOException e) {
			Create.LOGGER.error("Exception Thrown when starting Upload: " + playerSchematicId);
			e.printStackTrace();
		}
	}

	protected boolean validateSchematicSizeOnServer(ServerPlayerEntity player, long size) {
		Integer maxFileSize = getConfig().maxTotalSchematicSize.get();
		if (size > maxFileSize * 1000) {
			player.sendMessage(Lang.translateDirect("schematics.uploadTooLarge")
				.append(Components.literal(" (" + size / 1000 + " KB).")));
			player.sendMessage(Lang.translateDirect("schematics.maxAllowedSize")
				.append(Components.literal(" " + maxFileSize + " KB")));
			return false;
		}
		return true;
	}

	public CSchematics getConfig() {
		return AllConfigs.server().schematics;
	}

	public void handleWriteRequest(ServerPlayerEntity player, String schematic, byte[] data) {
		String playerSchematicId = player.getGameProfile()
			.getName() + "/" + schematic;

		if (activeUploads.containsKey(playerSchematicId)) {
			SchematicUploadEntry entry = activeUploads.get(playerSchematicId);
			entry.bytesUploaded += data.length;

			// Size Validations
			if (data.length > getConfig().maxSchematicPacketSize.get()) {
				Create.LOGGER.warn("Oversized Upload Packet received: " + playerSchematicId);
				cancelUpload(playerSchematicId);
				return;
			}

			if (entry.bytesUploaded > entry.totalBytes) {
				Create.LOGGER.warn("Received more data than Expected: " + playerSchematicId);
				cancelUpload(playerSchematicId);
				return;
			}

			try {
				entry.stream.write(data);
				entry.idleTime = 0;

				SchematicTableBlockEntity table = getTable(entry.world, entry.tablePos);
				if (table == null)
					return;
				table.uploadingProgress = (float) ((double) entry.bytesUploaded / entry.totalBytes);
				table.sendUpdate = true;

			} catch (IOException e) {
				Create.LOGGER.error("Exception Thrown when uploading Schematic: " + playerSchematicId);
				e.printStackTrace();
				cancelUpload(playerSchematicId);
			}
		}
	}

	protected void cancelUpload(String playerSchematicId) {
		if (!activeUploads.containsKey(playerSchematicId))
			return;

		SchematicUploadEntry entry = activeUploads.remove(playerSchematicId);
		try {
			entry.stream.close();
			Files.deleteIfExists(Paths.get(getSchematicPath(), playerSchematicId));
			Create.LOGGER.warn("Cancelled Schematic Upload: " + playerSchematicId);

		} catch (IOException e) {
			Create.LOGGER.error("Exception Thrown when cancelling Upload: " + playerSchematicId);
			e.printStackTrace();
		}

		BlockPos pos = entry.tablePos;
		if (pos == null)
			return;

		SchematicTableBlockEntity table = getTable(entry.world, pos);
		if (table != null)
			table.finishUpload();
	}

	public SchematicTableBlockEntity getTable(World world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (!(be instanceof SchematicTableBlockEntity))
			return null;
		SchematicTableBlockEntity table = (SchematicTableBlockEntity) be;
		return table;
	}

	public void handleFinishedUpload(ServerPlayerEntity player, String schematic) {
		String playerSchematicId = player.getGameProfile()
			.getName() + "/" + schematic;

		if (activeUploads.containsKey(playerSchematicId)) {
			try {
				activeUploads.get(playerSchematicId).stream.close();
				SchematicUploadEntry removed = activeUploads.remove(playerSchematicId);
				World world = removed.world;
				BlockPos pos = removed.tablePos;

				Create.LOGGER.info("New Schematic Uploaded: " + playerSchematicId);
				if (pos == null)
					return;

				BlockState blockState = world.getBlockState(pos);
				if (AllBlocks.SCHEMATIC_TABLE.get() != blockState.getBlock())
					return;

				SchematicTableBlockEntity table = getTable(world, pos);
				if (table == null)
					return;
				table.finishUpload();
				table.inventory.setStackInSlot(1, SchematicItem.create(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), schematic, player.getGameProfile()
					.getName()));

			} catch (IOException e) {
				Create.LOGGER.error("Exception Thrown when finishing Upload: " + playerSchematicId);
				e.printStackTrace();
			}
		}
	}

	public void handleInstantSchematic(ServerPlayerEntity player, String schematic, World world, BlockPos pos,
		BlockPos bounds) {
		String playerName = player.getGameProfile().getName();
		String playerPath = getSchematicPath() + "/" + playerName;
		String playerSchematicId = playerName + "/" + schematic;
		FilesHelper.createFolderIfMissing(playerPath);

		// Unsupported Format
		if (!schematic.endsWith(".nbt")) {
			Create.LOGGER.warn("Attempted Schematic Upload with non-supported Format: {}", playerSchematicId);
			return;
		}

		Path schematicPath = Paths.get(getSchematicPath())
			.toAbsolutePath();

		Path path = schematicPath.resolve(playerSchematicId)
			.normalize();
		if (!path.startsWith(schematicPath)) {
			Create.LOGGER.warn("Attempted Schematic Upload with directory escape: {}", playerSchematicId);
			return;
		}

		// Not holding S&Q
		if (!AllItems.SCHEMATIC_AND_QUILL.isIn(player.getMainHandStack()))
			return;

		// if there's too many schematics, delete oldest
		Path playerSchematics = Paths.get(playerPath);

		if (!tryDeleteOldestSchematic(playerSchematics))
			return;

		SchematicExportResult result = SchematicExport.saveSchematic(
				playerSchematics, schematic, true,
				world, pos, pos.add(bounds).add(-1, -1, -1)
		);
		if (result != null)
			player.setStackInHand(Hand.MAIN_HAND,
				SchematicItem.create(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), schematic, playerName));
		else
			Lang.translate("schematicAndQuill.instant_failed")
				.style(Formatting.RED)
				.sendStatus(player);
	}

	private boolean tryDeleteOldestSchematic(Path dir) {
		try (Stream<Path> stream = Files.list(dir)) {
			List<Path> files = stream.toList();
			if (files.size() < getConfig().maxSchematics.get())
				return true;
			Optional<Path> oldest = files.stream().min(Comparator.comparingLong(this::getLastModifiedTime));
			Files.delete(oldest.orElseThrow());
			return true;
		} catch (IOException | IllegalStateException e) {
			Create.LOGGER.error("Error deleting oldest schematic", e);
			return false;
		}
	}

	private long getLastModifiedTime(Path file) {
		try {
			return Files.getLastModifiedTime(file).toMillis();
		} catch (IOException e) {
			Create.LOGGER.error("Error getting modification time of file " + file.getFileName(), e);
			throw new IllegalStateException(e);
		}
	}

}
