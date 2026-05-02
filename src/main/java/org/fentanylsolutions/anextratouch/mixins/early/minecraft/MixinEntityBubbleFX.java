package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.handlers.client.effects.FallingWaterFX;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityBubbleFX.class)
public abstract class MixinEntityBubbleFX extends EntityFX {

    @Unique
    private static final ResourceLocation ANEXTRATOUCH$NEUTRAL_BUBBLE_TEXTURE = new ResourceLocation(
        AnExtraTouch.MODID,
        "textures/particles/bubble.png");

    @Unique
    private boolean anextratouch$useNeutralBubbleTexture;

    protected MixinEntityBubbleFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anextratouch$tintBubble(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ, CallbackInfo ci) {
        anextratouch$applyWaterTint();
        this.anextratouch$useNeutralBubbleTexture = true;
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$tintBubbleAfterMove(CallbackInfo ci) {
        EntityFX particle = (EntityFX) (Object) this;
        if (!particle.isDead) {
            anextratouch$applyWaterTint();
        }
    }

    @Override
    public int getFXLayer() {
        return this.anextratouch$useNeutralBubbleTexture ? 3 : 0;
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTicks, float rotationX, float rotationZ,
        float rotationYZ, float rotationXY, float rotationXZ) {
        if (!this.anextratouch$useNeutralBubbleTexture) {
            super.renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            return;
        }

        anextratouch$updateInterpPos(partialTicks);

        float scale = 0.1F * this.particleScale;
        float x = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - interpPosX);
        float y = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - interpPosY);
        float z = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - interpPosZ);

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);

            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(ANEXTRATOUCH$NEUTRAL_BUBBLE_TEXTURE);
            tessellator.startDrawingQuads();
            tessellator.setBrightness(this.getBrightnessForRender(partialTicks));
            tessellator.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
            tessellator.addVertexWithUV(
                (double) (x - rotationX * scale - rotationXY * scale),
                (double) (y - rotationZ * scale),
                (double) (z - rotationYZ * scale - rotationXZ * scale),
                1.0D,
                1.0D);
            tessellator.addVertexWithUV(
                (double) (x - rotationX * scale + rotationXY * scale),
                (double) (y + rotationZ * scale),
                (double) (z - rotationYZ * scale + rotationXZ * scale),
                1.0D,
                0.0D);
            tessellator.addVertexWithUV(
                (double) (x + rotationX * scale + rotationXY * scale),
                (double) (y + rotationZ * scale),
                (double) (z + rotationYZ * scale + rotationXZ * scale),
                0.0D,
                0.0D);
            tessellator.addVertexWithUV(
                (double) (x + rotationX * scale - rotationXY * scale),
                (double) (y - rotationZ * scale),
                (double) (z + rotationYZ * scale - rotationXZ * scale),
                0.0D,
                1.0D);
            tessellator.draw();
        } finally {
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
        }
    }

    @Unique
    private static void anextratouch$updateInterpPos(float partialTicks) {
        Entity viewer = Minecraft.getMinecraft().renderViewEntity;
        if (viewer == null) {
            return;
        }

        interpPosX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * (double) partialTicks;
        interpPosY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * (double) partialTicks;
        interpPosZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * (double) partialTicks;
    }

    private void anextratouch$applyWaterTint() {
        EntityFX particle = (EntityFX) (Object) this;
        float[] rgb = FallingWaterFX.getWaterColor(particle.worldObj, particle.posX, particle.posY, particle.posZ);
        particle.setRBGColorF(rgb[0], rgb[1], rgb[2]);
    }
}
