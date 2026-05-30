package org.fentanylsolutions.anextratouch.handlers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.compat.ShoulderSurfingCompat;
import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientHandler {

    // Saved entity rotation for RenderWorldLastEvent swap
    private float savedYaw, savedPitch, savedPrevYaw, savedPrevPitch;
    private boolean rotationSwapped;

    // Player fade transparency state
    private boolean playerFadeActive;

    private static void prepareGuiIconsState(Minecraft mc) {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        mc.getTextureManager()
            .bindTexture(Gui.icons);
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        AnExtraTouch.vic.serverHasAET = false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DecoupledCameraHandler.tick();
        }
    }

    /**
     * Replace vanilla/SS crosshair when decoupled camera is active with SS.
     * Render is done in ALL-post to avoid per-mod ordering quirks in CROSSHAIRS stage.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        if (!DecoupledCameraHandler.isActive() || DecoupledCameraHandler.isAimFirstPerson()
            || !ShoulderSurfingCompat.isAvailable()) {
            return;
        }
        event.setCanceled(true);

        // Preserve vanilla side effects for following HUD elements.
        prepareGuiIconsState(Minecraft.getMinecraft());
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRenderBossHealthPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
            return;
        }

        // Forge 1.7.10 boss bar rendering path does not bind icons itself.
        // Ensure deterministic HUD state regardless of canceled/overridden crosshair handlers.
        prepareGuiIconsState(Minecraft.getMinecraft());
    }

    /**
     * Render crosshair at screen center when decoupled camera is active with SS,
     * respecting SS CrosshairVisibility; no crosshair without SS.
     */
    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!DecoupledCameraHandler.isActive()) return;
        if (DecoupledCameraHandler.isAimFirstPerson()) return;
        if (!ShoulderSurfingCompat.shouldRenderCrosshair()) return;

        Minecraft mc = Minecraft.getMinecraft();
        int width = event.resolution.getScaledWidth();
        int height = event.resolution.getScaledHeight();

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        mc.getTextureManager()
            .bindTexture(Gui.icons);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);

            int x = width / 2 - 7;
            int y = height / 2 - 7;
            float z = -90.0f;
            float u1 = 0f;
            float v1 = 0f;
            float u2 = 16f / 256f;
            float v2 = 16f / 256f;

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x, y + 16, z, u1, v2);
            tess.addVertexWithUV(x + 16, y + 16, z, u2, v2);
            tess.addVertexWithUV(x + 16, y, z, u2, v1);
            tess.addVertexWithUV(x, y, z, u1, v1);
            tess.draw();
        } finally {
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }

    /**
     * Fade out the view entity as the camera gets close.
     * Enables GL blending with computed alpha when within fade range.
     */
    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        if (playerFadeActive) {
            GL11.glPopAttrib();
            playerFadeActive = false;
        }
        playerFadeActive = false;
        if (!Config.cameraPlayerFadeEnabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entity != mc.renderViewEntity) return;
        if (mc.gameSettings.thirdPersonView == 0 && !DecoupledCameraHandler.isActive()) return;

        float alpha = DecoupledCameraHandler.getPlayerAlpha();
        if (alpha <= 0f) {
            event.setCanceled(true);
            return;
        }
        if (alpha >= 1f) return;

        playerFadeActive = true;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, alpha);
    }

    /**
     * Restore GL state after faded entity render.
     */
    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post event) {
        if (!playerFadeActive) return;
        GL11.glPopAttrib();
        playerFadeActive = false;
    }

    /**
     * Swap entity rotation before ShoulderSurfing's updateDynamicRaytrace (NORMAL priority).
     * This makes the dynamic crosshair raytrace use the camera direction.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderWorldLastSwap(RenderWorldLastEvent event) {
        rotationSwapped = false;
        if (!DecoupledCameraHandler.isActive()) return;
        if (DecoupledCameraHandler.isAimFirstPerson()) return; // vanilla FP handles rotation
        if (!ShoulderSurfingCompat.isAvailable()) return;

        EntityLivingBase entity = Minecraft.getMinecraft().renderViewEntity;
        if (entity == null) return;

        savedYaw = entity.rotationYaw;
        savedPitch = entity.rotationPitch;
        savedPrevYaw = entity.prevRotationYaw;
        savedPrevPitch = entity.prevRotationPitch;

        entity.rotationYaw = DecoupledCameraHandler.getEffectiveYaw();
        entity.rotationPitch = DecoupledCameraHandler.getEffectivePitch();
        entity.prevRotationYaw = DecoupledCameraHandler.getEffectivePrevYaw();
        entity.prevRotationPitch = DecoupledCameraHandler.getEffectivePrevPitch();

        rotationSwapped = true;
    }

    /**
     * Restore entity rotation after ShoulderSurfing's updateDynamicRaytrace.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderWorldLastRestore(RenderWorldLastEvent event) {
        if (!rotationSwapped) return;

        EntityLivingBase entity = Minecraft.getMinecraft().renderViewEntity;
        if (entity == null) return;

        entity.rotationYaw = savedYaw;
        entity.rotationPitch = savedPitch;
        entity.prevRotationYaw = savedPrevYaw;
        entity.prevRotationPitch = savedPrevPitch;
        rotationSwapped = false;
    }
}
