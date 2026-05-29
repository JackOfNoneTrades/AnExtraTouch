package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WaterSurfaceRenderCompat {

    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(
        AnExtraTouch.MODID,
        "textures/generated/water_surface_white");

    private static Boolean swanSongLoaded;
    private static boolean whiteTextureLoaded;

    private WaterSurfaceRenderCompat() {}

    public static boolean shouldUseLateSwanSongPass() {
        if (!isSwanSongLoaded()) {
            return false;
        }

        try {
            return SwanSongShaderCompat.isInitialized();
        } catch (LinkageError ignored) {
            return false;
        }
    }

    static void bindWhiteTexture(Minecraft mc) {
        if (!whiteTextureLoaded) {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0xFFFFFFFF);
            mc.getTextureManager()
                .loadTexture(WHITE_TEXTURE, new DynamicTexture(image));
            whiteTextureLoaded = true;
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.getTextureManager()
            .bindTexture(WHITE_TEXTURE);
    }

    private static boolean isSwanSongLoaded() {
        if (swanSongLoaded == null) {
            swanSongLoaded = Boolean.valueOf(Loader.isModLoaded("swansong"));
        }
        return swanSongLoaded.booleanValue();
    }
}
