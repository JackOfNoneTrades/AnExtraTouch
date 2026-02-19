package org.fentanylsolutions.anextratouch.core;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

public class MixinBuilder {

    public enum Side {
        CLIENT,
        SERVER,
        BOTH
    }

    public static boolean isServer() {
        return FMLLaunchHandler.side()
            .isServer();
    }

    private final List<String> mixins = new ArrayList<>();

    public MixinBuilder addMixin(String name, Side side, String modid) {
        if ((side == Side.CLIENT && isServer()) || (side == Side.SERVER && !isServer())) {
            return this;
        }
        if (!modid.equals("minecraft") && !Loader.isModLoaded(modid)) {
            return this;
        }

        mixins.add(modid + "." + name);
        return this;
    }

    public MixinBuilder addMixin(String name, Side side) {
        return addMixin(name, side, "minecraft");
    }

    public List<String> build() {
        return mixins;
    }

}
