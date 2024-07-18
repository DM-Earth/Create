package com.simibubi.create.content.trains.entity;

import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.Create;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class TrainIconType {

	public static Map<Identifier, TrainIconType> REGISTRY = new HashMap<>();

	public static void register(Identifier id, Identifier sheet, int x, int y) {
		REGISTRY.put(id, new TrainIconType(id, sheet, x, y));
	}

	static {
		Identifier sheet = Create.asResource("textures/gui/assemble.png");
		register(Create.asResource("traditional"), sheet, 2, 205);
		register(Create.asResource("electric"), sheet, 2, 216);
		register(Create.asResource("modern"), sheet, 2, 227);
	}

	Identifier sheet;
	Identifier id;
	int x, y;

	public TrainIconType(Identifier id, Identifier sheet, int x, int y) {
		this.id = id;
		this.sheet = sheet;
		this.x = x;
		this.y = y;
	}

	public static TrainIconType byId(Identifier id) {
		return REGISTRY.getOrDefault(id, getDefault());
	}

	public static TrainIconType getDefault() {
		return REGISTRY.get(Create.asResource("traditional"));
	}

	public Identifier getId() {
		return id;
	}

	public static final int ENGINE = -1;
	public static final int FLIPPED_ENGINE = -2;

	@Environment(EnvType.CLIENT)
	public int render(int lengthOrEngine, DrawContext graphics, int x, int y) {
		int offset = getIconOffset(lengthOrEngine);
		int width = getIconWidth(lengthOrEngine);
		graphics.drawTexture(sheet, x, y, 0, this.x + offset, this.y, width, 10, 256, 256);
		return width;
	}

	public int getIconWidth(int lengthOrEngine) {
		if (lengthOrEngine == FLIPPED_ENGINE)
			return 19;
		if (lengthOrEngine == ENGINE)
			return 19;
		if (lengthOrEngine < 3)
			return 7;
		if (lengthOrEngine < 9)
			return 13;
		return 19;
	}

	public int getIconOffset(int lengthOrEngine) {
		if (lengthOrEngine == FLIPPED_ENGINE)
			return 0;
		if (lengthOrEngine == ENGINE)
			return 62;
		if (lengthOrEngine < 3)
			return 34;
		if (lengthOrEngine < 9)
			return 20;
		return 42;
	}

}
