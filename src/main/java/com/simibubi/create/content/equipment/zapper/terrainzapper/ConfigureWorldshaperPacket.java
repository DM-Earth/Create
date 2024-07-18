package com.simibubi.create.content.equipment.zapper.terrainzapper;

import com.simibubi.create.content.equipment.zapper.ConfigureZapperPacket;
import com.simibubi.create.content.equipment.zapper.PlacementPatterns;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

public class ConfigureWorldshaperPacket extends ConfigureZapperPacket {

	protected TerrainBrushes brush;
	protected int brushParamX;
	protected int brushParamY;
	protected int brushParamZ;
	protected TerrainTools tool;
	protected PlacementOptions placement;

	public ConfigureWorldshaperPacket(Hand hand, PlacementPatterns pattern, TerrainBrushes brush, int brushParamX, int brushParamY, int brushParamZ, TerrainTools tool, PlacementOptions placement) {
		super(hand, pattern);
		this.brush = brush;
		this.brushParamX = brushParamX;
		this.brushParamY = brushParamY;
		this.brushParamZ = brushParamZ;
		this.tool = tool;
		this.placement = placement;
	}

	public ConfigureWorldshaperPacket(PacketByteBuf buffer) {
		super(buffer);
		brush = buffer.readEnumConstant(TerrainBrushes.class);
		brushParamX = buffer.readVarInt();
		brushParamY = buffer.readVarInt();
		brushParamZ = buffer.readVarInt();
		tool = buffer.readEnumConstant(TerrainTools.class);
		placement = buffer.readEnumConstant(PlacementOptions.class);
	}

	@Override
	public void write(PacketByteBuf buffer) {
		super.write(buffer);
		buffer.writeEnumConstant(brush);
		buffer.writeVarInt(brushParamX);
		buffer.writeVarInt(brushParamY);
		buffer.writeVarInt(brushParamZ);
		buffer.writeEnumConstant(tool);
		buffer.writeEnumConstant(placement);
	}

	@Override
	public void configureZapper(ItemStack stack) {
		WorldshaperItem.configureSettings(stack, pattern, brush, brushParamX, brushParamY, brushParamZ, tool, placement);
	}

}
