package org.fentanylsolutions.anextratouch.varinstances.configcaches;

import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

public class BreathCache {

    public Object2FloatOpenHashMap<Class<? extends Entity>> breathUpOffsets;
    public Object2FloatOpenHashMap<Class<? extends Entity>> breathForwardDists;
    public Object2FloatOpenHashMap<Class<? extends Entity>> babyBreathUpOffsets;
    public Object2FloatOpenHashMap<Class<? extends Entity>> babyBreathForwardDists;
    private HashMap<Integer, String> breathDimensionModes;
    private HashSet<String> breathColdBiomes;
    private HashSet<Integer> breathColdBiomeIds;

    public void populateFromConfig() {
        breathUpOffsets = new Object2FloatOpenHashMap<>();
        breathForwardDists = new Object2FloatOpenHashMap<>();
        babyBreathUpOffsets = new Object2FloatOpenHashMap<>();
        babyBreathForwardDists = new Object2FloatOpenHashMap<>();

        // Parse adult breath overrides
        Object2FloatOpenHashMap<String> breathUpOverrides = new Object2FloatOpenHashMap<>();
        Object2FloatOpenHashMap<String> breathFwdOverrides = new Object2FloatOpenHashMap<>();

        for (String entry : Config.breathEntityOverrides) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 3) {
                AnExtraTouch.LOG.error("Invalid breath entity override entry: {}", entry);
                continue;
            }
            try {
                if (!parts[1].isEmpty()) breathUpOverrides.put(parts[0], Float.parseFloat(parts[1]));
                if (!parts[2].isEmpty()) breathFwdOverrides.put(parts[0], Float.parseFloat(parts[2]));
            } catch (NumberFormatException e) {
                AnExtraTouch.LOG.error("Invalid value in breath override entry: {}", entry);
            }
        }

        // Parse baby breath overrides
        Object2FloatOpenHashMap<String> babyBreathUpOverrides = new Object2FloatOpenHashMap<>();
        Object2FloatOpenHashMap<String> babyBreathFwdOverrides = new Object2FloatOpenHashMap<>();

        for (String entry : Config.breathBabyEntityOverrides) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 3) {
                AnExtraTouch.LOG.error("Invalid baby breath entity override entry: {}", entry);
                continue;
            }
            try {
                if (!parts[1].isEmpty()) babyBreathUpOverrides.put(parts[0], Float.parseFloat(parts[1]));
                if (!parts[2].isEmpty()) babyBreathFwdOverrides.put(parts[0], Float.parseFloat(parts[2]));
            } catch (NumberFormatException e) {
                AnExtraTouch.LOG.error("Invalid value in baby breath override entry: {}", entry);
            }
        }

        // Build breath maps for all allowed entities
        for (Class<? extends Entity> c : EntityList.stringToClassMapping.values()) {
            if (!EntityLivingBase.class.isAssignableFrom(c)) continue;

            String name = EntityList.classToStringMapping.get(c);
            boolean inBreathList = false;
            for (String s : Config.breathEntityClassList) {
                if (name.equals(s)) {
                    inBreathList = true;
                    break;
                }
            }
            if (Config.breathEntityClassListIsBlacklist == inBreathList) continue;

            float upOff = breathUpOverrides.getOrDefault(name, Config.breathDefaultUpOffset);
            float fwdDist = breathFwdOverrides.getOrDefault(name, Config.breathDefaultForwardDist);
            breathUpOffsets.put(c, upOff);
            breathForwardDists.put(c, fwdDist);

            babyBreathUpOffsets.put(c, babyBreathUpOverrides.getOrDefault(name, Config.breathDefaultBabyUpOffset));
            babyBreathForwardDists
                .put(c, babyBreathFwdOverrides.getOrDefault(name, Config.breathDefaultBabyForwardDist));
        }

        // Add player classes for breath (configurable as "Player")
        String playerName = "Player";
        boolean playerInBreathList = false;
        for (String s : Config.breathEntityClassList) {
            if (playerName.equals(s)) {
                playerInBreathList = true;
                break;
            }
        }
        if (Config.breathEntityClassListIsBlacklist != playerInBreathList) {
            float upOff = breathUpOverrides.getOrDefault(playerName, Config.breathDefaultUpOffset);
            float fwdDist = breathFwdOverrides.getOrDefault(playerName, Config.breathDefaultForwardDist);
            breathUpOffsets.put(EntityClientPlayerMP.class, upOff);
            breathUpOffsets.put(EntityOtherPlayerMP.class, upOff);
            breathForwardDists.put(EntityClientPlayerMP.class, fwdDist);
            breathForwardDists.put(EntityOtherPlayerMP.class, fwdDist);

            babyBreathUpOffsets.put(EntityClientPlayerMP.class, Config.breathDefaultBabyUpOffset);
            babyBreathUpOffsets.put(EntityOtherPlayerMP.class, Config.breathDefaultBabyUpOffset);
            babyBreathForwardDists.put(EntityClientPlayerMP.class, Config.breathDefaultBabyForwardDist);
            babyBreathForwardDists.put(EntityOtherPlayerMP.class, Config.breathDefaultBabyForwardDist);
        }

        // Parse dimension rules for breath
        breathDimensionModes = new HashMap<>();
        for (String entry : Config.breathDimensionRules) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                AnExtraTouch.LOG.error("Invalid breath dimension rule: {}", entry);
                continue;
            }
            try {
                int dimId = Integer.parseInt(parts[0]);
                String mode = parts[1].toLowerCase();
                if (!"normal".equals(mode) && !"always".equals(mode) && !"never".equals(mode)) {
                    AnExtraTouch.LOG
                        .error("Invalid breath dimension mode '{}' in: {}. Use normal/always/never.", mode, entry);
                    continue;
                }
                breathDimensionModes.put(dimId, mode);
            } catch (NumberFormatException e) {
                AnExtraTouch.LOG.error("Invalid dimension ID in breath dimension rule: {}", entry);
            }
        }

        breathColdBiomes = new HashSet<>();
        breathColdBiomeIds = new HashSet<>();
        for (String rawEntry : Config.breathColdBiomes) {
            if (rawEntry == null) {
                continue;
            }
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            try {
                breathColdBiomeIds.add(Integer.parseInt(entry));
            } catch (NumberFormatException ignored) {
                breathColdBiomes.add(entry);
            }
        }
    }

    public String getBreathDimensionMode(int dimensionId) {
        return breathDimensionModes.getOrDefault(dimensionId, Config.breathDefaultDimensionMode);
    }

    public boolean isColdBiome(String biomeName) {
        return breathColdBiomes.contains(biomeName);
    }

    public boolean isColdBiome(BiomeGenBase biome) {
        if (biome == null) {
            return false;
        }
        return breathColdBiomes.contains(biome.biomeName) || breathColdBiomeIds.contains(biome.biomeID);
    }
}
