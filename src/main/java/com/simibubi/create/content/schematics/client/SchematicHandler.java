package com.simibubi.create.content.schematics.client;

import java.util.List;
import java.util.Vector;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.content.schematics.packet.SchematicPlacePacket;
import com.simibubi.create.content.schematics.packet.SchematicSyncPacket;
import com.simibubi.create.foundation.outliner.AABBOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;

public class SchematicHandler {

	private String displayedSchematic;
	private SchematicTransformation transformation;
	private Box bounds;
	private boolean deployed;
	private boolean active;
	private ToolType currentTool;

	private static final int SYNC_DELAY = 10;
	private int syncCooldown;
	private int activeHotbarSlot;
	private ItemStack activeSchematicItem;
	private AABBOutline outline;

	private Vector<SchematicRenderer> renderers;
	private SchematicHotbarSlotOverlay overlay;
	private ToolSelectionScreen selectionScreen;

	public SchematicHandler() {
		renderers = new Vector<>(3);
		for (int i = 0; i < renderers.capacity(); i++)
			renderers.add(new SchematicRenderer());

		overlay = new SchematicHotbarSlotOverlay();
		currentTool = ToolType.DEPLOY;
		selectionScreen = new ToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
		transformation = new SchematicTransformation();
	}

	public void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
			if (active) {
				active = false;
				syncCooldown = 0;
				activeHotbarSlot = 0;
				activeSchematicItem = null;
				renderers.forEach(r -> r.setActive(false));
			}
			return;
		}

		if (activeSchematicItem != null && transformation != null)
			transformation.tick();

		ClientPlayerEntity player = mc.player;
		ItemStack stack = findBlueprintInHand(player);
		if (stack == null) {
			active = false;
			syncCooldown = 0;
			if (activeSchematicItem != null && itemLost(player)) {
				activeHotbarSlot = 0;
				activeSchematicItem = null;
				renderers.forEach(r -> r.setActive(false));
			}
			return;
		}

		if (!active || !stack.getNbt()
			.getString("File")
			.equals(displayedSchematic))
			init(player, stack);
		if (!active)
			return;

		renderers.forEach(SchematicRenderer::tick);
		if (syncCooldown > 0)
			syncCooldown--;
		if (syncCooldown == 1)
			sync();

		selectionScreen.update();
		currentTool.getTool()
			.updateSelection();
	}

	private void init(ClientPlayerEntity player, ItemStack stack) {
		loadSettings(stack);
		displayedSchematic = stack.getNbt()
			.getString("File");
		active = true;
		if (deployed) {
			setupRenderer();
			ToolType toolBefore = currentTool;
			selectionScreen = new ToolSelectionScreen(ToolType.getTools(player.isCreative()), this::equip);
			if (toolBefore != null) {
				selectionScreen.setSelectedElement(toolBefore);
				equip(toolBefore);
			}
		} else
			selectionScreen = new ToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
	}

	private void setupRenderer() {
		World clientWorld = MinecraftClient.getInstance().world;
		StructureTemplate schematic =
			SchematicItem.loadSchematic(clientWorld.createCommandRegistryWrapper(RegistryKeys.BLOCK), activeSchematicItem);
		Vec3i size = schematic.getSize();
		if (size.equals(Vec3i.ZERO))
			return;

		SchematicWorld w = new SchematicWorld(clientWorld);
		SchematicWorld wMirroredFB = new SchematicWorld(clientWorld);
		SchematicWorld wMirroredLR = new SchematicWorld(clientWorld);
		StructurePlacementData placementSettings = new StructurePlacementData();
		StructureTransform transform;
		BlockPos pos;

		pos = BlockPos.ORIGIN;

		try {
			schematic.place(w, pos, pos, placementSettings, w.getRandom(), Block.NOTIFY_LISTENERS);
		} catch (Exception e) {
			MinecraftClient.getInstance().player.sendMessage(Lang.translate("schematic.error")
				.component(), false);
			Create.LOGGER.error("Failed to load Schematic for Previewing", e);
			return;
		}

		placementSettings.setMirror(BlockMirror.FRONT_BACK);
		pos = BlockPos.ORIGIN.east(size.getX() - 1);
		schematic.place(wMirroredFB, pos, pos, placementSettings, wMirroredFB.getRandom(), Block.NOTIFY_LISTENERS);
		transform = new StructureTransform(placementSettings.getPosition(), Axis.Y, BlockRotation.NONE,
			placementSettings.getMirror());
		for (BlockEntity be : wMirroredFB.getRenderedBlockEntities())
			transform.apply(be);

		placementSettings.setMirror(BlockMirror.LEFT_RIGHT);
		pos = BlockPos.ORIGIN.south(size.getZ() - 1);
		schematic.place(wMirroredLR, pos, pos, placementSettings, wMirroredFB.getRandom(), Block.NOTIFY_LISTENERS);
		transform = new StructureTransform(placementSettings.getPosition(), Axis.Y, BlockRotation.NONE,
			placementSettings.getMirror());
		for (BlockEntity be : wMirroredLR.getRenderedBlockEntities())
			transform.apply(be);

		renderers.get(0)
			.display(w);
		renderers.get(1)
			.display(wMirroredFB);
		renderers.get(2)
			.display(wMirroredLR);
	}

	public void render(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera) {
		boolean present = activeSchematicItem != null;
		if (!active && !present)
			return;

		if (active) {
			ms.push();
			currentTool.getTool()
				.renderTool(ms, buffer, camera);
			ms.pop();
		}

		ms.push();
		transformation.applyTransformations(ms, camera);

		if (!renderers.isEmpty()) {
			float pt = AnimationTickHolder.getPartialTicks();
			boolean lr = transformation.getScaleLR()
				.getValue(pt) < 0;
			boolean fb = transformation.getScaleFB()
				.getValue(pt) < 0;
			if (lr && !fb)
				renderers.get(2)
					.render(ms, buffer);
			else if (fb && !lr)
				renderers.get(1)
					.render(ms, buffer);
			else
				renderers.get(0)
					.render(ms, buffer);
		}

		if (active)
			currentTool.getTool()
				.renderOnSchematic(ms, buffer);
		ms.pop();

	}

	public void updateRenderers() {
		for (SchematicRenderer renderer : renderers) {
			renderer.update();
		}
	}

	public void renderOverlay(DrawContext graphics, float partialTicks, Window window) {
		if (MinecraftClient.getInstance().options.hudHidden || !active)
			return;
		if (activeSchematicItem != null)
			this.overlay.renderOn(graphics, activeHotbarSlot);
		currentTool.getTool()
			.renderOverlay(graphics, partialTicks, window.getScaledWidth(), window.getScaledHeight());
		selectionScreen.renderPassive(graphics, partialTicks);
	}

	public boolean onMouseInput(int button, boolean pressed) {
		if (!active)
			return false;
		if (!pressed || button != 1)
			return false;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player.isSneaking())
			return false;
		if (mc.crosshairTarget instanceof BlockHitResult) {
			BlockHitResult blockRayTraceResult = (BlockHitResult) mc.crosshairTarget;
			BlockState clickedBlock = mc.world.getBlockState(blockRayTraceResult.getBlockPos());
			if (AllBlocks.SCHEMATICANNON.has(clickedBlock))
				return false;
			if (AllBlocks.DEPLOYER.has(clickedBlock))
				return false;
		}
		return currentTool.getTool()
			.handleRightClick();
	}

	public void onKeyInput(int key, boolean pressed) {
		if (!active)
			return;
		if (key != AllKeys.TOOL_MENU.getBoundCode())
			return;

		if (pressed && !selectionScreen.focused)
			selectionScreen.focused = true;
		if (!pressed && selectionScreen.focused) {
			selectionScreen.focused = false;
			selectionScreen.close();
		}
	}

	public boolean mouseScrolled(double delta) {
		if (!active)
			return false;

		if (selectionScreen.focused) {
			selectionScreen.cycle((int) delta);
			return true;
		}
		if (AllKeys.ctrlDown())
			return currentTool.getTool()
				.handleMouseWheel(delta);
		return false;
	}

	private ItemStack findBlueprintInHand(PlayerEntity player) {
		ItemStack stack = player.getMainHandStack();
		if (!AllItems.SCHEMATIC.isIn(stack))
			return null;
		if (!stack.hasNbt())
			return null;

		activeSchematicItem = stack;
		activeHotbarSlot = player.getInventory().selectedSlot;
		return stack;
	}

	private boolean itemLost(PlayerEntity player) {
		for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
			if (!ItemStack.areEqual(player.getInventory()
				.getStack(i), activeSchematicItem))
				continue;
			return false;
		}
		return true;
	}

	public void markDirty() {
		syncCooldown = SYNC_DELAY;
	}

	public void sync() {
		if (activeSchematicItem == null)
			return;
		AllPackets.getChannel().sendToServer(new SchematicSyncPacket(activeHotbarSlot, transformation.toSettings(),
			transformation.getAnchor(), deployed));
	}

	public void equip(ToolType tool) {
		this.currentTool = tool;
		currentTool.getTool()
			.init();
	}

	public void loadSettings(ItemStack blueprint) {
		NbtCompound tag = blueprint.getNbt();
		BlockPos anchor = BlockPos.ORIGIN;
		StructurePlacementData settings = SchematicItem.getSettings(blueprint);
		transformation = new SchematicTransformation();

		deployed = tag.getBoolean("Deployed");
		if (deployed)
			anchor = NbtHelper.toBlockPos(tag.getCompound("Anchor"));
		Vec3i size = NBTHelper.readVec3i(tag.getList("Bounds", NbtElement.INT_TYPE));

		bounds = new Box(0, 0, 0, size.getX(), size.getY(), size.getZ());
		outline = new AABBOutline(bounds);
		outline.getParams()
			.colored(0x6886c5)
			.lineWidth(1 / 16f);
		transformation.init(anchor, settings, bounds);
	}

	public void deploy() {
		if (!deployed) {
			List<ToolType> tools = ToolType.getTools(MinecraftClient.getInstance().player.isCreative());
			selectionScreen = new ToolSelectionScreen(tools, this::equip);
		}
		deployed = true;
		setupRenderer();
	}

	public String getCurrentSchematicName() {
		return displayedSchematic != null ? displayedSchematic : "-";
	}

	public void printInstantly() {
		AllPackets.getChannel().sendToServer(new SchematicPlacePacket(activeSchematicItem.copy()));
		NbtCompound nbt = activeSchematicItem.getNbt();
		nbt.putBoolean("Deployed", false);
		activeSchematicItem.setNbt(nbt);
		SchematicInstances.clearHash(activeSchematicItem);
		renderers.forEach(r -> r.setActive(false));
		active = false;
		markDirty();
	}

	public boolean isActive() {
		return active;
	}

	public Box getBounds() {
		return bounds;
	}

	public SchematicTransformation getTransformation() {
		return transformation;
	}

	public boolean isDeployed() {
		return deployed;
	}

	public ItemStack getActiveSchematicItem() {
		return activeSchematicItem;
	}

	public AABBOutline getOutline() {
		return outline;
	}

}
