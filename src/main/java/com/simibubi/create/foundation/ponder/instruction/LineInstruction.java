package com.simibubi.create.foundation.ponder.instruction;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.PonderScene;
import net.minecraft.util.math.Vec3d;

public class LineInstruction extends TickingInstruction {

	private PonderPalette color;
	private Vec3d start;
	private Vec3d end;
	private boolean big;

	public LineInstruction(PonderPalette color, Vec3d start, Vec3d end, int ticks, boolean big) {
		super(false, ticks);
		this.color = color;
		this.start = start;
		this.end = end;
		this.big = big;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		scene.getOutliner()
			.showLine(start, start, end)
			.lineWidth(big ? 1 / 8f : 1 / 16f)
			.colored(color.getColor());
	}

}
