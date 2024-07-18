package com.simibubi.create.content.redstone.displayLink.target;

import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.utility.Lang;

public class LecternDisplayTarget extends DisplayTarget {

	@Override
	public void acceptText(int line, List<MutableText> text, DisplayLinkContext context) {
		BlockEntity be = context.getTargetBlockEntity();
		if (!(be instanceof LecternBlockEntity lectern))
			return;
		ItemStack book = lectern.getBook();
		if (book.isEmpty())
			return;

		if (book.isOf(Items.WRITABLE_BOOK))
			lectern.setBook(book = signBook(book));
		if (!book.isOf(Items.WRITTEN_BOOK))
			return;

		NbtList tag = book.getNbt()
			.getList("pages", NbtElement.STRING_TYPE);

		boolean changed = false;
		for (int i = 0; i - line < text.size() && i < 50; i++) {
			if (tag.size() <= i)
				tag.add(NbtString.of(i < line ? "" : Text.Serializer.toJson(text.get(i - line))));

			else if (i >= line) {
				if (i - line == 0)
					reserve(i, lectern, context);
				if (i - line > 0 && isReserved(i - line, lectern, context))
					break;

				tag.set(i, NbtString.of(Text.Serializer.toJson(text.get(i - line))));
			}
			changed = true;
		}

		book.getNbt()
			.put("pages", tag);
		lectern.setBook(book);

		if (changed)
			context.level().updateListeners(context.getTargetPos(), lectern.getCachedState(), lectern.getCachedState(), 2);
	}

	@Override
	public DisplayTargetStats provideStats(DisplayLinkContext context) {
		return new DisplayTargetStats(50, 256, this);
	}

	public Text getLineOptionText(int line) {
		return Lang.translateDirect("display_target.page", line + 1);
	}

	private ItemStack signBook(ItemStack book) {
		ItemStack written = new ItemStack(Items.WRITTEN_BOOK);
		NbtCompound compoundtag = book.getNbt();
		if (compoundtag != null)
			written.setNbt(compoundtag.copy());

		written.setSubNbt("author", NbtString.of("Data Gatherer"));
		written.setSubNbt("filtered_title", NbtString.of("Printed Book"));
		written.setSubNbt("title", NbtString.of("Printed Book"));

		return written;
	}

}
