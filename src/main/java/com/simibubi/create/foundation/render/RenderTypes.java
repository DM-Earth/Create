package com.simibubi.create.foundation.render;

import java.io.IOException;

import com.jozufozu.flywheel.backend.ShadersModHandler;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.RenderTypeAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.RegistrationContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

// TODO 1.17: use custom shaders instead of vanilla ones
public class RenderTypes extends RenderPhase {

	public static final RenderPhase.ShaderProgram GLOWING_SHADER = new RenderPhase.ShaderProgram(() -> Shaders.glowingShader);

	private static final RenderLayer OUTLINE_SOLID =
		RenderTypeAccessor.port_lib$create(createLayerName("outline_solid"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false,
			false, RenderLayer.MultiPhaseParameters.builder()
				.program(ENTITY_SOLID_PROGRAM)
				.texture(new RenderPhase.Texture(AllSpecialTextures.BLANK.getLocation(), false, false))
				.cull(ENABLE_CULLING)
				.lightmap(ENABLE_LIGHTMAP)
				.overlay(ENABLE_OVERLAY_COLOR)
				.build(false));

	public static RenderLayer getOutlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderLayer getOutlineTranslucent(Identifier texture, boolean cull) {
		return RenderTypeAccessor.port_lib$create(createLayerName("outline_translucent" + (cull ? "_cull" : "")),
			VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true, RenderLayer.MultiPhaseParameters.builder()
				.program(cull ? ENTITY_TRANSLUCENT_CULL_PROGRAM : ENTITY_TRANSLUCENT_PROGRAM)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(TRANSLUCENT_TRANSPARENCY)
				.cull(cull ? ENABLE_CULLING : DISABLE_CULLING)
				.lightmap(ENABLE_LIGHTMAP)
				.overlay(ENABLE_OVERLAY_COLOR)
				.writeMaskState(COLOR_MASK)
				.build(false));
	}

	public static RenderLayer getGlowingSolid(Identifier texture) {
		return RenderTypeAccessor.port_lib$create(createLayerName("glowing_solid"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256,
			true, false, RenderLayer.MultiPhaseParameters.builder()
				.program(GLOWING_SHADER)
				.texture(new RenderPhase.Texture(texture, false, false))
				.cull(ENABLE_CULLING)
				.lightmap(ENABLE_LIGHTMAP)
				.overlay(ENABLE_OVERLAY_COLOR)
				.build(true));
	}

	private static final RenderLayer GLOWING_SOLID_DEFAULT = getGlowingSolid(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

	public static RenderLayer getGlowingSolid() {
		return GLOWING_SOLID_DEFAULT;
	}

	public static RenderLayer getGlowingTranslucent(Identifier texture) {
		return RenderTypeAccessor.port_lib$create(createLayerName("glowing_translucent"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
			256, true, true, RenderLayer.MultiPhaseParameters.builder()
				.program(GLOWING_SHADER)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(TRANSLUCENT_TRANSPARENCY)
				.lightmap(ENABLE_LIGHTMAP)
				.overlay(ENABLE_OVERLAY_COLOR)
				.build(true));
	}

	private static final RenderLayer ADDITIVE = RenderLayer.of(createLayerName("additive"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
		VertexFormat.DrawMode.QUADS, 256, true, true, RenderLayer.MultiPhaseParameters.builder()
			.program(SOLID_PROGRAM)
			.texture(new RenderPhase.Texture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, false, false))
			.transparency(ADDITIVE_TRANSPARENCY)
			.cull(DISABLE_CULLING)
			.lightmap(ENABLE_LIGHTMAP)
			.overlay(ENABLE_OVERLAY_COLOR)
			.build(true));

	public static RenderLayer getAdditive() {
		return ADDITIVE;
	}

	private static final RenderLayer GLOWING_TRANSLUCENT_DEFAULT = getGlowingTranslucent(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

	public static RenderLayer getGlowingTranslucent() {
		return GLOWING_TRANSLUCENT_DEFAULT;
	}

	private static final RenderLayer ITEM_PARTIAL_SOLID =
		RenderTypeAccessor.port_lib$create(createLayerName("item_partial_solid"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true,
			false, RenderLayer.MultiPhaseParameters.builder()
				.program(ENTITY_SOLID_PROGRAM)
				.texture(BLOCK_ATLAS_TEXTURE)
				.cull(ENABLE_CULLING)
				.lightmap(ENABLE_LIGHTMAP)
				.overlay(ENABLE_OVERLAY_COLOR)
				.build(true));

	public static RenderLayer getItemPartialSolid() {
		return ITEM_PARTIAL_SOLID;
	}

	private static final RenderLayer ITEM_PARTIAL_TRANSLUCENT = RenderTypeAccessor.port_lib$create(createLayerName("item_partial_translucent"),
		VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, RenderLayer.MultiPhaseParameters.builder()
			.program(ENTITY_TRANSLUCENT_CULL_PROGRAM)
			.texture(BLOCK_ATLAS_TEXTURE)
			.transparency(TRANSLUCENT_TRANSPARENCY)
			.lightmap(ENABLE_LIGHTMAP)
			.overlay(ENABLE_OVERLAY_COLOR)
			.build(true));

	public static RenderLayer getItemPartialTranslucent() {
		return ITEM_PARTIAL_TRANSLUCENT;
	}

	private static final RenderLayer FLUID = RenderTypeAccessor.port_lib$create(createLayerName("fluid"),
		VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true, RenderLayer.MultiPhaseParameters.builder()
			.program(ENTITY_TRANSLUCENT_CULL_PROGRAM)
			.texture(MIPMAP_BLOCK_ATLAS_TEXTURE)
			.transparency(TRANSLUCENT_TRANSPARENCY)
			.lightmap(ENABLE_LIGHTMAP)
			.overlay(ENABLE_OVERLAY_COLOR)
			.build(true));

	public static RenderLayer getFluid() {
		return FLUID;
	}

	private static String createLayerName(String name) {
		return Create.ID + ":" + name;
	}

	// Mmm gimme those protected fields
	private RenderTypes() {
		super(null, null, null);
	}

	public static void init() {
		CoreShaderRegistrationCallback.EVENT.register(Shaders::onRegisterShaders);
	}

	private static class Shaders {
		private static net.minecraft.client.gl.ShaderProgram glowingShader;

		public static void onRegisterShaders(RegistrationContext ctx) throws IOException {
			ctx.register(Create.asResource("glowing_shader"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, shader -> glowingShader = shader);
		}
	}

}
