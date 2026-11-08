package uiutils.meteor.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uiutils.meteor.gui.Panel;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void uiutils$addPanel(CallbackInfo ci) {
        Panel.build(widget -> addRenderableWidget(widget), widget -> removeWidget(widget));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void uiutils$chatEnter(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Panel.onKey(event.key())) cir.setReturnValue(true);
    }
}
