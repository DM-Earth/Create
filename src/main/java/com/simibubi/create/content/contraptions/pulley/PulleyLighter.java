package com.simibubi.create.content.contraptions.pulley;

import com.jozufozu.flywheel.util.box.GridAlignedBB;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PulleyLighter extends ContraptionLighter<PulleyContraption> {
    public PulleyLighter(PulleyContraption contraption) {
        super(contraption);
    }

    @Override
    public GridAlignedBB getContraptionBounds() {

        GridAlignedBB bounds = GridAlignedBB.from(contraption.bounds);

        World world = contraption.entity.getWorld();

        BlockPos.Mutable pos = contraption.anchor.mutableCopy();
        while (!AllBlocks.ROPE_PULLEY.has(world.getBlockState(pos)) && pos.getY() < world.getTopY())
            pos.move(0, 1, 0);

        bounds.translate(pos);
        bounds.setMinY(world.getBottomY());
        return bounds;
    }
}
