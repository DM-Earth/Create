package com.simibubi.create.content.contraptions.glue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.google.common.base.Objects;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

public class SuperGlueSelectionHandler {

	private static final int PASSIVE = 0x4D9162;
	private static final int HIGHLIGHT = 0x68c586;
	private static final int FAIL = 0xc5b548;

	private Object clusterOutlineSlot = new Object();
	private Object bbOutlineSlot = new Object();
	private int clusterCooldown;

	private BlockPos firstPos;
	private BlockPos hoveredPos;
	private Set<BlockPos> currentCluster;
	private int glueRequired;

	private SuperGlueEntity selected;
	private BlockPos soundSourceForRemoval;

	public void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		BlockPos hovered = null;
		ItemStack stack = player.getMainHandStack();

		if (!isGlue(stack)) {
			if (firstPos != null)
				discard();
			return;
		}

		if (clusterCooldown > 0) {
			if (clusterCooldown == 25)
				player.sendMessage(Components.immutableEmpty(), true);
			CreateClient.OUTLINER.keep(clusterOutlineSlot);
			clusterCooldown--;
		}

		Box scanArea = player.getBoundingBox()
			.expand(32, 16, 32);

		List<SuperGlueEntity> glueNearby = mc.world.getNonSpectatingEntities(SuperGlueEntity.class, scanArea);

		selected = null;
		if (firstPos == null) {
			double range = ReachUtil.reach(player) + 1;
			Vec3d traceOrigin = RaycastHelper.getTraceOrigin(player);
			Vec3d traceTarget = RaycastHelper.getTraceTarget(player, range, traceOrigin);

			double bestDistance = Double.MAX_VALUE;
			for (SuperGlueEntity glueEntity : glueNearby) {
				Optional<Vec3d> clip = glueEntity.getBoundingBox()
					.raycast(traceOrigin, traceTarget);
				if (clip.isEmpty())
					continue;
				Vec3d vec3 = clip.get();
				double distanceToSqr = vec3.squaredDistanceTo(traceOrigin);
				if (distanceToSqr > bestDistance)
					continue;
				selected = glueEntity;
				soundSourceForRemoval = BlockPos.ofFloored(vec3);
				bestDistance = distanceToSqr;
			}

			for (SuperGlueEntity glueEntity : glueNearby) {
				boolean h = clusterCooldown == 0 && glueEntity == selected;
				AllSpecialTextures faceTex = h ? AllSpecialTextures.GLUE : null;
				CreateClient.OUTLINER.showAABB(glueEntity, glueEntity.getBoundingBox())
					.colored(h ? HIGHLIGHT : PASSIVE)
					.withFaceTextures(faceTex, faceTex)
					.disableLineNormals()
					.lineWidth(h ? 1 / 16f : 1 / 64f);
			}
		}

		HitResult hitResult = mc.crosshairTarget;
		if (hitResult != null && hitResult.getType() == Type.BLOCK)
			hovered = ((BlockHitResult) hitResult).getBlockPos();

		if (hovered == null) {
			hoveredPos = null;
			return;
		}

		if (firstPos != null && !firstPos.isWithinDistance(hovered, 24)) {
			Lang.translate("super_glue.too_far")
				.color(FAIL)
				.sendStatus(player);
			return;
		}

		boolean cancel = player.isSneaking();
		if (cancel && firstPos == null)
			return;

		Box currentSelectionBox = getCurrentSelectionBox();

		boolean unchanged = Objects.equal(hovered, hoveredPos);

		if (unchanged) {
			if (currentCluster != null) {
				boolean canReach = currentCluster.contains(hovered);
				boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);
				int color = HIGHLIGHT;
				String key = "super_glue.click_to_confirm";

				if (!canReach) {
					color = FAIL;
					key = "super_glue.cannot_reach";
				} else if (!canAfford) {
					color = FAIL;
					key = "super_glue.not_enough";
				} else if (cancel) {
					color = FAIL;
					key = "super_glue.click_to_discard";
				}

				Lang.translate(key)
					.color(color)
					.sendStatus(player);

				if (currentSelectionBox != null)
					CreateClient.OUTLINER.showAABB(bbOutlineSlot, currentSelectionBox)
						.colored(canReach && canAfford && !cancel ? HIGHLIGHT : FAIL)
						.withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
						.disableLineNormals()
						.lineWidth(1 / 16f);

				CreateClient.OUTLINER.showCluster(clusterOutlineSlot, currentCluster)
					.colored(0x4D9162)
					.disableLineNormals()
					.lineWidth(1 / 64f);
			}

			return;
		}

		hoveredPos = hovered;

		Set<BlockPos> cluster = SuperGlueSelectionHelper.searchGlueGroup(mc.world, firstPos, hoveredPos, true);
		currentCluster = cluster;
		glueRequired = 1;
	}

	private boolean isGlue(ItemStack stack) {
		return stack.getItem() instanceof SuperGlueItem;
	}

	private Box getCurrentSelectionBox() {
		return firstPos == null || hoveredPos == null ? null : new Box(firstPos, hoveredPos).stretch(1, 1, 1);
	}

	public boolean onMouseInput(boolean attack) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		ClientWorld level = mc.world;

		if (!isGlue(player.getMainHandStack()))
			return false;
		if (!player.canModifyBlocks())
			return false;

		if (attack) {
			if (selected == null)
				return false;
			AllPackets.getChannel().sendToServer(new SuperGlueRemovalPacket(selected.getId(), soundSourceForRemoval));
			selected = null;
			clusterCooldown = 0;
			return true;
		}

		if (player.isSneaking()) {
			if (firstPos != null) {
				discard();
				return true;
			}
			return false;
		}

		if (hoveredPos == null)
			return false;

		Direction face = null;
		if (mc.crosshairTarget instanceof BlockHitResult bhr) {
			face = bhr.getSide();
			BlockState blockState = level.getBlockState(hoveredPos);
			if (blockState.getBlock()instanceof AbstractChassisBlock cb)
				if (cb.getGlueableSide(blockState, bhr.getSide()) != null)
					return false;
		}

		player.swingHand(Hand.MAIN_HAND);
		if (firstPos != null && currentCluster != null) {
			boolean canReach = currentCluster.contains(hoveredPos);
			boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);

			if (!canReach || !canAfford)
				return true;

			confirm();
			return true;
		}

		firstPos = hoveredPos;
		if (face != null)
			SuperGlueItem.spawnParticles(level, firstPos, face, true);
		Lang.translate("super_glue.first_pos")
			.sendStatus(player);
		AllSoundEvents.SLIME_ADDED.playAt(level, firstPos, 0.5F, 0.85F, false);
		level.playSound(player, firstPos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.75f, 1);
		return true;
	}

	public void discard() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		currentCluster = null;
		firstPos = null;
		Lang.translate("super_glue.abort")
			.sendStatus(player);
		clusterCooldown = 0;
	}

	public void confirm() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		AllPackets.getChannel().sendToServer(new SuperGlueSelectionPacket(firstPos, hoveredPos));
		AllSoundEvents.SLIME_ADDED.playAt(player.getWorld(), hoveredPos, 0.5F, 0.95F, false);
		player.getWorld().playSound(player, hoveredPos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.75f, 1);

		if (currentCluster != null)
			CreateClient.OUTLINER.showCluster(clusterOutlineSlot, currentCluster)
				.colored(0xB5F2C6)
				.withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.HIGHLIGHT_CHECKERED)
				.disableLineNormals()
				.lineWidth(1 / 24f);

		discard();
		Lang.translate("super_glue.success")
			.sendStatus(player);
		clusterCooldown = 40;
	}

}
