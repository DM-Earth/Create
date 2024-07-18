package com.simibubi.create.foundation.ponder.instruction;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.PonderScene;
import net.minecraft.util.math.Box;

public class ChaseAABBInstruction extends TickingInstruction {

	private Box bb;
	private Object slot;
	private PonderPalette color;

	public ChaseAABBInstruction(PonderPalette color, Object slot, Box bb, int ticks) {
		super(false, ticks);
		this.color = color;
		this.slot = slot;
		this.bb = bb;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		scene.getOutliner()
			.chaseAABB(slot, bb)
			.lineWidth(1 / 16f)
			.colored(color.getColor());
	}

}
