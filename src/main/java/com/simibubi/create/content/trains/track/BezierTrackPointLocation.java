package com.simibubi.create.content.trains.track;

import net.minecraft.util.math.BlockPos;

public record BezierTrackPointLocation(BlockPos curveTarget, int segment) {
}
