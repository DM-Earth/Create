package com.simibubi.create.content.kinetics.deployer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.Mode;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DeployerMovementBehaviour implements MovementBehaviour {

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(DeployerBlock.FACING)
			.getVector())
			.multiply(2);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		if (context.world.isClient)
			return;

		tryGrabbingItem(context);
		DeployerFakePlayer player = getPlayer(context);
		Mode mode = getMode(context);
		if (mode == Mode.USE && !DeployerHandler.shouldActivate(player.getMainHandStack(), context.world, pos, null))
			return;

		activate(context, pos, player, mode);
		tryDisposeOfExcess(context);
		context.stall = player.blockBreakingProgress != null;
	}

	public void activate(MovementContext context, BlockPos pos, DeployerFakePlayer player, Mode mode) {
		World world = context.world;

		FilterItemStack filter = context.getFilterFromBE();
		if (AllItems.SCHEMATIC.isIn(filter.item()))
			activateAsSchematicPrinter(context, pos, player, world, filter.item());

		Vec3d facingVec = Vec3d.of(context.state.get(DeployerBlock.FACING)
			.getVector());
		facingVec = context.rotation.apply(facingVec);
		Vec3d vec = context.position.subtract(facingVec.multiply(2));

		float xRot = AbstractContraptionEntity.pitchFromVector(facingVec) - 90;
		if (Math.abs(xRot) > 89) {
			Vec3d initial = new Vec3d(0, 0, 1);
			if (context.contraption.entity instanceof OrientedContraptionEntity oce)
				initial = VecHelper.rotate(initial, oce.getInitialYaw(), Axis.Y);
			if (context.contraption.entity instanceof CarriageContraptionEntity cce)
				initial = VecHelper.rotate(initial, 90, Axis.Y);
			facingVec = context.rotation.apply(initial);
		}

		player.setYaw(AbstractContraptionEntity.yawFromVector(facingVec));
		player.setPitch(xRot);
		player.placedTracks = false;

		DeployerHandler.activate(player, vec, pos, facingVec, mode);

		if ((context.contraption instanceof MountedContraption || context.contraption instanceof CarriageContraption)
			&& player.placedTracks && context.blockEntityData != null && context.blockEntityData.contains("Owner"))
			AllAdvancements.SELF_DEPLOYING.awardTo(world.getPlayerByUuid(context.blockEntityData.getUuid("Owner")));
	}

	protected void activateAsSchematicPrinter(MovementContext context, BlockPos pos, DeployerFakePlayer player,
		World world, ItemStack filter) {
		if (!filter.hasNbt())
			return;
		if (!world.getBlockState(pos)
			.isReplaceable())
			return;

		NbtCompound tag = filter.getNbt();
		if (!tag.getBoolean("Deployed"))
			return;
		SchematicWorld schematicWorld = SchematicInstances.get(world, filter);
		if (schematicWorld == null)
			return;
		if (!schematicWorld.getBounds()
			.contains(pos.subtract(schematicWorld.anchor)))
			return;
		BlockState blockState = schematicWorld.getBlockState(pos);
		ItemRequirement requirement = ItemRequirement.of(blockState, schematicWorld.getBlockEntity(pos));
		if (requirement.isInvalid() || requirement.isEmpty())
			return;
		if (AllBlocks.BELT.has(blockState))
			return;

		List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
		ItemStack contextStack = requiredItems.isEmpty() ? ItemStack.EMPTY : requiredItems.get(0).stack;

		if (!context.contraption.hasUniversalCreativeCrate) {
			Storage<ItemVariant> itemHandler = context.contraption.getSharedInventory();
			try (Transaction t = TransferUtil.getTransaction()) {
				for (ItemRequirement.StackRequirement required : requiredItems) {
					int count = required.stack.getCount();
					ResourceAmount<ItemVariant> resource = TransferUtil.extractMatching(itemHandler, required::matches, count, t);
					if (resource == null || resource.amount() != count)
						return; // didn't extract what we needed, skip
				}
				// if we get here all requirements were met
				t.commit();
			}
		}

		NbtCompound data = BlockHelper.prepareBlockEntityData(blockState, schematicWorld.getBlockEntity(pos));
//		BlockSnapshot blocksnapshot = BlockSnapshot.create(world.dimension(), world, pos);
		BlockHelper.placeSchematicBlock(world, blockState, pos, contextStack, data);
//		if (ForgeEventFactory.onBlockPlace(player, blocksnapshot, Direction.UP))
//			blocksnapshot.restore(true, false);
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClient)
			return;
		if (!context.stall)
			return;

		DeployerFakePlayer player = getPlayer(context);
		Mode mode = getMode(context);

		Pair<BlockPos, Float> blockBreakingProgress = player.blockBreakingProgress;
		if (blockBreakingProgress != null) {
			int timer = context.data.getInt("Timer");
			if (timer < 20) {
				timer++;
				context.data.putInt("Timer", timer);
				return;
			}

			context.data.remove("Timer");
			activate(context, blockBreakingProgress.getKey(), player, mode);
			tryDisposeOfExcess(context);
		}

		context.stall = player.blockBreakingProgress != null;
	}

	@Override
	public void cancelStall(MovementContext context) {
		if (context.world.isClient)
			return;

		MovementBehaviour.super.cancelStall(context);
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		if (player.blockBreakingProgress == null)
			return;
		context.world.setBlockBreakingInfo(player.getId(), player.blockBreakingProgress.getKey(), -1);
		player.blockBreakingProgress = null;
	}

	@Override
	public void stopMoving(MovementContext context) {
		if (context.world.isClient)
			return;

		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;

		cancelStall(context);
		context.blockEntityData.put("Inventory", player.getInventory()
			.writeNbt(new NbtList()));
		player.discard();
	}

	private void tryGrabbingItem(MovementContext context) {
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		if (player.getMainHandStack()
			.isEmpty()) {
			FilterItemStack filter = context.getFilterFromBE();
			if (AllItems.SCHEMATIC.isIn(filter.item()))
				return;
			ItemStack held = ItemHelper.extract(context.contraption.getSharedInventory(),
				stack -> filter.test(context.world, stack), 1, false);
			player.setStackInHand(Hand.MAIN_HAND, held);
		}
	}

	private void tryDisposeOfExcess(MovementContext context) {
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		PlayerInventory inv = player.getInventory();
		FilterItemStack filter = context.getFilterFromBE();

		for (List<ItemStack> list : Arrays.asList(inv.armor, inv.offHand, inv.main)) {
			for (int i = 0; i < list.size(); ++i) {
				ItemStack itemstack = list.get(i);
				if (itemstack.isEmpty())
					continue;

				if (list == inv.main && i == inv.selectedSlot && filter.test(context.world, itemstack))
					continue;

				dropItem(context, itemstack);
				list.set(i, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public void writeExtraData(MovementContext context) {
		DeployerFakePlayer player = getPlayer(context);
		if (player == null)
			return;
		context.data.put("HeldItem", NBTSerializer.serializeNBT(player.getMainHandStack()));
	}

	private DeployerFakePlayer getPlayer(MovementContext context) {
		if (!(context.temporaryData instanceof DeployerFakePlayer) && context.world instanceof ServerWorld) {
			UUID owner = context.blockEntityData.contains("Owner") ? context.blockEntityData.getUuid("Owner") : null;
			DeployerFakePlayer deployerFakePlayer = new DeployerFakePlayer((ServerWorld) context.world, owner);
			deployerFakePlayer.onMinecartContraption = context.contraption instanceof MountedContraption;
			deployerFakePlayer.getInventory()
				.readNbt(context.blockEntityData.getList("Inventory", NbtElement.COMPOUND_TYPE));
			if (context.data.contains("HeldItem"))
				deployerFakePlayer.setStackInHand(Hand.MAIN_HAND,
					ItemStack.fromNbt(context.data.getCompound("HeldItem")));
			context.blockEntityData.remove("Inventory");
			context.temporaryData = deployerFakePlayer;
		}
		return (DeployerFakePlayer) context.temporaryData;
	}

	private Mode getMode(MovementContext context) {
		return NBTHelper.readEnum(context.blockEntityData, "Mode", Mode.class);
	}

	@Override
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffers) {
		if (!ContraptionRenderDispatcher.canInstance())
			DeployerRenderer.renderInContraption(context, renderWorld, matrices, buffers);
	}

	@Override
	public boolean hasSpecialInstancedRendering() {
		return true;
	}

	@Nullable
	@Override
	public ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld,
		MovementContext context) {
		return new DeployerActorInstance(materialManager, simulationWorld, context);
	}
}
