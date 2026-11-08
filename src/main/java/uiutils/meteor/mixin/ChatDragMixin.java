package uiutils.meteor.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uiutils.meteor.gui.ChatDrag;

@Mixin(AbstractContainerScreen.class)
public abstract class ChatDragMixin extends Screen {
    protected ChatDragMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void uiutils$dragInit(CallbackInfo ci) {
        ChatDrag.tick(width, height, children());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void uiutils$dragTick(CallbackInfo ci) {
        ChatDrag.tick(width, height, children());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void uiutils$dragClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ChatDrag.click(width, height, children(), event.x(), event.y(), event.button())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void uiutils$dragMove(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (ChatDrag.drag(width, height, event.x(), event.y())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void uiutils$dragRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatDrag.release(event.button())) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void uiutils$typeKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatDrag.key(minecraft, children(), event)) cir.setReturnValue(true);
    }
}
