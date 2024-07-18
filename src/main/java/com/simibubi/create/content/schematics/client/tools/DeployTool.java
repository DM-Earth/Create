package com.simibubi.create.content.schematics.client.tools;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.foundation.outliner.AABBOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class DeployTool extends PlacementToolBase {

	@Override
	public void init() {
		super.init();
		selectionRange = -1;
	}

	@Override
	public void updateSelection() {
		if (schematicHandler.isActive() && selectionRange == -1) {
			selectionRange = (int) (schematicHandler.getBounds()
				.getCenter()
				.length() / 2);
			selectionRange = MathHelper.clamp(selectionRange, 1, 100);
		}
		selectIgnoreBlocks = AllKeys.ACTIVATE_TOOL.isPressed();
		super.updateSelection();
	}

	@Override
	public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera) {
		super.renderTool(ms, buffer, camera);

		if (selectedPos == null)
			return;

		ms.push();
		float pt = AnimationTickHolder.getPartialTicks();
		double x = MathHelper.lerp(pt, lastChasingSelectedPos.x, chasingSelectedPos.x);
		double y = MathHelper.lerp(pt, lastChasingSelectedPos.y, chasingSelectedPos.y);
		double z = MathHelper.lerp(pt, lastChasingSelectedPos.z, chasingSelectedPos.z);

		SchematicTransformation transformation = schematicHandler.getTransformation();
		Box bounds = schematicHandler.getBounds();
		Vec3d center = bounds.getCenter();
		Vec3d rotationOffset = transformation.getRotationOffset(true);
		int centerX = (int) center.x;
		int centerZ = (int) center.z;
		double xOrigin = bounds.getXLength() / 2f;
		double zOrigin = bounds.getZLength() / 2f;
		Vec3d origin = new Vec3d(xOrigin, 0, zOrigin);

		ms.translate(x - centerX - camera.x, y - camera.y, z - centerZ - camera.z);
		TransformStack.cast(ms)
			.translate(origin)
			.translate(rotationOffset)
			.rotateY(transformation.getCurrentRotation())
			.translateBack(rotationOffset)
			.translateBack(origin);

		AABBOutline outline = schematicHandler.getOutline();
		outline.render(ms, buffer, Vec3d.ZERO, pt);
		outline.getParams()
			.clearTextures();
		ms.pop();
	}

	@Override
	public boolean handleMouseWheel(double delta) {
		if (!selectIgnoreBlocks)
			return super.handleMouseWheel(delta);
		selectionRange += delta;
		selectionRange = MathHelper.clamp(selectionRange, 1, 100);
		return true;
	}

	@Override
	public boolean handleRightClick() {
		if (selectedPos == null)
			return super.handleRightClick();
		Vec3d center = schematicHandler.getBounds()
			.getCenter();
		BlockPos target = selectedPos.add(-((int) center.x), 0, -((int) center.z));

		ItemStack item = schematicHandler.getActiveSchematicItem();
		if (item != null) {
			item.getNbt()
				.putBoolean("Deployed", true);
			item.getNbt()
				.put("Anchor", NbtHelper.fromBlockPos(target));
			schematicHandler.getTransformation()
				.startAt(target);
		}

		schematicHandler.getTransformation()
			.moveTo(target);
		schematicHandler.markDirty();
		schematicHandler.deploy();
		return true;
	}

}
