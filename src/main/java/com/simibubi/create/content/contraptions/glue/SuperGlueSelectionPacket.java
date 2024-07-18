package com.simibubi.create.content.contraptions.glue;

import java.util.Set;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

public class SuperGlueSelectionPacket extends SimplePacketBase {

	private BlockPos from;
	private BlockPos to;

	public SuperGlueSelectionPacket(BlockPos from, BlockPos to) {
		this.from = from;
		this.to = to;
	}

	public SuperGlueSelectionPacket(PacketByteBuf buffer) {
		from = buffer.readBlockPos();
		to = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(from);
		buffer.writeBlockPos(to);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (AdventureUtil.isAdventure(player))
				return;

			double range = ReachUtil.reach(player) + 2;
			if (player.squaredDistanceTo(Vec3d.ofCenter(to)) > range * range)
				return;
			if (!to.isWithinDistance(from, 25))
				return;

			Set<BlockPos> group = SuperGlueSelectionHelper.searchGlueGroup(player.getWorld(), from, to, false);
			if (group == null)
				return;
			if (!group.contains(to))
				return;
			if (!SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, true))
				return;

			Box bb = SuperGlueEntity.span(from, to);
			SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, false);
			SuperGlueEntity entity = new SuperGlueEntity(player.getWorld(), bb);
			player.getWorld().spawnEntity(entity);
			entity.spawnParticles();

			AllAdvancements.SUPER_GLUE.awardTo(player);
		});
		return true;
	}

}
