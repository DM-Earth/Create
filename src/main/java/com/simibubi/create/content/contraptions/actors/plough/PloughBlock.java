package com.simibubi.create.content.contraptions.actors.plough;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.contraptions.actors.AttachedActorBlock;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.world.ServerWorld;

public class PloughBlock extends AttachedActorBlock {

	public PloughBlock(Settings p_i48377_1_) {
		super(p_i48377_1_);
	}

	/**
	 * The OnHoeUse event takes a player, so we better not pass null
	 */
	static class PloughFakePlayer extends FakePlayer {

		public static final GameProfile PLOUGH_PROFILE =
				new GameProfile(UUID.fromString("9e2faded-eeee-4ec2-c314-dad129ae971d"), "Plough");

		public PloughFakePlayer(ServerWorld world) {
			super(world, PLOUGH_PROFILE);
		}

	}

}