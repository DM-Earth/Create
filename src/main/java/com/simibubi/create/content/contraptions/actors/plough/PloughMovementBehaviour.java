package com.simibubi.create.content.contraptions.actors.plough;

import com.simibubi.create.content.contraptions.actors.plough.PloughBlock.PloughFakePlayer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

public class PloughMovementBehaviour extends BlockBreakingMovementBehaviour {

	@Override
	public boolean isActive(MovementContext context) {
		return super.isActive(context)
			&& !VecHelper.isVecPointingTowards(context.relativeMotion, context.state.get(PloughBlock.FACING)
				.getOpposite());
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		super.visitNewPosition(context, pos);
		World world = context.world;
		if (world.isClient)
			return;
		BlockPos below = pos.down();
		if (!world.canSetBlock(below))
			return;

		Vec3d vec = VecHelper.getCenterOf(pos);
		PloughFakePlayer player = getPlayer(context);

		if (player == null)
			return;

		BlockHitResult ray = world.raycast(new RaycastContext(vec, vec.add(0, -1, 0), ShapeType.OUTLINE, FluidHandling.NONE, player));
		if (ray.getType() != Type.BLOCK)
			return;

		ItemUsageContext ctx = new ItemUsageContext(player, Hand.MAIN_HAND, ray);
		new ItemStack(Items.DIAMOND_HOE).useOnBlock(ctx);
	}

	@Override
	protected void throwEntity(MovementContext context, Entity entity) {
		super.throwEntity(context, entity);
		if (!(entity instanceof FallingBlockEntity fbe))
			return;
		if (!(fbe.getBlockState()
			.getBlock() instanceof AnvilBlock))
			return;
		if (entity.getVelocity()
			.length() < 0.25f)
			return;
		entity.getWorld().getNonSpectatingEntities(PlayerEntity.class, new Box(entity.getBlockPos()).expand(32))
			.forEach(AllAdvancements.ANVIL_PLOUGH::awardTo);
	}

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(PloughBlock.FACING)
			.getVector())
			.multiply(.45);
	}

	@Override
	protected boolean throwsEntities(World level) {
		return true;
	}

	@Override
	public boolean canBreak(World world, BlockPos breakingPos, BlockState state) {
		if (state.isAir())
			return false;
		if (world.getBlockState(breakingPos.down())
			.getBlock() instanceof FarmlandBlock)
			return false;
		if (state.getBlock() instanceof FluidBlock)
			return false;
		if (state.getBlock() instanceof BubbleColumnBlock)
			return false;
		if (state.getBlock() instanceof NetherPortalBlock)
			return false;
		if (state.getBlock() instanceof ITrackBlock)
			return true;
		if (state.getBlock() instanceof FakeTrackBlock)
			return false;
		return state.getCollisionShape(world, breakingPos)
			.isEmpty();
	}

	@Override
	protected void onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState) {
		super.onBlockBroken(context, pos, brokenState);

		if (brokenState.getBlock() == Blocks.SNOW && context.world instanceof ServerWorld) {
			ServerWorld world = (ServerWorld) context.world;
			brokenState.getDroppedStacks(new LootContextParameterSet.Builder(world).add(LootContextParameters.BLOCK_STATE, brokenState)
				.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
				.add(LootContextParameters.THIS_ENTITY, getPlayer(context))
				.add(LootContextParameters.TOOL, new ItemStack(Items.IRON_SHOVEL)))
				.forEach(s -> dropItem(context, s));
		}
	}

	@Override
	public void stopMoving(MovementContext context) {
		super.stopMoving(context);
		if (context.temporaryData instanceof PloughFakePlayer)
			((PloughFakePlayer) context.temporaryData).discard();
	}

	private PloughFakePlayer getPlayer(MovementContext context) {
		if (!(context.temporaryData instanceof PloughFakePlayer) && context.world != null) {
			PloughFakePlayer player = new PloughFakePlayer((ServerWorld) context.world);
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_HOE));
			context.temporaryData = player;
		}
		return (PloughFakePlayer) context.temporaryData;
	}

}
