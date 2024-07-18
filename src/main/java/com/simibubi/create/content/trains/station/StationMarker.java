package com.simibubi.create.content.trains.station;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.joml.Matrix4f;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.map.CustomRenderedMapDecoration;
import com.simibubi.create.foundation.utility.Components;

public class StationMarker {
	// Not MANSION or MONUMENT to allow map extending
	public static final MapIcon.Type TYPE = MapIcon.Type.RED_MARKER;

	private final BlockPos source;
	private final BlockPos target;
	private final Text name;
	private final String id;

	public StationMarker(BlockPos source, BlockPos target, Text name) {
		this.source = source;
		this.target = target;
		this.name = name;
		id = "create:station-" + target.getX() + "," + target.getY() + "," + target.getZ();
	}

	public static StationMarker load(NbtCompound tag) {
		BlockPos source = NbtHelper.toBlockPos(tag.getCompound("source"));
		BlockPos target = NbtHelper.toBlockPos(tag.getCompound("target"));
		Text name = Text.Serializer.fromJson(tag.getString("name"));
		if (name == null) name = Components.immutableEmpty();

		return new StationMarker(source, target, name);
	}

	public static StationMarker fromWorld(BlockView level, BlockPos pos) {
		Optional<StationBlockEntity> stationOption = AllBlockEntityTypes.TRACK_STATION.get(level, pos);

		if (stationOption.isEmpty() || stationOption.get().getStation() == null)
			return null;

		String name = stationOption.get()
			.getStation().name;
		return new StationMarker(pos, BlockEntityBehaviour.get(stationOption.get(), TrackTargetingBehaviour.TYPE)
			.getPositionForMapMarker(), Components.literal(name));
	}

	public NbtCompound save() {
		NbtCompound tag = new NbtCompound();
		tag.put("source", NbtHelper.fromBlockPos(source));
		tag.put("target", NbtHelper.fromBlockPos(target));
		tag.putString("name", Text.Serializer.toJson(name));

		return tag;
	}

	public BlockPos getSource() {
		return source;
	}

	public BlockPos getTarget() {
		return target;
	}

	public Text getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StationMarker that = (StationMarker) o;

		if (!target.equals(that.target)) return false;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, name);
	}

	public static class Decoration extends MapIcon implements CustomRenderedMapDecoration {
		private static final Identifier TEXTURE = Create.asResource("textures/gui/station_map_icon.png");

		public Decoration(byte x, byte y, Text name) {
			super(TYPE, x, y, (byte) 0, name);
		}

		public static Decoration from(MapIcon decoration) {
			return new StationMarker.Decoration(decoration.getX(), decoration.getZ(), decoration.getText());
		}

		@Override
		public boolean isAlwaysRendered() {
			return true;
		}

		@Override
		public void render(MatrixStack poseStack, VertexConsumerProvider bufferSource, boolean active, int packedLight, MapState mapData, int index) {
			poseStack.push();

			poseStack.translate(getX() / 2D + 64.0, getZ() / 2D + 64.0, -0.02D);

			poseStack.push();

			poseStack.translate(0.5f, 0f, 0);
			poseStack.scale(4.5F, 4.5F, 3.0F);

			VertexConsumer buffer = bufferSource.getBuffer(RenderLayer.getText(TEXTURE));
			Matrix4f mat = poseStack.peek().getPositionMatrix();
			float zOffset = -0.001f;
			buffer.vertex(mat, -1, -1, zOffset * index).color(255, 255, 255, 255).texture(0.0f		, 0.0f		 ).light(packedLight).next();
			buffer.vertex(mat, -1,  1, zOffset * index).color(255, 255, 255, 255).texture(0.0f		, 0.0f + 1.0f).light(packedLight).next();
			buffer.vertex(mat,  1,  1, zOffset * index).color(255, 255, 255, 255).texture(0.0f + 1.0f, 0.0f + 1.0f).light(packedLight).next();
			buffer.vertex(mat,  1, -1, zOffset * index).color(255, 255, 255, 255).texture(0.0f + 1.0f, 0.0f		 ).light(packedLight).next();

			poseStack.pop();

			if (getText() != null) {
				TextRenderer font = MinecraftClient.getInstance().textRenderer;
				Text component = getText();
				float f6 = (float)font.getWidth(component);
//				float f7 = Mth.clamp(25.0F / f6, 0.0F, 6.0F / 9.0F);
				poseStack.push();
//				poseStack.translate((double)(0.0F + (float)getX() / 2.0F + 64.0F / 2.0F), (double)(0.0F + (float)getY() / 2.0F + 64.0F + 4.0F), (double)-0.025F);
				poseStack.translate(0, 6.0D, -0.005F);

				poseStack.scale(0.8f, 0.8f, 1.0F);
				poseStack.translate(-f6 / 2f + .5f, 0, 0);
//				poseStack.scale(f7, f7, 1.0F);
				font.draw(component, 0.0F, 0.0F, -1, false, poseStack.peek()
					.getPositionMatrix(), bufferSource, TextRenderer.TextLayerType.NORMAL, Integer.MIN_VALUE, packedLight);
				poseStack.pop();
			}

			poseStack.pop();
		}

		@Override
		public boolean render(int index) {
			return true;
		}
	}
}
