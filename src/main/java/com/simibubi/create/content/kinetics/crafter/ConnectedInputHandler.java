package com.simibubi.create.content.kinetics.crafter;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity.Inventory;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;

public class ConnectedInputHandler {

	public static boolean shouldConnect(World world, BlockPos pos, Direction face, Direction direction) {
		BlockState refState = world.getBlockState(pos);
		if (!refState.contains(HORIZONTAL_FACING))
			return false;
		Direction refDirection = refState.get(HORIZONTAL_FACING);
		if (direction.getAxis() == refDirection.getAxis())
			return false;
		if (face == refDirection)
			return false;
		BlockState neighbour = world.getBlockState(pos.offset(direction));
		if (!AllBlocks.MECHANICAL_CRAFTER.has(neighbour))
			return false;
		if (refDirection != neighbour.get(HORIZONTAL_FACING))
			return false;
		return true;
	}

	public static void toggleConnection(World world, BlockPos pos, BlockPos pos2) {
		MechanicalCrafterBlockEntity crafter1 = CrafterHelper.getCrafter(world, pos);
		MechanicalCrafterBlockEntity crafter2 = CrafterHelper.getCrafter(world, pos2);

		if (crafter1 == null || crafter2 == null)
			return;

		BlockPos controllerPos1 = crafter1.getPos()
			.add(crafter1.input.data.get(0));
		BlockPos controllerPos2 = crafter2.getPos()
			.add(crafter2.input.data.get(0));

		if (controllerPos1.equals(controllerPos2)) {
			MechanicalCrafterBlockEntity controller = CrafterHelper.getCrafter(world, controllerPos1);

			Set<BlockPos> positions = controller.input.data.stream()
				.map(controllerPos1::add)
				.collect(Collectors.toSet());
			List<BlockPos> frontier = new LinkedList<>();
			List<BlockPos> splitGroup = new ArrayList<>();

			frontier.add(pos2);
			positions.remove(pos2);
			positions.remove(pos);
			while (!frontier.isEmpty()) {
				BlockPos current = frontier.remove(0);
				for (Direction direction : Iterate.directions) {
					BlockPos next = current.offset(direction);
					if (!positions.remove(next))
						continue;
					splitGroup.add(next);
					frontier.add(next);
				}
			}

			initAndAddAll(world, crafter1, positions);
			initAndAddAll(world, crafter2, splitGroup);

			crafter1.markDirty();
			crafter1.connectivityChanged();
			crafter2.markDirty();
			crafter2.connectivityChanged();
			return;
		}

		if (!crafter1.input.isController)
			crafter1 = CrafterHelper.getCrafter(world, controllerPos1);
		if (!crafter2.input.isController)
			crafter2 = CrafterHelper.getCrafter(world, controllerPos2);
		if (crafter1 == null || crafter2 == null)
			return;

		connectControllers(world, crafter1, crafter2);

		world.setBlockState(crafter1.getPos(), crafter1.getCachedState(), 3);

		crafter1.markDirty();
		crafter1.connectivityChanged();
		crafter2.markDirty();
		crafter2.connectivityChanged();
	}

	public static void initAndAddAll(World world, MechanicalCrafterBlockEntity crafter, Collection<BlockPos> positions) {
		crafter.input = new ConnectedInput();
		positions.forEach(splitPos -> {
			modifyAndUpdate(world, splitPos, input -> {
				input.attachTo(crafter.getPos(), splitPos);
				crafter.input.data.add(splitPos.subtract(crafter.getPos()));
			});
		});
	}

	public static void connectControllers(World world, MechanicalCrafterBlockEntity crafter1,
		MechanicalCrafterBlockEntity crafter2) {

		crafter1.input.data.forEach(offset -> {
			BlockPos connectedPos = crafter1.getPos()
				.add(offset);
			modifyAndUpdate(world, connectedPos, input -> {
			});
		});

		crafter2.input.data.forEach(offset -> {
			if (offset.equals(BlockPos.ORIGIN))
				return;
			BlockPos connectedPos = crafter2.getPos()
				.add(offset);
			modifyAndUpdate(world, connectedPos, input -> {
				input.attachTo(crafter1.getPos(), connectedPos);
				crafter1.input.data.add(BlockPos.ORIGIN.subtract(input.data.get(0)));
			});
		});

		crafter2.input.attachTo(crafter1.getPos(), crafter2.getPos());
		crafter1.input.data.add(BlockPos.ORIGIN.subtract(crafter2.input.data.get(0)));
	}

	private static void modifyAndUpdate(World world, BlockPos pos, Consumer<ConnectedInput> callback) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof MechanicalCrafterBlockEntity))
			return;

		MechanicalCrafterBlockEntity crafter = (MechanicalCrafterBlockEntity) blockEntity;
		callback.accept(crafter.input);
		crafter.markDirty();
		crafter.connectivityChanged();
	}

	public static class ConnectedInput {
		boolean isController;
		List<BlockPos> data = Collections.synchronizedList(new ArrayList<>());

		public ConnectedInput() {
			isController = true;
			data.add(BlockPos.ORIGIN);
		}

		public void attachTo(BlockPos controllerPos, BlockPos myPos) {
			isController = false;
			data.clear();
			data.add(controllerPos.subtract(myPos));
		}

		@Nullable // fabric: don't create useless ItemStackHandlers
		public Storage<ItemVariant> getItemHandler(World world, BlockPos pos) {
			if (!isController) {
				BlockPos controllerPos = pos.add(data.get(0));
				ConnectedInput input = CrafterHelper.getInput(world, controllerPos);
				if (input == this || input == null || !input.isController)
					return null;
				return input.getItemHandler(world, controllerPos);
			}

			Direction facing = Direction.SOUTH;
			BlockState blockState = world.getBlockState(pos);
			if (blockState.contains(MechanicalCrafterBlock.HORIZONTAL_FACING))
				facing = blockState.get(MechanicalCrafterBlock.HORIZONTAL_FACING);
			AxisDirection axisDirection = facing.getDirection();
			Axis compareAxis = facing.rotateYClockwise()
				.getAxis();

			Comparator<BlockPos> invOrdering = (p1, p2) -> {
				int compareY = -Integer.compare(p1.getY(), p2.getY());
				int modifier = axisDirection.offset() * (compareAxis == Axis.Z ? -1 : 1);
				int c1 = compareAxis.choose(p1.getX(), p1.getY(), p1.getZ());
				int c2 = compareAxis.choose(p2.getX(), p2.getY(), p2.getZ());
				return compareY != 0 ? compareY : modifier * Integer.compare(c1, c2);
			};

			List<Inventory> list = data.stream()
				.sorted(invOrdering)
				.map(l -> CrafterHelper.getCrafter(world, pos.add(l)))
				.filter(Objects::nonNull)
				.map(crafter -> crafter.getInventory())
				.collect(Collectors.toList());
			return new CombinedStorage<>(list);
		}

		public void write(NbtCompound nbt) {
			nbt.putBoolean("Controller", isController);
			NbtList list = new NbtList();
			data.forEach(pos -> list.add(NbtHelper.fromBlockPos(pos)));
			nbt.put("Data", list);
		}

		public void read(NbtCompound nbt) {
			isController = nbt.getBoolean("Controller");
			data.clear();
			nbt.getList("Data", NbtElement.COMPOUND_TYPE)
				.forEach(inbt -> data.add(NbtHelper.toBlockPos((NbtCompound) inbt)));

			// nbt got wiped -> reset
			if (data.isEmpty()) {
				isController = true;
				data.add(BlockPos.ORIGIN);
			}
		}

	}

}
