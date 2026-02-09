package org.fentanylsolutions.anextratouch;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.anextratouch.varinstances.VarInstanceClient;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = AnExtraTouch.MODID,
    version = Tags.VERSION,
    name = "An Extra Touch",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    guiFactory = "org.fentanylsolutions.anextratouch.gui.GuiFactory")
public class AnExtraTouch {

    public static final String MODID = "anextratouch";
    public static final String MODGROUP = "org.fentanylsolutions";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static boolean DEBUG_MODE;
    public static File confFile;
    public static VarInstanceClient vic;

    @SidedProxy(
        clientSide = MODGROUP + "." + MODID + ".ClientProxy",
        serverSide = MODGROUP + "." + MODID + ".CommonProxy")
    public static org.fentanylsolutions.anextratouch.CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        String debugVar = System.getenv("MCMODDING_DEBUG_MODE");
        DEBUG_MODE = debugVar != null;
        AnExtraTouch.LOG.info("Debugmode: {}", DEBUG_MODE);
        confFile = event.getSuggestedConfigurationFile();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    public static void debug(String message) {
        if (DEBUG_MODE || Config.debugMode) {
            LOG.info("DEBUG: {}", message);
        }
    }

    public static boolean isDebug() {
        return DEBUG_MODE || Config.debugMode;
    }

    // MAYBEDO
    // Make blazes melt shit below
    // Make blizzes freeze shit below
}
