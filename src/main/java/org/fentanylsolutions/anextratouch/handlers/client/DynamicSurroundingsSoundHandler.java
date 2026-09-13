package org.fentanylsolutions.anextratouch.handlers.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Filters DS ambience by sound ID without requiring Dynamic Surroundings to be installed. */
public final class DynamicSurroundingsSoundHandler {

    public static final DynamicSurroundingsSoundHandler INSTANCE = new DynamicSurroundingsSoundHandler();

    private static final Set<String> ANIMAL_SOUNDS = new HashSet<>(
        Arrays.asList(
            "bird",
            "woodpecker",
            "owl",
            "bison",
            "coyote",
            "wolf",
            "frog",
            "crocodile",
            "rattlesnake",
            "elephant",
            "primates",
            "seagulls",
            "whale",
            "bees",
            "insectcrawl",
            "insectbuzz",
            "gnatt",
            "grasshopper",
            "crickets",
            "hiss",
            "monstergrowl",
            // Outdoor recordings can mix animal calls into the background ambience.
            "forest",
            "jungle",
            "plains"));

    // Keep currently playing sounds available for live muting without retaining finished sounds.
    private final Set<ISound> playingAnimalSounds = Collections.newSetFromMap(new WeakHashMap<ISound, Boolean>());

    private DynamicSurroundingsSoundHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundEvent17 event) {
        ISound sound = event.result;
        if (sound == null || !isAnimalSound(sound.getPositionedSoundLocation())) return;

        if (Config.muteDynamicSurroundingsAnimalSounds) {
            event.result = null;
        } else {
            synchronized (playingAnimalSounds) {
                playingAnimalSounds.add(sound);
            }
        }
    }

    public void onConfigReload() {
        if (!Config.muteDynamicSurroundingsAnimalSounds) return;

        ISound[] sounds;
        synchronized (playingAnimalSounds) {
            sounds = playingAnimalSounds.toArray(new ISound[0]);
            playingAnimalSounds.clear();
        }
        SoundHandler soundHandler = Minecraft.getMinecraft()
            .getSoundHandler();
        for (ISound sound : sounds) {
            soundHandler.stopSound(sound);
        }
    }

    private static boolean isAnimalSound(ResourceLocation location) {
        return location != null && "dsurround".equals(location.getResourceDomain())
            && ANIMAL_SOUNDS.contains(location.getResourcePath());
    }
}
