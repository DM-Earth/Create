package com.simibubi.create.content.trains.entity;

import static com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity.BOGEY_DATA_KEY;
import static com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity.BOGEY_STYLE_KEY;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyInstance;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

public class CarriageBogey {

	public static final String UPSIDE_DOWN_KEY = "UpsideDown";

	public Carriage carriage;
	boolean isLeading;

	public NbtCompound bogeyData;

	AbstractBogeyBlock<?> type;
	boolean upsideDown;
	Couple<TravellingPoint> points;

	LerpedFloat wheelAngle;
	LerpedFloat yaw;
	LerpedFloat pitch;

	public Couple<Vec3d> couplingAnchors;

	int derailAngle;

	public CarriageBogey(AbstractBogeyBlock<?> type, boolean upsideDown, NbtCompound bogeyData, TravellingPoint point, TravellingPoint point2) {
		this.type = type;
		this.upsideDown = type.canBeUpsideDown() && upsideDown;
		point.upsideDown = this.upsideDown;
		point2.upsideDown = this.upsideDown;
		if (bogeyData == null || bogeyData.isEmpty())
			bogeyData = this.createBogeyData(); // Prevent Crash When Updating
		bogeyData.putBoolean(UPSIDE_DOWN_KEY, upsideDown);
		this.bogeyData = bogeyData;
		points = Couple.create(point, point2);
		wheelAngle = LerpedFloat.angular();
		yaw = LerpedFloat.angular();
		pitch = LerpedFloat.angular();
		derailAngle = Create.RANDOM.nextInt(60) - 30;
		couplingAnchors = Couple.create(null, null);
	}

	public RegistryKey<World> getDimension() {
		TravellingPoint leading = leading();
		TravellingPoint trailing = trailing();
		if (leading.edge == null || trailing.edge == null)
			return null;
		if (leading.edge.isInterDimensional() || trailing.edge.isInterDimensional())
			return null;
		RegistryKey<World> dimension1 = leading.node1.getLocation().dimension;
		RegistryKey<World> dimension2 = trailing.node1.getLocation().dimension;
		if (dimension1.equals(dimension2))
			return dimension1;
		return null;
	}

	public void updateAngles(CarriageContraptionEntity entity, double distanceMoved) {
		double angleDiff = 360 * distanceMoved / (Math.PI * 2 * type.getWheelRadius());

		float xRot = 0;
		float yRot = 0;

		if (leading().edge == null || carriage.train.derailed) {
			yRot = -90 + entity.yaw - derailAngle;
		} else if (!entity.getWorld().getRegistryKey()
				.equals(getDimension())) {
			yRot = -90 + entity.yaw;
			xRot = 0;
		} else {
			Vec3d positionVec = leading().getPosition(carriage.train.graph);
			Vec3d coupledVec = trailing().getPosition(carriage.train.graph);
			double diffX = positionVec.x - coupledVec.x;
			double diffY = positionVec.y - coupledVec.y;
			double diffZ = positionVec.z - coupledVec.z;
			yRot = AngleHelper.deg(MathHelper.atan2(diffZ, diffX)) + 90;
			xRot = AngleHelper.deg(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));
		}

		double newWheelAngle = (wheelAngle.getValue() - angleDiff) % 360;

		for (boolean twice : Iterate.trueAndFalse) {
			if (twice && !entity.firstPositionUpdate)
				continue;
			wheelAngle.setValue(newWheelAngle);
			pitch.setValue(xRot);
			yaw.setValue(-yRot);
		}
	}

	public TravellingPoint leading() {
		TravellingPoint point = points.getFirst();
		point.upsideDown = isUpsideDown();
		return point;
	}

	public TravellingPoint trailing() {
		TravellingPoint point = points.getSecond();
		point.upsideDown = isUpsideDown();
		return point;
	}

	public double getStress() {
		if (getDimension() == null)
			return 0;
		if (carriage.train.derailed)
			return 0;
		return type.getWheelPointSpacing() - leading().getPosition(carriage.train.graph)
				.distanceTo(trailing().getPosition(carriage.train.graph));
	}

	@Nullable
	public Vec3d getAnchorPosition() {
		return getAnchorPosition(false);
	}

	@Nullable
	public Vec3d getAnchorPosition(boolean flipUpsideDown) {
		if (leading().edge == null)
			return null;
		return points.getFirst()
				.getPosition(carriage.train.graph, flipUpsideDown)
				.add(points.getSecond()
						.getPosition(carriage.train.graph, flipUpsideDown))
				.multiply(.5);
	}

	public void updateCouplingAnchor(Vec3d entityPos, float entityXRot, float entityYRot, int bogeySpacing,
									 float partialTicks, boolean leading) {
		boolean selfUpsideDown = isUpsideDown();
		boolean leadingUpsideDown = carriage.leadingBogey().isUpsideDown();
		Vec3d thisOffset = type.getConnectorAnchorOffset(selfUpsideDown);
		thisOffset = thisOffset.multiply(1, 1, leading ? -1 : 1);

		thisOffset = VecHelper.rotate(thisOffset, pitch.getValue(partialTicks), Axis.X);
		thisOffset = VecHelper.rotate(thisOffset, yaw.getValue(partialTicks), Axis.Y);
		thisOffset = VecHelper.rotate(thisOffset, -entityYRot - 90, Axis.Y);
		thisOffset = VecHelper.rotate(thisOffset, entityXRot, Axis.X);
		thisOffset = VecHelper.rotate(thisOffset, -180, Axis.Y);
		thisOffset = thisOffset.add(0, 0, leading ? 0 : -bogeySpacing);
		thisOffset = VecHelper.rotate(thisOffset, 180, Axis.Y);
		thisOffset = VecHelper.rotate(thisOffset, -entityXRot, Axis.X);
		thisOffset = VecHelper.rotate(thisOffset, entityYRot + 90, Axis.Y);
		if (selfUpsideDown != leadingUpsideDown)
			thisOffset = thisOffset.add(0, selfUpsideDown ? -2 : 2, 0);

		couplingAnchors.set(leading, entityPos.add(thisOffset));
	}

	public NbtCompound write(DimensionPalette dimensions) {
		NbtCompound tag = new NbtCompound();
		tag.putString("Type", RegisteredObjects.getKeyOrThrow((Block) type)
				.toString());
		tag.put("Points", points.serializeEach(tp -> tp.write(dimensions)));
		tag.putBoolean("UpsideDown", upsideDown);
		bogeyData.putBoolean(UPSIDE_DOWN_KEY, upsideDown);
		NBTHelper.writeResourceLocation(bogeyData, BOGEY_STYLE_KEY, getStyle().name);
		tag.put(BOGEY_DATA_KEY, bogeyData);
		return tag;
	}

	public static CarriageBogey read(NbtCompound tag, TrackGraph graph, DimensionPalette dimensions) {
		Identifier location = new Identifier(tag.getString("Type"));
		Block block = Registries.BLOCK.get(location);
		AbstractBogeyBlock<?> type = block instanceof AbstractBogeyBlock<?> bogey ? bogey : AllBlocks.SMALL_BOGEY.get();
		boolean upsideDown = tag.getBoolean("UpsideDown");
		Couple<TravellingPoint> points = Couple.deserializeEach(tag.getList("Points", NbtElement.COMPOUND_TYPE),
				c -> TravellingPoint.read(c, graph, dimensions));
		NbtCompound data = tag.getCompound(AbstractBogeyBlockEntity.BOGEY_DATA_KEY);
		return new CarriageBogey(type, upsideDown, data, points.getFirst(), points.getSecond());
	}

	public BogeyInstance createInstance(MaterialManager materialManager) {
		return this.getStyle().createInstance(this, type.getSize(), materialManager);
	}

	public BogeyStyle getStyle() {
		Identifier location = NBTHelper.readResourceLocation(this.bogeyData, BOGEY_STYLE_KEY);
		BogeyStyle style = AllBogeyStyles.BOGEY_STYLES.get(location);
		return style != null ? style : AllBogeyStyles.STANDARD; // just for safety
	}

	private NbtCompound createBogeyData() {
		BogeyStyle style = type != null ? type.getDefaultStyle() : AllBogeyStyles.STANDARD;
		NbtCompound nbt = style.defaultData != null ? style.defaultData : new NbtCompound();
		NBTHelper.writeResourceLocation(nbt, BOGEY_STYLE_KEY, style.name);
		nbt.putBoolean(UPSIDE_DOWN_KEY, isUpsideDown());
		return nbt;
	}

	void setLeading() {
		isLeading = true;
	}

	public boolean isUpsideDown() {
		return type.canBeUpsideDown() && upsideDown;
	}
}
