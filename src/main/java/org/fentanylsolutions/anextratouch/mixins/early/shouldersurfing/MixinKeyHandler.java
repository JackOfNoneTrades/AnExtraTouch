package org.fentanylsolutions.anextratouch.mixins.early.shouldersurfing;

import org.fentanylsolutions.anextratouch.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.teamderpy.shouldersurfing.client.KeyHandler;
import com.teamderpy.shouldersurfing.config.Perspective;

@Mixin(value = KeyHandler.class, remap = false)
public class MixinKeyHandler {

    @Redirect(
        method = "onInput",
        at = @At(
            value = "INVOKE",
            target = "Lcom/teamderpy/shouldersurfing/config/Perspective;next()Lcom/teamderpy/shouldersurfing/config/Perspective;"),
        require = 1)
    private static Perspective anextratouch$simplePerspectiveToggle(Perspective current) {
        if (!Config.simplePerspectiveToggle) {
            return current.next();
        }

        return current == Perspective.FIRST_PERSON ? Perspective.SHOULDER_SURFING : Perspective.FIRST_PERSON;
    }
}
