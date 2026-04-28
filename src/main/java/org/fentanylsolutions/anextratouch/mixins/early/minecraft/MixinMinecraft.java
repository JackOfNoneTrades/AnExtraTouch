package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;

import org.fentanylsolutions.anextratouch.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Minecraft.class)
public class MixinMinecraft {

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
                target = "Lnet/minecraft/client/settings/GameSettings;keyBindTogglePerspective:Lnet/minecraft/client/settings/KeyBinding;")))
    private int anextratouch$capPerspectiveCycle(int constant) {
        return Config.simplePerspectiveToggle ? 1 : constant;
    }
}
