package com.simibubi.create.content.trains.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import com.google.gson.JsonElement;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.utility.DynamicComponent;
import com.simibubi.create.foundation.utility.NBTHelper;

public class FlapDisplayBlockEntity extends KineticBlockEntity {

	public List<FlapDisplayLayout> lines;
	public boolean isController;
	public boolean isRunning;
	public int xSize, ySize;
	public DyeColor[] colour;
	public boolean[] glowingLines;
	public boolean[] manualLines;

	public FlapDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
		isController = false;
		xSize = 1;
		ySize = 1;
		colour = new DyeColor[2];
		manualLines = new boolean[2];
		glowingLines = new boolean[2];
	}

	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		updateControllerStatus();
	}

	public void updateControllerStatus() {
		if (world.isClient)
			return;

		BlockState blockState = getCachedState();
		if (!(blockState.getBlock() instanceof FlapDisplayBlock))
			return;

		Direction leftDirection = blockState.get(FlapDisplayBlock.HORIZONTAL_FACING)
			.rotateYClockwise();
		boolean shouldBeController = !blockState.get(FlapDisplayBlock.UP)
			&& world.getBlockState(pos.offset(leftDirection)) != blockState;

		int newXSize = 1;
		int newYSize = 1;

		if (shouldBeController) {
			for (int xOffset = 1; xOffset < 32; xOffset++) {
				if (world.getBlockState(pos.offset(leftDirection.getOpposite(), xOffset)) != blockState)
					break;
				newXSize++;
			}
			for (int yOffset = 0; yOffset < 32; yOffset++) {
				if (!world.getBlockState(pos.offset(Direction.DOWN, yOffset))
					.getOrEmpty(FlapDisplayBlock.DOWN)
					.orElse(false))
					break;
				newYSize++;
			}
		}

		if (isController == shouldBeController && newXSize == xSize && newYSize == ySize)
			return;

		isController = shouldBeController;
		xSize = newXSize;
		ySize = newYSize;
		colour = Arrays.copyOf(colour, ySize * 2);
		glowingLines = Arrays.copyOf(glowingLines, ySize * 2);
		manualLines = new boolean[ySize * 2];
		lines = null;
		sendData();
	}

	@Override
	public void tick() {
		super.tick();
		isRunning = super.isSpeedRequirementFulfilled();
		if ((!world.isClient || !isRunning) && !isVirtual())
			return;
		int activeFlaps = 0;
		boolean instant = getSpeed() > 128;
		for (FlapDisplayLayout line : lines)
			for (FlapDisplaySection section : line.getSections())
				activeFlaps += section.tick(instant);
		if (activeFlaps == 0)
			return;

		float volume = MathHelper.clamp(activeFlaps / 20f, 0.25f, 1.5f);
		float bgVolume = MathHelper.clamp(activeFlaps / 40f, 0.25f, 1f);
		BlockPos middle = pos.offset(getDirection().rotateYClockwise(), xSize / 2)
			.offset(Direction.DOWN, ySize / 2);
		AllSoundEvents.SCROLL_VALUE.playAt(world, middle, volume, 0.56f, false);
		world.playSound(middle.getX(), middle.getY(), middle.getZ(), SoundEvents.BLOCK_CALCITE_HIT, SoundCategory.BLOCKS,
			.35f * bgVolume, 1.95f, false);
	}

	@Override
	protected boolean isNoisy() {
		return false;
	}

	@Override
	public boolean isSpeedRequirementFulfilled() {
		return isRunning;
	}

	public void applyTextManually(int lineIndex, String rawComponentText) {
		List<FlapDisplayLayout> lines = getLines();
		if (lineIndex >= lines.size())
			return;
		
		FlapDisplayLayout layout = lines.get(lineIndex);
		if (!layout.isLayout("Default"))
			layout.loadDefault(getMaxCharCount());
		List<FlapDisplaySection> sections = layout.getSections();

		FlapDisplaySection flapDisplaySection = sections.get(0);
		if (rawComponentText == null) {
			manualLines[lineIndex] = false;
			flapDisplaySection.setText(Components.immutableEmpty());
			notifyUpdate();
			return;
		}

		JsonElement json = DynamicComponent.getJsonFromString(rawComponentText);
		if (json == null)
			return;

		manualLines[lineIndex] = true;
		Text text = isVirtual() ? Text.Serializer.fromJson(rawComponentText)
			: DynamicComponent.parseCustomText(world, pos, json);
		flapDisplaySection.setText(text);
		if (isVirtual())
			flapDisplaySection.refresh(true);
		else
			notifyUpdate();
	}

	public void setColour(int lineIndex, DyeColor color) {
		colour[lineIndex] = color == DyeColor.WHITE ? null : color;
		notifyUpdate();
	}
	
	public void setGlowing(int lineIndex) {
		glowingLines[lineIndex] = true;
		notifyUpdate();
	}

	public List<FlapDisplayLayout> getLines() {
		if (lines == null)
			initDefaultSections();
		return lines;
	}

	public void initDefaultSections() {
		lines = new ArrayList<>();
		for (int i = 0; i < ySize * 2; i++)
			lines.add(new FlapDisplayLayout(getMaxCharCount()));
	}

	public int getMaxCharCount() {
		return getMaxCharCount(0);
	}

	public int getMaxCharCount(int gaps) {
		return (int) ((xSize * 16f - 2f - 4f * gaps) / 3.5f);
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		super.write(tag, clientPacket);

		tag.putBoolean("Controller", isController);
		tag.putInt("XSize", xSize);
		tag.putInt("YSize", ySize);

		for (int j = 0; j < manualLines.length; j++)
			if (manualLines[j])
				NBTHelper.putMarker(tag, "CustomLine" + j);
		
		for (int j = 0; j < glowingLines.length; j++)
			if (glowingLines[j])
				NBTHelper.putMarker(tag, "GlowingLine" + j);

		for (int j = 0; j < colour.length; j++)
			if (colour[j] != null)
				NBTHelper.writeEnum(tag, "Dye" + j, colour[j]);

		List<FlapDisplayLayout> lines = getLines();
		for (int i = 0; i < lines.size(); i++)
			tag.put("Display" + i, lines.get(i)
				.write());
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		boolean wasActive = isController;
		int prevX = xSize;
		int prevY = ySize;

		isController = tag.getBoolean("Controller");
		xSize = tag.getInt("XSize");
		ySize = tag.getInt("YSize");

		manualLines = new boolean[ySize * 2];
		for (int i = 0; i < ySize * 2; i++)
			manualLines[i] = tag.contains("CustomLine" + i);
		
		glowingLines = new boolean[ySize * 2];
		for (int i = 0; i < ySize * 2; i++)
			glowingLines[i] = tag.contains("GlowingLine" + i);

		colour = new DyeColor[ySize * 2];
		for (int i = 0; i < ySize * 2; i++)
			colour[i] = tag.contains("Dye" + i) ? NBTHelper.readEnum(tag, "Dye" + i, DyeColor.class) : null;

		if (clientPacket && wasActive != isController || prevX != xSize || prevY != ySize) {
			invalidateRenderBoundingBox();
			lines = null;
		}

		List<FlapDisplayLayout> lines = getLines();
		for (int i = 0; i < lines.size(); i++)
			lines.get(i)
				.read(tag.getCompound("Display" + i));
	}

	public int getLineIndexAt(double yCoord) {
		return (int) MathHelper.clamp(Math.floor(2 * (pos.getY() - yCoord + 1)), 0, ySize * 2);
	}

	public FlapDisplayBlockEntity getController() {
		if (isController)
			return this;

		BlockState blockState = getCachedState();
		if (!(blockState.getBlock() instanceof FlapDisplayBlock))
			return null;

		Mutable pos = getPos().mutableCopy();
		Direction side = blockState.get(FlapDisplayBlock.HORIZONTAL_FACING)
			.rotateYClockwise();

		for (int i = 0; i < 64; i++) {
			BlockState other = world.getBlockState(pos);

			if (other.getOrEmpty(FlapDisplayBlock.UP)
				.orElse(false)) {
				pos.move(Direction.UP);
				continue;
			}

			if (!world.getBlockState(pos.offset(side))
				.getOrEmpty(FlapDisplayBlock.UP)
				.orElse(true)) {
				pos.move(side);
				continue;
			}

			BlockEntity found = world.getBlockEntity(pos);
			if (found instanceof FlapDisplayBlockEntity flap && flap.isController)
				return flap;

			break;
		}

		return null;
	}

	@Override
	protected Box createRenderBoundingBox() {
		Box aabb = new Box(pos);
		if (!isController)
			return aabb;
		Vec3i normal = getDirection().rotateYClockwise()
			.getVector();
		return aabb.stretch(normal.getX() * xSize, -ySize, normal.getZ() * xSize);
	}

	public Direction getDirection() {
		return getCachedState().getOrEmpty(FlapDisplayBlock.HORIZONTAL_FACING)
			.orElse(Direction.SOUTH)
			.getOpposite();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public int getLineColor(int line) {
		DyeColor color = colour[line];
		return color == null ? 0xFF_D3C6BA
			: DyeHelper.DYE_TABLE.get(color)
				.getFirst() | 0xFF_000000;
	}
	
	public boolean isLineGlowing(int line) {
		return glowingLines[line];
	}
	
}
