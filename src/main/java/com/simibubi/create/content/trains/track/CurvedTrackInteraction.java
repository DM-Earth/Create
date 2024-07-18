package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class CurvedTrackInteraction {

	static final int breakerId = new Object().hashCode();

	static int breakTicks;
	static int breakTimeout;
	static float breakProgress;
	static BlockPos breakPos;

	public static void clientTick() {
		BezierPointSelection result = TrackBlockOutline.result;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		ClientWorld level = mc.world;

		if (!player.getAbilities().allowModifyWorld)
			return;

		if (mc.options.attackKey.isPressed() && result != null) {
			breakPos = result.blockEntity()
				.getPos();
			BlockState blockState = level.getBlockState(breakPos);
			if (blockState.isAir()) {
				resetBreakProgress();
				return;
			}

			if (breakTicks % 4.0F == 0.0F) {
				BlockSoundGroup soundtype = blockState.getSoundGroup();
				mc.getSoundManager()
					.play(new PositionedSoundInstance(soundtype.getHitSound(), SoundCategory.BLOCKS,
						(soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F,
						level.random, BlockPos.ofFloored(result.vec())));
			}

			boolean creative = player.getAbilities().creativeMode;

			breakTicks++;
			breakTimeout = 2;
			breakProgress += creative ? 0.125f : blockState.calcBlockBreakingDelta(player, level, breakPos) / 8f;

			Vec3d vec = VecHelper.offsetRandomly(result.vec(), level.random, 0.25f);
			level.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), vec.x, vec.y, vec.z, 0, 0, 0);

			int progress = (int) (breakProgress * 10.0F) - 1;
			level.setBlockBreakingInfo(player.getId(), breakPos, progress);
			player.swingHand(Hand.MAIN_HAND);

			if (breakProgress >= 1) {
				AllPackets.getChannel().sendToServer(new CurvedTrackDestroyPacket(breakPos, result.loc()
					.curveTarget(), BlockPos.ofFloored(result.vec()), false));
				resetBreakProgress();
			}

			return;
		}

		if (breakTimeout == 0)
			return;
		if (--breakTimeout > 0)
			return;

		resetBreakProgress();
	}

	private static void resetBreakProgress() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientWorld level = mc.world;

		if (breakPos != null && level != null)
			level.setBlockBreakingInfo(mc.player.getId(), breakPos, -1);

		breakProgress = 0;
		breakTicks = 0;
		breakPos = null;
	}

	public static boolean onClickInput(boolean isUse, boolean isAttack) {
		BezierPointSelection result = TrackBlockOutline.result;
		if (result == null)
			return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		ClientWorld level = mc.world;

		if (player == null || level == null)
			return false;

		if (isUse) {
			ItemStack heldItem = player.getMainHandStack();
			Item item = heldItem.getItem();
			if (AllTags.AllBlockTags.TRACKS.matches(heldItem)) {
				player.sendMessage(Lang.translateDirect("track.turn_start")
					.formatted(Formatting.RED), true);
				player.swingHand(Hand.MAIN_HAND);
				return true;
			}
			if (item instanceof TrackTargetingBlockItem ttbi && ttbi.useOnCurve(result, heldItem)) {
				player.swingHand(Hand.MAIN_HAND);
				return true;
			}
			if (AllItems.WRENCH.isIn(heldItem) && player.isSneaking()) {
				AllPackets.getChannel()
					.sendToServer(new CurvedTrackDestroyPacket(result.blockEntity()
						.getPos(),
						result.loc()
							.curveTarget(),
						BlockPos.ofFloored(result.vec()), true));
				resetBreakProgress();
				player.swingHand(Hand.MAIN_HAND);
				return true;
			}
		}

		if (isAttack)
			return true;

		return false;
	}

}
