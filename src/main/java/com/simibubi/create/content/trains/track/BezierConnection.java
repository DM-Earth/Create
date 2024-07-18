package com.simibubi.create.content.trains.track;

import java.util.Iterator;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class BezierConnection implements Iterable<BezierConnection.Segment> {

	public Couple<BlockPos> tePositions;
	public Couple<Vec3d> starts;
	public Couple<Vec3d> axes;
	public Couple<Vec3d> normals;
	public Couple<Integer> smoothing;
	public boolean primary;
	public boolean hasGirder;
	protected TrackMaterial trackMaterial;

	// runtime

	Vec3d finish1;
	Vec3d finish2;
	private boolean resolved;
	private double length;
	private float[] stepLUT;
	private int segments;

	private double radius;
	private double handleLength;

	private Box bounds;

	public BezierConnection(Couple<BlockPos> positions, Couple<Vec3d> starts, Couple<Vec3d> axes, Couple<Vec3d> normals,
		boolean primary, boolean girder, TrackMaterial material) {
		tePositions = positions;
		this.starts = starts;
		this.axes = axes;
		this.normals = normals;
		this.primary = primary;
		this.hasGirder = girder;
		this.trackMaterial = material;
		resolved = false;
	}

	public BezierConnection secondary() {
		BezierConnection bezierConnection = new BezierConnection(tePositions.swap(), starts.swap(), axes.swap(),
			normals.swap(), !primary, hasGirder, trackMaterial);
		if (smoothing != null)
			bezierConnection.smoothing = smoothing.swap();
		return bezierConnection;
	}

	public BezierConnection clone() {
		return secondary().secondary();
	}

	private static boolean coupleEquals(Couple<?> a, Couple<?> b) {
		return (a.getFirst()
			.equals(b.getFirst())
			&& a.getSecond()
				.equals(b.getSecond()))
			|| (a.getFirst() instanceof Vec3d aFirst && a.getSecond() instanceof Vec3d aSecond
				&& b.getFirst() instanceof Vec3d bFirst && b.getSecond() instanceof Vec3d bSecond
				&& aFirst.isInRange(bFirst, 1e-6) && aSecond.isInRange(bSecond, 1e-6));
	}

	public boolean equalsSansMaterial(BezierConnection other) {
		return equalsSansMaterialInner(other) || equalsSansMaterialInner(other.secondary());
	}

	private boolean equalsSansMaterialInner(BezierConnection other) {
		return this == other || (other != null && coupleEquals(this.tePositions, other.tePositions)
			&& coupleEquals(this.starts, other.starts) && coupleEquals(this.axes, other.axes)
			&& coupleEquals(this.normals, other.normals) && this.hasGirder == other.hasGirder);
	}

	public BezierConnection(NbtCompound compound, BlockPos localTo) {
		this(Couple.deserializeEach(compound.getList("Positions", NbtElement.COMPOUND_TYPE), NbtHelper::toBlockPos)
			.map(b -> b.add(localTo)),
			Couple.deserializeEach(compound.getList("Starts", NbtElement.COMPOUND_TYPE), VecHelper::readNBTCompound)
				.map(v -> v.add(Vec3d.of(localTo))),
			Couple.deserializeEach(compound.getList("Axes", NbtElement.COMPOUND_TYPE), VecHelper::readNBTCompound),
			Couple.deserializeEach(compound.getList("Normals", NbtElement.COMPOUND_TYPE), VecHelper::readNBTCompound),
			compound.getBoolean("Primary"), compound.getBoolean("Girder"), TrackMaterial.deserialize(compound.getString("Material")));

		if (compound.contains("Smoothing"))
			smoothing =
				Couple.deserializeEach(compound.getList("Smoothing", NbtElement.COMPOUND_TYPE), NBTHelper::intFromCompound);
	}

	public NbtCompound write(BlockPos localTo) {
		Couple<BlockPos> tePositions = this.tePositions.map(b -> b.subtract(localTo));
		Couple<Vec3d> starts = this.starts.map(v -> v.subtract(Vec3d.of(localTo)));

		NbtCompound compound = new NbtCompound();
		compound.putBoolean("Girder", hasGirder);
		compound.putBoolean("Primary", primary);
		compound.put("Positions", tePositions.serializeEach(NbtHelper::fromBlockPos));
		compound.put("Starts", starts.serializeEach(VecHelper::writeNBTCompound));
		compound.put("Axes", axes.serializeEach(VecHelper::writeNBTCompound));
		compound.put("Normals", normals.serializeEach(VecHelper::writeNBTCompound));
		compound.putString("Material", getMaterial().id.toString());

		if (smoothing != null)
			compound.put("Smoothing", smoothing.serializeEach(NBTHelper::intToCompound));

		return compound;
	}

	public BezierConnection(PacketByteBuf buffer) {
		this(Couple.create(buffer::readBlockPos), Couple.create(() -> VecHelper.read(buffer)),
			Couple.create(() -> VecHelper.read(buffer)), Couple.create(() -> VecHelper.read(buffer)),
			buffer.readBoolean(), buffer.readBoolean(), TrackMaterial.deserialize(buffer.readString()));
		if (buffer.readBoolean())
			smoothing = Couple.create(buffer::readVarInt);
	}

	public void write(PacketByteBuf buffer) {
		tePositions.forEach(buffer::writeBlockPos);
		starts.forEach(v -> VecHelper.write(v, buffer));
		axes.forEach(v -> VecHelper.write(v, buffer));
		normals.forEach(v -> VecHelper.write(v, buffer));
		buffer.writeBoolean(primary);
		buffer.writeBoolean(hasGirder);
		buffer.writeString(getMaterial().id.toString());
		buffer.writeBoolean(smoothing != null);
		if (smoothing != null)
			smoothing.forEach(buffer::writeVarInt);
	}

	public BlockPos getKey() {
		return tePositions.getSecond();
	}

	public boolean isPrimary() {
		return primary;
	}

	public int yOffsetAt(Vec3d end) {
		if (smoothing == null)
			return 0;
		if (TrackBlockEntityTilt.compareHandles(starts.getFirst(), end))
			return smoothing.getFirst();
		if (TrackBlockEntityTilt.compareHandles(starts.getSecond(), end))
			return smoothing.getSecond();
		return 0;
	}

	// Runtime information

	public double getLength() {
		resolve();
		return length;
	}

	public float[] getStepLUT() {
		resolve();
		return stepLUT;
	}

	public int getSegmentCount() {
		resolve();
		return segments;
	}

	public Vec3d getPosition(double t) {
		resolve();
		return VecHelper.bezier(starts.getFirst(), starts.getSecond(), finish1, finish2, (float) t);
	}

	public double getRadius() {
		resolve();
		return radius;
	}

	public double getHandleLength() {
		resolve();
		return handleLength;
	}

	public float getSegmentT(int index) {
		return index == segments ? 1 : index * stepLUT[index] / segments;
	}

	public double incrementT(double currentT, double distance) {
		resolve();
		double dx =
			VecHelper.bezierDerivative(starts.getFirst(), starts.getSecond(), finish1, finish2, (float) currentT)
				.length() / getLength();
		return currentT + distance / dx;

	}

	public Box getBounds() {
		resolve();
		return bounds;
	}

	public Vec3d getNormal(double t) {
		resolve();
		Vec3d end1 = starts.getFirst();
		Vec3d end2 = starts.getSecond();
		Vec3d fn1 = normals.getFirst();
		Vec3d fn2 = normals.getSecond();

		Vec3d derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, (float) t)
			.normalize();
		Vec3d faceNormal = fn1.equals(fn2) ? fn1 : VecHelper.slerp((float) t, fn1, fn2);
		Vec3d normal = faceNormal.crossProduct(derivative)
			.normalize();
		return derivative.crossProduct(normal);
	}

	private void resolve() {
		if (resolved)
			return;
		resolved = true;

		Vec3d end1 = starts.getFirst();
		Vec3d end2 = starts.getSecond();
		Vec3d axis1 = axes.getFirst()
			.normalize();
		Vec3d axis2 = axes.getSecond()
			.normalize();

		determineHandles(end1, end2, axis1, axis2);

		finish1 = axis1.multiply(handleLength)
			.add(end1);
		finish2 = axis2.multiply(handleLength)
			.add(end2);

		int scanCount = 16;
		length = 0;

		{
			Vec3d previous = end1;
			for (int i = 0; i <= scanCount; i++) {
				float t = i / (float) scanCount;
				Vec3d result = VecHelper.bezier(end1, end2, finish1, finish2, t);
				if (previous != null)
					length += result.distanceTo(previous);
				previous = result;
			}
		}

		segments = (int) (length * 2);
		stepLUT = new float[segments + 1];
		stepLUT[0] = 1;
		float combinedDistance = 0;

		bounds = new Box(end1, end2);

		// determine step lut
		{
			Vec3d previous = end1;
			for (int i = 0; i <= segments; i++) {
				float t = i / (float) segments;
				Vec3d result = VecHelper.bezier(end1, end2, finish1, finish2, t);
				bounds = bounds.union(new Box(result, result));
				if (i > 0) {
					combinedDistance += result.distanceTo(previous) / length;
					stepLUT[i] = (float) (t / combinedDistance);
				}
				previous = result;
			}
		}

		bounds = bounds.expand(1.375f);
	}

	private void determineHandles(Vec3d end1, Vec3d end2, Vec3d axis1, Vec3d axis2) {
		Vec3d cross1 = axis1.crossProduct(new Vec3d(0, 1, 0));
		Vec3d cross2 = axis2.crossProduct(new Vec3d(0, 1, 0));

		radius = 0;
		double a1 = MathHelper.atan2(-axis2.z, -axis2.x);
		double a2 = MathHelper.atan2(axis1.z, axis1.x);
		double angle = a1 - a2;

		float circle = 2 * MathHelper.PI;
		angle = (angle + circle) % circle;
		if (Math.abs(circle - angle) < Math.abs(angle))
			angle = circle - angle;

		if (MathHelper.approximatelyEquals(angle, 0)) {
			double[] intersect = VecHelper.intersect(end1, end2, axis1, cross2, Axis.Y);
			if (intersect != null) {
				double t = Math.abs(intersect[0]);
				double u = Math.abs(intersect[1]);
				double min = Math.min(t, u);
				double max = Math.max(t, u);

				if (min > 1.2 && max / min > 1 && max / min < 3) {
					handleLength = (max - min);
					return;
				}
			}

			handleLength = end2.distanceTo(end1) / 3;
			return;
		}

		double n = circle / angle;
		double factor = 4 / 3d * Math.tan(Math.PI / (2 * n));
		double[] intersect = VecHelper.intersect(end1, end2, cross1, cross2, Axis.Y);

		if (intersect == null) {
			handleLength = end2.distanceTo(end1) / 3;
			return;
		}

		radius = Math.abs(intersect[1]);
		handleLength = radius * factor;
		if (MathHelper.approximatelyEquals(handleLength, 0))
			handleLength = 1;
	}

	@Override
	public Iterator<Segment> iterator() {
		resolve();
		var offset = Vec3d.of(tePositions.getFirst())
			.multiply(-1)
			.add(0, 3 / 16f, 0);
		return new Bezierator(this, offset);
	}

	public void addItemsToPlayer(PlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		int tracks = getTrackItemCost();
		while (tracks > 0) {
			inv.offerOrDrop(new ItemStack(getMaterial().getBlock(), Math.min(64, tracks)));
			tracks -= 64;
		}
		int girders = getGirderItemCost();
		while (girders > 0) {
			inv.offerOrDrop(AllBlocks.METAL_GIRDER.asStack(Math.min(64, girders)));
			girders -= 64;
		}
	}

	public int getGirderItemCost() {
		return hasGirder ? getTrackItemCost() * 2 : 0;
	}

	public int getTrackItemCost() {
		return (getSegmentCount() + 1) / 2;
	}

	public void spawnItems(World level) {
		if (!level.getGameRules()
			.getBoolean(GameRules.DO_TILE_DROPS))
			return;
		Vec3d origin = Vec3d.of(tePositions.getFirst());
		for (Segment segment : this) {
			if (segment.index % 2 != 0 || segment.index == getSegmentCount())
				continue;
			Vec3d v = VecHelper.offsetRandomly(segment.position, level.random, .125f)
				.add(origin);
			ItemEntity entity = new ItemEntity(level, v.x, v.y, v.z, getMaterial().asStack());
			entity.setToDefaultPickupDelay();
			level.spawnEntity(entity);
			if (!hasGirder)
				continue;
			for (int i = 0; i < 2; i++) {
				entity = new ItemEntity(level, v.x, v.y, v.z, AllBlocks.METAL_GIRDER.asStack());
				entity.setToDefaultPickupDelay();
				level.spawnEntity(entity);
			}
		}
	}

	public void spawnDestroyParticles(World level) {
		BlockStateParticleEffect data = new BlockStateParticleEffect(ParticleTypes.BLOCK, getMaterial().getBlock().getDefaultState());
		BlockStateParticleEffect girderData =
			new BlockStateParticleEffect(ParticleTypes.BLOCK, AllBlocks.METAL_GIRDER.getDefaultState());
		if (!(level instanceof ServerWorld slevel))
			return;
		Vec3d origin = Vec3d.of(tePositions.getFirst());
		for (Segment segment : this) {
			for (int offset : Iterate.positiveAndNegative) {
				Vec3d v = segment.position.add(segment.normal.multiply(14 / 16f * offset))
					.add(origin);
				slevel.spawnParticles(data, v.x, v.y, v.z, 1, 0, 0, 0, 0);
				if (!hasGirder)
					continue;
				slevel.spawnParticles(girderData, v.x, v.y - .5f, v.z, 1, 0, 0, 0, 0);
			}
		}
	}

	public TrackMaterial getMaterial() {
		return trackMaterial;
	}

	public void setMaterial(TrackMaterial material) {
		trackMaterial = material;
	}

	public static class Segment {

		public int index;
		public Vec3d position;
		public Vec3d derivative;
		public Vec3d faceNormal;
		public Vec3d normal;

	}

	private static class Bezierator implements Iterator<Segment> {

		private final BezierConnection bc;
		private final Segment segment;
		private final Vec3d end1;
		private final Vec3d end2;
		private final Vec3d finish1;
		private final Vec3d finish2;
		private final Vec3d faceNormal1;
		private final Vec3d faceNormal2;

		private Bezierator(BezierConnection bc, Vec3d offset) {
			bc.resolve();
			this.bc = bc;

			end1 = bc.starts.getFirst()
				.add(offset);
			end2 = bc.starts.getSecond()
				.add(offset);

			finish1 = bc.axes.getFirst()
				.multiply(bc.handleLength)
				.add(end1);
			finish2 = bc.axes.getSecond()
				.multiply(bc.handleLength)
				.add(end2);

			faceNormal1 = bc.normals.getFirst();
			faceNormal2 = bc.normals.getSecond();
			segment = new Segment();
			segment.index = -1; // will get incremented to 0 in #next()
		}

		@Override
		public boolean hasNext() {
			return segment.index + 1 <= bc.segments;
		}

		@Override
		public Segment next() {
			segment.index++;
			float t = this.bc.getSegmentT(segment.index);
			segment.position = VecHelper.bezier(end1, end2, finish1, finish2, t);
			segment.derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
				.normalize();
			segment.faceNormal =
				faceNormal1.equals(faceNormal2) ? faceNormal1 : VecHelper.slerp(t, faceNormal1, faceNormal2);
			segment.normal = segment.faceNormal.crossProduct(segment.derivative)
				.normalize();
			return segment;
		}
	}

	private SegmentAngles[] bakedSegments;
	private GirderAngles[] bakedGirders;

	@Environment(EnvType.CLIENT)
	public static class SegmentAngles {

		public Entry tieTransform;
		public Couple<Entry> railTransforms;
		public BlockPos lightPosition;

	}

	@Environment(EnvType.CLIENT)
	public static class GirderAngles {

		public Couple<Entry> beams;
		public Couple<Couple<Entry>> beamCaps;
		public BlockPos lightPosition;

	}

	@Environment(EnvType.CLIENT)
	public SegmentAngles[] getBakedSegments() {
		if (bakedSegments != null)
			return bakedSegments;

		int segmentCount = getSegmentCount();
		bakedSegments = new SegmentAngles[segmentCount + 1];
		Couple<Vec3d> previousOffsets = null;

		for (BezierConnection.Segment segment : this) {
			int i = segment.index;
			boolean end = i == 0 || i == segmentCount;

			SegmentAngles angles = bakedSegments[i] = new SegmentAngles();
			Couple<Vec3d> railOffsets = Couple.create(segment.position.add(segment.normal.multiply(.965f)),
				segment.position.subtract(segment.normal.multiply(.965f)));
			Vec3d railMiddle = railOffsets.getFirst()
				.add(railOffsets.getSecond())
				.multiply(.5);

			if (previousOffsets == null) {
				previousOffsets = railOffsets;
				continue;
			}

			// Tie
			Vec3d prevMiddle = previousOffsets.getFirst()
				.add(previousOffsets.getSecond())
				.multiply(.5);
			Vec3d tieAngles = TrackRenderer.getModelAngles(segment.normal, railMiddle.subtract(prevMiddle));
			angles.lightPosition = BlockPos.ofFloored(railMiddle);
			angles.railTransforms = Couple.create(null, null);

			MatrixStack poseStack = new MatrixStack();
			TransformStack.cast(poseStack)
				.translate(prevMiddle)
				.rotateYRadians(tieAngles.y)
				.rotateXRadians(tieAngles.x)
				.rotateZRadians(tieAngles.z)
				.translate(-1 / 2f, -2 / 16f - 1 / 256f, 0);
			angles.tieTransform = poseStack.peek();

			// Rails
			float scale = end ? 2.2f : 2.1f;
			for (boolean first : Iterate.trueAndFalse) {
				Vec3d railI = railOffsets.get(first);
				Vec3d prevI = previousOffsets.get(first);
				Vec3d diff = railI.subtract(prevI);
				Vec3d anglesI = TrackRenderer.getModelAngles(segment.normal, diff);

				poseStack = new MatrixStack();
				TransformStack.cast(poseStack)
					.translate(prevI)
					.rotateYRadians(anglesI.y)
					.rotateXRadians(anglesI.x)
					.rotateZRadians(anglesI.z)
					.translate(0, -2 / 16f - 1 / 256f, -1 / 32f)
					.scale(1, 1, (float) diff.length() * scale);
				angles.railTransforms.set(first, poseStack.peek());
			}

			previousOffsets = railOffsets;
		}

		return bakedSegments;
	}

	@Environment(EnvType.CLIENT)
	public GirderAngles[] getBakedGirders() {
		if (bakedGirders != null)
			return bakedGirders;

		int segmentCount = getSegmentCount();
		bakedGirders = new GirderAngles[segmentCount + 1];
		Couple<Couple<Vec3d>> previousOffsets = null;

		for (BezierConnection.Segment segment : this) {
			int i = segment.index;
			boolean end = i == 0 || i == segmentCount;
			GirderAngles angles = bakedGirders[i] = new GirderAngles();

			Vec3d leftGirder = segment.position.add(segment.normal.multiply(.965f));
			Vec3d rightGirder = segment.position.subtract(segment.normal.multiply(.965f));
			Vec3d upNormal = segment.derivative.normalize()
				.crossProduct(segment.normal);
			Vec3d firstGirderOffset = upNormal.multiply(-8 / 16f);
			Vec3d secondGirderOffset = upNormal.multiply(-10 / 16f);
			Vec3d leftTop = segment.position.add(segment.normal.multiply(1))
				.add(firstGirderOffset);
			Vec3d rightTop = segment.position.subtract(segment.normal.multiply(1))
				.add(firstGirderOffset);
			Vec3d leftBottom = leftTop.add(secondGirderOffset);
			Vec3d rightBottom = rightTop.add(secondGirderOffset);

			angles.lightPosition = BlockPos.ofFloored(leftGirder.add(rightGirder)
				.multiply(.5));

			Couple<Couple<Vec3d>> offsets =
				Couple.create(Couple.create(leftTop, rightTop), Couple.create(leftBottom, rightBottom));

			if (previousOffsets == null) {
				previousOffsets = offsets;
				continue;
			}

			angles.beams = Couple.create(null, null);
			angles.beamCaps = Couple.create(Couple.create(null, null), Couple.create(null, null));
			float scale = end ? 2.3f : 2.2f;

			for (boolean first : Iterate.trueAndFalse) {

				// Middle
				Vec3d currentBeam = offsets.getFirst()
					.get(first)
					.add(offsets.getSecond()
						.get(first))
					.multiply(.5);
				Vec3d previousBeam = previousOffsets.getFirst()
					.get(first)
					.add(previousOffsets.getSecond()
						.get(first))
					.multiply(.5);
				Vec3d beamDiff = currentBeam.subtract(previousBeam);
				Vec3d beamAngles = TrackRenderer.getModelAngles(segment.normal, beamDiff);

				MatrixStack poseStack = new MatrixStack();
				TransformStack.cast(poseStack)
					.translate(previousBeam)
					.rotateYRadians(beamAngles.y)
					.rotateXRadians(beamAngles.x)
					.rotateZRadians(beamAngles.z)
					.translate(0, 2 / 16f + (segment.index % 2 == 0 ? 1 : -1) / 2048f - 1 / 1024f, -1 / 32f)
					.scale(1, 1, (float) beamDiff.length() * scale);
				angles.beams.set(first, poseStack.peek());

				// Caps
				for (boolean top : Iterate.trueAndFalse) {
					Vec3d current = offsets.get(top)
						.get(first);
					Vec3d previous = previousOffsets.get(top)
						.get(first);
					Vec3d diff = current.subtract(previous);
					Vec3d capAngles = TrackRenderer.getModelAngles(segment.normal, diff);

					poseStack = new MatrixStack();
					TransformStack.cast(poseStack)
						.translate(previous)
						.rotateYRadians(capAngles.y)
						.rotateXRadians(capAngles.x)
						.rotateZRadians(capAngles.z)
						.translate(0, 2 / 16f + (segment.index % 2 == 0 ? 1 : -1) / 2048f - 1 / 1024f, -1 / 32f)
						.rotateZ(top ? 0 : 0)
						.scale(1, 1, (float) diff.length() * scale);
					angles.beamCaps.get(top)
						.set(first, poseStack.peek());
				}
			}

			previousOffsets = offsets;

		}

		return bakedGirders;
	}

}
