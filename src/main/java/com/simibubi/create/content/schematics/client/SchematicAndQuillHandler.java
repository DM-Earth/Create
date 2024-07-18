package com.simibubi.create.content.schematics.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicExport.SchematicExportResult;
import com.simibubi.create.content.schematics.packet.InstantSchematicPacket;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import com.simibubi.create.foundation.utility.VecHelper;

public class SchematicAndQuillHandler {

	private Object outlineSlot = new Object();

	public BlockPos firstPos;
	public BlockPos secondPos;
	private BlockPos selectedPos;
	private Direction selectedFace;
	private int range = 10;

	public boolean mouseScrolled(double delta) {
		if (!isActive())
			return false;
		if (!AllKeys.ctrlDown())
			return false;
		if (secondPos == null)
			range = (int) MathHelper.clamp(range + delta, 1, 100);
		if (selectedFace == null)
			return true;

		Box bb = new Box(firstPos, secondPos);
		Vec3i vec = selectedFace.getVector();
		Vec3d projectedView = MinecraftClient.getInstance().gameRenderer.getCamera()
			.getPos();
		if (bb.contains(projectedView))
			delta *= -1;

		int x = (int) (vec.getX() * delta);
		int y = (int) (vec.getY() * delta);
		int z = (int) (vec.getZ() * delta);

		AxisDirection axisDirection = selectedFace.getDirection();
		if (axisDirection == AxisDirection.NEGATIVE)
			bb = bb.offset(-x, -y, -z);

		double maxX = Math.max(bb.maxX - x * axisDirection.offset(), bb.minX);
		double maxY = Math.max(bb.maxY - y * axisDirection.offset(), bb.minY);
		double maxZ = Math.max(bb.maxZ - z * axisDirection.offset(), bb.minZ);
		bb = new Box(bb.minX, bb.minY, bb.minZ, maxX, maxY, maxZ);

		firstPos = BlockPos.ofFloored(bb.minX, bb.minY, bb.minZ);
		secondPos = BlockPos.ofFloored(bb.maxX, bb.maxY, bb.maxZ);
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		Lang.translate("schematicAndQuill.dimensions", (int) bb.getXLength() + 1, (int) bb.getYLength() + 1,
			(int) bb.getZLength() + 1)
			.sendStatus(player);

		return true;
	}

	public boolean onMouseInput(int button, boolean pressed) {
		if (!pressed || button != 1)
			return false;
		if (!isActive())
			return false;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;

		if (player.isSneaking()) {
			discard();
			return true;
		}

		if (secondPos != null) {
			ScreenOpener.open(new SchematicPromptScreen());
			return true;
		}

		if (selectedPos == null) {
			Lang.translate("schematicAndQuill.noTarget")
				.sendStatus(player);
			return true;
		}

		if (firstPos != null) {
			secondPos = selectedPos;
			Lang.translate("schematicAndQuill.secondPos")
				.sendStatus(player);
			return true;
		}

		firstPos = selectedPos;
		Lang.translate("schematicAndQuill.firstPos")
			.sendStatus(player);
		return true;
	}

	public void discard() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		firstPos = null;
		secondPos = null;
		Lang.translate("schematicAndQuill.abort")
			.sendStatus(player);
	}

	public void tick() {
		if (!isActive())
			return;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (AllKeys.ACTIVATE_TOOL.isPressed()) {
			float pt = AnimationTickHolder.getPartialTicks();
			Vec3d targetVec = player.getCameraPosVec(pt)
				.add(player.getRotationVector()
					.multiply(range));
			selectedPos = BlockPos.ofFloored(targetVec);

		} else {
			BlockHitResult trace = RaycastHelper.rayTraceRange(player.getWorld(), player, 75);
			if (trace != null && trace.getType() == Type.BLOCK) {

				BlockPos hit = trace.getBlockPos();
				boolean replaceable = player.getWorld().getBlockState(hit)
					.canReplace(new ItemPlacementContext(new ItemUsageContext(player, Hand.MAIN_HAND, trace)));
				if (trace.getSide()
					.getAxis()
					.isVertical() && !replaceable)
					hit = hit.offset(trace.getSide());
				selectedPos = hit;
			} else
				selectedPos = null;
		}

		selectedFace = null;
		if (secondPos != null) {
			Box bb = new Box(firstPos, secondPos).stretch(1, 1, 1)
				.expand(.45f);
			Vec3d projectedView = MinecraftClient.getInstance().gameRenderer.getCamera()
				.getPos();
			boolean inside = bb.contains(projectedView);
			PredicateTraceResult result =
				RaycastHelper.rayTraceUntil(player, 70, pos -> inside ^ bb.contains(VecHelper.getCenterOf(pos)));
			selectedFace = result.missed() ? null
				: inside ? result.getFacing()
					.getOpposite() : result.getFacing();
		}

		Box currentSelectionBox = getCurrentSelectionBox();
		if (currentSelectionBox != null)
			outliner().chaseAABB(outlineSlot, currentSelectionBox)
				.colored(0x6886c5)
				.withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
				.lineWidth(1 / 16f)
				.highlightFace(selectedFace);
	}

	private Box getCurrentSelectionBox() {
		if (secondPos == null) {
			if (firstPos == null)
				return selectedPos == null ? null : new Box(selectedPos);
			return selectedPos == null ? new Box(firstPos) : new Box(firstPos, selectedPos).stretch(1, 1, 1);
		}
		return new Box(firstPos, secondPos).stretch(1, 1, 1);
	}

	private boolean isActive() {
		return isPresent() && AllItems.SCHEMATIC_AND_QUILL.isIn(MinecraftClient.getInstance().player.getMainHandStack());
	}

	private boolean isPresent() {
		return MinecraftClient.getInstance() != null && MinecraftClient.getInstance().world != null
			&& MinecraftClient.getInstance().currentScreen == null;
	}

	public void saveSchematic(String string, boolean convertImmediately) {
		SchematicExportResult result = SchematicExport.saveSchematic(
				SchematicExport.SCHEMATICS, string, false,
				MinecraftClient.getInstance().world, firstPos, secondPos
		);
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (result == null) {
			Lang.translate("schematicAndQuill.failed")
					.style(Formatting.RED)
					.sendStatus(player);
			return;
		}
		Path file = result.file();
		Lang.translate("schematicAndQuill.saved", file.getFileName())
				.sendStatus(player);
		firstPos = null;
		secondPos = null;
		if (!convertImmediately)
			return;
		try {
			if (!ClientSchematicLoader.validateSizeLimitation(Files.size(file)))
				return;
			AllPackets.getChannel()
				.sendToServer(new InstantSchematicPacket(result.fileName(), result.origin(), result.bounds()));
		} catch (IOException e) {
			Create.LOGGER.error("Error instantly uploading Schematic file: " + file, e);
		}
	}

	private Outliner outliner() {
		return CreateClient.OUTLINER;
	}

}
