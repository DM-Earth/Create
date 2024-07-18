package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.equipment.zapper.PlacementPatterns;
import com.simibubi.create.content.equipment.zapper.ZapperItem;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WorldshaperItem extends ZapperItem {

	public WorldshaperItem(Settings properties) {
		super(properties);
	}

	@Override
	@Environment(value = EnvType.CLIENT)
	protected void openHandgunGUI(ItemStack item, Hand hand) {
		ScreenOpener.open(new WorldshaperScreen(item, hand));
	}

	@Override
	protected int getZappingRange(ItemStack stack) {
		return 128;
	}

	@Override
	protected int getCooldownDelay(ItemStack item) {
		return 2;
	}

	@Override
	public Text validateUsage(ItemStack item) {
		if (!item.getOrCreateNbt()
			.contains("BrushParams"))
			return Lang.translateDirect("terrainzapper.shiftRightClickToSet");
		return super.validateUsage(item);
	}

	@Override
	protected boolean canActivateWithoutSelectedBlock(ItemStack stack) {
		NbtCompound tag = stack.getOrCreateNbt();
		TerrainTools tool = NBTHelper.readEnum(tag, "Tool", TerrainTools.class);
		return !tool.requiresSelectedBlock();
	}

	@Override
	protected boolean activate(World world, PlayerEntity player, ItemStack stack, BlockState stateToUse,
		BlockHitResult raytrace, NbtCompound data) {

		BlockPos targetPos = raytrace.getBlockPos();
		List<BlockPos> affectedPositions = new ArrayList<>();

		NbtCompound tag = stack.getOrCreateNbt();
		Brush brush = NBTHelper.readEnum(tag, "Brush", TerrainBrushes.class)
			.get();
		BlockPos params = NbtHelper.toBlockPos(tag.getCompound("BrushParams"));
		PlacementOptions option = NBTHelper.readEnum(tag, "Placement", PlacementOptions.class);
		TerrainTools tool = NBTHelper.readEnum(tag, "Tool", TerrainTools.class);

		brush.set(params.getX(), params.getY(), params.getZ());
		targetPos = targetPos.add(brush.getOffset(player.getRotationVector(), raytrace.getSide(), option));
		brush.addToGlobalPositions(world, targetPos, raytrace.getSide(), affectedPositions, tool);
		PlacementPatterns.applyPattern(affectedPositions, stack);
		brush.redirectTool(tool)
			.run(world, affectedPositions, raytrace.getSide(), stateToUse, data, player);

		return true;
	}

	public static void configureSettings(ItemStack stack, PlacementPatterns pattern, TerrainBrushes brush,
		int brushParamX, int brushParamY, int brushParamZ, TerrainTools tool, PlacementOptions placement) {
		ZapperItem.configureSettings(stack, pattern);
		NbtCompound nbt = stack.getOrCreateNbt();
		NBTHelper.writeEnum(nbt, "Brush", brush);
		nbt.put("BrushParams", NbtHelper.fromBlockPos(new BlockPos(brushParamX, brushParamY, brushParamZ)));
		NBTHelper.writeEnum(nbt, "Tool", tool);
		NBTHelper.writeEnum(nbt, "Placement", placement);
	}

//	@Override
//	@OnlyIn(Dist.CLIENT)
//	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new WorldshaperItemRenderer()));
//	}

}
