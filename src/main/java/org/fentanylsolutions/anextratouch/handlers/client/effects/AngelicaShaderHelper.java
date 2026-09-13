package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.lang.reflect.Method;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AngelicaShaderHelper {

    private static Boolean angelicaLoaded;
    private static boolean irisApiLookupComplete;
    private static Object irisApi;
    private static Method isShaderPackInUseMethod;
    private static boolean waterRenderingUnavailable;
    private static boolean surfaceDepthUnavailable;
    private static boolean surfaceDepthCaptured;

    private AngelicaShaderHelper() {}

    public interface WaterRenderScope {

        void close();
    }

    public static void captureSurfaceDepth() {
        surfaceDepthCaptured = false;
        if (surfaceDepthUnavailable || !isShaderPackInUse()) return;
        boolean waves = Config.wavesEnabled && Config.waveShaderWater && WaterWaveManager.INSTANCE.hasActiveWaves();
        boolean wakes = Config.waterWakesEnabled && Config.waterWakeShaderWater
            && WakeTrailManager.INSTANCE.hasActiveWakes();
        if (!waves && !wakes) return;
        try {
            surfaceDepthCaptured = AngelicaSurfaceDepth.capture();
        } catch (RuntimeException | LinkageError failure) {
            disableSurfaceDepth(failure);
        }
    }

    public static WaterRenderScope beginSurfaceDepth() {
        if (!surfaceDepthCaptured || surfaceDepthUnavailable) return null;
        surfaceDepthCaptured = false;
        try {
            return AngelicaSurfaceDepth.begin();
        } catch (RuntimeException | LinkageError failure) {
            disableSurfaceDepth(failure);
            return null;
        }
    }

    private static void disableSurfaceDepth(Throwable failure) {
        surfaceDepthUnavailable = true;
        AnExtraTouch.LOG
            .warn("Angelica surface depth correction is unavailable; retaining normal depth testing", failure);
    }

    static WaterRenderScope beginWaterRendering() {
        return beginRendering(true);
    }

    static WaterRenderScope beginOverlayRendering() {
        return beginRendering(false);
    }

    private static WaterRenderScope beginRendering(boolean waterMaterial) {
        if (waterRenderingUnavailable || !isShaderPackInUse()) {
            return null;
        }
        try {
            // Keep optional Angelica types in a separate class so AET also loads without it.
            return waterMaterial ? AngelicaWaterRenderer.begin() : AngelicaWaterRenderer.beginOverlay();
        } catch (RuntimeException | LinkageError e) {
            waterRenderingUnavailable = true;
            AnExtraTouch.LOG.warn("Angelica water rendering is unavailable; using regular water effects", e);
            return null;
        }
    }

    static boolean isShaderPackInUse() {
        if (!isAngelicaLoaded()) {
            return false;
        }

        if (!irisApiLookupComplete) {
            irisApiLookupComplete = true;

            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstance = irisApiClass.getMethod("getInstance");
                irisApi = getInstance.invoke(null);
                isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            } catch (Throwable ignored) {
                irisApi = null;
                isShaderPackInUseMethod = null;
            }
        }

        if (irisApi == null || isShaderPackInUseMethod == null) {
            return false;
        }

        try {
            Object result = isShaderPackInUseMethod.invoke(irisApi);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isAngelicaLoaded() {
        if (angelicaLoaded == null) {
            angelicaLoaded = Loader.isModLoaded("angelica");
        }
        return angelicaLoaded.booleanValue();
    }
}
