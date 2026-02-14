package org.fentanylsolutions.anextratouch;

import net.minecraftforge.common.MinecraftForge;

import org.fentanylsolutions.anextratouch.commands.CommandReload;
import org.fentanylsolutions.anextratouch.handlers.server.GrassTramplingHandler;
import org.fentanylsolutions.anextratouch.handlers.server.ServerArmorHandler;
import org.fentanylsolutions.anextratouch.handlers.server.ServerHandler;
import org.fentanylsolutions.anextratouch.util.MobUtil;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.loadConfig(AnExtraTouch.confFile);
        ServerArmorHandler.init();
        ServerHandler serverHandler = new ServerHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(serverHandler);
        MinecraftForge.EVENT_BUS.register(serverHandler);
    }

    public void init(FMLInitializationEvent event) {
        GrassTramplingHandler.INSTANCE.initHook();
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Config.printMobNames) {
            MobUtil.printMobNames();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandReload());
    }
}
