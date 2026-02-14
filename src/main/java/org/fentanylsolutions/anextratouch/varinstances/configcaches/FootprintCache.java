package org.fentanylsolutions.anextratouch.varinstances.configcaches;

import java.util.Collections;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class FootprintCache {

    public Object2FloatOpenHashMap<Class<? extends Entity>> entityStrides;
    public Object2FloatOpenHashMap<Class<? extends Entity>> entityFootSizes;
    public Object2FloatOpenHashMap<Class<? extends Entity>> entityStanceWidths;

    public Object2FloatOpenHashMap<Class<? extends Entity>> babyEntityStrides;
    public Object2FloatOpenHashMap<Class<? extends Entity>> babyEntityFootSizes;
    public Object2FloatOpenHashMap<Class<? extends Entity>> babyEntityStanceWidths;

    private final Object2BooleanOpenHashMap<Block> blockFootprintCache = new Object2BooleanOpenHashMap<>();
    private final Object2IntOpenHashMap<Block> blockLifespanCache = new Object2IntOpenHashMap<>();
    private final Object2FloatOpenHashMap<Block> blockOpacityCache = new Object2FloatOpenHashMap<>();
    private HashSet<String> allowedSoundTypes;
    private HashSet<String> blacklistedBlocks;
    private HashSet<String> whitelistedBlocks;
    private Object2IntOpenHashMap<String> soundTypeLifespanMap;
    private Object2FloatOpenHashMap<String> soundTypeOpacityMap;
    private Object2FloatOpenHashMap<String> blockOpacityOverrideMap;

    public void populateFromConfig() {
        blockFootprintCache.clear();
        blockLifespanCache.clear();
        blockOpacityCache.clear();

        allowedSoundTypes = new HashSet<>();
        Collections.addAll(allowedSoundTypes, Config.footprintSoundTypes);

        blacklistedBlocks = new HashSet<>();
        Collections.addAll(blacklistedBlocks, Config.blockBlacklist);

        whitelistedBlocks = new HashSet<>();
        Collections.addAll(whitelistedBlocks, Config.blockWhitelist);

        soundTypeLifespanMap = new Object2IntOpenHashMap<>();
        for (String entry : Config.soundTypeLifespans) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                AnExtraTouch.LOG.error("Invalid sound type lifespan entry: {}", entry);
                continue;
            }
            try {
                soundTypeLifespanMap.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                AnExtraTouch.LOG.error("Invalid lifespan value in entry: {}", entry);
            }
        }

        soundTypeOpacityMap = new Object2FloatOpenHashMap<>();
        for (String entry : Config.soundTypeOpacities) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                AnExtraTouch.LOG.error("Invalid sound type opacity entry: {}", entry);
                continue;
            }
            try {
                soundTypeOpacityMap.put(parts[0], Float.parseFloat(parts[1]));
            } catch (NumberFormatException e) {
                AnExtraTouch.LOG.error("Invalid opacity value in entry: {}", entry);
            }
        }

        blockOpacityOverrideMap = new Object2FloatOpenHashMap<>();
        for (String entry : Config.blockOpacityOverrides) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                AnExtraTouch.LOG.error("Invalid block opacity override entry: {}", entry);
                continue;
            }
            try {
                blockOpacityOverrideMap.put(parts[0], Float.parseFloat(parts[1]));
            } catch (NumberFormatException e) {
                AnExtraTouch.LOG.error("Invalid opacity value in entry: {}", entry);
            }
        }

        entityStrides = new Object2FloatOpenHashMap<>();
        entityFootSizes = new Object2FloatOpenHashMap<>();
        entityStanceWidths = new Object2FloatOpenHashMap<>();
        babyEntityStrides = new Object2FloatOpenHashMap<>();
        babyEntityFootSizes = new Object2FloatOpenHashMap<>();
        babyEntityStanceWidths = new Object2FloatOpenHashMap<>();

        // Parse adult overrides
        Object2FloatOpenHashMap<String> strideOverrides = new Object2FloatOpenHashMap<>();
        Object2FloatOpenHashMap<String> footSizeOverrides = new Object2FloatOpenHashMap<>();
        Object2FloatOpenHashMap<String> stanceWidthOverrides = new Object2FloatOpenHashMap<>();

        for (String entry : Config.entityOverrides) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 4) {
                AnExtraTouch.LOG.error("Invalid entity override entry: {}", entry);
                continue;
            }
            if (!parts[1].isEmpty()) {
                strideOverrides.put(parts[0], Float.parseFloat(parts[1]));
            }
            if (!parts[2].isEmpty()) {
                footSizeOverrides.put(parts[0], Float.parseFloat(parts[2]));
            }
            if (!parts[3].isEmpty()) {
                stanceWidthOverrides.put(parts[0], Float.parseFloat(parts[3]));
            }
        }

        // Parse baby overrides
        Object2FloatOpenHashMap<String> babyStrideOverrides = new Object2FloatOpenHashMap<>();
        Object2FloatOpenHashMap<String> babyFootSizeOverrides = new Object2FloatOpenHashMap<>();
        Object2FloatOpenHashMap<String> babyStanceWidthOverrides = new Object2FloatOpenHashMap<>();

        for (String entry : Config.babyEntityOverrides) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 4) {
                AnExtraTouch.LOG.error("Invalid baby entity override entry: {}", entry);
                continue;
            }
            if (!parts[1].isEmpty()) {
                babyStrideOverrides.put(parts[0], Float.parseFloat(parts[1]));
            }
            if (!parts[2].isEmpty()) {
                babyFootSizeOverrides.put(parts[0], Float.parseFloat(parts[2]));
            }
            if (!parts[3].isEmpty()) {
                babyStanceWidthOverrides.put(parts[0], Float.parseFloat(parts[3]));
            }
        }

        // Build the final maps for all allowed entities
        for (Class<? extends Entity> c : EntityList.stringToClassMapping.values()) {
            if (!EntityLivingBase.class.isAssignableFrom(c)) {
                continue;
            }

            String name = EntityList.classToStringMapping.get(c);
            boolean inList = false;
            for (String s : Config.entityClassList) {
                if (name.equals(s)) {
                    inList = true;
                    break;
                }
            }

            // Blacklist: skip if in list. Whitelist: skip if NOT in list.
            if (Config.entityClassListIsBlacklist == inList) {
                continue;
            }

            float stride = strideOverrides.getOrDefault(name, Config.defaultStride);
            float footSize = footSizeOverrides.getOrDefault(name, Config.defaultFootSize);
            float stanceWidth = stanceWidthOverrides.getOrDefault(name, Config.defaultStanceWidth);

            entityStrides.put(c, stride);
            entityFootSizes.put(c, footSize);
            entityStanceWidths.put(c, stanceWidth);

            babyEntityStrides.put(c, babyStrideOverrides.getOrDefault(name, stride * Config.babyStrideMultiplier));
            babyEntityFootSizes
                .put(c, babyFootSizeOverrides.getOrDefault(name, footSize * Config.babyFootSizeMultiplier));
            babyEntityStanceWidths
                .put(c, babyStanceWidthOverrides.getOrDefault(name, stanceWidth * Config.babyStanceWidthMultiplier));
        }

        // Player classes are not in EntityList, add manually (configurable as "Player")
        String playerName = "Player";
        boolean playerInList = false;
        for (String s : Config.entityClassList) {
            if (playerName.equals(s)) {
                playerInList = true;
                break;
            }
        }
        if (Config.entityClassListIsBlacklist != playerInList) {
            float stride = strideOverrides.getOrDefault(playerName, Config.defaultStride);
            float footSize = footSizeOverrides.getOrDefault(playerName, Config.defaultFootSize);
            float stanceWidth = stanceWidthOverrides.getOrDefault(playerName, Config.defaultStanceWidth);

            entityStrides.put(EntityClientPlayerMP.class, stride);
            entityStrides.put(EntityOtherPlayerMP.class, stride);
            entityFootSizes.put(EntityClientPlayerMP.class, footSize);
            entityFootSizes.put(EntityOtherPlayerMP.class, footSize);
            entityStanceWidths.put(EntityClientPlayerMP.class, stanceWidth);
            entityStanceWidths.put(EntityOtherPlayerMP.class, stanceWidth);
        }
    }

    public boolean hasFootprint(Block block) {
        if (blockFootprintCache.containsKey(block)) {
            return blockFootprintCache.getBoolean(block);
        }
        boolean result = checkFootprintSupport(block);
        blockFootprintCache.put(block, result);
        return result;
    }

    private boolean checkFootprintSupport(Block block) {
        String registryName = Block.blockRegistry.getNameForObject(block);
        if (whitelistedBlocks.contains(registryName)) {
            return true;
        }
        if (blacklistedBlocks.contains(registryName)) {
            return false;
        }
        return allowedSoundTypes.contains(block.stepSound.soundName);
    }

    public int getLifespan(Block block) {
        if (blockLifespanCache.containsKey(block)) {
            return blockLifespanCache.getInt(block);
        }
        int lifespan = resolveLifespan(block);
        blockLifespanCache.put(block, lifespan);
        return lifespan;
    }

    private int resolveLifespan(Block block) {
        String soundType = block.stepSound.soundName;
        if (soundTypeLifespanMap.containsKey(soundType)) {
            return soundTypeLifespanMap.getInt(soundType);
        }
        return Config.defaultFootprintLifespan;
    }

    public float getOpacity(Block block) {
        if (blockOpacityCache.containsKey(block)) {
            return blockOpacityCache.getFloat(block);
        }
        float opacity = resolveOpacity(block);
        blockOpacityCache.put(block, opacity);
        return opacity;
    }

    private float resolveOpacity(Block block) {
        String registryName = Block.blockRegistry.getNameForObject(block);
        if (blockOpacityOverrideMap.containsKey(registryName)) {
            return blockOpacityOverrideMap.getFloat(registryName);
        }
        String soundType = block.stepSound.soundName;
        if (soundTypeOpacityMap.containsKey(soundType)) {
            return soundTypeOpacityMap.getFloat(soundType);
        }
        return Config.defaultFootprintOpacity;
    }
}
