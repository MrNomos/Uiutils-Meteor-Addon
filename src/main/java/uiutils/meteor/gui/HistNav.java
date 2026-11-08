package uiutils.meteor.gui;

import net.minecraft.client.gui.components.EditBox;

import java.util.List;

/** Up/down arrow history for the panel's chat box. */
public final class HistNav {
    private static String draft = "";

    private HistNav() {}

    public static void move(EditBox box, int dir) {
        try {
            List<String> history = Panel.history;
            if (history == null || history.isEmpty() || box == null) return;

            int size = history.size();
            int pos = Panel.hi;
            if (pos < 0 || pos > size) pos = size;

            if (dir < 0) {
                if (pos <= 0) return;
                if (pos == size) draft = box.getValue();
                pos--;
                box.setValue(history.get(pos));
            } else {
                if (pos >= size) return;
                pos++;
                box.setValue(pos >= size ? (draft == null ? "" : draft) : history.get(pos));
            }

            Panel.hi = pos;
        } catch (Throwable ignored) {}
    }
}
