package org.fentanylsolutions.anextratouch.varinstances.configcaches;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

public class ArmorCache {

    public HashSet<Class<? extends Entity>> armorSoundEntities;
    private HashMap<String, String> armorCategoryOverrideMap;

    public void populateFromConfig() {
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
}
