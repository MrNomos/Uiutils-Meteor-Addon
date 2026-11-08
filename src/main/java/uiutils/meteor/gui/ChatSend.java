package uiutils.meteor.gui;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ChatSend {
    private ChatSend() {}

    public static void send(String text) {
        ClientPacketListener connection = MeteorClient.mc.getConnection();
        if (connection == null) return;

        try {
            MeteorClient.mc.gui.hud.getChat().addRecentChat(text);
        } catch (Throwable ignored) {}

        String prefix = Config.get().prefix.get();

        if (prefix != null && !prefix.isEmpty() && text.startsWith(prefix)) {
            try {
                Commands.dispatch(text.substring(prefix.length()));
            } catch (CommandSyntaxException e) {
                ChatUtils.error(e.getMessage());
            }
        } else if (text.startsWith("/")) {
            connection.sendCommand(text.substring(1));
        } else {
            connection.sendChat(text);
        }
    }
}
