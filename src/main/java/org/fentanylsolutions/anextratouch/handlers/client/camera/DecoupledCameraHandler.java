package org.fentanylsolutions.anextratouch.handlers.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.compat.ShoulderSurfingCompat;
import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.settings.KeyBinding;

@SideOnly(Side.CLIENT)
public final class DecoupledCameraHandler {

    public static final KeyBinding FREE_LOOK_KEY = new KeyBinding(
        "key.anextratouch.free_look", Keyboard.KEY_LMENU, "key.categories.anextratouch");

    // State
    private static boolean active;
    private static boolean wasActive;
    private static boolean freeLooking;

    // Independent camera rotation (matches modern ShoulderSurfingCamera.xRot/yRot)
    private static float cameraYaw;
    private static float cameraPitch;
    private static float prevCameraYaw;
    private static float prevCameraPitch;

    // Free look offsets that decay when Alt released (matches modern xRotOffset/yRotOffset)
    private static float yawOffset;
    private static float pitchOffset;
    private static float prevYawOffset;
    private static float prevPitchOffset;

    // Camera yaw snapshot when free look is NOT active (matches modern freeLookYRot)
    private static float freeLookYaw;

    // Entity tracking for reset
    private static int lastEntityId = Integer.MIN_VALUE;

    private DecoupledCameraHandler() {}

    public static void registerKeybinding() {
        ClientRegistry.registerKeyBinding(FREE_LOOK_KEY);
    }

    /**
     * Called every client tick. Updates state, decays free look offsets.
     * Ported from ShoulderSurfingCamera.tick() + ShoulderSurfingImpl.tick().
     */
    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused()) {
            return;
        }

        boolean shouldBeActive = Config.decoupledCameraEnabled
            && (ShoulderSurfingCompat.isShoulderSurfingActive()
                || (!ShoulderSurfingCompat.isAvailable() && mc.gameSettings.thirdPersonView > 0));

        EntityLivingBase entity = mc.renderViewEntity;
        if (entity == null) {
            shouldBeActive = false;
        }

        // Detect entity change -> reset
        if (entity != null) {
            int entityId = entity.getEntityId();
            if (entityId != lastEntityId) {
                lastEntityId = entityId;
                resetState(entity);
            }
        }

        // Handle activation transition
        if (shouldBeActive && !wasActive && entity != null) {
            cameraYaw = entity.rotationYaw;
            cameraPitch = entity.rotationPitch;
            prevCameraYaw = cameraYaw;
            prevCameraPitch = cameraPitch;
            yawOffset = 0f;
            pitchOffset = 0f;
            prevYawOffset = 0f;
            prevPitchOffset = 0f;
            freeLookYaw = cameraYaw;
        }

        wasActive = active;
        active = shouldBeActive;

        if (!active) {
            return;
        }

        // Store previous tick values for interpolation
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        prevYawOffset = yawOffset;
        prevPitchOffset = pitchOffset;

        // Update free look state
        freeLooking = FREE_LOOK_KEY.getKeyCode() != 0 && Keyboard.isKeyDown(FREE_LOOK_KEY.getKeyCode());

        // When not free-looking, store freeLookYaw and decay offsets
        // Matches modern ShoulderSurfingCamera.tick() lines 98-103
        if (!freeLooking) {
            freeLookYaw = cameraYaw;
            yawOffset *= Config.decoupledCameraOffsetDecay;
            pitchOffset *= Config.decoupledCameraOffsetDecay;
        }
    }

    /**
     * Called from Entity.setAngles mixin. Returns true to cancel vanilla rotation.
     * Ported from ShoulderSurfingCamera.turn().
     *
     * In 1.7.10, Entity.setAngles receives raw sensitivity-scaled deltas and applies * 0.15 internally.
     * We replicate that same scaling, matching the sign conventions:
     *   vanilla: rotationYaw += yaw * 0.15;  rotationPitch -= pitch * 0.15;
     */
    public static boolean onSetAngles(float yaw, float pitch) {
        if (!active) {
            return false;
        }

        // Match vanilla Entity.setAngles scaling
        float scaledYaw = yaw * 0.15f;
        float scaledPitch = pitch * 0.15f;

        if (freeLooking) {
            // Free look: accumulate into offsets, set prev=current for no interpolation lag
            // Matches modern turn() lines 406-412
            yawOffset = MathHelper.wrapAngleTo180_float(yawOffset + scaledYaw);
            pitchOffset = MathHelper.clamp_float(pitchOffset - scaledPitch, -90f, 90f);
            prevYawOffset = yawOffset;
            prevPitchOffset = pitchOffset;
            return true;
        }

        // Decoupled: update independent camera rotation
        // Matches modern turn() lines 416-417
        cameraYaw += scaledYaw;
        cameraPitch = MathHelper.clamp_float(cameraPitch - scaledPitch, -90f, 90f);

        return true;
    }

    /**
     * Called from EntityPlayerSP.updateEntityActionState RETURN mixin.
     * Rotates moveStrafing/moveForward to be camera-relative and turns player body toward movement.
     *
     * Ported directly from modern InputHandler.updateMovementInput().
     */
    public static void transformMovement(EntityPlayerSP player) {
        if (!active) {
            return;
        }

        float strafe = player.moveStrafing;
        float forward = player.moveForward;
        boolean isMoving = strafe * strafe + forward * forward > 0f;

        // Smooth head pitch toward half camera pitch when moving
        // Matches modern: xRot = xRotO + degreesDifference(xRotO, cameraXRot * 0.5F) * 0.25F
        // Don't update prevRotationPitch, let vanilla's tick-start prev=current cycle handle it,
        // so the renderer interpolates smoothly between ticks instead of snapping.
        if (isMoving) {
            float targetPitch = cameraPitch * 0.5f;
            player.rotationPitch += degreesDifference(player.rotationPitch, targetPitch)
                * Config.decoupledCameraPlayerTurnSpeed;
        }

        if (freeLooking) {
            // Free look: rotate input so "forward" goes in the freeLookYaw direction
            // Matches modern: moveVector.rotateDegrees(degreesDifference(cameraEntity.getYRot(), freeLookYRot))
            float angle = degreesDifference(player.rotationYaw, freeLookYaw);
            float[] rotated = rotateDegrees(strafe, forward, angle);
            player.moveStrafing = rotated[0];
            player.moveForward = rotated[1];
            return;
        }

        if (!isMoving) {
            return;
        }

        float yRot = player.rotationYaw;

        // Step 1: Rotate raw input by camera yaw to get world-space movement direction
        // Matches modern: Vec2f rotated = moveVector.rotateDegrees(cameraYRot)
        float[] worldMove = rotateDegrees(strafe, forward, cameraYaw);

        // Step 2: Calculate target player yaw from world-space movement
        // Matches modern: yRot = atan2(-rotated.x(), rotated.y()) * RAD_TO_DEG
        float targetYaw = (float) (Math.atan2(-worldMove[0], worldMove[1]) * (180.0 / Math.PI));

        // Step 3: Smooth player yaw toward target
        // Matches modern: yRot = yRotO + degreesDifference(yRotO, yRot) * 0.25F
        float newYaw = yRot + degreesDifference(yRot, targetYaw) * Config.decoupledCameraPlayerTurnSpeed;

        // Update player rotation with proper prev tracking to avoid jitter
        if (player.ridingEntity == null) {
            player.prevRotationYaw += newYaw - player.rotationYaw;
            player.rotationYaw = newYaw;
        }

        // Step 4: Rotate raw input by difference between (new) player yaw and camera yaw
        // Matches modern: moveVector = moveVector.rotateDegrees(degreesDifference(yRot, camera.getYRot()))
        float angle = degreesDifference(player.rotationYaw, cameraYaw);
        float[] result = rotateDegrees(strafe, forward, angle);
        player.moveStrafing = result[0];
        player.moveForward = result[1];
    }

    /**
     * Returns interpolated camera yaw including free look offset.
     */
    public static float getCameraYaw(float partialTicks) {
        float baseYaw = prevCameraYaw + (cameraYaw - prevCameraYaw) * partialTicks;
        float offsetYaw = prevYawOffset + (yawOffset - prevYawOffset) * partialTicks;
        return baseYaw + offsetYaw;
    }

    /**
     * Returns interpolated camera pitch including free look offset.
     */
    public static float getCameraPitch(float partialTicks) {
        float basePitch = prevCameraPitch + (cameraPitch - prevCameraPitch) * partialTicks;
        float offsetPitch = prevPitchOffset + (pitchOffset - prevPitchOffset) * partialTicks;
        return MathHelper.clamp_float(basePitch + offsetPitch, -90f, 90f);
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isFreeLooking() {
        return active && freeLooking;
    }

    /**
     * Raw (non-interpolated) effective camera yaw for entity rotation swapping.
     */
    public static float getEffectiveYaw() {
        return cameraYaw + yawOffset;
    }

    /**
     * Raw (non-interpolated) effective previous camera yaw for entity rotation swapping.
     */
    public static float getEffectivePrevYaw() {
        return prevCameraYaw + prevYawOffset;
    }

    /**
     * Raw (non-interpolated) effective camera pitch for entity rotation swapping.
     */
    public static float getEffectivePitch() {
        return MathHelper.clamp_float(cameraPitch + pitchOffset, -90f, 90f);
    }

    /**
     * Raw (non-interpolated) effective previous camera pitch for entity rotation swapping.
     */
    public static float getEffectivePrevPitch() {
        return MathHelper.clamp_float(prevCameraPitch + prevPitchOffset, -90f, 90f);
    }

    private static void resetState(EntityLivingBase entity) {
        cameraYaw = entity.rotationYaw;
        cameraPitch = entity.rotationPitch;
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        yawOffset = 0f;
        pitchOffset = 0f;
        prevYawOffset = 0f;
        prevPitchOffset = 0f;
        freeLookYaw = cameraYaw;
        freeLooking = false;
    }

    /**
     * Standard 2D rotation: (x', y') = (x*cos - y*sin, x*sin + y*cos)
     * Matches modern Vec2f.rotateDegrees().
     */
    private static float[] rotateDegrees(float x, float y, float degrees) {
        float rad = (float) Math.toRadians(degrees);
        float cos = MathHelper.cos(rad);
        float sin = MathHelper.sin(rad);
        return new float[] { x * cos - y * sin, x * sin + y * cos };
    }

    /**
     * Matches modern Mth.degreesDifference(from, to) = wrapDegrees(to - from).
     */
    private static float degreesDifference(float from, float to) {
        return MathHelper.wrapAngleTo180_float(to - from);
    }
}
