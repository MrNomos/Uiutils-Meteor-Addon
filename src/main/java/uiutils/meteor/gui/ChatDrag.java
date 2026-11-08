package uiutils.meteor.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.List;

/**
 * Drag, type and key handling for the panel's chat box on container screens.
 * Left drag (past a small threshold) or right drag moves it, middle click resets it.
 */
public final class ChatDrag {
    private static EditBox cur;
    private static int baseX, baseY;
    private static int offX, offY;
    private static boolean dragging;
    private static int dragButton;
    private static int grabX, grabY;
    private static boolean resetHeld;
    private static boolean pending;
    private static double pressX, pressY;

    // Mouse button numbers as delivered by MouseButtonEvent.
    private static final int LEFT = 1;
    private static final int MIDDLE = 2;
    private static final int RIGHT = 3;

    // Key codes as delivered by KeyEvent.shortcutKey().
    private static final int KEY_PAGE_UP = 1073741899;
    private static final int KEY_PAGE_DOWN = 1073741902;
    private static final int KEY_DOWN = 1073741905;
    private static final int KEY_UP = 1073741906;
    private static final int KEY_KP_ENTER = 1073741912;

    private ChatDrag() {}

    /** Maps the screen's key codes to the codes Panel.onKey understands. */
    private static int toPanelKey(int key) {
        return switch (key) {
            case 13 -> 257;
            case 9 -> 258;
            case KEY_KP_ENTER -> 335;
            case KEY_PAGE_UP -> 266;
            case KEY_PAGE_DOWN -> 267;
            default -> key;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static void sync(EditBox box) {
        if (box == cur) return;

        cur = box;
        baseX = box.getX();
        baseY = box.getY();
        dragging = false;
        pending = false;
    }

    private static void place(EditBox box, int width, int height) {
        box.setX(clamp(baseX + offX, 0, width - box.getWidth()));
        box.setY(clamp(baseY + offY, 0, height - box.getHeight()));
    }

    /** Keeps the suggestion buttons attached to the box, above it if there is room. */
    private static void layout(EditBox box) {
        List<Button> suggestions = Panel.sug;
        if (suggestions == null || suggestions.isEmpty()) return;

        int count = suggestions.size();
        boolean above = box.getY() >= 16 * count;

        for (int i = 0; i < count; i++) {
            Button button = suggestions.get(i);
            button.setX(box.getX());
            button.setY(above ? box.getY() - 16 * (i + 1) : box.getY() + box.getHeight() + 2 + 16 * i);
        }
    }

    private static EditBox active(List<?> children) {
        EditBox box = Panel.chat;
        return box != null && children.contains(box) ? box : null;
    }

    public static void tick(int width, int height, List<?> children) {
        EditBox box = active(children);
        if (box == null) return;

        sync(box);
        if (!dragging) place(box, width, height);
        layout(box);
    }

    public static boolean click(int width, int height, List<?> children, double mouseX, double mouseY, int button) {
        if (button != LEFT && button != MIDDLE && button != RIGHT) return false;

        EditBox box = active(children);
        if (box == null || !box.isMouseOver(mouseX, mouseY)) return false;

        sync(box);

        if (button == MIDDLE) {
            dragging = false;
            pending = false;
            offX = 0;
            offY = 0;
            resetHeld = true;
            place(box, width, height);
            layout(box);
            return true;
        }

        grabX = (int) (mouseX - box.getX());
        grabY = (int) (mouseY - box.getY());

        if (button == LEFT) {
            // Not a drag yet: let the click focus the box, start dragging once the mouse moves.
            pending = true;
            pressX = mouseX;
            pressY = mouseY;
            return false;
        }

        dragging = true;
        dragButton = button;
        return true;
    }

    public static boolean drag(int width, int height, double mouseX, double mouseY) {
        EditBox box = Panel.chat;
        if (box == null || box != cur) return false;

        if (!dragging && pending) {
            if (Math.abs(mouseX - pressX) < 4.0 && Math.abs(mouseY - pressY) < 4.0) return false;

            dragging = true;
            dragButton = LEFT;
            pending = false;
        }

        if (!dragging) return false;

        int x = clamp((int) mouseX - grabX, 0, width - box.getWidth());
        int y = clamp((int) mouseY - grabY, 0, height - box.getHeight());

        offX = x - baseX;
        offY = y - baseY;
        box.setX(x);
        box.setY(y);
        layout(box);
        return true;
    }

    public static boolean release(int button) {
        if (button == LEFT) pending = false;

        if (button == MIDDLE && resetHeld) {
            resetHeld = false;
            return true;
        }

        if (!dragging || button != dragButton) return false;

        dragging = false;
        return true;
    }

    private static ChatComponent vanillaChat(Minecraft mc) {
        return mc == null || mc.gui == null || mc.gui.hud == null ? null : mc.gui.hud.getChat();
    }

    public static boolean type(List<?> children, CharacterEvent event) {
        EditBox box = active(children);
        return box != null && box.canConsumeInput() && box.charTyped(event);
    }

    public static boolean key(Minecraft mc, List<?> children, KeyEvent event) {
        EditBox box = active(children);
        if (box == null || !box.canConsumeInput()) return false;
        if (event.isEscape()) return false;

        int key = event.shortcutKey();

        if (key == KEY_PAGE_UP || key == KEY_UP) {
            HistNav.move(box, -1);
            return true;
        }

        if (key == KEY_PAGE_DOWN || key == KEY_DOWN) {
            HistNav.move(box, 1);
            return true;
        }

        int panelKey = toPanelKey(key);
        boolean enter = panelKey == 257 || panelKey == 335;
        String text = enter ? box.getValue() : null;

        if (!Panel.onKey(panelKey)) box.keyPressed(event);

        // The panel sent and cleared the box: also put it in vanilla chat history.
        if (enter && text != null && !text.isBlank() && box.getValue().isEmpty()) {
            ChatComponent chat = vanillaChat(mc);
            if (chat != null) chat.addRecentChat(text);
        }

        return true;
    }
}
