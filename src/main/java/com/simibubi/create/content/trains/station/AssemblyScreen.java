package com.simibubi.create.content.trains.station;

import java.lang.ref.WeakReference;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.Lang;

public class AssemblyScreen extends AbstractStationScreen {

	private IconButton quitAssembly;
	private IconButton toggleAssemblyButton;
	private List<Identifier> iconTypes;
	private ScrollInput iconTypeScroll;

	public AssemblyScreen(StationBlockEntity be, GlobalStation station) {
		super(be, station);
		background = AllGuiTextures.STATION_ASSEMBLING;
	}

	@Override
	protected void init() {
		super.init();
		int x = guiLeft;
		int y = guiTop;
		int by = y + background.height - 24;

		Drawable widget = drawables.get(0);
		if (widget instanceof IconButton ib) {
			ib.setIcon(AllIcons.I_PRIORITY_VERY_LOW);
			ib.setToolTip(Lang.translateDirect("station.close"));
		}

		iconTypes = TrainIconType.REGISTRY.keySet()
			.stream()
			.toList();
		iconTypeScroll = new ScrollInput(x + 4, y + 17, 184, 14).titled(Lang.translateDirect("station.icon_type"));
		iconTypeScroll.withRange(0, iconTypes.size());
		iconTypeScroll.withStepFunction(ctx -> -iconTypeScroll.standardStep()
			.apply(ctx));
		iconTypeScroll.calling(s -> {
			Train train = displayedTrain.get();
			if (train != null)
				train.icon = TrainIconType.byId(iconTypes.get(s));
		});
		iconTypeScroll.active = iconTypeScroll.visible = false;
		addDrawableChild(iconTypeScroll);

		toggleAssemblyButton = new WideIconButton(x + 94, by, AllGuiTextures.I_ASSEMBLE_TRAIN);
		toggleAssemblyButton.active = false;
		toggleAssemblyButton.setToolTip(Lang.translateDirect("station.assemble_train"));
		toggleAssemblyButton.withCallback(() -> {
			AllPackets.getChannel()
				.sendToServer(StationEditPacket.tryAssemble(blockEntity.getPos()));
		});

		quitAssembly = new IconButton(x + 73, by, AllIcons.I_DISABLE);
		quitAssembly.active = true;
		quitAssembly.setToolTip(Lang.translateDirect("station.cancel"));
		quitAssembly.withCallback(() -> {
			AllPackets.getChannel()
				.sendToServer(StationEditPacket.configure(blockEntity.getPos(), false, station.name, null));
			client.setScreen(new StationScreen(blockEntity, station));
		});

		addDrawableChild(toggleAssemblyButton);
		addDrawableChild(quitAssembly);

		tickTrainDisplay();
	}

	@Override
	public void tick() {
		super.tick();
		tickTrainDisplay();
		Train train = displayedTrain.get();
		toggleAssemblyButton.active = blockEntity.bogeyCount > 0 || train != null;

		if (train != null) {
			AllPackets.getChannel()
				.sendToServer(StationEditPacket.configure(blockEntity.getPos(), false, station.name, null));
			client.setScreen(new StationScreen(blockEntity, station));
			for (Carriage carriage : train.carriages)
				carriage.updateConductors();
		}
	}

	private void tickTrainDisplay() {
		if (getImminent() == null) {
			displayedTrain = new WeakReference<>(null);
			quitAssembly.active = true;
			iconTypeScroll.active = iconTypeScroll.visible = false;
			toggleAssemblyButton.setToolTip(Lang.translateDirect("station.assemble_train"));
			toggleAssemblyButton.setIcon(AllGuiTextures.I_ASSEMBLE_TRAIN);
			toggleAssemblyButton.withCallback(() -> {
				AllPackets.getChannel()
					.sendToServer(StationEditPacket.tryAssemble(blockEntity.getPos()));
			});
		} else {
			AllPackets.getChannel()
				.sendToServer(StationEditPacket.configure(blockEntity.getPos(), false, station.name, null));
			client.setScreen(new StationScreen(blockEntity, station));
		}
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWindow(graphics, mouseX, mouseY, partialTicks);
		int x = guiLeft;
		int y = guiTop;

		MutableText header = Lang.translateDirect("station.assembly_title");
		graphics.drawText(textRenderer, header, x + background.width / 2 - textRenderer.getWidth(header) / 2, y + 4, 0x0E2233, false);

		AssemblyException lastAssemblyException = blockEntity.lastException;
		if (lastAssemblyException != null) {
			MutableText text = Lang.translateDirect("station.failed");
			graphics.drawText(textRenderer, text, x + 97 - textRenderer.getWidth(text) / 2, y + 47, 0x775B5B, false);
			int offset = 0;
			if (blockEntity.failedCarriageIndex != -1) {
				graphics.drawText(textRenderer, Lang.translateDirect("station.carriage_number", blockEntity.failedCarriageIndex), x + 30,
					y + 67, 0x7A7A7A, false);
				offset += 10;
			}
			graphics.drawTextWrapped(textRenderer, lastAssemblyException.component, x + 30, y + 67 + offset, 134, 0x775B5B);
			offset += textRenderer.wrapLines(lastAssemblyException.component, 134)
				.size() * 9 + 5;
			graphics.drawTextWrapped(textRenderer, Lang.translateDirect("station.retry"), x + 30, y + 67 + offset, 134, 0x7A7A7A);
			return;
		}

		int bogeyCount = blockEntity.bogeyCount;

		MutableText text = Lang.translateDirect(
			bogeyCount == 0 ? "station.no_bogeys" : bogeyCount == 1 ? "station.one_bogey" : "station.more_bogeys",
			bogeyCount);
		graphics.drawText(textRenderer, text, x + 97 - textRenderer.getWidth(text) / 2, y + 47, 0x7A7A7A, false);

		graphics.drawTextWrapped(textRenderer, Lang.translateDirect("station.how_to"), x + 28, y + 62, 134, 0x7A7A7A);
		graphics.drawTextWrapped(textRenderer, Lang.translateDirect("station.how_to_1"), x + 28, y + 94, 134, 0x7A7A7A);
		graphics.drawTextWrapped(textRenderer, Lang.translateDirect("station.how_to_2"), x + 28, y + 117, 138, 0x7A7A7A);
	}

	@Override
	public void removed() {
		super.removed();
		Train train = displayedTrain.get();
		if (train != null) {
			Identifier iconId = iconTypes.get(iconTypeScroll.getState());
			train.icon = TrainIconType.byId(iconId);
			AllPackets.getChannel()
				.sendToServer(new TrainEditPacket(train.id, "", iconId));
		}
	}

	@Override
	protected PartialModel getFlag(float partialTicks) {
		return AllPartialModels.STATION_ASSEMBLE;
	}

}
