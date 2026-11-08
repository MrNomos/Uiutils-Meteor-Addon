package uiutils.meteor.modules;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import uiutils.meteor.gui.Panel;

import java.util.ArrayList;
import java.util.List;

public class UiUtilsModule extends Module {
    public static UiUtilsModule I;

    public boolean sendPackets = true;
    public boolean delayPackets = false;
    public int spam = 1;
    public final List<Packet<?>> queue = new ArrayList<>();

    private boolean bypass;
    private Screen savedScreen;
    private AbstractContainerMenu savedMenu;

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> chat = sg.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Print what each button did.")
        .defaultValue(true)
        .build()
    );

    public UiUtilsModule() {
        super(Categories.Misc, "ui-utils", "Packet and GUI tools for container screens (close/desync, delay queue, fabricate, save/load GUI).");
        I = this;
    }

    @Override
    public void onDeactivate() {
        sendPackets = true;
        delayPackets = false;
        queue.clear();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        queue.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Panel.refresh();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSend(PacketEvent.Send event) {
        if (bypass) return;
        if (!(event.packet instanceof ServerboundContainerClickPacket) && !(event.packet instanceof ServerboundContainerButtonClickPacket)) return;

        if (!sendPackets) {
            event.cancel();
        } else if (delayPackets) {
            queue.add(event.packet);
            event.cancel();
        }
    }

    public void closeWithoutPacket() {
        mc.gui.setScreen(null);
        say("Closed GUI without packet");
    }

    public void desync() {
        if (mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        say("Sent close packet, GUI kept open");
    }

    public void toggleSend() {
        sendPackets = !sendPackets;
        say("Send packets: " + sendPackets);
    }

    public void toggleDelay() {
        delayPackets = !delayPackets;

        if (!delayPackets) {
            say("Sent " + flush(1) + " delayed packet(s)");
            queue.clear();
        }

        say("Delay packets: " + delayPackets);
    }

    /** Sends the whole queue {@code times} times and returns how many packets went out. */
    public int flush(int times) {
        int sent = 0;

        for (int i = 0; i < times; i++) {
            for (Packet<?> packet : new ArrayList<>(queue)) {
                raw(packet);
                sent++;
            }
        }

        return sent;
    }

    public void leaveAndSend() {
        if (mc.getConnection() == null) return;

        delayPackets = false;
        flush(1);
        queue.clear();
        disconnect();
    }

    public void disconnect() {
        if (mc.getConnection() != null) {
            mc.getConnection().getConnection().disconnect(Component.literal("Disconnected by UI Utils"));
        }
    }

    public void sendQueue() {
        say("Sent " + flush(spam) + " packet(s)");
    }

    public void sendOne() {
        if (queue.isEmpty()) {
            say("Queue is empty");
            return;
        }

        raw(queue.remove(0));
        say("Sent one queued packet");
    }

    public void popLast() {
        if (queue.isEmpty()) {
            say("Queue is empty");
            return;
        }

        queue.remove(queue.size() - 1);
        say("Removed last queued packet");
    }

    public void clearQueue() {
        say("Cleared " + queue.size() + " packet(s)");
        queue.clear();
    }

    public void resyncInventory() {
        if (mc.player == null || mc.getConnection() == null) return;

        AbstractContainerMenu menu = mc.player.containerMenu;

        raw(new ServerboundContainerClickPacket(
            menu.containerId,
            (menu.getStateId() + 1) & 32767,
            (short) -1,
            (byte) 0,
            ContainerInput.PICKUP,
            new Int2ObjectArrayMap<>(),
            HashedStack.create(menu.getCarried(), mc.getConnection().decoratedHashOpsGenenerator())
        ));

        say("Requested inventory resync");
    }

    public void copyTitleJson() {
        if (mc.gui.screen() == null) return;

        JsonElement json = ComponentSerialization.CODEC
            .encodeStart(JsonOps.INSTANCE, mc.gui.screen().getTitle())
            .result()
            .orElse(null);

        if (json == null) {
            say("Could not encode title");
            return;
        }

        mc.keyboardHandler.setClipboard(new Gson().toJson(json));
        say("Copied GUI title JSON");
    }

    public void saveGui() {
        if (mc.gui.screen() == null || mc.player == null) return;

        savedScreen = mc.gui.screen();
        savedMenu = mc.player.containerMenu;
        say("Saved GUI");
    }

    public void loadGui() {
        if (savedScreen == null || mc.player == null) {
            say("No saved GUI");
            return;
        }

        mc.gui.setScreen(savedScreen);
        mc.player.containerMenu = savedMenu;
        say("Loaded GUI");
    }

    /** Sends a packet straight to the connection, skipping our own send filter. */
    public void raw(Packet<?> packet) {
        if (mc.getConnection() == null) return;

        bypass = true;
        try {
            mc.getConnection().send(packet);
        } finally {
            bypass = false;
        }
    }

    private void say(String message) {
        if (chat.get()) info(message);
    }
}
