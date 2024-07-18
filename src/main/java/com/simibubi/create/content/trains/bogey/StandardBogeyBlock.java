package com.simibubi.create.content.trains.bogey;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public class StandardBogeyBlock extends AbstractBogeyBlock<StandardBogeyBlockEntity>
	implements IBE<StandardBogeyBlockEntity>, ProperWaterloggedBlock, ISpecialBlockItemRequirement {

	public StandardBogeyBlock(Settings props, BogeySizes.BogeySize size) {
		super(props, size);
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	public TrackMaterial.TrackType getTrackType(BogeyStyle style) {
		return TrackMaterial.TrackType.STANDARD;
	}

	@Override
	public double getWheelPointSpacing() {
		return 2;
	}

	@Override
	public double getWheelRadius() {
		return (size == BogeySizes.LARGE ? 12.5 : 6.5) / 16d;
	}

	@Override
	public Vec3d getConnectorAnchorOffset() {
		return new Vec3d(0, 7 / 32f, 1);
	}

	@Override
	public BogeyStyle getDefaultStyle() {
		return AllBogeyStyles.STANDARD;
	}

	@Override
	public ItemStack getPickStack(BlockView level, BlockPos pos, BlockState state) {
		return AllBlocks.RAILWAY_CASING.asStack();
	}

	@Override
	public Class<StandardBogeyBlockEntity> getBlockEntityClass() {
		return StandardBogeyBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StandardBogeyBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.BOGEY.get();
	}

}
