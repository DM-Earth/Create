package com.simibubi.create.content.logistics.depot;

import org.joml.Vector3f;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class EjectorTargetHandler {

	static BlockPos currentSelection;
	static ItemStack currentItem;
	static long lastHoveredBlockPos = -1;
	static EntityLauncher launcher;

	public static ActionResult rightClickingBlocksSelectsThem(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (currentItem == null)
			return ActionResult.PASS;
		BlockPos pos = hitResult.getBlockPos();//event.getPos();
//		Level world = event.getWorld();
		if (!world.isClient)
			return ActionResult.PASS;
//		Player player = event.getPlayer();
		if (player == null || player.isSpectator() || !player.isSneaking())
			return ActionResult.PASS;

		String key = "weighted_ejector.target_set";
		Formatting colour = Formatting.GOLD;
		player.sendMessage(Lang.translateDirect(key)
			.formatted(colour), true);
		currentSelection = pos;
		launcher = null;
//		event.setCanceled(true);
//		event.setCancellationResult(InteractionResult.SUCCESS);
		return ActionResult.SUCCESS;
	}

	public static ActionResult leftClickingBlocksDeselectsThem(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
		if (currentItem == null)
			return ActionResult.PASS;
		if (!world.isClient)
			return ActionResult.PASS;
		if (player.isSpectator())
			return ActionResult.PASS;
		if (!player
			.isSneaking())
			return ActionResult.PASS;
//		BlockPos pos = event.getPos();
		if (pos.equals(currentSelection)) {
			currentSelection = null;
			launcher = null;
//			event.setCanceled(true);
//			event.setCancellationResult(InteractionResult.SUCCESS);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	public static void flushSettings(BlockPos pos) {
		int h = 0;
		int v = 0;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		String key = "weighted_ejector.target_not_valid";
		Formatting colour = Formatting.WHITE;

		if (currentSelection == null)
			key = "weighted_ejector.no_target";

		Direction validTargetDirection = getValidTargetDirection(pos);
		if (validTargetDirection == null) {
			player.sendMessage(Lang.translateDirect(key)
				.formatted(colour), true);
			currentItem = null;
			currentSelection = null;
			return;
		}

		key = "weighted_ejector.targeting";
		colour = Formatting.GREEN;

		player.sendMessage(
			Lang.translateDirect(key, currentSelection.getX(), currentSelection.getY(), currentSelection.getZ())
				.formatted(colour),
			true);

		BlockPos diff = pos.subtract(currentSelection);
		h = Math.abs(diff.getX() + diff.getZ());
		v = -diff.getY();

		AllPackets.getChannel().sendToServer(new EjectorPlacementPacket(h, v, pos, validTargetDirection));
		currentSelection = null;
		currentItem = null;

	}

	public static Direction getValidTargetDirection(BlockPos pos) {
		if (currentSelection == null)
			return null;
		if (VecHelper.onSameAxis(pos, currentSelection, Axis.Y))
			return null;

		int xDiff = currentSelection.getX() - pos.getX();
		int zDiff = currentSelection.getZ() - pos.getZ();
		int max = AllConfigs.server().kinetics.maxEjectorDistance.get();

		if (Math.abs(xDiff) > max || Math.abs(zDiff) > max)
			return null;

		if (xDiff == 0)
			return Direction.get(zDiff < 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE, Axis.Z);
		if (zDiff == 0)
			return Direction.get(xDiff < 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE, Axis.X);

		return null;
	}

	public static void tick() {
		PlayerEntity player = MinecraftClient.getInstance().player;

		if (player == null)
			return;

		ItemStack heldItemMainhand = player.getMainHandStack();
		if (!AllBlocks.WEIGHTED_EJECTOR.isIn(heldItemMainhand)) {
			currentItem = null;
		} else {
			if (heldItemMainhand != currentItem) {
				currentSelection = null;
				currentItem = heldItemMainhand;
			}
			drawOutline(currentSelection);
		}

		checkForWrench(heldItemMainhand);
		drawArc();
	}

	protected static void drawArc() {
		MinecraftClient mc = MinecraftClient.getInstance();
		boolean wrench = AllItems.WRENCH.isIn(mc.player.getMainHandStack());

		if (currentSelection == null)
			return;
		if (currentItem == null && !wrench)
			return;

		HitResult objectMouseOver = mc.crosshairTarget;
		if (!(objectMouseOver instanceof BlockHitResult))
			return;
		BlockHitResult blockRayTraceResult = (BlockHitResult) objectMouseOver;
		if (blockRayTraceResult.getType() == Type.MISS)
			return;

		BlockPos pos = blockRayTraceResult.getBlockPos();
		if (!wrench)
			pos = pos.offset(blockRayTraceResult.getSide());

		int xDiff = currentSelection.getX() - pos.getX();
		int yDiff = currentSelection.getY() - pos.getY();
		int zDiff = currentSelection.getZ() - pos.getZ();
		int validX = Math.abs(zDiff) > Math.abs(xDiff) ? 0 : xDiff;
		int validZ = Math.abs(zDiff) < Math.abs(xDiff) ? 0 : zDiff;

		BlockPos validPos = currentSelection.add(validX, yDiff, validZ);
		Direction d = getValidTargetDirection(validPos);
		if (d == null)
			return;
		if (launcher == null || lastHoveredBlockPos != pos.asLong()) {
			lastHoveredBlockPos = pos.asLong();
			launcher = new EntityLauncher(Math.abs(validX + validZ), yDiff);
		}

		double totalFlyingTicks = launcher.getTotalFlyingTicks() + 3;
		int segments = (((int) totalFlyingTicks) / 3) + 1;
		double tickOffset = totalFlyingTicks / segments;
		boolean valid = xDiff == validX && zDiff == validZ;
		int intColor = valid ? 0x9ede73 : 0xff7171;
		Vector3f color = new Color(intColor).asVectorF();
		DustParticleEffect data = new DustParticleEffect(color, 1);
		ClientWorld world = mc.world;

		Box bb = new Box(0, 0, 0, 1, 0, 1).offset(currentSelection.add(-validX, -yDiff, -validZ));
		CreateClient.OUTLINER.chaseAABB("valid", bb)
			.colored(intColor)
			.lineWidth(1 / 16f);

		for (int i = 0; i < segments; i++) {
			double ticks = ((AnimationTickHolder.getRenderTime() / 3) % tickOffset) + i * tickOffset;
			Vec3d vec = launcher.getGlobalPos(ticks, d, pos)
				.add(xDiff - validX, 0, zDiff - validZ);
			world.addParticle(data, vec.x, vec.y, vec.z, 0, 0, 0);
		}
	}

	private static void checkForWrench(ItemStack heldItem) {
		if (!AllItems.WRENCH.isIn(heldItem))
			return;
		HitResult objectMouseOver = MinecraftClient.getInstance().crosshairTarget;
		if (!(objectMouseOver instanceof BlockHitResult))
			return;
		BlockHitResult result = (BlockHitResult) objectMouseOver;
		BlockPos pos = result.getBlockPos();

		BlockEntity be = MinecraftClient.getInstance().world.getBlockEntity(pos);
		if (!(be instanceof EjectorBlockEntity)) {
			lastHoveredBlockPos = -1;
			currentSelection = null;
			return;
		}

		if (lastHoveredBlockPos == -1 || lastHoveredBlockPos != pos.asLong()) {
			EjectorBlockEntity ejector = (EjectorBlockEntity) be;
			if (!ejector.getTargetPosition()
				.equals(ejector.getPos()))
				currentSelection = ejector.getTargetPosition();
			lastHoveredBlockPos = pos.asLong();
			launcher = null;
		}

		if (lastHoveredBlockPos != -1)
			drawOutline(currentSelection);
	}

	public static void drawOutline(BlockPos selection) {
		World world = MinecraftClient.getInstance().world;
		if (selection == null)
			return;

		BlockPos pos = selection;
		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getOutlineShape(world, pos);
		Box boundingBox = shape.isEmpty() ? new Box(BlockPos.ORIGIN) : shape.getBoundingBox();
		CreateClient.OUTLINER.showAABB("target", boundingBox.offset(pos))
			.colored(0xffcb74)
			.lineWidth(1 / 16f);
	}

}
