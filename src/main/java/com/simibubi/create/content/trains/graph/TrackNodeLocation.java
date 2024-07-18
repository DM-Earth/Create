package com.simibubi.create.content.trains.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.utility.Iterate;

public class TrackNodeLocation extends Vec3i {

	public RegistryKey<World> dimension;
	public int yOffsetPixels;

	public TrackNodeLocation(Vec3d vec) {
		this(vec.x, vec.y, vec.z);
	}

	public TrackNodeLocation(double x, double y, double z) {
		super(MathHelper.floor(Math.round(x * 2)), MathHelper.floor(y) * 2, MathHelper.floor(Math.round(z * 2)));
	}

	public TrackNodeLocation in(World level) {
		return in(level.getRegistryKey());
	}

	public TrackNodeLocation in(RegistryKey<World> dimension) {
		this.dimension = dimension;
		return this;
	}

	private static TrackNodeLocation fromPackedPos(BlockPos bufferPos) {
		return new TrackNodeLocation(bufferPos);
	}

	private TrackNodeLocation(BlockPos readBlockPos) {
		super(readBlockPos.getX(), readBlockPos.getY(), readBlockPos.getZ());
	}

	public Vec3d getLocation() {
		return new Vec3d(getX() / 2.0, getY() / 2.0 + yOffsetPixels / 16.0, getZ() / 2.0);
	}

	public RegistryKey<World> getDimension() {
		return dimension;
	}

	@Override
	public boolean equals(Object pOther) {
		return equalsIgnoreDim(pOther) && pOther instanceof TrackNodeLocation tnl
			&& Objects.equals(tnl.dimension, dimension);
	}

	public boolean equalsIgnoreDim(Object pOther) {
		return super.equals(pOther) && pOther instanceof TrackNodeLocation tnl && tnl.yOffsetPixels == yOffsetPixels;
	}

	@Override
	public int hashCode() {
		return (getY() + ((getZ() + yOffsetPixels * 31) * 31 + dimension.hashCode()) * 31) * 31 + getX();
	}

	public NbtCompound write(DimensionPalette dimensions) {
		NbtCompound c = NbtHelper.fromBlockPos(new BlockPos(this));
		if (dimensions != null)
			c.putInt("D", dimensions.encode(dimension));
		if (yOffsetPixels != 0)
			c.putInt("YO", yOffsetPixels);
		return c;
	}

	public static TrackNodeLocation read(NbtCompound tag, DimensionPalette dimensions) {
		TrackNodeLocation location = fromPackedPos(NbtHelper.toBlockPos(tag));
		if (dimensions != null)
			location.dimension = dimensions.decode(tag.getInt("D"));
		location.yOffsetPixels = tag.getInt("YO");
		return location;
	}

	public void send(PacketByteBuf buffer, DimensionPalette dimensions) {
		buffer.writeVarInt(getX());
		buffer.writeShort(getY());
		buffer.writeVarInt(getZ());
		buffer.writeVarInt(yOffsetPixels);
		buffer.writeVarInt(dimensions.encode(dimension));
	}

	public static TrackNodeLocation receive(PacketByteBuf buffer, DimensionPalette dimensions) {
		TrackNodeLocation location = fromPackedPos(new BlockPos(
				buffer.readVarInt(),
				buffer.readShort(),
				buffer.readVarInt()
		));
		location.yOffsetPixels = buffer.readVarInt();
		location.dimension = dimensions.decode(buffer.readVarInt());
		return location;
	}

	public Collection<BlockPos> allAdjacent() {
		Set<BlockPos> set = new HashSet<>();
		Vec3d vec3 = getLocation().subtract(0, yOffsetPixels / 16.0, 0);
		double step = 1 / 8f;
		for (int x : Iterate.positiveAndNegative)
			for (int y : Iterate.positiveAndNegative)
				for (int z : Iterate.positiveAndNegative)
					set.add(BlockPos.ofFloored(vec3.add(x * step, y * step, z * step)));
		return set;
	}

	public static class DiscoveredLocation extends TrackNodeLocation {

		BezierConnection turn = null;
		boolean forceNode = false;
		Vec3d direction;
		Vec3d normal;
		TrackMaterial materialA;
		TrackMaterial materialB;

		public DiscoveredLocation(World level, double x, double y, double z) {
			super(x, y, z);
			in(level);
		}

		public DiscoveredLocation(RegistryKey<World> dimension, Vec3d vec) {
			super(vec);
			in(dimension);
		}

		public DiscoveredLocation(World level, Vec3d vec) {
			this(level.getRegistryKey(), vec);
		}

		public DiscoveredLocation materialA(TrackMaterial material) {
			this.materialA = material;
			return this;
		}

		public DiscoveredLocation materialB(TrackMaterial material) {
			this.materialB = material;
			return this;
		}

		public DiscoveredLocation materials(TrackMaterial materialA, TrackMaterial materialB) {
			this.materialA = materialA;
			this.materialB = materialB;
			return this;
		}

		public DiscoveredLocation viaTurn(BezierConnection turn) {
			this.turn = turn;
			if (turn != null)
				forceNode();
			return this;
		}

		public DiscoveredLocation forceNode() {
			forceNode = true;
			return this;
		}

		public DiscoveredLocation withNormal(Vec3d normal) {
			this.normal = normal;
			return this;
		}
		
		public DiscoveredLocation withYOffset(int yOffsetPixels) {
			this.yOffsetPixels = yOffsetPixels;
			return this;
		}

		public DiscoveredLocation withDirection(Vec3d direction) {
			this.direction = direction == null ? null : direction.normalize();
			return this;
		}

		public boolean connectedViaTurn() {
			return turn != null;
		}

		public BezierConnection getTurn() {
			return turn;
		}

		public boolean shouldForceNode() {
			return forceNode;
		}

		public boolean differentMaterials() {
			return materialA != materialB;
		}

		public boolean notInLineWith(Vec3d direction) {
			return this.direction != null
				&& Math.max(direction.dotProduct(this.direction), direction.dotProduct(this.direction.multiply(-1))) < 7 / 8f;
		}

		public Vec3d getDirection() {
			return direction;
		}

	}

}
