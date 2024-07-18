package com.simibubi.create.content.kinetics.base.flwdata;

import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

public class RotatingData extends KineticData {
    byte rotationAxisX;
    byte rotationAxisY;
    byte rotationAxisZ;

    public RotatingData setRotationAxis(Direction.Axis axis) {
        Direction orientation = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        return setRotationAxis(orientation.getUnitVector());
    }

    public RotatingData setRotationAxis(Vector3f axis) {
        return setRotationAxis(axis.x(), axis.y(), axis.z());
	}

	public RotatingData setRotationAxis(float rotationAxisX, float rotationAxisY, float rotationAxisZ) {
		this.rotationAxisX = (byte) (rotationAxisX * 127);
		this.rotationAxisY = (byte) (rotationAxisY * 127);
		this.rotationAxisZ = (byte) (rotationAxisZ * 127);
		markDirty();
		return this;
	}

}
