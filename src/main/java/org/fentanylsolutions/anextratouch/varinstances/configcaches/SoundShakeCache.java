package org.fentanylsolutions.anextratouch.varinstances.configcaches;

import java.util.HashMap;
import java.util.Locale;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

public class SoundShakeCache {

    public static final class SoundShakeEntry {

        public final float trauma;
        public final float radius;
        public final float frequency;
        public final float duration;

        SoundShakeEntry(float trauma, float radius, float frequency, float duration) {
            this.trauma = trauma;
            this.radius = radius;
            this.frequency = frequency;
            this.duration = duration;
        }
    }

    private HashMap<String, SoundShakeEntry> soundShakeMap = new HashMap<>();

    public void populateFromConfig() {
        soundShakeMap.clear();
        for (String raw : Config.cameraSoundShakes) {
            String[] tokens = raw.split(";");
            String sound = null;
            float trauma = -1f;
            float radius = 16f;
            float frequency = 1.0f;
            float duration = 0.3f;

            for (String token : tokens) {
                int eq = token.indexOf('=');
                if (eq < 0) continue;

                String key = token.substring(0, eq)
                    .trim()
                    .toLowerCase(Locale.ROOT);
                String val = token.substring(eq + 1)
                    .trim();

                try {
                    switch (key) {
                        case "sound":
                            sound = val;
                            break;
                        case "trauma":
                            trauma = Float.parseFloat(val);
                            break;
                        case "radius":
                            radius = Float.parseFloat(val);
                            break;
                        case "frequency":
                            frequency = Float.parseFloat(val);
                            break;
                        case "duration":
                            duration = Float.parseFloat(val);
                            break;
                        default:
                            AnExtraTouch.LOG.warn("Unknown sound shake key '{}' in entry: {}", key, raw);
                            break;
                    }
                } catch (NumberFormatException e) {
                    AnExtraTouch.LOG.warn("Invalid number for key '{}' in sound shake entry: {}", key, raw);
                }
            }

            if (sound == null || trauma < 0f) {
                AnExtraTouch.LOG.warn("Sound shake entry missing required sound/trauma: {}", raw);
                continue;
            }

            soundShakeMap.put(sound, new SoundShakeEntry(trauma, radius, frequency, duration));
        }
    }

    public SoundShakeEntry getSoundShake(String name) {
        return soundShakeMap.get(name);
    }

    public boolean hasSoundShakes() {
        return !soundShakeMap.isEmpty();
    }
}
