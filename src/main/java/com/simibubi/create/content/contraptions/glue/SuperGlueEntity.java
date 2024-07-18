package com.simibubi.create.content.contraptions.glue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.entity.IEntityAdditionalSpawnData;
import io.github.fabricators_of_create.porting_lib.entity.PortingLibEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class SuperGlueEntity extends Entity
		implements IEntityAdditionalSpawnData, ISpecialEntityItemRequirement {

	public static Box span(BlockPos startPos, BlockPos endPos) {
		return new Box(startPos, endPos).stretch(1, 1, 1);
	}

	public static boolean isGlued(WorldAccess level, BlockPos blockPos, Direction direction,
								  Set<SuperGlueEntity> cached) {
		BlockPos targetPos = blockPos.offset(direction);
		if (cached != null)
			for (SuperGlueEntity glueEntity : cached)
				if (glueEntity.contains(blockPos) && glueEntity.contains(targetPos))
					return true;
		for (SuperGlueEntity glueEntity : level.getNonSpectatingEntities(SuperGlueEntity.class,
				span(blockPos, targetPos).expand(16))) {
			if (!glueEntity.contains(blockPos) || !glueEntity.contains(targetPos))
				continue;
			if (cached != null)
				cached.add(glueEntity);
			return true;
		}
		return false;
	}

	public static List<SuperGlueEntity> collectCropped(World level, Box bb) {
		List<SuperGlueEntity> glue = new ArrayList<>();
		for (SuperGlueEntity glueEntity : level.getNonSpectatingEntities(SuperGlueEntity.class, bb)) {
			Box glueBox = glueEntity.getBoundingBox();
			Box intersect = bb.intersection(glueBox);
			if (intersect.getXLength() * intersect.getYLength() * intersect.getZLength() == 0)
				continue;
			if (MathHelper.approximatelyEquals(intersect.getAverageSideLength(), 1))
				continue;
			glue.add(new SuperGlueEntity(level, intersect));
		}
		return glue;
	}

	public SuperGlueEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	public SuperGlueEntity(World world, Box boundingBox) {
		this(AllEntityTypes.SUPER_GLUE.get(), world);
		setBoundingBox(boundingBox);
		resetPositionToBB();
	}

	public void resetPositionToBB() {
		Box bb = getBoundingBox();
		setPos(bb.getCenter().x, bb.minY, bb.getCenter().z);
	}

	@Override
	protected void initDataTracker() {
	}

	public static boolean isValidFace(World world, BlockPos pos, Direction direction) {
		BlockState state = world.getBlockState(pos);
		if (BlockMovementChecks.isBlockAttachedTowards(state, world, pos, direction))
			return true;
		if (!BlockMovementChecks.isMovementNecessary(state, world, pos))
			return false;
		if (BlockMovementChecks.isNotSupportive(state, direction))
			return false;
		return true;
	}

	public static boolean isSideSticky(World world, BlockPos pos, Direction direction) {
		BlockState state = world.getBlockState(pos);
		if (AllBlocks.STICKY_MECHANICAL_PISTON.has(state))
			return state.get(DirectionalKineticBlock.FACING) == direction;

		if (AllBlocks.STICKER.has(state))
			return state.get(FacingBlock.FACING) == direction;

		if (state.getBlock() == Blocks.SLIME_BLOCK)
			return true;
		if (state.getBlock() == Blocks.HONEY_BLOCK)
			return true;

		if (AllBlocks.CART_ASSEMBLER.has(state))
			return Direction.UP == direction;

		if (AllBlocks.GANTRY_CARRIAGE.has(state))
			return state.get(DirectionalKineticBlock.FACING) == direction;

		if (state.getBlock() instanceof BearingBlock) {
			return state.get(DirectionalKineticBlock.FACING) == direction;
		}

		if (state.getBlock() instanceof AbstractChassisBlock) {
			BooleanProperty glueableSide = ((AbstractChassisBlock) state.getBlock()).getGlueableSide(state, direction);
			if (glueableSide == null)
				return false;
			return state.get(glueableSide);
		}

		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (getBoundingBox().getXLength() == 0)
			discard();
	}

	@Override
	public void setPosition(double x, double y, double z) {
		Box bb = getBoundingBox();
		setPos(x, y, z);
		Vec3d center = bb.getCenter();
		setBoundingBox(bb.offset(-center.x, -bb.minY, -center.z)
				.offset(x, y, z));
	}

	@Override
	public void move(MovementType typeIn, Vec3d pos) {
		if (!getWorld().isClient && isAlive() && pos.lengthSquared() > 0.0D)
			discard();
	}

	@Override
	public void addVelocity(double x, double y, double z) {
		if (!getWorld().isClient && isAlive() && x * x + y * y + z * z > 0.0D)
			discard();
	}

	@Override
	protected float getEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
		return 0.0F;
	}

	public void playPlaceSound() {
		AllSoundEvents.SLIME_ADDED.playFrom(this, 0.5F, 0.75F);
	}

	@Override
	public void pushAwayFrom(Entity entityIn) {
		super.pushAwayFrom(entityIn);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		return ActionResult.PASS;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound compound) {
		Vec3d position = getPos();
		writeBoundingBox(compound, getBoundingBox().offset(position.multiply(-1)));
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound compound) {
		Vec3d position = getPos();
		setBoundingBox(readBoundingBox(compound).offset(position));
	}

	public static void writeBoundingBox(NbtCompound compound, Box bb) {
		compound.put("From", VecHelper.writeNBT(new Vec3d(bb.minX, bb.minY, bb.minZ)));
		compound.put("To", VecHelper.writeNBT(new Vec3d(bb.maxX, bb.maxY, bb.maxZ)));
	}

	public static Box readBoundingBox(NbtCompound compound) {
		Vec3d from = VecHelper.readNBT(compound.getList("From", NbtElement.DOUBLE_TYPE));
		Vec3d to = VecHelper.readNBT(compound.getList("To", NbtElement.DOUBLE_TYPE));
		return new Box(from, to);
	}

	@Override
	protected boolean shouldSetPositionOnLoad() {
		return false;
	}

	@Override
	public float applyRotation(BlockRotation transformRotation) {
		Box bb = getBoundingBox().offset(getPos().multiply(-1));
		if (transformRotation == BlockRotation.CLOCKWISE_90 || transformRotation == BlockRotation.COUNTERCLOCKWISE_90)
			setBoundingBox(new Box(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX).offset(getPos()));
		return super.applyRotation(transformRotation);
	}

	@Override
	public float applyMirror(BlockMirror transformMirror) {
		return super.applyMirror(transformMirror);
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightningBolt) {
	}

	@Override
	public void calculateDimensions() {
	}

	public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
//		@SuppressWarnings("unchecked")
//		EntityType.Builder<SuperGlueEntity> entityBuilder = (EntityType.Builder<SuperGlueEntity>) builder;
		return builder;
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return PortingLibEntity.getEntitySpawningPacket(this);
	}

	@Override
	public void writeSpawnData(PacketByteBuf buffer) {
		NbtCompound compound = new NbtCompound();
		writeCustomDataToNbt(compound);
		buffer.writeNbt(compound);
	}

	@Override
	public void readSpawnData(PacketByteBuf additionalData) {
		readCustomDataFromNbt(additionalData.readNbt());
	}

	@Override
	public ItemRequirement getRequiredItems() {
		return new ItemRequirement(ItemUseType.DAMAGE, AllItems.SUPER_GLUE.get());
	}

	@Override
	public boolean canAvoidTraps() {
		return true;
	}

	public boolean contains(BlockPos pos) {
		return getBoundingBox().contains(Vec3d.ofCenter(pos));
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	public void setPortalEntrancePos() {
		lastNetherPortalPosition = getBlockPos();
	}

	@Override
	public TeleportTarget getTeleportTarget(ServerWorld pDestination) {
		return super.getTeleportTarget(pDestination);
	}

	public void spawnParticles() {
		Box bb = getBoundingBox();
		Vec3d origin = new Vec3d(bb.minX, bb.minY, bb.minZ);
		Vec3d extents = new Vec3d(bb.getXLength(), bb.getYLength(), bb.getZLength());

		if (!(getWorld() instanceof ServerWorld slevel))
			return;

		for (Axis axis : Iterate.axes) {
			AxisDirection positive = AxisDirection.POSITIVE;
			double max = axis.choose(extents.x, extents.y, extents.z);
			Vec3d normal = Vec3d.of(Direction.from(axis, positive)
					.getVector());
			for (Axis axis2 : Iterate.axes) {
				if (axis2 == axis)
					continue;
				double max2 = axis2.choose(extents.x, extents.y, extents.z);
				Vec3d normal2 = Vec3d.of(Direction.from(axis2, positive)
						.getVector());
				for (Axis axis3 : Iterate.axes) {
					if (axis3 == axis2 || axis3 == axis)
						continue;
					double max3 = axis3.choose(extents.x, extents.y, extents.z);
					Vec3d normal3 = Vec3d.of(Direction.from(axis3, positive)
							.getVector());

					for (int i = 0; i <= max * 2; i++) {
						for (int o1 : Iterate.zeroAndOne) {
							for (int o2 : Iterate.zeroAndOne) {
								Vec3d v = origin.add(normal.multiply(i / 2f))
										.add(normal2.multiply(max2 * o1))
										.add(normal3.multiply(max3 * o2));

								slevel.spawnParticles(ParticleTypes.ITEM_SLIME, v.x, v.y, v.z, 1, 0, 0, 0, 0);

							}
						}
					}
					break;
				}
				break;
			}
		}
	}
}
