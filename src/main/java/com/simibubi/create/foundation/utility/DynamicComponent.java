package com.simibubi.create.foundation.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.Create;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DynamicComponent {

	private JsonElement rawCustomText;
	private Text parsedCustomText;

	public DynamicComponent() {}

	public void displayCustomText(World level, BlockPos pos, String tagElement) {
		if (tagElement == null)
			return;

		rawCustomText = getJsonFromString(tagElement);
		parsedCustomText = parseCustomText(level, pos, rawCustomText);
	}

	public boolean sameAs(String tagElement) {
		return isValid() && rawCustomText.equals(getJsonFromString(tagElement));
	}

	public boolean isValid() {
		return parsedCustomText != null && rawCustomText != null;
	}

	public String resolve() {
		return parsedCustomText.getString();
	}

	public MutableText get() {
		return parsedCustomText == null ? Components.empty() : parsedCustomText.copy();
	}

	public void read(World level, BlockPos pos, NbtCompound nbt) {
		rawCustomText = getJsonFromString(nbt.getString("RawCustomText"));
		try {
			parsedCustomText = Text.Serializer.fromJson(nbt.getString("CustomText"));
		} catch (JsonParseException e) {
			parsedCustomText = null;
		}
	}

	public void write(NbtCompound nbt) {
		if (!isValid())
			return;

		nbt.putString("RawCustomText", rawCustomText.toString());
		nbt.putString("CustomText", Text.Serializer.toJson(parsedCustomText));
	}

	public static JsonElement getJsonFromString(String string) {
		try {
			return JsonParser.parseString(string);
		} catch (JsonParseException e) {
			return null;
		}
	}

	public static Text parseCustomText(World level, BlockPos pos, JsonElement customText) {
		if (!(level instanceof ServerWorld serverLevel))
			return null;
		try {
			return Texts.parse(getCommandSource(serverLevel, pos),
				Text.Serializer.fromJson(customText), null, 0);
		} catch (JsonParseException e) {
			return null;
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	public static ServerCommandSource getCommandSource(ServerWorld level, BlockPos pos) {
		return new ServerCommandSource(CommandOutput.DUMMY, Vec3d.ofCenter(pos), Vec2f.ZERO, level, 2, Create.ID,
			Components.literal(Create.ID), level.getServer(), null);
	}

}
