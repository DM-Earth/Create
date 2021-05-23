package com.simibubi.create.foundation.render;

import com.jozufozu.flywheel.Flywheel;
import com.simibubi.create.Create;

import net.minecraft.util.ResourceLocation;

public class AllProgramSpecs {

	public static final ResourceLocation MODEL = new ResourceLocation(Flywheel.ID, "model");//, Locations.MODEL_VERT, Locations.BLOCK);
	public static final ResourceLocation ORIENTED = new ResourceLocation(Flywheel.ID, "oriented");//, Locations.ORIENTED, Locations.BLOCK);

	public static final ResourceLocation ROTATING = loc("rotating");
	public static final ResourceLocation CHROMATIC = loc("chromatic");
	public static final ResourceLocation BELT = loc("belt");
	public static final ResourceLocation FLAPS = loc("flap");
	public static final ResourceLocation STRUCTURE = loc("contraption_structure");
	public static final ResourceLocation ACTOR = loc("contraption_actor");

	private static ResourceLocation loc(String name) {
		return new ResourceLocation(Create.ID, name);
	}
}
