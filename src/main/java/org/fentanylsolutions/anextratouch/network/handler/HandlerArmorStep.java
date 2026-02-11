package org.fentanylsolutions.anextratouch.network.handler;

import net.minecraft.client.Minecraft;

import org.fentanylsolutions.anextratouch.handlers.client.StepSoundHandler;
import org.fentanylsolutions.anextratouch.network.message.MessageArmorStep;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerArmorStep implements IMessageHandler<MessageArmorStep, IMessage> {

    @Override
    public IMessage onMessage(final MessageArmorStep message, MessageContext ctx) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> StepSoundHandler.onServerArmorStep(message.getEntityId()));
        return null;
    }
}
