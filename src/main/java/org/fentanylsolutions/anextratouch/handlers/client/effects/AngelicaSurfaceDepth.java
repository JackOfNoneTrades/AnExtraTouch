package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.gl.texture.DepthCopyStrategy;
import net.coderbot.iris.rendertarget.DepthTexture;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.texture.DepthBufferFormat;
import com.gtnewhorizons.angelica.glsm.texture.TextureInfoCache;

/** Optional depth snapshots. All copies use Angelica's backend and preserve framebuffer bindings. */
final class AngelicaSurfaceDepth {

    private static GlFramebuffer scene;
    private static GlFramebuffer opaque;
    private static GlFramebuffer backup;
    private static DepthTexture opaqueTexture;
    private static DepthTexture backupTexture;
    private static DepthCopyStrategy copier;
    private static int depthId;
    private static int width;
    private static int height;
    private static DepthBufferFormat format;
    private static boolean captured;

    private AngelicaSurfaceDepth() {}

    static boolean capture() {
        captured = false;
        if (IrisApi.getInstance()
            .isRenderingShadowPass()) return false;
        Framebuffer main = Minecraft.getMinecraft()
            .getFramebuffer();
        int depth = ((IRenderTargetExt) main).iris$getDepthTextureId();
        if (depth <= 0 || !isActiveDepth(depth)) return false;
        DepthBufferFormat nextFormat = DepthBufferFormat.fromGlEnum(
            TextureInfoCache.INSTANCE.getInfo(depth)
                .getInternalFormat());
        if (nextFormat == null) return false;
        int read = GLStateManager.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GLStateManager.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int texture = GLStateManager.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            if (scene == null || depthId != depth
                || width != main.framebufferWidth
                || height != main.framebufferHeight
                || format != nextFormat) {
                destroy();
                depthId = depth;
                width = main.framebufferWidth;
                height = main.framebufferHeight;
                format = nextFormat;
                opaqueTexture = new DepthTexture(width, height, format);
                backupTexture = new DepthTexture(width, height, format);
                scene = depthFramebuffer(depthId);
                opaque = depthFramebuffer(opaqueTexture.getTextureId());
                backup = depthFramebuffer(backupTexture.getTextureId());
                copier = DepthCopyStrategy.fastest(format.isCombinedStencil());
            }
            copier.copy(scene, depthId, opaque, opaqueTexture.getTextureId(), width, height);
            captured = true;
            return true;
        } finally {
            GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            restoreBindings(read, draw);
        }
    }

    static AngelicaShaderHelper.WaterRenderScope begin() {
        if (!captured) return null;
        captured = false;
        Framebuffer main = Minecraft.getMinecraft()
            .getFramebuffer();
        if (((IRenderTargetExt) main).iris$getDepthTextureId() != depthId || !isActiveDepth(depthId)
            || main.framebufferWidth != width
            || main.framebufferHeight != height) return null;
        copy(scene, depthId, backup, backupTexture.getTextureId());
        try {
            // Every world gbuffer shares this depth texture. Keep its ID/attachments and colors intact.
            copy(opaque, opaqueTexture.getTextureId(), scene, depthId);
        } catch (RuntimeException | LinkageError failure) {
            copy(backup, backupTexture.getTextureId(), scene, depthId);
            throw failure;
        }
        return () -> copy(backup, backupTexture.getTextureId(), scene, depthId);
    }

    private static boolean isActiveDepth(int texture) {
        if (GLStateManager.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == 0) return false;
        return GLStateManager.glGetFramebufferAttachmentParameteri(
            GL30.GL_DRAW_FRAMEBUFFER,
            GL30.GL_DEPTH_ATTACHMENT,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) == GL11.GL_TEXTURE
            && GLStateManager.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) == texture;
    }

    private static GlFramebuffer depthFramebuffer(int texture) {
        GlFramebuffer framebuffer = new GlFramebuffer();
        framebuffer.addDepthAttachment(texture);
        framebuffer.noDrawBuffers();
        RenderSystem.readBuffer(framebuffer.getId(), GL11.GL_NONE);
        if (!framebuffer.isComplete()) {
            framebuffer.destroy();
            throw new IllegalStateException("Incomplete surface overlay depth framebuffer");
        }
        return framebuffer;
    }

    private static void copy(GlFramebuffer source, int sourceTexture, GlFramebuffer destination,
        int destinationTexture) {
        int read = GLStateManager.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GLStateManager.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            copier.copy(source, sourceTexture, destination, destinationTexture, width, height);
        } finally {
            restoreBindings(read, draw);
        }
    }

    private static void restoreBindings(int read, int draw) {
        GLStateManager.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GLStateManager.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
    }

    private static void destroy() {
        if (scene != null) scene.destroy();
        if (opaque != null) opaque.destroy();
        if (backup != null) backup.destroy();
        if (opaqueTexture != null) opaqueTexture.destroy();
        if (backupTexture != null) backupTexture.destroy();
        scene = opaque = backup = null;
        opaqueTexture = backupTexture = null;
    }
}
