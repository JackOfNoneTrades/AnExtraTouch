package org.fentanylsolutions.anextratouch.handlers.client;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.anextratouch.handlers.client.ItemSoundRegistry.Category;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ItemSoundRegistryTest {

    private static final Item SHIELD = new Item() {

        @Override
        public EnumAction getItemUseAction(ItemStack stack) {
            return EnumAction.block;
        }
    }.setMaxDamage(336);
    private static final Item SWORD = new ItemSword(Item.ToolMaterial.IRON);

    @BeforeClass
    public static void registerItems() throws ReflectiveOperationException {
        // Populate the test registry without starting Forge's mod loader/LaunchClassLoader.
        Method addObjectRaw = Item.itemRegistry.getClass()
            .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
        addObjectRaw.setAccessible(true);
        addObjectRaw.invoke(Item.itemRegistry, 31998, "aet_sound_test:shield", SHIELD);
        addObjectRaw.invoke(Item.itemRegistry, 31999, "aet_sound_test:sword", SWORD);
    }

    @Before
    @After
    public void resetOverrides() {
        ItemSoundRegistry.reload(new String[0]);
    }

    @Test
    public void blockingItemsAreShieldsButSwordsRemainSwords() {
        assertEquals(Category.SHIELD, ItemSoundRegistry.resolve(new ItemStack(SHIELD)));
        assertEquals(Category.SWORD, ItemSoundRegistry.resolve(new ItemStack(SWORD)));
    }

    @Test
    public void shieldClassificationDoesNotDependOnCooldownOrDurability() {
        ItemStack shield = new ItemStack(SHIELD, 1, 10);
        shield.setTagCompound(new NBTTagCompound());
        shield.getTagCompound()
            .setLong("cooldown_end", 1200L);
        assertEquals(Category.SHIELD, ItemSoundRegistry.resolve(shield));
    }

    @Test
    public void explicitShieldOverridesStillWin() {
        ItemSoundRegistry.reload(new String[] { "aet_sound_test:shield=none" });
        assertEquals(Category.NONE, ItemSoundRegistry.resolve(new ItemStack(SHIELD)));

        ItemSoundRegistry.reload(new String[] { "aet_sound_test:shield=sword" });
        assertEquals(Category.SWORD, ItemSoundRegistry.resolve(new ItemStack(SHIELD)));
    }
}
