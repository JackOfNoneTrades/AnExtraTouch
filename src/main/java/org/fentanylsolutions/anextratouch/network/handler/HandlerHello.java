package org.fentanylsolutions.anextratouch.network.handler;

import org.fentanylsolutions.anextratouch.network.message.MessageHello;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerHello implements IMessageHandler<MessageHello, IMessage> {

    @Override
    public IMessage onMessage(MessageHello message, MessageContext ctx) {
        // Decode-only compatibility with older servers. New servers do not send this packet.
        return null;
    }
}
