package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.tags.Tags.Items;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MechanicalPistonBlock extends DirectionalAxisKineticBlock implements IBE<MechanicalPistonBlockEntity> {

	public static final EnumProperty<PistonState> STATE = EnumProperty.of("state", PistonState.class);
	protected boolean isSticky;

	public static MechanicalPistonBlock normal(Settings properties) {
		return new MechanicalPistonBlock(properties, false);
	}

	public static MechanicalPistonBlock sticky(Settings properties) {
		return new MechanicalPistonBlock(properties, true);
	}

	protected MechanicalPistonBlock(Settings properties, boolean sticky) {
		super(properties);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH)
			.with(STATE, PistonState.RETRACTED));
		isSticky = sticky;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(STATE);
		super.appendProperties(builder);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (!player.canModifyBlocks())
			return ActionResult.PASS;
		if (player.isSneaking())
			return ActionResult.PASS;
		if (!player.getStackInHand(handIn)
			.isIn(Items.SLIMEBALLS)) {
			if (player.getStackInHand(handIn)
				.isEmpty()) {
				withBlockEntityDo(worldIn, pos, be -> be.assembleNextTick = true);
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		}
		if (state.get(STATE) != PistonState.RETRACTED)
			return ActionResult.PASS;
		Direction direction = state.get(FACING);
		if (hit.getSide() != direction)
			return ActionResult.PASS;
		if (((MechanicalPistonBlock) state.getBlock()).isSticky)
			return ActionResult.PASS;
		if (worldIn.isClient) {
			Vec3d vec = hit.getPos();
			worldIn.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0, 0, 0);
			return ActionResult.SUCCESS;
		}
		AllSoundEvents.SLIME_ADDED.playOnServer(worldIn, pos, .5f, 1);
		if (!player.isCreative())
			player.getStackInHand(handIn)
				.decrement(1);
		worldIn.setBlockState(pos, AllBlocks.STICKY_MECHANICAL_PISTON.getDefaultState()
			.with(FACING, direction)
			.with(AXIS_ALONG_FIRST_COORDINATE, state.get(AXIS_ALONG_FIRST_COORDINATE)));
		return ActionResult.SUCCESS;
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block p_220069_4_, BlockPos fromPos,
		boolean p_220069_6_) {
		Direction direction = state.get(FACING);
		if (!fromPos.equals(pos.offset(direction.getOpposite())))
			return;
		if (!world.isClient && !world.getBlockTickScheduler()
			.isTicking(pos, this))
			world.scheduleBlockTick(pos, this, 0);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random r) {
		Direction direction = state.get(FACING);
		BlockState pole = worldIn.getBlockState(pos.offset(direction.getOpposite()));
		if (!AllBlocks.PISTON_EXTENSION_POLE.has(pole))
			return;
		if (pole.get(PistonExtensionPoleBlock.FACING)
			.getAxis() != direction.getAxis())
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			if (be.lastException == null)
				return;
			be.lastException = null;
			be.sendData();
		});
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (state.get(STATE) != PistonState.RETRACTED)
			return ActionResult.PASS;
		return super.onWrenched(state, context);
	}

	public enum PistonState implements StringIdentifiable {
		RETRACTED, MOVING, EXTENDED;

		@Override
		public String asString() {
			return Lang.asId(name());
		}
	}

	@Override
	public void onBreak(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
		Direction direction = state.get(FACING);
		BlockPos pistonHead = null;
		BlockPos pistonBase = pos;
		boolean dropBlocks = player == null || !player.isCreative();

		Integer maxPoles = maxAllowedPistonPoles();
		for (int offset = 1; offset < maxPoles; offset++) {
			BlockPos currentPos = pos.offset(direction, offset);
			BlockState block = worldIn.getBlockState(currentPos);

			if (isExtensionPole(block) && direction.getAxis() == block.get(Properties.FACING)
				.getAxis())
				continue;

			if (isPistonHead(block) && block.get(Properties.FACING) == direction) {
				pistonHead = currentPos;
			}

			break;
		}

		if (pistonHead != null && pistonBase != null) {
			BlockPos.stream(pistonBase, pistonHead)
				.filter(p -> !p.equals(pos))
				.forEach(p -> worldIn.breakBlock(p, dropBlocks));
		}

		for (int offset = 1; offset < maxPoles; offset++) {
			BlockPos currentPos = pos.offset(direction.getOpposite(), offset);
			BlockState block = worldIn.getBlockState(currentPos);

			if (isExtensionPole(block) && direction.getAxis() == block.get(Properties.FACING)
				.getAxis()) {
				worldIn.breakBlock(currentPos, dropBlocks);
				continue;
			}

			break;
		}

		super.onBreak(worldIn, pos, state, player);
	}

	public static int maxAllowedPistonPoles() {
		return AllConfigs.server().kinetics.maxPistonPoles.get();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {

		if (state.get(STATE) == PistonState.EXTENDED)
			return AllShapes.MECHANICAL_PISTON_EXTENDED.get(state.get(FACING));

		if (state.get(STATE) == PistonState.MOVING)
			return AllShapes.MECHANICAL_PISTON.get(state.get(FACING));

		return VoxelShapes.fullCube();
	}

	@Override
	public Class<MechanicalPistonBlockEntity> getBlockEntityClass() {
		return MechanicalPistonBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalPistonBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MECHANICAL_PISTON.get();
	}

	public static boolean isPiston(BlockState state) {
		return AllBlocks.MECHANICAL_PISTON.has(state) || isStickyPiston(state);
	}

	public static boolean isStickyPiston(BlockState state) {
		return AllBlocks.STICKY_MECHANICAL_PISTON.has(state);
	}

	public static boolean isExtensionPole(BlockState state) {
		return AllBlocks.PISTON_EXTENSION_POLE.has(state);
	}

	public static boolean isPistonHead(BlockState state) {
		return AllBlocks.MECHANICAL_PISTON_HEAD.has(state);
	}
}
