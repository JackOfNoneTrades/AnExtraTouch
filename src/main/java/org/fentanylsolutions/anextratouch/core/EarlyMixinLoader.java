package org.fentanylsolutions.anextratouch.core;

import java.util.List;
import java.util.Set;

import org.fentanylsolutions.anextratouch.AnExtraTouch;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinLoader extends FentEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + AnExtraTouch.MODID + ".early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return new MixinBuilder()
            // Accessors

            // Rest
            .addMixin("MixinEntity", MixinBuilder.Side.BOTH)
            .addMixin("MixinGuiScreen", MixinBuilder.Side.CLIENT)
            .addMixin("MixinEntityRenderer", MixinBuilder.Side.CLIENT)
            .addMixin("MixinExplosion", MixinBuilder.Side.CLIENT)
            .addMixin("MixinEntityLightningBolt", MixinBuilder.Side.CLIENT)
            .addMixin("MixinEntityLivingBase", MixinBuilder.Side.CLIENT)
            .addMixin("MixinEntitySetAngles", MixinBuilder.Side.CLIENT)
            .addMixin("MixinEntityPlayerSP", MixinBuilder.Side.CLIENT)
            .build();
    }
}
