package org.fentanylsolutions.anextratouch.handlers.client;

import java.util.HashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class SmoothGuiHandler {

    private static final float FADE_OFFSET = 9f;

    private static long lastScreenOpenedTime = 0;
    private static long lastScreenChangedTime = 0;
    private static float appliedDisplacement = 0f;

    public static float getAppliedDisplacement() {
        return appliedDisplacement;
    }

    public static float getAlphaSince(long time) {
        float fadeTime = Config.smoothGuiAnimationTime;
        float elapsed = Math.min((float) (System.currentTimeMillis() - time), fadeTime);
        return elapsed / fadeTime;
    }

    public static long getLastScreenOpenedTime() {
        return lastScreenOpenedTime;
    }

    private static float applyEasing(float t) {
        if ("CUBIC".equalsIgnoreCase(Config.smoothGuiAnimationStyle)) {
            return t * t * t;
        }
        // BACK (default)
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * t * t * t - c1 * t * t;
    }

    private static float calculateDisplacement() {
        float alpha = getAlphaSince(lastScreenChangedTime);
        float directionMod = "UP".equalsIgnoreCase(Config.smoothGuiAnimationDirection) ? -1f : 1f;
        return FADE_OFFSET * applyEasing(1f - alpha) * Config.smoothGuiAnimationScale * directionMod;
    }

    private static boolean isValidScreen(GuiScreen screen) {
        if (screen == null) {
            return false;
        }
        HashSet<String> excluded = AnExtraTouch.vic.smoothGuiExcludedScreens;
        if (excluded == null) {
            return true;
        }
        return !excluded.contains(
            screen.getClass()
                .getSimpleName())
            && !excluded.contains(
                screen.getClass()
                    .getName());
    }

    private static boolean isInMainMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld == null && mc.thePlayer == null;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen newScreen = event.gui;
        GuiScreen oldScreen = Minecraft.getMinecraft().currentScreen;
        if (newScreen != null) {
            long now = System.currentTimeMillis();
            if (oldScreen != null && oldScreen.getClass()
                .equals(newScreen.getClass())) {
                return;
            }
            lastScreenChangedTime = now;
            if (oldScreen == null) {
                lastScreenOpenedTime = now;
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!Config.smoothGuiEnabled) {
            return;
        }
        if (!isValidScreen(event.gui)) {
            return;
        }
        if (isInMainMenu()) return;
        appliedDisplacement = calculateDisplacement();
        if (appliedDisplacement == 0f) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(0f, -appliedDisplacement, 0f);
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (appliedDisplacement == 0f) {
            return;
        }
        GL11.glPopMatrix();
        appliedDisplacement = 0f;
    }
}
