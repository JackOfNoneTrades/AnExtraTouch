package org.fentanylsolutions.anextratouch.varinstances;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class VarInstanceClient {

    // Footprints
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

    // Breath
    public Object2FloatOpenHashMap<Class<? extends Entity>> breathUpOffsets;
    public Object2FloatOpenHashMap<Class<? extends Entity>> breathForwardDists;
    public Object2FloatOpenHashMap<Class<? extends Entity>> babyBreathUpOffsets;
    public Object2FloatOpenHashMap<Class<? extends Entity>> babyBreathForwardDists;
    private HashMap<Integer, String> breathDimensionModes;
    private HashSet<String> breathColdBiomes;

    // Wetness
    public HashSet<Class<? extends Entity>> wetnessEntities;

    // Armor
    public HashSet<Class<? extends Entity>> armorSoundEntities;
    private HashMap<String, String> armorCategoryOverrideMap;

    public boolean serverHasAET = false;

    public VarInstanceClient() {}

    public void populateListsFromConfig() {
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
            if (!EntityLivingBase.class.isAssignableFrom(c)) continue;

            String name = EntityList.classToStringMapping.get(c);
            boolean inList = false;
            for (String s : Config.entityClassList) {
                if (name.equals(s)) {
                    inList = true;
                    break;
                }
            }

            // Blacklist: skip if in list. Whitelist: skip if NOT in list.
            if (Config.entityClassListIsBlacklist == inList) continue;

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

        // --- Breath effect ---
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
        Collections.addAll(breathColdBiomes, Config.breathColdBiomes);

        // --- Wetness ---
        wetnessEntities = new HashSet<>();
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
        // Player classes not in EntityList
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

        // --- Armor sounds ---
        armorSoundEntities = new HashSet<>();
        HashSet<String> armorEntityNames = new HashSet<>();
        Collections.addAll(armorEntityNames, Config.armorSoundEntityWhitelist);
        for (Class<? extends Entity> c : EntityList.stringToClassMapping.values()) {
            if (!EntityLivingBase.class.isAssignableFrom(c)) continue;
            String name = EntityList.classToStringMapping.get(c);
            if (armorEntityNames.contains(name)) {
                armorSoundEntities.add(c);
            }
        }
        if (armorEntityNames.contains("Player")) {
            armorSoundEntities.add(EntityClientPlayerMP.class);
            armorSoundEntities.add(EntityOtherPlayerMP.class);
            armorSoundEntities.add(EntityPlayerMP.class);
        }

        armorCategoryOverrideMap = new HashMap<>();
        for (String entry : Config.armorCategoryOverrides) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                AnExtraTouch.LOG.error("Invalid armor category override entry: {}", entry);
                continue;
            }
            armorCategoryOverrideMap.put(parts[0], parts[1].toLowerCase());
        }
    }

    public String getBreathDimensionMode(int dimensionId) {
        return breathDimensionModes.getOrDefault(dimensionId, "normal");
    }

    public boolean isColdBiome(String biomeName) {
        return breathColdBiomes.contains(biomeName);
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

    public String resolveArmorCategory(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        // Check override map by registry name
        String registryName = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
        if (registryName != null && armorCategoryOverrideMap.containsKey(registryName)) {
            return armorCategoryOverrideMap.get(registryName);
        }

        // Map armor material to category
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor.ArmorMaterial mat = ((ItemArmor) stack.getItem()).getArmorMaterial();
            if (mat == ItemArmor.ArmorMaterial.CLOTH) return "light";
            if (mat == ItemArmor.ArmorMaterial.CHAIN) return "medium";
            if (mat == ItemArmor.ArmorMaterial.IRON) return "heavy";
            if (mat == ItemArmor.ArmorMaterial.GOLD) return "heavy";
            if (mat == ItemArmor.ArmorMaterial.DIAMOND) return "crystal";
        }

        return Config.armorDefaultCategory;
    }

    public int getArmorPriority(String category) {
        if (category == null) return -1;
        switch (category) {
            case "heavy":
                return 3;
            case "crystal":
                return 2;
            case "medium":
                return 1;
            case "light":
                return 0;
            default:
                return -1;
        }
    }

    public void preInitHook() {}

    public void postInitHook() {
        populateListsFromConfig();
    }
}
