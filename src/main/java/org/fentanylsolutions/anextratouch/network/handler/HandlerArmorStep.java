package org.fentanylsolutions.anextratouch.network.handler;

import org.fentanylsolutions.anextratouch.network.message.MessageArmorStep;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerArmorStep implements IMessageHandler<MessageArmorStep, IMessage> {

    @Override
    public IMessage onMessage(MessageArmorStep message, MessageContext ctx) {
        // Decode-only compatibility with older servers. Past Footsteps owns movement accents.
        return null;
    }
}
