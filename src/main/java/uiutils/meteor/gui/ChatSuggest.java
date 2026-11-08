package uiutils.meteor.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.List;

/** Command suggestions for the panel's chat box (Meteor commands and server commands). */
public final class ChatSuggest {
    private ChatSuggest() {}

    private static String prefix() {
        try {
            String prefix = Config.get().prefix.get();
            return prefix == null ? "" : prefix;
        } catch (Throwable t) {
            return "";
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void suggest(String text) {
        try {
            Minecraft mc = MeteorClient.mc;
            ClientPacketListener connection = mc.getConnection();
            String prefix = prefix();

            CommandDispatcher dispatcher;
            String command;

            if (!prefix.isEmpty() && text.startsWith(prefix)) {
                dispatcher = Commands.DISPATCHER;
                command = text.substring(prefix.length());
            } else if (connection != null && text.startsWith("/")) {
                dispatcher = connection.getCommands();
                command = text.substring(1);
            } else {
                Panel.showSuggestions(text, List.of());
                return;
            }

            if (connection == null || dispatcher == null) {
                Panel.showSuggestions(text, List.of());
                return;
            }

            ParseResults parse = dispatcher.parse(command, connection.getSuggestionsProvider());

            dispatcher.getCompletionSuggestions(parse, command.length())
                .thenAccept(suggestions -> mc.execute(() -> {
                    try {
                        Panel.showSuggestions(text, ((com.mojang.brigadier.suggestion.Suggestions) suggestions).getList());
                    } catch (Throwable ignored) {}
                }))
                .exceptionally(error -> null);
        } catch (Throwable t) {
            try {
                Panel.showSuggestions(text, List.of());
            } catch (Throwable ignored) {}
        }
    }

    public static void pick(Suggestion suggestion, EditBox box) {
        try {
            String text = box.getValue();
            if (text == null || text.isEmpty()) return;

            String prefix = prefix();

            if (!prefix.isEmpty() && text.startsWith(prefix)) {
                box.setValue(prefix + suggestion.apply(text.substring(prefix.length())));
            } else if (text.startsWith("/")) {
                box.setValue("/" + suggestion.apply(text.substring(1)));
            }
        } catch (Throwable ignored) {}
    }
}
