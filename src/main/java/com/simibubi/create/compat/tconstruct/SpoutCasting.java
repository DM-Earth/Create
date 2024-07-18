package com.simibubi.create.compat.tconstruct;

import com.simibubi.create.api.behaviour.BlockSpoutingBehaviour;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SpoutCasting extends BlockSpoutingBehaviour {

	static Boolean TICON_PRESENT = null;

	Identifier TABLE = new Identifier("tconstruct", "table");
	Identifier BASIN = new Identifier("tconstruct", "basin");

	@Override
	public long fillBlock(World level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid,
		boolean simulate) {
		if (!enabled())
			return 0;

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity == null)
			return 0;

		Identifier registryName = RegisteredObjects.getKeyOrThrow(blockEntity.getType());
		if (!registryName.equals(TABLE) && !registryName.equals(BASIN))
			return 0;

		Storage<FluidVariant> handler = TransferUtil.getFluidStorage(level, pos, blockEntity, Direction.UP);
		if (handler == null)
			return 0;

		// Do not fill if it would only partially fill the table (unless > 1000mb)
		long amount = availableFluid.getAmount();
		try (Transaction t = TransferUtil.getTransaction()) {
			long inserted = handler.insert(availableFluid.getType(), amount, t);
			if (amount < FluidConstants.BUCKET) {
				try (Transaction nested = t.openNested()) {
					if (handler.insert(availableFluid.getType(), 1, nested) == 1)
						return 0;
				}
			}

			if (!simulate) t.commit();
			return inserted;
		}
	}

	private boolean enabled() {
		if (TICON_PRESENT == null)
			TICON_PRESENT = Mods.TCONSTRUCT.isLoaded();
		if (!TICON_PRESENT)
			return false;
		return AllConfigs.server().recipes.allowCastingBySpout.get();
	}

}
