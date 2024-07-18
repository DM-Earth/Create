package com.simibubi.create.content.contraptions.mounted;

import java.util.List;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.minecart.CouplingHandler;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

public class CartAssemblerBlockEntity extends SmartBlockEntity implements IDisplayAssemblyExceptions {
	private static final int assemblyCooldown = 8;

	protected ScrollOptionBehaviour<CartMovementMode> movementMode;
	private int ticksSinceMinecartUpdate;
	protected AssemblyException lastException;
	protected AbstractMinecartEntity cartToAssemble;

	public CartAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		ticksSinceMinecartUpdate = assemblyCooldown;
	}

	@Override
	public void tick() {
		super.tick();
		if (ticksSinceMinecartUpdate < assemblyCooldown) {
			ticksSinceMinecartUpdate++;
		}

		tryAssemble(cartToAssemble);
		cartToAssemble = null;
	}

	public void tryAssemble(AbstractMinecartEntity cart) {
		if (cart == null)
			return;

		if (!isMinecartUpdateValid())
			return;
		resetTicksSinceMinecartUpdate();

		BlockState state = world.getBlockState(pos);
		if (!AllBlocks.CART_ASSEMBLER.has(state))
			return;
		CartAssemblerBlock block = (CartAssemblerBlock) state.getBlock();

		CartAssemblerBlock.CartAssemblerAction action = CartAssemblerBlock.getActionForCart(state, cart);
		if (action.shouldAssemble())
			assemble(world, pos, cart);
		if (action.shouldDisassemble())
			disassemble(world, pos, cart);
		if (action == CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE) {
			if (cart.getVelocity()
				.length() > 1 / 128f) {
				Direction facing = cart.getMovementDirection();
				RailShape railShape = state.get(CartAssemblerBlock.RAIL_SHAPE);
				for (Direction d : Iterate.directionsInAxis(railShape == RailShape.EAST_WEST ? Axis.X : Axis.Z))
					if (world.getBlockState(pos.offset(d))
						.isSolidBlock(world, pos.offset(d)))
						facing = d.getOpposite();

				float speed = 0.4f;//block.getRailMaxSpeed(state, level, worldPosition, cart);
				cart.setVelocity(facing.getOffsetX() * speed, facing.getOffsetY() * speed, facing.getOffsetZ() * speed);
			}
		}
		if (action == CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE_DIRECTIONAL) {
			Vec3i accelerationVector =
				ControllerRailBlock.getAccelerationVector(AllBlocks.CONTROLLER_RAIL.getDefaultState()
					.with(ControllerRailBlock.SHAPE, state.get(CartAssemblerBlock.RAIL_SHAPE))
					.with(ControllerRailBlock.BACKWARDS, state.get(CartAssemblerBlock.BACKWARDS)));
			float speed = 0.4f;//block.getRailMaxSpeed(state, level, worldPosition, cart);
			cart.setVelocity(Vec3d.of(accelerationVector)
				.multiply(speed));
		}
		if (action == CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE_BRAKE) {
			Vec3d diff = VecHelper.getCenterOf(pos)
				.subtract(cart.getPos());
			cart.setVelocity(diff.x / 16f, 0, diff.z / 16f);
		}
	}

	protected void assemble(World world, BlockPos pos, AbstractMinecartEntity cart) {
		if (!cart.getPassengerList()
			.isEmpty())
			return;

		if (cart.create$getController()
			.isCoupledThroughContraption())
			return;

		CartMovementMode mode = CartMovementMode.values()[movementMode.value];

		MountedContraption contraption = new MountedContraption(mode);
		try {
			if (!contraption.assemble(world, pos))
				return;

			lastException = null;
			sendData();
		} catch (AssemblyException e) {
			lastException = e;
			sendData();
			return;
		}

		boolean couplingFound = contraption.connectedCart != null;
		Direction initialOrientation = CartAssemblerBlock.getHorizontalDirection(getCachedState());

		if (couplingFound) {
			cart.setPosition(pos.getX() + .5f, pos.getY(), pos.getZ() + .5f);
			if (!CouplingHandler.tryToCoupleCarts(null, world, cart.getId(),
				contraption.connectedCart.getId()))
				return;
		}

		contraption.removeBlocksFromWorld(world, BlockPos.ORIGIN);
		contraption.startMoving(world);
		contraption.expandBoundsAroundAxis(Axis.Y);

		if (couplingFound) {
			Vec3d diff = contraption.connectedCart.getPos()
				.subtract(cart.getPos());
			initialOrientation = Direction.fromRotation(MathHelper.atan2(diff.z, diff.x) * 180 / Math.PI);
		}

		OrientedContraptionEntity entity = OrientedContraptionEntity.create(world, contraption, initialOrientation);
		if (couplingFound)
			entity.setCouplingId(cart.getUuid());
		entity.setPosition(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		world.spawnEntity(entity);
		entity.startRiding(cart);

		if (cart instanceof FurnaceMinecartEntity) {
			NbtCompound nbt = NBTSerializer.serializeNBTCompound(cart);
			nbt.putDouble("PushZ", 0);
			nbt.putDouble("PushX", 0);
			NBTSerializer.deserializeNBT(cart, nbt);
		}

		if (contraption.containsBlockBreakers())
			award(AllAdvancements.CONTRAPTION_ACTORS);
	}

	protected void disassemble(World world, BlockPos pos, AbstractMinecartEntity cart) {
		if (cart.getPassengerList()
			.isEmpty())
			return;
		Entity entity = cart.getPassengerList()
			.get(0);
		if (!(entity instanceof OrientedContraptionEntity))
			return;
		OrientedContraptionEntity contraption = (OrientedContraptionEntity) entity;
		UUID couplingId = contraption.getCouplingId();

		if (couplingId == null) {
			contraption.yaw = CartAssemblerBlock.getHorizontalDirection(getCachedState())
				.asRotation();
			disassembleCart(cart);
			return;
		}

		Couple<MinecartController> coupledCarts = contraption.getCoupledCartsIfPresent();
		if (coupledCarts == null)
			return;

		// Make sure connected cart is present and being disassembled
		for (boolean current : Iterate.trueAndFalse) {
			MinecartController minecartController = coupledCarts.get(current);
			if (minecartController.cart() == cart)
				continue;
			BlockPos otherPos = minecartController.cart()
				.getBlockPos();
			BlockState blockState = world.getBlockState(otherPos);
			if (!AllBlocks.CART_ASSEMBLER.has(blockState))
				return;
			if (!CartAssemblerBlock.getActionForCart(blockState, minecartController.cart())
				.shouldDisassemble())
				return;
		}

		for (boolean current : Iterate.trueAndFalse)
			coupledCarts.get(current)
				.removeConnection(current);
		disassembleCart(cart);
	}

	protected void disassembleCart(AbstractMinecartEntity cart) {
		cart.removeAllPassengers();
		if (cart instanceof FurnaceMinecartEntity) {
			NbtCompound nbt = NBTSerializer.serializeNBTCompound(cart);
			nbt.putDouble("PushZ", cart.getVelocity().x);
			nbt.putDouble("PushX", cart.getVelocity().z);
			NBTSerializer.deserializeNBT(cart, nbt);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		movementMode = new ScrollOptionBehaviour<>(CartMovementMode.class,
			Lang.translateDirect("contraptions.cart_movement_mode"), this, getMovementModeSlot());
		behaviours.add(movementMode);
		registerAwardables(behaviours, AllAdvancements.CONTRAPTION_ACTORS);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		AssemblyException.write(compound, lastException);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		lastException = AssemblyException.read(compound);
		super.read(compound, clientPacket);
	}

	@Override
	public AssemblyException getLastAssemblyException() {
		return lastException;
	}

	protected ValueBoxTransform getMovementModeSlot() {
		return new CartAssemblerValueBoxTransform();
	}

	private class CartAssemblerValueBoxTransform extends CenteredSideValueBoxTransform {

		public CartAssemblerValueBoxTransform() {
			super((state, d) -> {
				if (d.getAxis()
					.isVertical())
					return false;
				if (!state.contains(CartAssemblerBlock.RAIL_SHAPE))
					return false;
				RailShape railShape = state.get(CartAssemblerBlock.RAIL_SHAPE);
				return (d.getAxis() == Axis.X) == (railShape == RailShape.NORTH_SOUTH);
			});
		}

		@Override
		protected Vec3d getSouthLocation() {
			return VecHelper.voxelSpace(8, 7, 17.5);
		}

	}

	public enum CartMovementMode implements INamedIconOptions {

		ROTATE(AllIcons.I_CART_ROTATE),
		ROTATE_PAUSED(AllIcons.I_CART_ROTATE_PAUSED),
		ROTATION_LOCKED(AllIcons.I_CART_ROTATE_LOCKED),

		;

		private String translationKey;
		private AllIcons icon;

		CartMovementMode(AllIcons icon) {
			this.icon = icon;
			translationKey = "contraptions.cart_movement_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}
	}

	public void resetTicksSinceMinecartUpdate() {
		ticksSinceMinecartUpdate = 0;
	}

	public void assembleNextTick(AbstractMinecartEntity cart) {
		if (cartToAssemble == null)
			cartToAssemble = cart;
	}

	public boolean isMinecartUpdateValid() {
		return ticksSinceMinecartUpdate >= assemblyCooldown;
	}

}
