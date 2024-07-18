package com.simibubi.create.content.trains.track;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.AttachedRegistry;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.entity.ITeleporter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public class AllPortalTracks {

	// Portals must be entered from the side and must lead to a different dimension
	// than the one entered from

	@FunctionalInterface
	public interface PortalTrackProvider extends UnaryOperator<Pair<ServerWorld, BlockFace>> {
	};

	private static final AttachedRegistry<Block, PortalTrackProvider> PORTAL_BEHAVIOURS =
		new AttachedRegistry<>(Registries.BLOCK);

	public static void registerIntegration(Identifier block, PortalTrackProvider provider) {
		PORTAL_BEHAVIOURS.register(block, provider);
	}

	public static void registerIntegration(Block block, PortalTrackProvider provider) {
		PORTAL_BEHAVIOURS.register(block, provider);
	}

	public static boolean isSupportedPortal(BlockState state) {
		return PORTAL_BEHAVIOURS.get(state.getBlock()) != null;
	}

	public static Pair<ServerWorld, BlockFace> getOtherSide(ServerWorld level, BlockFace inboundTrack) {
		BlockPos portalPos = inboundTrack.getConnectedPos();
		BlockState portalState = level.getBlockState(portalPos);
		PortalTrackProvider provider = PORTAL_BEHAVIOURS.get(portalState.getBlock());
		return provider == null ? null : provider.apply(Pair.of(level, inboundTrack));
	}

	// Builtin handlers

	public static void registerDefaults() {
		registerIntegration(Blocks.NETHER_PORTAL, AllPortalTracks::nether);
		if (Mods.AETHER.isLoaded())
			registerIntegration(new Identifier("aether", "aether_portal"), AllPortalTracks::aether);
	}

	private static Pair<ServerWorld, BlockFace> nether(Pair<ServerWorld, BlockFace> inbound) {
		return standardPortalProvider(inbound, World.OVERWORLD, World.NETHER, AllPortalTracks::getTeleporter);
	}

	private static Pair<ServerWorld, BlockFace> aether(Pair<ServerWorld, BlockFace> inbound) {
		RegistryKey<World> aetherLevelKey =
			RegistryKey.of(RegistryKeys.WORLD, new Identifier("aether", "the_aether"));
		return standardPortalProvider(inbound, World.OVERWORLD, aetherLevelKey, level -> {
			try {
				return (ITeleporter) Class.forName("com.aetherteam.aether.block.portal.AetherPortalForcer")
					.getDeclaredConstructor(ServerWorld.class, boolean.class)
					.newInstance(level, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return getTeleporter(level);
		});
	}

	private static ITeleporter getTeleporter(ServerWorld level) {
		return (ITeleporter) level.getPortalForcer();
	}

	public static Pair<ServerWorld, BlockFace> standardPortalProvider(Pair<ServerWorld, BlockFace> inbound,
		RegistryKey<World> firstDimension, RegistryKey<World> secondDimension,
		Function<ServerWorld, ITeleporter> customPortalForcer) {
		ServerWorld level = inbound.getFirst();
		RegistryKey<World> resourcekey = level.getRegistryKey() == secondDimension ? firstDimension : secondDimension;
		MinecraftServer minecraftserver = level.getServer();
		ServerWorld otherLevel = minecraftserver.getWorld(resourcekey);

		if (otherLevel == null || !minecraftserver.isNetherAllowed())
			return null;

		BlockFace inboundTrack = inbound.getSecond();
		BlockPos portalPos = inboundTrack.getConnectedPos();
		BlockState portalState = level.getBlockState(portalPos);
		ITeleporter teleporter = customPortalForcer.apply(otherLevel);

		SuperGlueEntity probe = new SuperGlueEntity(level, new Box(portalPos));
		probe.setYaw(inboundTrack.getFace()
			.asRotation());
		probe.setPortalEntrancePos();

		TeleportTarget portalinfo = teleporter.getPortalInfo(probe, otherLevel, probe::getTeleportTarget);
		if (portalinfo == null)
			return null;

		BlockPos otherPortalPos = BlockPos.ofFloored(portalinfo.position);
		BlockState otherPortalState = otherLevel.getBlockState(otherPortalPos);
		if (otherPortalState.getBlock() != portalState.getBlock())
			return null;

		Direction targetDirection = inboundTrack.getFace();
		if (targetDirection.getAxis() == otherPortalState.get(Properties.HORIZONTAL_AXIS))
			targetDirection = targetDirection.rotateYClockwise();
		BlockPos otherPos = otherPortalPos.offset(targetDirection);
		return Pair.of(otherLevel, new BlockFace(otherPos, targetDirection.getOpposite()));
	}

}
