package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.SmoothGuiHandler;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen extends Gui {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected Minecraft mc;

    @Unique
    private static final int anextratouch$COLOR_TOP = -1072689136;

    @Unique
    private static final int anextratouch$COLOR_BOTTOM = -804253680;

    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true)
    private void onDrawWorldBackground(int tint, CallbackInfo ci) {
        if (!Config.smoothGuiEnabled || !Config.smoothGuiFadeBackground) {
            return;
        }
        if (this.mc.theWorld == null) {
            return;
        }

        ci.cancel();
        float fade = Math.min(SmoothGuiHandler.getAlphaSince(SmoothGuiHandler.getLastScreenOpenedTime()) * 2.6f, 1f);
        int top = anextratouch$applyFade(anextratouch$COLOR_TOP, fade);
        int bottom = anextratouch$applyFade(anextratouch$COLOR_BOTTOM, fade);

        // undo the slide displacement so the background stays fixed (matching og mod)
        float displacement = SmoothGuiHandler.getAppliedDisplacement();
        if (displacement != 0f) {
            GL11.glTranslatef(0f, displacement, 0f);
        }
        this.drawGradientRect(0, 0, this.width, this.height, top, bottom);
        if (displacement != 0f) {
            GL11.glTranslatef(0f, -displacement, 0f);
        }
    }

    @Unique
    private static int anextratouch$applyFade(int color, float fade) {
        int a = (int) (((color >> 24) & 0xFF) * fade);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
