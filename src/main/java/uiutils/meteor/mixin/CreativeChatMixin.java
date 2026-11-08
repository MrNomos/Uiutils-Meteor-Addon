package uiutils.meteor.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uiutils.meteor.gui.ChatDrag;

// The creative inventory overrides charTyped/keyPressed itself, so it needs its own hook.
@Pseudo
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
public abstract class CreativeChatMixin extends Screen {
    protected CreativeChatMixin(Component title) {
        super(title);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 0)
    private void uiutils$creativeChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatDrag.type(children(), event)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void uiutils$creativeKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatDrag.key(minecraft, children(), event)) cir.setReturnValue(true);
    }
}
