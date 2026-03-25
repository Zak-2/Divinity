package divinity.gui.alt;

import divinity.gui.alt.util.SavedAltData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GuiAltList extends GuiListExtended {

    final List<AltListEntry> entries = new CopyOnWriteArrayList<>();
    int selectedEntry = -1;

    public GuiAltList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
    }

    public void addAlt(SavedAltData alt) {
        entries.add(new AltListEntry(this, alt));
    }

    public void setAlts(Collection<SavedAltData> alts) {
        entries.clear();
        alts.forEach(this::addAlt);
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return entries.get(index);
    }

    @Override
    protected int getSize() {
        return entries.size();
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return slotIndex == selectedEntry;
    }

    protected int getScrollBarX() {
        return super.getScrollBarX() + 30;
    }

    public int getListWidth() {
        return super.getListWidth() + 85;
    }
}