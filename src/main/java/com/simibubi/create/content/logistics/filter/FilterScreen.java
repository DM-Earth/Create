package com.simibubi.create.content.logistics.filter;

import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class FilterScreen extends AbstractFilterScreen<FilterMenu> {

	private static final String PREFIX = "gui.filter.";

	private Text allowN = Lang.translateDirect(PREFIX + "allow_list");
	private Text allowDESC = Lang.translateDirect(PREFIX + "allow_list.description");
	private Text denyN = Lang.translateDirect(PREFIX + "deny_list");
	private Text denyDESC = Lang.translateDirect(PREFIX + "deny_list.description");

	private Text respectDataN = Lang.translateDirect(PREFIX + "respect_data");
	private Text respectDataDESC = Lang.translateDirect(PREFIX + "respect_data.description");
	private Text ignoreDataN = Lang.translateDirect(PREFIX + "ignore_data");
	private Text ignoreDataDESC = Lang.translateDirect(PREFIX + "ignore_data.description");

	private IconButton whitelist, blacklist;
	private IconButton respectNBT, ignoreNBT;
	private Indicator whitelistIndicator, blacklistIndicator;
	private Indicator respectNBTIndicator, ignoreNBTIndicator;

	public FilterScreen(FilterMenu menu, PlayerInventory inv, Text title) {
		super(menu, inv, title, AllGuiTextures.FILTER);
	}

	@Override
	protected void init() {
		setWindowOffset(-11, 5);
		super.init();

		int screenX = x;
		int screenY = y;

		blacklist = new IconButton(screenX + 18, screenY + 75, AllIcons.I_BLACKLIST);
		blacklist.withCallback(() -> {
			handler.blacklist = true;
			sendOptionUpdate(Option.BLACKLIST);
		});
		blacklist.setToolTip(denyN);
		whitelist = new IconButton(screenX + 36, screenY + 75, AllIcons.I_WHITELIST);
		whitelist.withCallback(() -> {
			handler.blacklist = false;
			sendOptionUpdate(Option.WHITELIST);
		});
		whitelist.setToolTip(allowN);
		blacklistIndicator = new Indicator(screenX + 18, screenY + 69, Components.immutableEmpty());
		whitelistIndicator = new Indicator(screenX + 36, screenY + 69, Components.immutableEmpty());
		addRenderableWidgets(blacklist, whitelist, blacklistIndicator, whitelistIndicator);

		respectNBT = new IconButton(screenX + 60, screenY + 75, AllIcons.I_RESPECT_NBT);
		respectNBT.withCallback(() -> {
			handler.respectNBT = true;
			sendOptionUpdate(Option.RESPECT_DATA);
		});
		respectNBT.setToolTip(respectDataN);
		ignoreNBT = new IconButton(screenX + 78, screenY + 75, AllIcons.I_IGNORE_NBT);
		ignoreNBT.withCallback(() -> {
			handler.respectNBT = false;
			sendOptionUpdate(Option.IGNORE_DATA);
		});
		ignoreNBT.setToolTip(ignoreDataN);
		respectNBTIndicator = new Indicator(screenX + 60, screenY + 69, Components.immutableEmpty());
		ignoreNBTIndicator = new Indicator(screenX + 78, screenY + 69, Components.immutableEmpty());
		addRenderableWidgets(respectNBT, ignoreNBT, respectNBTIndicator, ignoreNBTIndicator);

		handleIndicators();
	}

	@Override
	protected List<IconButton> getTooltipButtons() {
		return Arrays.asList(blacklist, whitelist, respectNBT, ignoreNBT);
	}

	@Override
	protected List<MutableText> getTooltipDescriptions() {
		return Arrays.asList(denyDESC.copyContentOnly(), allowDESC.copyContentOnly(), respectDataDESC.copyContentOnly(), ignoreDataDESC.copyContentOnly());
	}

	@Override
	protected List<Indicator> getIndicators() {
		return Arrays.asList(blacklistIndicator, whitelistIndicator, respectNBTIndicator, ignoreNBTIndicator);
	}

	@Override
	protected boolean isButtonEnabled(IconButton button) {
		if (button == blacklist)
			return !handler.blacklist;
		if (button == whitelist)
			return handler.blacklist;
		if (button == respectNBT)
			return !handler.respectNBT;
		if (button == ignoreNBT)
			return handler.respectNBT;
		return true;
	}

	@Override
	protected boolean isIndicatorOn(Indicator indicator) {
		if (indicator == blacklistIndicator)
			return handler.blacklist;
		if (indicator == whitelistIndicator)
			return !handler.blacklist;
		if (indicator == respectNBTIndicator)
			return handler.respectNBT;
		if (indicator == ignoreNBTIndicator)
			return !handler.respectNBT;
		return false;
	}

}
