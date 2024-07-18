package com.simibubi.create.foundation.blockEntity;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface IMultiBlockEntityContainer {

	BlockPos getController();
	<T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE ();
	boolean isController();
	void setController(BlockPos pos);
	void removeController (boolean keepContents);
	BlockPos getLastKnownPos();

	void preventConnectivityUpdate ();
	void notifyMultiUpdated ();

	// only used for FluidTank windows at present. Might be useful for similar properties on other things?
	default void setExtraData (@Nullable Object data) {}
	@Nullable
	default Object getExtraData () { return null; }
	default Object modifyExtraData (Object data) { return data; }

	// multiblock structural information
	Direction.Axis getMainConnectionAxis();
	default Direction.Axis getMainAxisOf (BlockEntity be) { // this feels redundant, but it gives us a default to use when defining ::getMainConnectionAxis
		BlockState state = be.getCachedState();

		Direction.Axis axis;
		if (state.contains(Properties.HORIZONTAL_AXIS)) {
			axis = state.get(Properties.HORIZONTAL_AXIS);
		}
		else if (state.contains(Properties.FACING)) {
			axis = state.get(Properties.FACING).getAxis();
		}
		else if (state.contains(Properties.HORIZONTAL_FACING)) {
			axis = state.get(Properties.HORIZONTAL_FACING).getAxis();
		}
		else axis = Direction.Axis.Y;

		return axis;
	}

	int getMaxLength (Direction.Axis longAxis, int width);
	int getMaxWidth  ();

	int getHeight ();
	void setHeight (int height);
	int getWidth  ();
	void setWidth  (int width);

	public interface Inventory extends IMultiBlockEntityContainer {
		default boolean hasInventory() { return false; }
	}

	public interface Fluid extends IMultiBlockEntityContainer {
		// done here rather than through the Capability to allow greater flexibility
		default boolean hasTank() { return false; }

		default long getTankSize(int tank) {	return 0; }

		default void setTankSize(int tank, int blocks) {}

		default FluidTank getTank(int tank) { return null; }

		default FluidStack getFluid(int tank) {	return FluidStack.EMPTY; }
	}
}
