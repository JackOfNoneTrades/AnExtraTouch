package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.lang.reflect.Method;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class AngelicaShaderHelper {

    private static Boolean angelicaLoaded;
    private static boolean irisApiLookupComplete;
    private static Object irisApi;
    private static Method isShaderPackInUseMethod;

    private AngelicaShaderHelper() {}

    static boolean isShaderPackInUse() {
        if (!isAngelicaLoaded()) {
            return false;
        }

        if (!irisApiLookupComplete) {
            irisApiLookupComplete = true;

            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstance = irisApiClass.getMethod("getInstance");
                irisApi = getInstance.invoke(null);
                isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            } catch (Throwable ignored) {
                irisApi = null;
                isShaderPackInUseMethod = null;
            }
        }

        if (irisApi == null || isShaderPackInUseMethod == null) {
            return false;
        }

        try {
            Object result = isShaderPackInUseMethod.invoke(irisApi);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isAngelicaLoaded() {
        if (angelicaLoaded == null) {
            angelicaLoaded = Loader.isModLoaded("angelica");
        }
        return angelicaLoaded.booleanValue();
    }
}
