package uiutils.meteor.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import uiutils.meteor.modules.UiUtilsModule;

/** Builds a container click / button click packet from typed values and sends or queues it. */
public class FabricateScreen extends WindowScreen {
    private boolean click = true;
    private ContainerInput action = ContainerInput.PICKUP;

    private WTextBox sync;
    private WTextBox rev;
    private WTextBox slot;
    private WTextBox button;

    public FabricateScreen(GuiTheme theme) {
        super(theme, "Fabricate packet");
    }

    @Override
    public void initWidgets() {
        AbstractContainerMenu menu = MeteorClient.mc.player.containerMenu;

        WButton type = add(theme.button("Type: Click Slot")).expandX().widget();
        type.action = () -> {
            click = !click;
            type.set(click ? "Type: Click Slot" : "Type: Button Click");
        };

        sync = field("Sync ID", menu.containerId);
        rev = field("Revision", menu.getStateId());
        slot = field("Slot", 0);
        button = field("Button", 0);

        WButton mode = add(theme.button("Action: " + action.name())).expandX().widget();
        mode.action = () -> {
            ContainerInput[] values = ContainerInput.values();
            action = values[(action.ordinal() + 1) % values.length];
            mode.set("Action: " + action.name());
        };

        WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();
        buttons.add(theme.button("Send")).expandX().widget().action = () -> UiUtilsModule.I.raw(build());
        buttons.add(theme.button("Queue")).expandX().widget().action = () -> UiUtilsModule.I.queue.add(build());
    }

    private WTextBox field(String label, int value) {
        WHorizontalList row = add(theme.horizontalList()).expandX().widget();
        row.add(theme.label(label));
        return row.add(theme.textBox(String.valueOf(value), (text, c) -> Character.isDigit(c) || c == '-')).expandX().widget();
    }

    private static int num(WTextBox box) {
        try {
            return Integer.parseInt(box.get().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Packet<?> build() {
        if (!click) return new ServerboundContainerButtonClickPacket(num(sync), num(button));

        return new ServerboundContainerClickPacket(
            num(sync),
            num(rev),
            (short) num(slot),
            (byte) num(button),
            action,
            new Int2ObjectArrayMap<>(),
            HashedStack.create(
                MeteorClient.mc.player.containerMenu.getCarried(),
                MeteorClient.mc.getConnection().decoratedHashOpsGenenerator()
            )
        );
    }
}
