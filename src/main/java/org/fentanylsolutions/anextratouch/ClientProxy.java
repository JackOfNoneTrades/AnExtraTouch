package org.fentanylsolutions.anextratouch;

import net.minecraftforge.common.MinecraftForge;

import org.fentanylsolutions.anextratouch.footsteps.FootprintManager;
import org.fentanylsolutions.anextratouch.handlers.client.ClientHandler;
import org.fentanylsolutions.anextratouch.handlers.client.SmoothGuiHandler;
import org.fentanylsolutions.anextratouch.handlers.client.StepSoundHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.BreathHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.PlayerEffectHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WetParticleHandler;
import org.fentanylsolutions.anextratouch.varinstances.VarInstanceClient;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        AnExtraTouch.vic = new VarInstanceClient();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerEffectHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new StepSoundHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(FootprintManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new SmoothGuiHandler());
        MinecraftForge.EVENT_BUS.register(new BreathHandler());
        MinecraftForge.EVENT_BUS.register(new WetParticleHandler());
        MinecraftForge.EVENT_BUS.register(FootprintManager.INSTANCE);
        ClientHandler clientHandler = new ClientHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(clientHandler);
        MinecraftForge.EVENT_BUS.register(clientHandler);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        AnExtraTouch.vic.postInitHook();
    }

}
