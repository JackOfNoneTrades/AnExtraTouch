package org.fentanylsolutions.anextratouch.mixins.early.shouldersurfing;

import net.minecraft.client.settings.KeyBinding;

import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.teamderpy.shouldersurfing.client.KeyHandler;
import com.teamderpy.shouldersurfing.client.ShoulderInstance;
import com.teamderpy.shouldersurfing.config.Perspective;

@Mixin(value = KeyHandler.class, remap = false)
public class MixinKeyHandler {

    @Redirect(
        method = "onInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/settings/KeyBinding;getIsKeyPressed()Z",
            remap = true),
        require = 1,
        allow = 1)
    private static boolean anextratouch$disableHeldPerspectiveCycle(KeyBinding keyBinding) {
        // Minecraft's consumed perspective press now owns F5. SS also runs on unrelated input events.
        return false;
    }

    @Redirect(
        method = "onInput",
        at = @At(
            value = "INVOKE",
            target = "Lcom/teamderpy/shouldersurfing/client/ShoulderInstance;changePerspective(Lcom/teamderpy/shouldersurfing/config/Perspective;)V"),
        require = 1)
    private static void anextratouch$manualShoulderToggle(ShoulderInstance instance, Perspective perspective) {
        DecoupledCameraHandler.onManualPerspectiveChange();
        instance.changePerspective(perspective);
    }
}
