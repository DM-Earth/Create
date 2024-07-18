package com.simibubi.create.content.contraptions.mounted;

import static com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock.RAIL_SHAPE;

import java.util.Queue;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity.CartMovementMode;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import com.simibubi.create.content.contraptions.render.NonStationaryLighter;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;

public class MountedContraption extends Contraption {

	public CartMovementMode rotationMode;
	public AbstractMinecartEntity connectedCart;

	public MountedContraption() {
		this(CartMovementMode.ROTATE);
	}

	public MountedContraption(CartMovementMode mode) {
		rotationMode = mode;
	}

	@Override
	public ContraptionType getType() {
		return ContraptionType.MOUNTED;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) throws AssemblyException {
		BlockState state = world.getBlockState(pos);
		if (!state.contains(RAIL_SHAPE))
			return false;
		if (!searchMovedStructure(world, pos, null))
			return false;

		Axis axis = state.get(RAIL_SHAPE) == RailShape.EAST_WEST ? Axis.X : Axis.Z;
		addBlock(pos, Pair.of(new StructureBlockInfo(pos, AllBlocks.MINECART_ANCHOR.getDefaultState()
			.with(Properties.HORIZONTAL_AXIS, axis), null), null));

		if (blocks.size() == 1)
			return false;

		return true;
	}

	@Override
	protected boolean addToInitialFrontier(World world, BlockPos pos, Direction direction, Queue<BlockPos> frontier) {
		frontier.clear();
		frontier.add(pos.up());
		return true;
	}

	@Override
	protected Pair<StructureBlockInfo, BlockEntity> capture(World world, BlockPos pos) {
		Pair<StructureBlockInfo, BlockEntity> pair = super.capture(world, pos);
		StructureBlockInfo capture = pair.getKey();
		if (!AllBlocks.CART_ASSEMBLER.has(capture.state()))
			return pair;

		Pair<StructureBlockInfo, BlockEntity> anchorSwap =
			Pair.of(new StructureBlockInfo(pos, CartAssemblerBlock.createAnchor(capture.state()), null), pair.getValue());
		if (pos.equals(anchor) || connectedCart != null)
			return anchorSwap;

		for (Axis axis : Iterate.axes) {
			if (axis.isVertical() || !VecHelper.onSameAxis(anchor, pos, axis))
				continue;
			for (AbstractMinecartEntity abstractMinecartEntity : world.getNonSpectatingEntities(AbstractMinecartEntity.class,
				new Box(pos))) {
				if (!CartAssemblerBlock.canAssembleTo(abstractMinecartEntity))
					break;
				connectedCart = abstractMinecartEntity;
				connectedCart.setPosition(pos.getX() + .5, pos.getY(), pos.getZ() + .5f);
			}
		}

		return anchorSwap;
	}

	@Override
	protected boolean movementAllowed(BlockState state, World world, BlockPos pos) {
		if (!pos.equals(anchor) && AllBlocks.CART_ASSEMBLER.has(state))
			return testSecondaryCartAssembler(world, state, pos);
		return super.movementAllowed(state, world, pos);
	}

	protected boolean testSecondaryCartAssembler(World world, BlockState state, BlockPos pos) {
		for (Axis axis : Iterate.axes) {
			if (axis.isVertical() || !VecHelper.onSameAxis(anchor, pos, axis))
				continue;
			for (AbstractMinecartEntity abstractMinecartEntity : world.getNonSpectatingEntities(AbstractMinecartEntity.class,
				new Box(pos))) {
				if (!CartAssemblerBlock.canAssembleTo(abstractMinecartEntity))
					break;
				return true;
			}
		}
		return false;
	}

	@Override
	public NbtCompound writeNBT(boolean spawnPacket) {
		NbtCompound tag = super.writeNBT(spawnPacket);
		NBTHelper.writeEnum(tag, "RotationMode", rotationMode);
		return tag;
	}

	@Override
	public void readNBT(World world, NbtCompound nbt, boolean spawnData) {
		rotationMode = NBTHelper.readEnum(nbt, "RotationMode", CartMovementMode.class);
		super.readNBT(world, nbt, spawnData);
	}

	@Override
	protected boolean customBlockPlacement(WorldAccess world, BlockPos pos, BlockState state) {
		return AllBlocks.MINECART_ANCHOR.has(state);
	}

	@Override
	protected boolean customBlockRemoval(WorldAccess world, BlockPos pos, BlockState state) {
		return AllBlocks.MINECART_ANCHOR.has(state);
	}

	@Override
	public boolean canBeStabilized(Direction facing, BlockPos localPos) {
		return true;
	}

	public void addExtraInventories(Entity cart) {
		if (cart instanceof Inventory container)
			storage.attachExternal(new ContraptionInvWrapper(true, InventoryStorage.of(container, null)));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		return new NonStationaryLighter<>(this);
	}
}
