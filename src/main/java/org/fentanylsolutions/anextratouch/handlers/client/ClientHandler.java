package org.fentanylsolutions.anextratouch.handlers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
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
     * Cancel vanilla/SS crosshair when decoupled camera is active with SS, we render our own at center.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onRenderCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && DecoupledCameraHandler.isActive()
            && !DecoupledCameraHandler.isAimFirstPerson()
            && ShoulderSurfingCompat.isAvailable()) {
            event.setCanceled(true);
        }
    }

    /**
     * Render crosshair at screen center when decoupled camera is active with SS,
     * respecting SS's CrosshairVisibility config. No crosshair without SS.
     */
    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!DecoupledCameraHandler.isActive()) return;
        if (DecoupledCameraHandler.isAimFirstPerson()) return; // vanilla crosshair shown in FP
        if (!ShoulderSurfingCompat.shouldRenderCrosshair()) return;

        Minecraft mc = Minecraft.getMinecraft();
        int width = event.resolution.getScaledWidth();
        int height = event.resolution.getScaledHeight();

        mc.getTextureManager()
            .bindTexture(Gui.icons);
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

        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_BLEND);
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
