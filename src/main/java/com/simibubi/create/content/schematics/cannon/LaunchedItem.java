package com.simibubi.create.content.schematics.cannon;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

public abstract class LaunchedItem {

	public int totalTicks;
	public int ticksRemaining;
	public BlockPos target;
	public ItemStack stack;

	private LaunchedItem(BlockPos start, BlockPos target, ItemStack stack) {
		this(target, stack, ticksForDistance(start, target), ticksForDistance(start, target));
	}

	private static int ticksForDistance(BlockPos start, BlockPos target) {
		return (int) (Math.max(10, Math.sqrt(Math.sqrt(target.getSquaredDistance(start))) * 4f));
	}

	LaunchedItem() {}

	private LaunchedItem(BlockPos target, ItemStack stack, int ticksLeft, int total) {
		this.target = target;
		this.stack = stack;
		this.totalTicks = total;
		this.ticksRemaining = ticksLeft;
	}

	public boolean update(World world) {
		if (ticksRemaining > 0) {
			ticksRemaining--;
			return false;
		}
		if (world.isClient)
			return false;

		place(world);
		return true;
	}

	public NbtCompound serializeNBT() {
		NbtCompound c = new NbtCompound();
		c.putInt("TotalTicks", totalTicks);
		c.putInt("TicksLeft", ticksRemaining);
		c.put("Stack", NBTSerializer.serializeNBT(stack));
		c.put("Target", NbtHelper.fromBlockPos(target));
		return c;
	}

	public static LaunchedItem fromNBT(NbtCompound c, RegistryEntryLookup<Block> holderGetter) {
		LaunchedItem launched = c.contains("Length") ? new LaunchedItem.ForBelt()
			: c.contains("BlockState") ? new LaunchedItem.ForBlockState() : new LaunchedItem.ForEntity();
		launched.readNBT(c, holderGetter);
		return launched;
	}

	abstract void place(World world);

	void readNBT(NbtCompound c, RegistryEntryLookup<Block> holderGetter) {
		target = NbtHelper.toBlockPos(c.getCompound("Target"));
		ticksRemaining = c.getInt("TicksLeft");
		totalTicks = c.getInt("TotalTicks");
		stack = ItemStack.fromNbt(c.getCompound("Stack"));
	}

	public static class ForBlockState extends LaunchedItem {
		public BlockState state;
		public NbtCompound data;

		ForBlockState() {}

		public ForBlockState(BlockPos start, BlockPos target, ItemStack stack, BlockState state, NbtCompound data) {
			super(start, target, stack);
			this.state = state;
			this.data = data;
		}

		@Override
		public NbtCompound serializeNBT() {
			NbtCompound serializeNBT = super.serializeNBT();
			serializeNBT.put("BlockState", NbtHelper.fromBlockState(state));
			if (data != null) {
				data.remove("x");
				data.remove("y");
				data.remove("z");
				data.remove("id");
				serializeNBT.put("Data", data);
			}
			return serializeNBT;
		}

		@Override
		void readNBT(NbtCompound nbt, RegistryEntryLookup<Block> holderGetter) {
			super.readNBT(nbt, holderGetter);
			state = NbtHelper.toBlockState(holderGetter, nbt.getCompound("BlockState"));
			if (nbt.contains("Data", NbtElement.COMPOUND_TYPE)) {
				data = nbt.getCompound("Data");
			}
		}

		@Override
		void place(World world) {
			BlockHelper.placeSchematicBlock(world, state, target, stack, data);
		}

	}

	public static class ForBelt extends ForBlockState {
		public int length;
		public CasingType[] casings;

		public ForBelt() {}

		@Override
		public NbtCompound serializeNBT() {
			NbtCompound serializeNBT = super.serializeNBT();
			serializeNBT.putInt("Length", length);
			serializeNBT.putIntArray("Casing", Arrays.stream(casings)
				.map(CasingType::ordinal)
				.toList());
			return serializeNBT;
		}

		@Override
		void readNBT(NbtCompound nbt, RegistryEntryLookup<Block> holderGetter) {
			length = nbt.getInt("Length");
			int[] intArray = nbt.getIntArray("Casing");
			casings = new CasingType[length];
			for (int i = 0; i < casings.length; i++)
				casings[i] = i >= intArray.length ? CasingType.NONE
					: CasingType.values()[MathHelper.clamp(intArray[i], 0, CasingType.values().length - 1)];
			super.readNBT(nbt, holderGetter);
		}

		public ForBelt(BlockPos start, BlockPos target, ItemStack stack, BlockState state, CasingType[] casings) {
			super(start, target, stack, state, null);
			this.casings = casings;
			this.length = casings.length;
		}

		@Override
		void place(World world) {
			boolean isStart = state.get(BeltBlock.PART) == BeltPart.START;
			BlockPos offset = BeltBlock.nextSegmentPosition(state, BlockPos.ORIGIN, isStart);
			int i = length - 1;
			Axis axis = state.get(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS ? Axis.Y
				: state.get(BeltBlock.HORIZONTAL_FACING)
					.rotateYClockwise()
					.getAxis();
			world.setBlockState(target, AllBlocks.SHAFT.getDefaultState()
				.with(AbstractSimpleShaftBlock.AXIS, axis));
			BeltConnectorItem.createBelts(world, target,
				target.add(offset.getX() * i, offset.getY() * i, offset.getZ() * i));

			for (int segment = 0; segment < length; segment++) {
				if (casings[segment] == CasingType.NONE)
					continue;
				BlockPos casingTarget =
					target.add(offset.getX() * segment, offset.getY() * segment, offset.getZ() * segment);
				if (world.getBlockEntity(casingTarget) instanceof BeltBlockEntity bbe)
					bbe.setCasingType(casings[segment]);
			}
		}

	}

	public static class ForEntity extends LaunchedItem {
		public Entity entity;
		private NbtCompound deferredTag;

		ForEntity() {}

		public ForEntity(BlockPos start, BlockPos target, ItemStack stack, Entity entity) {
			super(start, target, stack);
			this.entity = entity;
		}

		@Override
		public boolean update(World world) {
			if (deferredTag != null && entity == null) {
				try {
					Optional<Entity> loadEntityUnchecked = EntityType.getEntityFromNbt(deferredTag, world);
					if (!loadEntityUnchecked.isPresent())
						return true;
					entity = loadEntityUnchecked.get();
				} catch (Exception var3) {
					return true;
				}
				deferredTag = null;
			}
			return super.update(world);
		}

		@Override
		public NbtCompound serializeNBT() {
			NbtCompound serializeNBT = super.serializeNBT();
			if (entity != null)
				serializeNBT.put("Entity", NBTSerializer.serializeNBT(entity));
			return serializeNBT;
		}

		@Override
		void readNBT(NbtCompound nbt, RegistryEntryLookup<Block> holderGetter) {
			super.readNBT(nbt, holderGetter);
			if (nbt.contains("Entity"))
				deferredTag = nbt.getCompound("Entity");
		}

		@Override
		void place(World world) {
			if (entity != null)
				world.spawnEntity(entity);
		}

	}

}
