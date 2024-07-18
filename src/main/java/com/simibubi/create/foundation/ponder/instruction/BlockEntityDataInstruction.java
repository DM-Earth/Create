package com.simibubi.create.foundation.ponder.instruction;

import java.util.function.UnaryOperator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.Selection;

public class BlockEntityDataInstruction extends WorldModifyInstruction {

	private boolean redraw;
	private UnaryOperator<NbtCompound> data;
	private Class<? extends BlockEntity> type;

	public BlockEntityDataInstruction(Selection selection, Class<? extends BlockEntity> type,
		UnaryOperator<NbtCompound> data, boolean redraw) {
		super(selection);
		this.type = type;
		this.data = data;
		this.redraw = redraw;
	}

	@Override
	protected void runModification(Selection selection, PonderScene scene) {
		PonderWorld world = scene.getWorld();
		selection.forEach(pos -> {
			if (!world.getBounds()
				.contains(pos))
				return;
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (!type.isInstance(blockEntity))
				return;
			NbtCompound apply = data.apply(blockEntity.createNbtWithIdentifyingData());
			if (blockEntity instanceof SyncedBlockEntity)
				((SyncedBlockEntity) blockEntity).readClient(apply);
			blockEntity.readNbt(apply);
		});
	}

	@Override
	protected boolean needsRedraw() {
		return redraw;
	}

}
