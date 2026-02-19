package org.fentanylsolutions.anextratouch.core;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@SuppressWarnings("unused")
@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.anextratouch.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return new MixinBuilder().addMixin("MixinDSFootsteps", MixinBuilder.Side.CLIENT, "dsurround")
            .addMixin("MixinBlizzSnowTrail", MixinBuilder.Side.BOTH, "ThermalFoundation")
            .addMixin("MixinExplosionNT", MixinBuilder.Side.BOTH, "hbm")
            .addMixin("MixinExplosionVNT", MixinBuilder.Side.BOTH, "hbm")
            .addMixin("MixinRenderTorex", MixinBuilder.Side.CLIENT, "hbm")
            .build();
    }
}
