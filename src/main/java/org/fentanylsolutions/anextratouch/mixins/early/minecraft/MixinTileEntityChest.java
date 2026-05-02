package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.tileentity.TileEntityChest;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.effects.ChestBubbleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityChest.class)
public class MixinTileEntityChest {

    @Unique
    private int anextratouch$ticksUntilNextBubbleSwitch = 20;

    @Unique
    private boolean anextratouch$autoBubbleOpen;

    @Unique
    private boolean anextratouch$fakeViewerApplied;

    @Unique
    private int anextratouch$realViewers;

    @Inject(method = "updateEntity", at = @At("HEAD"))
    private void anextratouch$updateSoulSandBubbles(CallbackInfo ci) {
        TileEntityChest chest = (TileEntityChest) (Object) this;
        if (!chest.hasWorldObj() || !chest.getWorldObj().isRemote) {
            return;
        }

        if (!Config.soulSandChestBubblesEnabled || !ChestBubbleManager.canRunSoulSandBubbles(chest)) {
            this.anextratouch$autoBubbleOpen = false;
            this.anextratouch$ticksUntilNextBubbleSwitch = 20;
            anextratouch$closeFakeViewer(chest);
            return;
        }

        if (--this.anextratouch$ticksUntilNextBubbleSwitch <= 0) {
            if (this.anextratouch$autoBubbleOpen) {
                this.anextratouch$autoBubbleOpen = false;
                this.anextratouch$ticksUntilNextBubbleSwitch = anextratouch$randomBetween(chest, 20 * 8, 20 * 24);
            } else {
                this.anextratouch$autoBubbleOpen = true;
                this.anextratouch$ticksUntilNextBubbleSwitch = anextratouch$randomBetween(chest, 20 * 2, 20 * 3);
                ChestBubbleManager.playVentOpenSound(chest);
            }
        }

        if (this.anextratouch$autoBubbleOpen) {
            anextratouch$openFakeViewer(chest);
        } else {
            anextratouch$closeFakeViewer(chest);
        }
    }

    @Inject(method = "updateEntity", at = @At("TAIL"))
    private void anextratouch$emitSoulSandBubbles(CallbackInfo ci) {
        TileEntityChest chest = (TileEntityChest) (Object) this;
        if (!chest.hasWorldObj() || !chest.getWorldObj().isRemote || !Config.soulSandChestBubblesEnabled) {
            return;
        }

        if (this.anextratouch$autoBubbleOpen && chest.lidAngle > 0.0F
            && this.anextratouch$ticksUntilNextBubbleSwitch > 10
            && this.anextratouch$ticksUntilNextBubbleSwitch % 2 == 0
            && ChestBubbleManager.canRunSoulSandBubbles(chest)) {
            ChestBubbleManager.spawnVentBubbles(chest);
        }
    }

    @Unique
    private int anextratouch$randomBetween(TileEntityChest chest, int min, int max) {
        return min + chest.getWorldObj().rand.nextInt(max - min + 1);
    }

    @Unique
    private void anextratouch$openFakeViewer(TileEntityChest chest) {
        if (!this.anextratouch$fakeViewerApplied) {
            this.anextratouch$fakeViewerApplied = true;
        }
        chest.numPlayersUsing = this.anextratouch$realViewers + 1;
    }

    @Unique
    private void anextratouch$closeFakeViewer(TileEntityChest chest) {
        if (this.anextratouch$fakeViewerApplied) {
            this.anextratouch$fakeViewerApplied = false;
            chest.numPlayersUsing = this.anextratouch$realViewers;
        }
    }

    @Inject(method = "receiveClientEvent", at = @At("TAIL"))
    private void anextratouch$trackRealViewers(int id, int type, CallbackInfoReturnable<Boolean> cir) {
        if (id == 1 && cir.getReturnValue()) {
            this.anextratouch$realViewers = type;
            TileEntityChest chest = (TileEntityChest) (Object) this;
            if (this.anextratouch$fakeViewerApplied) {
                chest.numPlayersUsing = type + 1;
            }
        }
    }
}
