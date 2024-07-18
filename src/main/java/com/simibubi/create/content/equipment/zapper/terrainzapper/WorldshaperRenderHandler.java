package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.NBTHelper;

public class WorldshaperRenderHandler {

	private static Supplier<Collection<BlockPos>> renderedPositions;

	public static void tick() {
		gatherSelectedBlocks();
		if (renderedPositions == null)
			return;

		CreateClient.OUTLINER.showCluster("terrainZapper", renderedPositions.get())
				.colored(0xbfbfbf)
				.disableLineNormals()
				.lineWidth(1 / 32f)
				.withFaceTexture(AllSpecialTextures.CHECKERED);
	}

	protected static void gatherSelectedBlocks() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		ItemStack heldMain = player.getMainHandStack();
		ItemStack heldOff = player.getOffHandStack();
		boolean zapperInMain = AllItems.WORLDSHAPER.isIn(heldMain);
		boolean zapperInOff = AllItems.WORLDSHAPER.isIn(heldOff);

		if (zapperInMain) {
			NbtCompound tag = heldMain.getOrCreateNbt();
			if (!tag.contains("_Swap") || !zapperInOff) {
				createBrushOutline(tag, player, heldMain);
				return;
			}
		}

		if (zapperInOff) {
			NbtCompound tag = heldOff.getOrCreateNbt();
			createBrushOutline(tag, player, heldOff);
			return;
		}

		renderedPositions = null;
	}

	public static void createBrushOutline(NbtCompound tag, ClientPlayerEntity player, ItemStack zapper) {
		if (!tag.contains("BrushParams")) {
			renderedPositions = null;
			return;
		}

		Brush brush = NBTHelper.readEnum(tag, "Brush", TerrainBrushes.class)
			.get();
		PlacementOptions placement = NBTHelper.readEnum(tag, "Placement", PlacementOptions.class);
		TerrainTools tool = NBTHelper.readEnum(tag, "Tool", TerrainTools.class);
		BlockPos params = NbtHelper.toBlockPos(tag.getCompound("BrushParams"));
		brush.set(params.getX(), params.getY(), params.getZ());

		Vec3d start = player.getPos()
			.add(0, player.getStandingEyeHeight(), 0);
		Vec3d range = player.getRotationVector()
			.multiply(128);
		BlockHitResult raytrace = player.getWorld()
			.raycast(new RaycastContext(start, start.add(range), ShapeType.OUTLINE, FluidHandling.NONE, player));
		if (raytrace == null || raytrace.getType() == Type.MISS) {
			renderedPositions = null;
			return;
		}

		BlockPos pos = raytrace.getBlockPos()
			.add(brush.getOffset(player.getRotationVector(), raytrace.getSide(), placement));
		renderedPositions =
			() -> brush.addToGlobalPositions(player.getWorld(), pos, raytrace.getSide(), new ArrayList<>(), tool);
	}

}
