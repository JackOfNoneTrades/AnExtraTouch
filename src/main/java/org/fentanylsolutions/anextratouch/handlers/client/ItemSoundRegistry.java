package org.fentanylsolutions.anextratouch.handlers.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemWritableBook;

import org.fentanylsolutions.anextratouch.AnExtraTouch;

/** Resolves an item's broad sound category, with configurable item overrides. */
public final class ItemSoundRegistry {

    public enum Category {
        NONE,
        SWORD,
        AXE,
        TOOL,
        BOW,
        CROSSBOW,
        SHIELD,
        POTION,
        BOOK,
        UTILITY
    }

    private static volatile Registry registry = Registry.EMPTY;

    private ItemSoundRegistry() {}

    public static Category resolve(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || stack.getItem() == null) {
            return Category.NONE;
        }

        String name = itemName(stack.getItem());
        if (name == null) {
            return Category.NONE;
        }

        Registry current = registry;
        Category category = current.exact.get(new MetaKey(name, stackMetadata(stack)));
        if (category == null) {
            category = current.wildcard.get(name);
        }
        if (category != null) {
            return category;
        }
        return classify(stack);
    }

    /** Replaces all overrides atomically. Invalid entries are ignored. */
    public static void reload(String[] entries) {
        Map<String, Category> wildcard = new HashMap<String, Category>();
        Map<MetaKey, Category> exact = new HashMap<MetaKey, Category>();
        if (entries != null) {
            for (String raw : entries) {
                ParsedEntry parsed = parse(raw);
                if (parsed == null) {
                    if (raw != null && !raw.trim()
                        .isEmpty()) {
                        AnExtraTouch.LOG.warn("Invalid item sound category override '{}'; ignoring.", raw);
                    }
                    continue;
                }
                if (parsed.metadata == null) {
                    wildcard.put(parsed.name, parsed.category);
                } else {
                    exact.put(new MetaKey(parsed.name, parsed.metadata.intValue()), parsed.category);
                }
            }
        }
        registry = new Registry(wildcard, exact);
    }

    private static Category classify(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemSword) {
            return Category.SWORD;
        }
        if (stack.getItemUseAction() == EnumAction.block) {
            return Category.SHIELD;
        }
        if (item instanceof ItemAxe) {
            return Category.AXE;
        }
        if (item instanceof ItemHoe || item instanceof ItemShears) {
            return Category.TOOL;
        }
        if (item instanceof ItemBow) {
            return Category.BOW;
        }
        if (item instanceof ItemPotion || item == Items.glass_bottle) {
            return Category.POTION;
        }
        if (item instanceof ItemBook || item instanceof ItemEnchantedBook
            || item instanceof ItemWritableBook
            || item instanceof ItemEditableBook) {
            return Category.BOOK;
        }
        if (item instanceof ItemBucket) {
            return Category.UTILITY;
        }
        Set<String> toolClasses = item.getToolClasses(stack);
        if (toolClasses != null) {
            for (String toolClass : toolClasses) {
                if ("axe".equalsIgnoreCase(toolClass)) {
                    return Category.AXE;
                }
            }
            if (!toolClasses.isEmpty()) {
                return Category.TOOL;
            }
        }
        if (item instanceof ItemTool) {
            return Category.TOOL;
        }
        return Category.UTILITY;
    }

    private static int stackMetadata(ItemStack stack) {
        return stack.isItemStackDamageable() ? 0 : stack.getItemDamage();
    }

    private static String itemName(Item item) {
        return Item.itemRegistry.getNameForObject(item);
    }

    private static ParsedEntry parse(String raw) {
        if (raw == null) {
            return null;
        }
        String entry = raw.trim();
        int equals = entry.indexOf('=');
        if (equals <= 0 || equals != entry.lastIndexOf('=')) {
            return null;
        }
        String itemPart = entry.substring(0, equals)
            .trim();
        String categoryPart = entry.substring(equals + 1)
            .trim()
            .toUpperCase(Locale.ENGLISH);
        if (itemPart.isEmpty() || categoryPart.isEmpty()) {
            return null;
        }
        int at = itemPart.indexOf('@');
        if (at >= 0 && at != itemPart.lastIndexOf('@')) {
            return null;
        }
        String name = at < 0 ? itemPart : itemPart.substring(0, at);
        if (!validName(name)) {
            return null;
        }
        Integer metadata = null;
        if (at >= 0) {
            try {
                metadata = Integer.valueOf(itemPart.substring(at + 1));
                if (metadata.intValue() < 0) {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return new ParsedEntry(name, metadata, Category.valueOf(categoryPart));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean validName(String name) {
        int colon = name.indexOf(':');
        if (colon <= 0 || colon != name.lastIndexOf(':') || colon == name.length() - 1) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isWhitespace(name.charAt(i)) || Character.isISOControl(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static final class ParsedEntry {

        private final String name;
        private final Integer metadata;
        private final Category category;

        private ParsedEntry(String name, Integer metadata, Category category) {
            this.name = name;
            this.metadata = metadata;
            this.category = category;
        }
    }

    private static final class MetaKey {

        private final String name;
        private final int metadata;

        private MetaKey(String name, int metadata) {
            this.name = name;
            this.metadata = metadata;
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + metadata;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof MetaKey)) {
                return false;
            }
            MetaKey other = (MetaKey) object;
            return metadata == other.metadata && name.equals(other.name);
        }
    }

    private static final class Registry {

        private static final Registry EMPTY = new Registry(
            Collections.<String, Category>emptyMap(),
            Collections.<MetaKey, Category>emptyMap());
        private final Map<String, Category> wildcard;
        private final Map<MetaKey, Category> exact;

        private Registry(Map<String, Category> wildcard, Map<MetaKey, Category> exact) {
            this.wildcard = Collections.unmodifiableMap(new HashMap<String, Category>(wildcard));
            this.exact = Collections.unmodifiableMap(new HashMap<MetaKey, Category>(exact));
        }
    }
}
