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
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return new EarlyMixinLoader.MixinBuilder()
            .addMixin("MixinDSFootsteps", EarlyMixinLoader.Side.CLIENT, "dsurround")
            .addMixin("MixinBlizzSnowTrail", EarlyMixinLoader.Side.BOTH, "ThermalFoundation")
            .build();
    }
}
