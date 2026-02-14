package org.fentanylsolutions.anextratouch.varinstances;

import java.util.Collections;
import java.util.HashSet;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.varinstances.configcaches.ArmorCache;
import org.fentanylsolutions.anextratouch.varinstances.configcaches.BreathCache;
import org.fentanylsolutions.anextratouch.varinstances.configcaches.FootprintCache;
import org.fentanylsolutions.anextratouch.varinstances.configcaches.SoundShakeCache;

public class VarInstanceClient {

    public final FootprintCache footprints = new FootprintCache();
    public final BreathCache breath = new BreathCache();
    public final ArmorCache armor = new ArmorCache();
    public final SoundShakeCache soundShakes = new SoundShakeCache();

    // Wetness
    public HashSet<Class<? extends Entity>> wetnessEntities;

    // Rain splash
    public HashSet<Class<? extends Entity>> rainSplashEntities;

    // Smooth GUI
    public HashSet<String> smoothGuiExcludedScreens;

    public boolean serverHasAET = false;

    public VarInstanceClient() {}

    public void populateListsFromConfig() {
        footprints.populateFromConfig();
        breath.populateFromConfig();
        armor.populateFromConfig();
        soundShakes.populateFromConfig();

        // Wetness
        wetnessEntities = new HashSet<>();
        String playerName = "Player";
        for (Class<? extends Entity> c : EntityList.stringToClassMapping.values()) {
            if (!EntityLivingBase.class.isAssignableFrom(c)) continue;

            String name = EntityList.classToStringMapping.get(c);
            boolean inWetnessList = false;
            for (String s : Config.wetnessEntityClassList) {
                if (name.equals(s)) {
                    inWetnessList = true;
                    break;
                }
            }
            if (Config.wetnessEntityClassListIsBlacklist == inWetnessList) continue;

            wetnessEntities.add(c);
        }
        boolean playerInWetnessList = false;
        for (String s : Config.wetnessEntityClassList) {
            if (playerName.equals(s)) {
                playerInWetnessList = true;
                break;
            }
        }
        if (Config.wetnessEntityClassListIsBlacklist != playerInWetnessList) {
            wetnessEntities.add(EntityClientPlayerMP.class);
            wetnessEntities.add(EntityOtherPlayerMP.class);
        }

        // Rain splash
        rainSplashEntities = new HashSet<>();
        for (Class<? extends Entity> c : EntityList.stringToClassMapping.values()) {
            if (!EntityLivingBase.class.isAssignableFrom(c)) {
                continue;
            }

            String name = EntityList.classToStringMapping.get(c);
            boolean inRainSplashList = false;
            for (String s : Config.rainSplashEntityClassList) {
                if (name.equals(s)) {
                    inRainSplashList = true;
                    break;
                }
            }
            if (Config.rainSplashEntityClassListIsBlacklist == inRainSplashList) {
                continue;
            }

            rainSplashEntities.add(c);
        }
        boolean playerInRainSplashList = false;
        for (String s : Config.rainSplashEntityClassList) {
            if (playerName.equals(s)) {
                playerInRainSplashList = true;
                break;
            }
        }
        if (Config.rainSplashEntityClassListIsBlacklist != playerInRainSplashList) {
            rainSplashEntities.add(EntityClientPlayerMP.class);
            rainSplashEntities.add(EntityOtherPlayerMP.class);
        }

        // Smooth GUI excluded screens
        smoothGuiExcludedScreens = new HashSet<>();
        Collections.addAll(smoothGuiExcludedScreens, Config.smoothGuiExcludedScreens);
    }

    public void preInitHook() {}

    public void postInitHook() {
        populateListsFromConfig();
    }
}
