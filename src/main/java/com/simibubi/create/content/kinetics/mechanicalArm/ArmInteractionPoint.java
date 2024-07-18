package com.simibubi.create.content.kinetics.mechanicalArm;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ArmInteractionPoint {

	protected final ArmInteractionPointType type;
	protected World level;
	protected final BlockPos pos;
	protected Mode mode = Mode.DEPOSIT;

	protected BlockState cachedState;
	protected StorageProvider<ItemVariant> handlerProvider = null;
	protected ArmAngleTarget cachedAngles;

	public ArmInteractionPoint(ArmInteractionPointType type, World level, BlockPos pos, BlockState state) {
		this.type = type;
		this.level = level;
		this.pos = pos;
		this.cachedState = state;
	}

	public ArmInteractionPointType getType() {
		return type;
	}

	public World getLevel() {
		return level;
	}

	public void setLevel(World level) {
		this.level = level;
	}

	public BlockPos getPos() {
		return pos;
	}

	public Mode getMode() {
		return mode;
	}

	public void cycleMode() {
		mode = mode == Mode.DEPOSIT ? Mode.TAKE : Mode.DEPOSIT;
	}

	protected Vec3d getInteractionPositionVector() {
		return VecHelper.getCenterOf(pos);
	}

	protected Direction getInteractionDirection() {
		return Direction.DOWN;
	}

	public ArmAngleTarget getTargetAngles(BlockPos armPos, boolean ceiling) {
		if (cachedAngles == null)
			cachedAngles =
				new ArmAngleTarget(armPos, getInteractionPositionVector(), getInteractionDirection(), ceiling);

		return cachedAngles;
	}

	public void updateCachedState() {
		cachedState = level.getBlockState(pos);
	}

	public boolean isValid() {
		updateCachedState();
		return type.canCreatePoint(level, pos, cachedState);
	}

	public void keepAlive() {}

	@Nullable
	protected Storage<ItemVariant> getHandler() {
		if (handlerProvider == null) {
			handlerProvider = StorageProvider.createForItems(level, pos);
		}
		return handlerProvider.get(Direction.UP);
	}

	public ItemStack insert(ItemStack stack, TransactionContext ctx) {
		Storage<ItemVariant> handler = getHandler();
		if (handler == null)
			return stack;
		long inserted = handler.insert(ItemVariant.of(stack), stack.getCount(), ctx);
		return ItemHandlerHelper.copyStackWithSize(stack, ItemHelper.truncateLong(stack.getCount() - inserted));
	}

	public ItemStack extract(int amount, TransactionContext ctx) {
		Storage<ItemVariant> handler = getHandler();
		if (handler == null)
			return ItemStack.EMPTY;
		return TransferUtil.extractAnyItem(handler, amount);
	}

	public ItemStack extract(TransactionContext ctx) {
		return extract(64, ctx);
	}

	protected void serialize(NbtCompound nbt, BlockPos anchor) {
		NBTHelper.writeEnum(nbt, "Mode", mode);
	}

	protected void deserialize(NbtCompound nbt, BlockPos anchor) {
		mode = NBTHelper.readEnum(nbt, "Mode", Mode.class);
	}

	public final NbtCompound serialize(BlockPos anchor) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("Type", type.getId().toString());
		nbt.put("Pos", NbtHelper.fromBlockPos(pos.subtract(anchor)));
		serialize(nbt, anchor);
		return nbt;
	}

	@Nullable
	public static ArmInteractionPoint deserialize(NbtCompound nbt, World level, BlockPos anchor) {
		Identifier id = Identifier.tryParse(nbt.getString("Type"));
		if (id == null)
			return null;
		ArmInteractionPointType type = ArmInteractionPointType.get(id);
		if (type == null)
			return null;
		BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("Pos")).add(anchor);
		BlockState state = level.getBlockState(pos);
		if (!type.canCreatePoint(level, pos, state))
			return null;
		ArmInteractionPoint point = type.createPoint(level, pos, state);
		if (point == null)
			return null;
		point.deserialize(nbt, anchor);
		return point;
	}

	public static void transformPos(NbtCompound nbt, StructureTransform transform) {
		BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("Pos"));
		pos = transform.applyWithoutOffset(pos);
		nbt.put("Pos", NbtHelper.fromBlockPos(pos));
	}

	public static boolean isInteractable(World level, BlockPos pos, BlockState state) {
		return ArmInteractionPointType.getPrimaryType(level, pos, state) != null;
	}

	@Nullable
	public static ArmInteractionPoint create(World level, BlockPos pos, BlockState state) {
		ArmInteractionPointType type = ArmInteractionPointType.getPrimaryType(level, pos, state);
		if (type == null)
			return null;
		return type.createPoint(level, pos, state);
	}

	public enum Mode {
		DEPOSIT("mechanical_arm.deposit_to", 0xDDC166),
		TAKE("mechanical_arm.extract_from", 0x7FCDE0);

		private final String translationKey;
		private final int color;

		Mode(String translationKey, int color) {
			this.translationKey = translationKey;
			this.color = color;
		}

		public String getTranslationKey() {
			return translationKey;
		}

		public int getColor() {
			return color;
		}
	}

}