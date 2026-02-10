package org.fentanylsolutions.anextratouch.handlers.client;

import org.fentanylsolutions.anextratouch.AnExtraTouch;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientHandler {

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        AnExtraTouch.vic.serverHasAET = false;
    }
}
