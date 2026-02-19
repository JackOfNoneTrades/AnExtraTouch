package org.fentanylsolutions.anextratouch.network.handler;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.camera.ScreenShakeManager;
import org.fentanylsolutions.anextratouch.network.message.MessageExplosionShake;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class HandlerExplosionShake implements IMessageHandler<MessageExplosionShake, IMessage> {

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(final MessageExplosionShake message, MessageContext ctx) {

        if (Config.cameraOverhaulEnabled) {
            ScreenShakeManager.Slot shake = ScreenShakeManager.createDirect();
            shake.position.set(message.posX, message.posY, message.posZ);
            shake.radius = message.size * 10f;
            shake.trauma = Config.cameraExplosionTrauma;
            shake.lengthInSeconds = Config.cameraExplosionLength;
        }

        return null;
    }

}
