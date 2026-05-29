package org.fentanylsolutions.anextratouch.handlers.client.effects;

import com.ventooth.swansong.api.ShaderStateInfo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class SwanSongShaderCompat {

    private SwanSongShaderCompat() {}

    static boolean isInitialized() {
        return ShaderStateInfo.isInitialized();
    }
}
