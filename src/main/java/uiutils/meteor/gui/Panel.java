package uiutils.meteor.gui;

import com.mojang.brigadier.suggestion.Suggestion;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import uiutils.meteor.modules.UiUtilsModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The button column and chat box added to every container screen while the module is active. */
public final class Panel {
    private static Button sync;
    private static Button send;
    private static Button delay;
    private static Button queue;
    private static Button spam;

    public static EditBox chat;

    private static Consumer<AbstractWidget> adder;
    private static Consumer<AbstractWidget> remover;

    public static List<Button> sug = new ArrayList<>();
    public static final List<String> history = new ArrayList<>();
    public static int hi;

    private static List<Suggestion> shown = List.of();

    private Panel() {}

    public static void build(Consumer<AbstractWidget> add, Consumer<AbstractWidget> remove) {
        UiUtilsModule m = UiUtilsModule.I;
        if (m == null || !m.isActive()) return;

        adder = add;
        remover = remove;
        shown = List.of();
        sug = new ArrayList<>();

        final int x = 4;
        int y = 4;
        final int w = 150;
        final int h = 16;
        final int gap = 2;
        final int half = (w - gap) / 2;
        final int third = (w - 2 * gap) / 3;

        sync = btn(x, y, w, h, () -> {}, add);
        sync.active = false;
        y += h + gap;

        btn("Close without packet", x, y, w, h, m::closeWithoutPacket, add);
        y += h + gap;

        btn("Desync", x, y, w, h, m::desync, add);
        y += h + gap;

        send = btn(x, y, w, h, m::toggleSend, add);
        y += h + gap;

        delay = btn(x, y, w, h, m::toggleDelay, add);
        y += h + gap;

        btn("Leave & Send Packets", x, y, w, h, m::leaveAndSend, add);
        y += h + gap;

        btn("Fabricate packet", x, y, w, h, () -> MeteorClient.mc.gui.setScreen(new FabricateScreen(GuiThemes.get())), add);
        y += h + gap;

        btn("Copy GUI Title JSON", x, y, w, h, m::copyTitleJson, add);
        y += h + gap;

        btn("Save GUI", x, y, half, h, m::saveGui, add);
        btn("Load GUI", x + half + gap, y, half, h, m::loadGui, add);
        y += h + gap;

        btn("Clear Queue", x, y, half, h, m::clearQueue, add);
        queue = btn(x + half + gap, y, half, h, () -> {}, add);
        queue.active = false;
        y += h + gap;

        btn("Resync Inv", x, y, half, h, m::resyncInventory, add);
        btn("Disconnect", x + half + gap, y, half, h, m::disconnect, add);
        y += h + gap;

        // "-" [spam: send queue x times] "+"
        btn("-", x, y, third / 2, h, () -> {
            m.spam = Math.max(1, m.spam - 1);
            refresh();
        }, add);
        spam = btn(x + third / 2 + gap, y, w - third - 2 * gap, h, m::sendQueue, add);
        btn("+", x + w - third / 2, y, third / 2, h, () -> {
            m.spam = Math.min(100, m.spam + 1);
            refresh();
        }, add);
        y += h + gap;

        btn("Send One", x, y, half, h, m::sendOne, add);
        btn("Pop Last", x + half + gap, y, half, h, m::popLast, add);
        y += h + gap + 48;

        chat = new EditBox(MeteorClient.mc.font, x, y, w, h, Component.literal("Chat"));
        chat.setMaxLength(256);
        chat.setHint(Component.literal("Chat..."));
        chat.setResponder(Panel::suggest);
        add.accept(chat);

        refresh();
    }

    /** Handles Enter, Tab and PageUp/PageDown (panel key codes) while the chat box is focused. */
    public static boolean onKey(int key) {
        if (chat == null || !chat.isFocused()) return false;

        switch (key) {
            case 257, 335 -> {
                String text = chat.getValue();

                if (!text.isBlank() && MeteorClient.mc.getConnection() != null) {
                    if (history.isEmpty() || !history.get(history.size() - 1).equals(text)) history.add(text);
                    hi = history.size();

                    ChatSend.send(text);
                    chat.setValue("");
                }
            }
            case 258 -> {
                if (!shown.isEmpty()) pick(shown.get(0));
            }
            case 266 -> {
                if (hi > 0) chat.setValue(history.get(--hi));
            }
            case 267 -> {
                if (hi < history.size() - 1) {
                    chat.setValue(history.get(++hi));
                } else {
                    hi = history.size();
                    chat.setValue("");
                }
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    private static void suggest(String text) {
        ChatSuggest.suggest(text);
    }

    private static void pick(Suggestion suggestion) {
        ChatSuggest.pick(suggestion, chat);
    }

    public static void showSuggestions(String text, List<Suggestion> suggestions) {
        // Drop results for text that is no longer in the box.
        if (chat == null || adder == null || !text.equals(chat.getValue())) return;

        sug.forEach(remover);
        sug.clear();

        shown = suggestions.subList(0, Math.min(3, suggestions.size()));

        int y = chat.getY() - 16;

        for (Suggestion suggestion : shown) {
            Button button = Button.builder(Component.literal(suggestion.getText()), b -> pick(suggestion))
                .bounds(chat.getX(), y, chat.getWidth(), 14)
                .build();

            y -= 16;
            sug.add(button);
            adder.accept(button);
        }
    }

    public static void refresh() {
        UiUtilsModule m = UiUtilsModule.I;
        if (m == null || sync == null || MeteorClient.mc.player == null) return;

        sync.setMessage(Component.literal("Sync: " + MeteorClient.mc.player.containerMenu.containerId
            + "  Rev: " + MeteorClient.mc.player.containerMenu.getStateId()));
        send.setMessage(Component.literal("Send packets: " + m.sendPackets));
        delay.setMessage(Component.literal("Delay packets: " + m.delayPackets));
        queue.setMessage(Component.literal("Queue: " + m.queue.size()));
        spam.setMessage(Component.literal("Spam (x" + m.spam + ")"));
    }

    private static Button btn(String label, int x, int y, int w, int h, Runnable action, Consumer<AbstractWidget> add) {
        Button button = Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, w, h).build();
        add.accept(button);
        return button;
    }

    private static Button btn(int x, int y, int w, int h, Runnable action, Consumer<AbstractWidget> add) {
        return btn("", x, y, w, h, action, add);
    }
}
