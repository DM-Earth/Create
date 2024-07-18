package com.simibubi.create.content.decoration.placard;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

public class PlacardBlockEntity extends SmartBlockEntity {

	ItemStack heldItem;
	int poweredTicks;

	public PlacardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		heldItem = ItemStack.EMPTY;
		poweredTicks = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isClient)
			return;
		if (poweredTicks == 0)
			return;

		poweredTicks--;
		if (poweredTicks > 0)
			return;

		BlockState blockState = getCachedState();
		world.setBlockState(pos, blockState.with(PlacardBlock.POWERED, false), 3);
		PlacardBlock.updateNeighbours(blockState, world, pos);
	}

	public ItemStack getHeldItem() {
		return heldItem;
	}

	public void setHeldItem(ItemStack heldItem) {
		this.heldItem = heldItem;
		notifyUpdate();
	}

	@Override
	protected void write(NbtCompound tag, boolean clientPacket) {
		tag.putInt("PoweredTicks", poweredTicks);
		tag.put("Item", NBTSerializer.serializeNBTCompound(heldItem));
		super.write(tag, clientPacket);
	}

	@Override
	protected void read(NbtCompound tag, boolean clientPacket) {
		int prevTicks = poweredTicks;
		poweredTicks = tag.getInt("PoweredTicks");
		heldItem = ItemStack.fromNbt(tag.getCompound("Item"));
		super.read(tag, clientPacket);

		if (clientPacket && prevTicks < poweredTicks)
			spawnParticles();
	}

	private void spawnParticles() {
		BlockState blockState = getCachedState();
		if (!AllBlocks.PLACARD.has(blockState))
			return;

		DustParticleEffect pParticleData = new DustParticleEffect(new Vector3f(1, .2f, 0), 1);
		Vec3d centerOf = VecHelper.getCenterOf(pos);
		Vec3d normal = Vec3d.of(PlacardBlock.connectedDirection(blockState)
			.getVector());
		Vec3d offset = VecHelper.axisAlingedPlaneOf(normal);

		for (int i = 0; i < 10; i++) {
			Vec3d v = VecHelper.offsetRandomly(Vec3d.ZERO, world.random, .5f)
				.multiply(offset)
				.normalize()
				.multiply(.45f)
				.add(normal.multiply(-.45f))
				.add(centerOf);
			world.addParticle(pParticleData, v.x, v.y, v.z, 0, 0, 0);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

}
