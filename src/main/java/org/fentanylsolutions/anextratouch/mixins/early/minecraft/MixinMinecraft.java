package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.compat.ShoulderSurfingCompat;
import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isPressed()Z"),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/settings/GameSettings;keyBindTogglePerspective:Lnet/minecraft/client/settings/KeyBinding;"),
            to = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/settings/GameSettings;keyBindSmoothCamera:Lnet/minecraft/client/settings/KeyBinding;")),
        require = 1,
        allow = 1)
    private boolean anextratouch$cyclePerspective(KeyBinding keyBinding) {
        if (!keyBinding.isPressed()) return false;

        DecoupledCameraHandler.onManualPerspectiveChange();
        if (ShoulderSurfingCompat.isAvailable()) {
            // Cycle once from the current view, before vanilla advances its three-state cycle.
            ShoulderSurfingCompat.cyclePerspective();
            return false;
        }
        return true;
    }

    /**
     * Skips the third-person front view in the F5 cycle when enabled.
     * Vanilla wraps thirdPersonView when it exceeds 2 (0 = first, 1 = back, 2 = front).
     * Lowering the wrap threshold to 1 makes F5 toggle between first person and third-person back.
     */
    @ModifyConstant(
        method = "runTick",
        constant = @Constant(intValue = 2),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/settings/GameSettings;keyBindTogglePerspective:Lnet/minecraft/client/settings/KeyBinding;"),
            to = @At(
                value = "FIELD",
                target = "Lnet/minecraft/client/settings/GameSettings;keyBindSmoothCamera:Lnet/minecraft/client/settings/KeyBinding;")),
        require = 1,
        allow = 1)
    private int anextratouch$capPerspectiveCycle(int constant) {
        return Config.simplePerspectiveToggle && !ShoulderSurfingCompat.isAvailable() ? 1 : constant;
    }
}
