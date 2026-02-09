package org.fentanylsolutions.anextratouch;

import org.fentanylsolutions.anextratouch.serverhandlers.GrassTramplingHandler;
import org.fentanylsolutions.anextratouch.util.MobUtil;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.loadConfig(AnExtraTouch.confFile);
    }

    public void init(FMLInitializationEvent event) {
        GrassTramplingHandler.INSTANCE.initHook();
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Config.printMobNames) {
            MobUtil.printMobNames();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}
