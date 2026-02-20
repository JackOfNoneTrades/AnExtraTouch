package org.fentanylsolutions.anextratouch.handlers.client.camera;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.compat.DbcAimingCompat;
import org.fentanylsolutions.anextratouch.compat.ShoulderSurfingCompat;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DecoupledCameraHandler {

    public static final KeyBinding FREE_LOOK_KEY = new KeyBinding(
        "key.anextratouch.free_look",
        Keyboard.KEY_LMENU,
        "key.categories.anextratouch");

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

    // Turning toward interaction target
    private static int turningLockTicks;

    // Keeps sprint active while decoupled movement is still valid.
    private static boolean decoupledSprintLatched;
    private static int forwardTapTimer;
    private static int backTapTimer;
    private static int leftTapTimer;
    private static int rightTapTimer;
    private static boolean wasForwardDown;
    private static boolean wasBackDown;
    private static boolean wasLeftDown;
    private static boolean wasRightDown;

    // Aiming state (bow draw, etc.) - player rotation follows camera
    private static boolean aiming;

    // Aim-to-first-person transition
    private static float aimTransition; // 0.0 = third person, 1.0 = first person
    private static float prevAimTransition;
    private static boolean aimFirstPersonActive; // true when thirdPersonView has been switched to 0
    private static int savedThirdPersonView = -1;

    // Camera world position (extracted from GL modelview matrix after orientCamera)
    private static final FloatBuffer MODELVIEW_BUFFER = BufferUtils.createFloatBuffer(16);
    private static double cameraWorldX, cameraWorldY, cameraWorldZ;
    private static boolean cameraPositionValid;

    // Camera-to-entity distance for player fade
    private static float cameraEntityDistance = Float.MAX_VALUE;

    // Sound centering: camera world state stored during orientCamera for the next frame's setListener
    private static float soundCamX, soundCamY, soundCamZ;
    private static float soundCamYaw, soundCamPitch;
    private static boolean soundListenerReady;

    // Entity tracking for reset
    private static int lastEntityId = Integer.MIN_VALUE;

    private DecoupledCameraHandler() {}

    public static void registerKeybinding() {
        ClientRegistry.registerKeyBinding(FREE_LOOK_KEY);
    }

    /**
     * Called from MixinEntityRenderer orientCamera RETURN to extract the camera's
     * world position from the GL modelview matrix. The GL matrix is entity-relative,
     * so we add the entity's interpolated position to get world coordinates.
     * This accounts for ShoulderSurfing's shoulder offset so aim raytraces
     * originate from the actual camera position.
     */
    public static void updateCameraPosition(float partialTicks) {
        EntityLivingBase entity = Minecraft.getMinecraft().renderViewEntity;
        if (entity == null) return;

        MODELVIEW_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_BUFFER);

        // Camera position relative to entity render origin = -R^T * t (column-major layout)
        float m0 = MODELVIEW_BUFFER.get(0), m1 = MODELVIEW_BUFFER.get(1), m2 = MODELVIEW_BUFFER.get(2);
        float m4 = MODELVIEW_BUFFER.get(4), m5 = MODELVIEW_BUFFER.get(5), m6 = MODELVIEW_BUFFER.get(6);
        float m8 = MODELVIEW_BUFFER.get(8), m9 = MODELVIEW_BUFFER.get(9), m10 = MODELVIEW_BUFFER.get(10);
        float m12 = MODELVIEW_BUFFER.get(12), m13 = MODELVIEW_BUFFER.get(13), m14 = MODELVIEW_BUFFER.get(14);

        double relX = -(m0 * m12 + m1 * m13 + m2 * m14);
        double relY = -(m4 * m12 + m5 * m13 + m6 * m14);
        double relZ = -(m8 * m12 + m9 * m13 + m10 * m14);

        // Convert to world coordinates using entity's interpolated render position
        cameraWorldX = relX + entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        cameraWorldY = relY + entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        cameraWorldZ = relZ + entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        cameraPositionValid = true;
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

        boolean shouldBeActive = Config.decoupledCameraEnabled && (ShoulderSurfingCompat.isShoulderSurfingActive()
            || (!ShoulderSurfingCompat.isAvailable() && mc.gameSettings.thirdPersonView > 0)
            || aimFirstPersonActive
            || aimTransition > 0f);

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
            decoupledSprintLatched = false;
            return;
        }

        // Store previous tick values for interpolation
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        prevYawOffset = yawOffset;
        prevPitchOffset = pitchOffset;

        // Update free look state
        freeLooking = FREE_LOOK_KEY.getKeyCode() != 0 && Keyboard.isKeyDown(FREE_LOOK_KEY.getKeyCode());

        // Disable free look while aiming (matches modern: isFreeLooking = FREE_LOOK.isDown() && !isAiming)
        if (aiming) {
            freeLooking = false;
        }

        // When not free-looking, store freeLookYaw and decay offsets
        // Matches modern ShoulderSurfingCamera.tick() lines 98-103
        if (!freeLooking) {
            freeLookYaw = cameraYaw;
            yawOffset *= Config.decoupledCameraOffsetDecay;
            pitchOffset *= Config.decoupledCameraOffsetDecay;
        }

        // Turning toward interaction target
        // Ported from modern ShoulderSurfingImpl.tick() + EntityHelper.lookAtTarget()
        if (turningLockTicks > 0) {
            turningLockTicks--;
        }

        if (!freeLooking && !aiming && !aimFirstPersonActive && entity instanceof EntityPlayerSP) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            boolean acting = mc.gameSettings.keyBindAttack.getIsKeyPressed()
                || mc.gameSettings.keyBindUseItem.getIsKeyPressed();

            if (acting && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.MISS
                && mc.objectMouseOver.hitVec != null) {
                lookAtTarget(player, mc.objectMouseOver.hitVec);
                turningLockTicks = Config.decoupledCameraTurningLockTicks;
            }
        }

        // Aim-to-first-person transition
        prevAimTransition = aimTransition;
        float target = (aiming && Config.decoupledCameraAimFirstPerson) ? 1.0f : 0.0f;
        float speed = 1.0f / Math.max(1, Config.decoupledCameraAimTransitionTicks);
        if (aimTransition < target) {
            aimTransition = Math.min(aimTransition + speed, target);
        } else if (aimTransition > target) {
            aimTransition = Math.max(aimTransition - speed, target);
        }

        // Handle first-person view switch
        boolean wasAimFP = aimFirstPersonActive;
        aimFirstPersonActive = aimTransition >= 1.0f && Config.decoupledCameraAimFirstPerson && aiming;

        if (aimFirstPersonActive && !wasAimFP) {
            savedThirdPersonView = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 0;
        } else if (!aimFirstPersonActive && wasAimFP) {
            mc.gameSettings.thirdPersonView = savedThirdPersonView;
            savedThirdPersonView = -1;
            // Re-enable shoulder surfing (SS disabled itself when thirdPersonView changed to 0)
            ShoulderSurfingCompat.setShoulderSurfing(true);
            // Sync camera to player rotation from first person
            if (entity != null) {
                cameraYaw = entity.rotationYaw;
                cameraPitch = entity.rotationPitch;
                prevCameraYaw = cameraYaw;
                prevCameraPitch = cameraPitch;
            }
        }

        // While in aim FP, continuously sync camera to player rotation
        if (aimFirstPersonActive && entity != null) {
            cameraYaw = entity.rotationYaw;
            cameraPitch = entity.rotationPitch;
            prevCameraYaw = cameraYaw;
            prevCameraPitch = cameraPitch;
        }

    }

    /**
     * Called from Entity.setAngles mixin. Returns true to cancel vanilla rotation.
     * Ported from ShoulderSurfingCamera.turn().
     *
     * In 1.7.10, Entity.setAngles receives raw sensitivity-scaled deltas and applies * 0.15 internally.
     * We replicate that same scaling, matching the sign conventions:
     * vanilla: rotationYaw += yaw * 0.15; rotationPitch -= pitch * 0.15;
     */
    public static boolean onSetAngles(float yaw, float pitch) {
        if (!active) {
            return false;
        }
        if (aimFirstPersonActive) {
            return false; // vanilla FP handles mouse
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

        // When aiming, immediately sync player rotation toward crosshair target.
        // Matches modern turn() lines 429-434 + lookAtCrosshairTargetInternal().
        // Uses parallax-corrected direction (player→hitVec) so arrows hit where the
        // crosshair points despite the camera's shoulder offset.
        if (aiming) {
            EntityLivingBase entity = Minecraft.getMinecraft().renderViewEntity;
            if (entity != null) {
                float[] aim = computeAimRotation(entity);
                entity.prevRotationYaw += MathHelper.wrapAngleTo180_float(aim[0] - entity.rotationYaw);
                entity.prevRotationPitch += aim[1] - entity.rotationPitch;
                entity.rotationYaw = aim[0];
                entity.rotationPitch = aim[1];
            }
        }

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
        boolean directionalDoubleTap = detectDirectionalDoubleTap();

        // Aiming detection - when using a bow etc., sync player rotation to camera
        // so projectiles fire where the crosshair points.
        // Computed here (before arrow creation in the same tick) for correct timing.
        boolean wasAiming = aiming;
        aiming = computeAiming(player);

        if (!wasAiming && aiming) {
            // Aiming just started - set turning lock so body doesn't snap away on release
            turningLockTicks = Config.decoupledCameraTurningLockTicks;
        }
        if (wasAiming && !aiming) {
            turningLockTicks = Config.decoupledCameraTurningLockTicks;
        }

        // When in full first-person aim mode, vanilla handles everything
        if (aimFirstPersonActive) {
            applyDecoupledSprint(player, false, directionalDoubleTap);
            return;
        }

        if (aiming) {
            // Sync player rotation toward crosshair target (parallax-corrected).
            // Arrow/projectile creation uses player.rotationYaw/Pitch directly.
            float[] aim = computeAimRotation(player);
            player.rotationYaw = aim[0];
            player.rotationPitch = aim[1];
            // No movement input rotation needed - player yaw already matches aim direction
            applyDecoupledSprint(
                player,
                player.moveStrafing * player.moveStrafing + player.moveForward * player.moveForward > 0f,
                directionalDoubleTap);
            return;
        }

        float strafe = player.moveStrafing;
        float forward = player.moveForward;
        boolean isMoving = strafe * strafe + forward * forward > 0f;
        boolean turningLocked = turningLockTicks > 0;

        // Smooth head pitch toward half camera pitch when moving
        // Matches modern: xRot = xRotO + degreesDifference(xRotO, cameraXRot * 0.5F) * 0.25F
        // Don't update prevRotationPitch, let vanilla's tick-start prev=current cycle handle it,
        // so the renderer interpolates smoothly between ticks instead of snapping.
        // Skip when turning is locked - lookAtTarget already set the pitch.
        if (isMoving && !turningLocked) {
            float targetPitch = cameraPitch * 0.5f;
            float basePitch = player.prevRotationPitch;
            player.rotationPitch = basePitch
                + degreesDifference(basePitch, targetPitch) * Config.decoupledCameraPlayerTurnSpeed;
        }

        if (freeLooking) {
            // Free look: rotate input so "forward" goes in the freeLookYaw direction
            // Matches modern: moveVector.rotateDegrees(degreesDifference(cameraEntity.getYRot(), freeLookYRot))
            float angle = degreesDifference(player.rotationYaw, freeLookYaw);
            float[] rotated = rotateDegrees(strafe, forward, angle);
            player.moveStrafing = rotated[0];
            player.moveForward = rotated[1];
            applyDecoupledSprint(player, isMoving, directionalDoubleTap);
            return;
        }

        if (!isMoving) {
            applyDecoupledSprint(player, false, directionalDoubleTap);
            return;
        }

        // When turning is locked (player facing interaction target), skip body yaw rotation
        // but still rotate movement input to be camera-relative (step 4)
        if (!turningLocked) {
            float yRot = player.prevRotationYaw;

            // Step 1: Rotate raw input by camera yaw to get world-space movement direction
            // Matches modern: Vec2f rotated = moveVector.rotateDegrees(cameraYRot)
            float[] worldMove = rotateDegrees(strafe, forward, cameraYaw);

            // Step 2: Calculate target player yaw from world-space movement
            // Matches modern: yRot = atan2(-rotated.x(), rotated.y()) * RAD_TO_DEG
            float targetYaw = (float) (Math.atan2(-worldMove[0], worldMove[1]) * (180.0 / Math.PI));

            // Step 3: Smooth player yaw toward target
            // Matches modern: yRot = yRotO + degreesDifference(yRotO, yRot) * 0.25F
            float newYaw = yRot + degreesDifference(yRot, targetYaw) * Config.decoupledCameraPlayerTurnSpeed;

            // Let vanilla keep prevRotationYaw from tick start for smooth interpolation.
            if (player.ridingEntity == null) {
                player.rotationYaw = newYaw;
            }
        }

        // Step 4: Rotate raw input by difference between (new) player yaw and camera yaw
        // Matches modern: moveVector = moveVector.rotateDegrees(degreesDifference(yRot, camera.getYRot()))
        float angle = degreesDifference(player.rotationYaw, cameraYaw);
        float[] result = rotateDegrees(strafe, forward, angle);
        player.moveStrafing = result[0];
        player.moveForward = result[1];
        applyDecoupledSprint(player, true, directionalDoubleTap);
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

    public static boolean isTurningLocked() {
        return active && turningLockTicks > 0;
    }

    public static boolean isAiming() {
        return active && aiming;
    }

    public static boolean isAimFirstPerson() {
        return aimFirstPersonActive;
    }

    /**
     * Called from MixinEntityRenderer after all GL camera manipulations to store
     * the current camera-to-entity distance for player fade calculations.
     */
    public static void updateCameraEntityDistance(float distance) {
        cameraEntityDistance = distance;
    }

    /**
     * Returns the alpha value for rendering the view entity based on camera proximity.
     * 1.0 = fully opaque, 0.0 = fully invisible.
     */
    public static float getPlayerAlpha() {
        if (!Config.cameraPlayerFadeEnabled) return 1f;
        float startDist = Config.cameraPlayerFadeStartDistance;
        float endDist = Config.cameraPlayerFadeEndDistance;
        if (startDist <= endDist) return 1f;
        if (cameraEntityDistance >= startDist) return 1f;
        if (cameraEntityDistance <= endDist) return 0f;
        return (cameraEntityDistance - endDist) / (startDist - endDist);
    }

    /**
     * Called from MixinEntityRenderer orientCamera RETURN (before rotation restore) to store
     * the camera world position and orientation for sound centering on the next frame.
     * Runs for ANY third person view, not just when the decoupled camera is active.
     * The entity's rotation at this point equals the camera direction (due to HEAD swap in
     * decoupled mode, or naturally in vanilla third person).
     */
    public static void updateSoundListener(float partialTicks, EntityLivingBase entity) {
        soundListenerReady = false;
        if (entity == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.thirdPersonView == 0 && !aimFirstPersonActive && aimTransition <= 0f) return;

        MODELVIEW_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_BUFFER);

        float m0 = MODELVIEW_BUFFER.get(0), m1 = MODELVIEW_BUFFER.get(1), m2 = MODELVIEW_BUFFER.get(2);
        float m4 = MODELVIEW_BUFFER.get(4), m5 = MODELVIEW_BUFFER.get(5), m6 = MODELVIEW_BUFFER.get(6);
        float m8 = MODELVIEW_BUFFER.get(8), m9 = MODELVIEW_BUFFER.get(9), m10 = MODELVIEW_BUFFER.get(10);
        float m12 = MODELVIEW_BUFFER.get(12), m13 = MODELVIEW_BUFFER.get(13), m14 = MODELVIEW_BUFFER.get(14);

        double relX = -(m0 * m12 + m1 * m13 + m2 * m14);
        double relY = -(m4 * m12 + m5 * m13 + m6 * m14);
        double relZ = -(m8 * m12 + m9 * m13 + m10 * m14);

        soundCamX = (float) (relX + entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks);
        soundCamY = (float) (relY + entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks);
        soundCamZ = (float) (relZ + entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks);
        soundCamYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        soundCamPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        soundListenerReady = true;
    }

    public static boolean isSoundListenerReady() {
        return soundListenerReady;
    }

    public static float getSoundCamX() {
        return soundCamX;
    }

    public static float getSoundCamY() {
        return soundCamY;
    }

    public static float getSoundCamZ() {
        return soundCamZ;
    }

    public static float getSoundCamYaw() {
        return soundCamYaw;
    }

    public static float getSoundCamPitch() {
        return soundCamPitch;
    }

    /**
     * Applies the aim-to-first-person camera transition by modifying the GL modelview matrix.
     * Smoothly moves the camera from its third-person position toward the entity eye (GL origin).
     * Called from MixinEntityRenderer after orientCamera and camera overhaul.
     */
    public static void applyAimTransition(float partialTicks, EntityLivingBase entity) {
        if (!Config.decoupledCameraAimFirstPerson) return;
        if (aimFirstPersonActive) return; // fully in vanilla FP, no GL manipulation needed

        float t = prevAimTransition + (aimTransition - prevAimTransition) * partialTicks;
        t = applyEasing(t, Config.decoupledCameraAimTransitionEasing);
        if (t < 0.001f) return;

        float eyeH = entity.yOffset - 1.62f;

        MODELVIEW_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_BUFFER);

        float m4 = MODELVIEW_BUFFER.get(4), m5 = MODELVIEW_BUFFER.get(5), m6 = MODELVIEW_BUFFER.get(6);
        float m12 = MODELVIEW_BUFFER.get(12), m13 = MODELVIEW_BUFFER.get(13), m14 = MODELVIEW_BUFFER.get(14);

        // Eye position in view space: M * (0, eyeH, 0, 1)
        float eyeViewX = m4 * eyeH + m12;
        float eyeViewY = m5 * eyeH + m13;
        float eyeViewZ = m6 * eyeH + m14;

        // Translate view so camera moves toward eye by factor t
        MODELVIEW_BUFFER.put(12, m12 - eyeViewX * t);
        MODELVIEW_BUFFER.put(13, m13 - eyeViewY * t);
        MODELVIEW_BUFFER.put(14, m14 - eyeViewZ * t);

        MODELVIEW_BUFFER.position(0);
        GL11.glLoadMatrix(MODELVIEW_BUFFER);
    }

    private static float applyEasing(float t, String easing) {
        switch (easing) {
            case "ease_in":
                return t * t;
            case "ease_out":
                return 1f - (1f - t) * (1f - t);
            case "ease_in_out":
                return t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
            case "smooth":
                return t * t * (3f - 2f * t);
            default:
                return t;
        }
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

    /**
     * Computes aim rotation so a projectile fired straight from the player's eye
     * hits where the crosshair points. Raytraces from the camera's world position
     * (accounting for SS shoulder offset) along the camera direction, then computes
     * yaw/pitch from the player's eye to the hit point. All in world coordinates.
     * Ported from modern ShoulderSurfingImpl.lookAtCrosshairTargetInternal().
     */
    private static float[] computeAimRotation(EntityLivingBase entity) {
        float effYaw = cameraYaw + yawOffset;
        float effPitch = MathHelper.clamp_float(cameraPitch + pitchOffset, -90f, 90f);

        Minecraft mc = Minecraft.getMinecraft();
        if (cameraPositionValid && mc.theWorld != null) {
            // Camera look direction (matches Entity.getLook() math)
            float yawRad = effYaw * 0.017453292f;
            float pitchRad = effPitch * 0.017453292f;
            float f1 = MathHelper.cos(-yawRad - (float) Math.PI);
            float f2 = MathHelper.sin(-yawRad - (float) Math.PI);
            float f3 = -MathHelper.cos(-pitchRad);
            float f4 = MathHelper.sin(-pitchRad);

            double reach = 256.0;
            Vec3 start = Vec3.createVectorHelper(cameraWorldX, cameraWorldY, cameraWorldZ);
            Vec3 end = Vec3.createVectorHelper(
                cameraWorldX + f2 * f3 * reach,
                cameraWorldY + f4 * reach,
                cameraWorldZ + f1 * f3 * reach);

            MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(start, end);
            if (hit != null && hit.hitVec != null) {
                // Direction from player eye to hit point (both world coords)
                double eyeY = entity.posY + entity.getEyeHeight();
                double dx = hit.hitVec.xCoord - entity.posX;
                double dy = hit.hitVec.yCoord - eyeY;
                double dz = hit.hitVec.zCoord - entity.posZ;

                // Validate that the hit is roughly in front of the camera direction.
                // When the camera is inside a block (e.g. looking steeply up in 3rd person),
                // rayTraceBlocks returns a hit behind/below the player, causing aim to flip.
                double camDirX = f2 * f3;
                double camDirY = f4;
                double camDirZ = f1 * f3;
                double dot = dx * camDirX + dy * camDirY + dz * camDirZ;
                if (dot < 0) {
                    return new float[] { effYaw, effPitch };
                }

                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (totalDist < 0.001) return new float[] { effYaw, effPitch };

                float computedYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                float pitch = MathHelper
                    .clamp_float((float) (-(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI))), -90.0f, 90.0f);

                // At steep angles the yaw from atan2 becomes degenerate (gimbal lock).
                // Blend toward camera yaw proportionally to how vertical the direction is.
                float horizRatio = (float) (horizontalDist / totalDist);
                float blend = Math.min(horizRatio * 5f, 1f);
                float yaw = effYaw + degreesDifference(effYaw, computedYaw) * blend;
                return new float[] { yaw, pitch };
            }
        }

        // No hit (sky) or no camera position yet use camera direction
        return new float[] { effYaw, effPitch };
    }

    /**
     * Checks if the player is aiming. Two modes:
     * 1. Currently using an item whose EnumAction matches the actions list (e.g. bow draw)
     * 2. Holding an item whose registry name matches the items list (e.g. snowball, ender pearl)
     * These couple while held so instant-throw projectiles fire at the crosshair.
     */
    private static boolean computeAiming(EntityPlayerSP player) {
        if (DbcAimingCompat.shouldRecouple(player)) {
            return true;
        }

        // Check sustained-use items by EnumAction (bow draw, etc.)
        if (player.isUsingItem()) {
            ItemStack itemInUse = player.getItemInUse();
            if (itemInUse != null) {
                String actionName = itemInUse.getItemUseAction()
                    .name();
                for (String action : Config.decoupledCameraAimingActions) {
                    if (actionName.equalsIgnoreCase(action)) {
                        return true;
                    }
                }
            }
        }

        // Check held item by registry name (instant-throw items like snowball, egg, etc.)
        ItemStack held = player.getHeldItem();
        if (held != null) {
            if (matchesItemSpec(held, Config.decoupledCameraAimingItems)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an ItemStack matches any entry in the item spec list.
     * Supports formats: "modid:name" (any meta), "modid:name@N" (exact meta),
     * "modid:name@N-M" (meta range inclusive).
     */
    private static boolean matchesItemSpec(ItemStack stack, String[] specs) {
        String registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        if (registryName == null) return false;
        int meta = stack.getItemDamage();

        for (String spec : specs) {
            int atIdx = spec.indexOf('@');
            if (atIdx < 0) {
                // No meta spec - match any meta
                if (registryName.equals(spec)) return true;
            } else {
                // Has meta spec - check registry name first
                if (!registryName.equals(spec.substring(0, atIdx))) continue;
                String metaSpec = spec.substring(atIdx + 1);
                int dashIdx = metaSpec.indexOf('-');
                try {
                    if (dashIdx < 0) {
                        // Exact meta: "modid:name@N"
                        if (meta == Integer.parseInt(metaSpec)) return true;
                    } else {
                        // Range: "modid:name@N-M"
                        int min = Integer.parseInt(metaSpec.substring(0, dashIdx));
                        int max = Integer.parseInt(metaSpec.substring(dashIdx + 1));
                        if (meta >= min && meta <= max) return true;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }

    /**
     * Rotates the player to look at the given target position.
     * Ported from modern EntityHelper.lookAtTarget().
     * Does NOT update prev rotation, so the renderer interpolates smoothly.
     */
    private static void lookAtTarget(EntityPlayerSP player, Vec3 hitVec) {
        double dx = hitVec.xCoord - player.posX;
        double dy = hitVec.yCoord - (player.posY + player.getEyeHeight());
        double dz = hitVec.zCoord - player.posZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (totalDist < 0.001) return;

        float computedYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        // At steep angles, blend yaw toward current to avoid gimbal lock jitter
        float horizRatio = (float) (horizontalDist / totalDist);
        float blend = Math.min(horizRatio * 5f, 1f);
        player.rotationYaw = player.rotationYaw + degreesDifference(player.rotationYaw, computedYaw) * blend;
        player.rotationPitch = MathHelper
            .clamp_float((float) (-(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI))), -90.0f, 90.0f);
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
        turningLockTicks = 0;
        aiming = false;
        decoupledSprintLatched = false;
        forwardTapTimer = backTapTimer = leftTapTimer = rightTapTimer = 0;
        wasForwardDown = wasBackDown = wasLeftDown = wasRightDown = false;
        cameraPositionValid = false;
        cameraEntityDistance = Float.MAX_VALUE;
        aimTransition = 0f;
        prevAimTransition = 0f;
        if (aimFirstPersonActive && savedThirdPersonView >= 0) {
            Minecraft.getMinecraft().gameSettings.thirdPersonView = savedThirdPersonView;
            ShoulderSurfingCompat.setShoulderSurfing(true);
        }
        aimFirstPersonActive = false;
        savedThirdPersonView = -1;
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

    private static void applyDecoupledSprint(EntityPlayerSP player, boolean hasMovementInput, boolean doubleTapped) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            decoupledSprintLatched = false;
            return;
        }

        boolean sprintKeyDown = mc.gameSettings.keyBindSprint.getIsKeyPressed();
        boolean canSprint = ((float) player.getFoodStats()
            .getFoodLevel() > 6.0F || player.capabilities.allowFlying) && !player.isUsingItem()
            && !player.isPotionActive(Potion.blindness)
            && !player.movementInput.sneak
            && !player.isRiding()
            && !player.isCollidedHorizontally;

        if (!hasMovementInput || !canSprint) {
            decoupledSprintLatched = false;
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            return;
        }

        // Respect vanilla starts (double-tap forward) and extend them to decoupled A/S/D movement.
        if (sprintKeyDown || doubleTapped || decoupledSprintLatched || player.isSprinting()) {
            decoupledSprintLatched = true;
            if (!player.isSprinting()) {
                player.setSprinting(true);
            }
        }
    }

    private static boolean detectDirectionalDoubleTap() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return false;
        }

        if (forwardTapTimer > 0) forwardTapTimer--;
        if (backTapTimer > 0) backTapTimer--;
        if (leftTapTimer > 0) leftTapTimer--;
        if (rightTapTimer > 0) rightTapTimer--;

        boolean forwardDown = mc.gameSettings.keyBindForward.getIsKeyPressed();
        boolean backDown = mc.gameSettings.keyBindBack.getIsKeyPressed();
        boolean leftDown = mc.gameSettings.keyBindLeft.getIsKeyPressed();
        boolean rightDown = mc.gameSettings.keyBindRight.getIsKeyPressed();

        boolean doubleTap = false;
        if (forwardDown && !wasForwardDown) {
            doubleTap |= forwardTapTimer > 0;
            forwardTapTimer = 7;
        }
        if (backDown && !wasBackDown) {
            doubleTap |= backTapTimer > 0;
            backTapTimer = 7;
        }
        if (leftDown && !wasLeftDown) {
            doubleTap |= leftTapTimer > 0;
            leftTapTimer = 7;
        }
        if (rightDown && !wasRightDown) {
            doubleTap |= rightTapTimer > 0;
            rightTapTimer = 7;
        }

        wasForwardDown = forwardDown;
        wasBackDown = backDown;
        wasLeftDown = leftDown;
        wasRightDown = rightDown;
        return doubleTap;
    }
}
