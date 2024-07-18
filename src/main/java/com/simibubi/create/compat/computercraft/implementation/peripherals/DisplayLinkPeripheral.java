package com.simibubi.create.compat.computercraft.implementation.peripherals;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import dan200.computercraft.api.lua.LuaFunction;

public class DisplayLinkPeripheral extends SyncedPeripheral<DisplayLinkBlockEntity> {

	public static final String TAG_KEY = "ComputerSourceList";
	private final AtomicInteger cursorX = new AtomicInteger();
	private final AtomicInteger cursorY = new AtomicInteger();

	public DisplayLinkPeripheral(DisplayLinkBlockEntity blockEntity) {
		super(blockEntity);
	}

	@LuaFunction
	public final void setCursorPos(int x, int y) {
		cursorX.set(x - 1);
		cursorY.set(y - 1);
	}

	@LuaFunction
	public final Object[] getCursorPos() {
		return new Object[] {cursorX.get() + 1, cursorY.get() + 1};
	}

	@LuaFunction(mainThread = true)
	public final Object[] getSize() {
		DisplayTargetStats stats = blockEntity.activeTarget.provideStats(new DisplayLinkContext(blockEntity.getWorld(), blockEntity));
		return new Object[]{stats.maxRows(), stats.maxColumns()};
	}

	@LuaFunction
	public final boolean isColor() {
		return false;
	}

	@LuaFunction
	public final boolean isColour() {
		return false;
	}

	@LuaFunction
	public final void write(String text) {
		NbtList tag = blockEntity.getSourceConfig().getList(TAG_KEY, NbtElement.STRING_TYPE);

		int x = cursorX.get();
		int y = cursorY.get();

		for (int i = tag.size(); i <= y; i++) {
			tag.add(NbtString.of(""));
		}

		StringBuilder builder = new StringBuilder(tag.getString(y));

		builder.append(" ".repeat(Math.max(0, x - builder.length())));
		builder.replace(x, x + text.length(), text);

		tag.set(y, NbtString.of(builder.toString()));

		synchronized (blockEntity) {
			blockEntity.getSourceConfig().put(TAG_KEY, tag);
		}

		cursorX.set(x + text.length());
	}

	@LuaFunction
	public final void clearLine() {
		NbtList tag = blockEntity.getSourceConfig().getList(TAG_KEY, NbtElement.STRING_TYPE);

		if (tag.size() > cursorY.get())
			tag.set(cursorY.get(), NbtString.of(""));

		synchronized (blockEntity) {
			blockEntity.getSourceConfig().put(TAG_KEY, tag);
		}
	}

	@LuaFunction
	public final void clear() {
		synchronized (blockEntity) {
			blockEntity.getSourceConfig().put(TAG_KEY, new NbtList());
		}
	}

	@LuaFunction(mainThread = true)
	public final void update() {
		blockEntity.tickSource();
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_DisplayLink";
	}

}
