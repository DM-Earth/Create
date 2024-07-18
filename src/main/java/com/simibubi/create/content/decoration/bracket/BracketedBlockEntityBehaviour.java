package com.simibubi.create.content.decoration.bracket;

import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.NBTHelper;

public class BracketedBlockEntityBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<BracketedBlockEntityBehaviour> TYPE = new BehaviourType<>();

	private BlockState bracket;
	private boolean reRender;

	private Predicate<BlockState> pred;

	public BracketedBlockEntityBehaviour(SmartBlockEntity be) {
		this(be, state -> true);
	}

	public BracketedBlockEntityBehaviour(SmartBlockEntity be, Predicate<BlockState> pred) {
		super(be);
		this.pred = pred;
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	public void applyBracket(BlockState state) {
		this.bracket = state;
		reRender = true;
		blockEntity.notifyUpdate();
		World world = getWorld();
		if (world.isClient)
			return;
		blockEntity.getCachedState()
			.updateNeighbors(world, getPos(), 3);
	}

	public void transformBracket(StructureTransform transform) {
		if (isBracketPresent()) {
			BlockState transformedBracket = transform.apply(bracket);
			applyBracket(transformedBracket);
		}
	}

	@Nullable
	public BlockState removeBracket(boolean inOnReplacedContext) {
		if (bracket == null) {
			return null;
		}

		BlockState removed = this.bracket;
		World world = getWorld();
		if (!world.isClient)
			world.syncWorldEvent(2001, getPos(), Block.getRawIdFromState(bracket));
		this.bracket = null;
		reRender = true;
		if (inOnReplacedContext) {
			blockEntity.sendData();
			return removed;
		}
		blockEntity.notifyUpdate();
		if (world.isClient)
			return removed;
		blockEntity.getCachedState()
			.updateNeighbors(world, getPos(), 3);
		return removed;
	}

	public boolean isBracketPresent() {
		return bracket != null;
	}

	@Nullable
	public BlockState getBracket() {
		return bracket;
	}

	public boolean canHaveBracket() {
		return pred.test(blockEntity.getCachedState());
	}

	@Override
	public ItemRequirement getRequiredItems() {
		if (!isBracketPresent()) {
			return ItemRequirement.NONE;
		}
		return ItemRequirement.of(bracket, null);
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		if (isBracketPresent()) {
			nbt.put("Bracket", NbtHelper.fromBlockState(bracket));
		}
		if (clientPacket && reRender) {
			NBTHelper.putMarker(nbt, "Redraw");
			reRender = false;
		}
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		if (nbt.contains("Bracket"))
			bracket = NbtHelper.toBlockState(blockEntity.blockHolderGetter(), nbt.getCompound("Bracket"));
		if (clientPacket && nbt.contains("Redraw"))
			getWorld().updateListeners(getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), 16);
		super.read(nbt, clientPacket);
	}

}
