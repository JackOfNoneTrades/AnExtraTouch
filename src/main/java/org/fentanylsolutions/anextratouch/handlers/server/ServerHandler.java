package org.fentanylsolutions.anextratouch.handlers.server;

import net.minecraft.entity.player.EntityPlayerMP;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.network.NetworkHandler;
import org.fentanylsolutions.anextratouch.network.message.MessageHello;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ServerHandler {

    @SubscribeEvent
    public void onPlayerLogin(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        EntityPlayerMP player = (EntityPlayerMP) e.player;
        AnExtraTouch.debug("Sending hello packet to " + player.getDisplayName());
        NetworkHandler.channel.sendTo(new MessageHello(), player);
    }
}
