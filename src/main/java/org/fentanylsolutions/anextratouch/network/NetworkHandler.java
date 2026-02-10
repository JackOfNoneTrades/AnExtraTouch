package org.fentanylsolutions.anextratouch.network;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.network.handler.HandlerHello;
import org.fentanylsolutions.anextratouch.network.message.MessageHello;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NetworkHandler {

    private static int discriminator = 0;
    public static final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(AnExtraTouch.MODID);

    public static void init() {
        channel.registerMessage(HandlerHello.class, MessageHello.class, discriminator++, Side.CLIENT);
    }
}
