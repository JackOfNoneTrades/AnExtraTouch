package org.fentanylsolutions.anextratouch.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement;
import cpw.mods.fml.client.config.IConfigElement;

/** Organizes the GUI without changing the categories or properties stored in the config file. */
@SuppressWarnings("rawtypes")
final class ConfigGuiLayout {

    private ConfigGuiLayout() {}

    static List<IConfigElement> create(Configuration config) {
        // Build fresh wrappers on each opening so a reload cannot leave stale Property references.
        return Arrays.asList(
            menu(
                "footprints",
                section(
                    config,
                    "footprints.entities_and_shape",
                    "footprints",
                    "footprintsEnabled",
                    "entityClassList",
                    "entityClassListIsBlacklist",
                    "entityOverrides",
                    "defaultStride",
                    "defaultFootSize",
                    "defaultStanceWidth",
                    "babyStrideMultiplier",
                    "babyFootSizeMultiplier",
                    "babyStanceWidthMultiplier",
                    "babyEntityOverrides"),
                section(
                    config,
                    "footprints.surfaces",
                    "footprints",
                    "footprintSoundTypes",
                    "blockBlacklist",
                    "blockWhitelist"),
                section(
                    config,
                    "footprints.lifetime_and_opacity",
                    "footprints",
                    "defaultFootprintLifespan",
                    "rainLifespanMultiplier",
                    "snowLifespanMultiplier",
                    "soundTypeLifespans",
                    "defaultFootprintOpacity",
                    "soundTypeOpacities",
                    "blockOpacityOverrides",
                    "footprintParticleCap")),
            menu(
                "entity_effects",
                section(
                    config,
                    "entity_effects.cold_breath_entities_and_position",
                    "breath",
                    "breathEnabled",
                    "breathEntityClassList",
                    "breathEntityClassListIsBlacklist",
                    "breathDefaultUpOffset",
                    "breathDefaultForwardDist",
                    "breathDefaultBabyUpOffset",
                    "breathDefaultBabyForwardDist",
                    "breathEntityOverrides",
                    "breathBabyEntityOverrides"),
                section(
                    config,
                    "entity_effects.cold_breath_climate",
                    "breath",
                    "breathTemperatureThreshold",
                    "breathAltitudeThreshold",
                    "breathDimensionRules",
                    "breathDefaultDimensionMode",
                    "breathColdBiomes",
                    "breathRenderDistance"),
                section(
                    config,
                    "entity_effects.wetness",
                    "wetness",
                    "wetParticlesEnabled",
                    "wetnessEntityClassList",
                    "wetnessEntityClassListIsBlacklist",
                    "wetnessRainEnabled",
                    "wetnessDuration",
                    "wetnessParticleDensity"),
                property(config, "misc", "cakeEatingParticlesEnabled")),
            menu(
                "sounds",
                section(
                    config,
                    "sounds.armor",
                    "armor",
                    "armorSoundsEnabled",
                    "armorSoundMode",
                    "armorSoundVolume",
                    "armorDefaultCategory",
                    "armorCategoryOverrides",
                    "armorSoundEntityWhitelist"),
                section(
                    config,
                    "sounds.rain_footsteps",
                    "rain_splash",
                    "rainSplashEnabled",
                    "rainSplashVolume",
                    "rainSplashEntityClassList",
                    "rainSplashEntityClassListIsBlacklist")),
            menu(
                "water_effects",
                section(
                    config,
                    "water_effects.entry_splashes",
                    "water_splash",
                    "waterSplashEnabled",
                    "waterSplashEntityBlacklist",
                    "waterSplashFallbackColor"),
                section(
                    config,
                    "water_effects.waterfalls",
                    "water_splash",
                    "waterCascadeEnabled",
                    "waterfallSprayEnabled",
                    "waterfallSoundEnabled",
                    "waterfallSoundVolume",
                    "waterfallSoundRange",
                    "waterfallSoundCutoff"),
                section(
                    config,
                    "water_effects.underwater_chests",
                    "water_splash",
                    "chestBubblesEnabled",
                    "soulSandChestBubblesEnabled"),
                section(
                    config,
                    "water_effects.rain_and_drip_ripples",
                    "water_splash",
                    "rainRipplesEnabled",
                    "waterDripRipplesEnabled",
                    "waterRippleAlpha",
                    "rainRippleDensity"),
                section(
                    config,
                    "water_effects.wakes_and_swimming_ripples",
                    "water_splash",
                    "waterWakesEnabled",
                    "waterWakeAlpha",
                    "waterWakeDensity"),
                section(
                    config,
                    "water_effects.fluid_filters",
                    "fluid_interactions",
                    "fluidInteractionBlacklist",
                    "splashFluidBlacklist",
                    "cascadeFluidBlacklist")),
            menu(
                "coastal_waves",
                section(
                    config,
                    "coastal_waves.spawning_and_size",
                    "waves",
                    "wavesEnabled",
                    "waveSearchDistance",
                    "waveSpawnDistance",
                    "waveSpawnAmount",
                    "waveSpawnFrequency",
                    "waveSpawnDistanceFromShoreMin",
                    "waveSpawnDistanceFromShoreMax",
                    "waveSpawningFOVLimit",
                    "waveScale"),
                section(
                    config,
                    "coastal_waves.breaking_sounds",
                    "waves",
                    "waveVolume",
                    "waveSoundRange",
                    "waveBreakingSoundChance"),
                section(config, "coastal_waves.biomes", "waves", "waveBiomeWhitelist", "waveBiomeBlacklist")),
            menu(
                "camera_overhaul",
                section(
                    config,
                    "camera_overhaul.general_camera_behavior",
                    "camera",
                    "cameraOverhaulEnabled",
                    "cameraOverhaulThirdPerson",
                    "cameraDisableWhilePaused",
                    "cameraKeepFirstPersonHandStable"),
                section(
                    config,
                    "camera_overhaul.turning_roll",
                    "camera",
                    "cameraTurningRollAccumulation",
                    "cameraTurningRollIntensity",
                    "cameraTurningRollSmoothing"),
                section(
                    config,
                    "camera_overhaul.idle_sway",
                    "camera",
                    "cameraSwayIntensity",
                    "cameraSwayFrequency",
                    "cameraSwayFadeInDelay",
                    "cameraSwayFadeInLength",
                    "cameraSwayFadeOutLength"),
                section(
                    config,
                    "camera_overhaul.falling_through_the_air",
                    "camera",
                    "cameraFallingShakeEnabled",
                    "cameraFallingShakeMinDistance",
                    "cameraFallingShakeMaxDistance",
                    "cameraFallingShakeIntensity",
                    "cameraFallingShakeFrequency"),
                section(
                    config,
                    "camera_overhaul.event_shakes",
                    "camera",
                    "cameraShakeMaxIntensity",
                    "cameraShakeMaxFrequency",
                    "cameraExplosionTrauma",
                    "cameraExplosionLength",
                    "cameraThunderTrauma",
                    "cameraHandSwingTrauma"),
                section(
                    config,
                    "camera_overhaul.landing_impacts",
                    "camera",
                    "cameraFallShakeEnabled",
                    "cameraFallShakeMinDistance",
                    "cameraFallShakeMaxDistance",
                    "cameraFallShakeMaxTrauma",
                    "cameraFallShakeFrequency",
                    "cameraFallShakeLength"),
                section(
                    config,
                    "camera_overhaul.walking",
                    "camera",
                    "cameraWalkStrafingRoll",
                    "cameraWalkForwardPitch",
                    "cameraWalkVerticalPitch",
                    "cameraWalkHorizSmoothing",
                    "cameraWalkVertSmoothing"),
                section(
                    config,
                    "camera_overhaul.elytra_flight",
                    "camera",
                    "cameraFlyStrafingRoll",
                    "cameraFlyForwardPitch",
                    "cameraFlyVerticalPitch",
                    "cameraFlyHorizSmoothing",
                    "cameraFlyVertSmoothing"),
                section(
                    config,
                    "camera_overhaul.riding",
                    "camera",
                    "cameraRideStrafingRoll",
                    "cameraRideForwardPitch",
                    "cameraRideVerticalPitch",
                    "cameraRideHorizSmoothing",
                    "cameraRideVertSmoothing"),
                section(
                    config,
                    "camera_overhaul.sound_triggered_shakes",
                    "camera",
                    "cameraSoundShakesEnabled",
                    "cameraSoundShakes")),
            menu(
                "shoulder_surfing_additions",
                section(
                    config,
                    "shoulder_surfing_additions.third_person_positioning_and_perspective",
                    "camera",
                    "cameraClippingSmoothing",
                    "cameraFollowSmoothing",
                    "cameraPlayerFadeEnabled",
                    "cameraPlayerFadeStartDistance",
                    "cameraPlayerFadeEndDistance",
                    "cameraSoundCentering",
                    "cameraVerticalOffset",
                    "cameraFovOverrideEnabled",
                    "cameraFovOverride",
                    "simplePerspectiveToggle"),
                section(
                    config,
                    "shoulder_surfing_additions.decoupled_movement",
                    "camera",
                    "decoupledCameraEnabled",
                    "decoupledCameraOffsetDecay",
                    "decoupledCameraPlayerTurnSpeed",
                    "decoupledCameraTurningLockTicks"),
                section(
                    config,
                    "shoulder_surfing_additions.aiming",
                    "camera",
                    "decoupledCameraAimingActions",
                    "decoupledCameraAimingItems",
                    "decoupledCameraAimFirstPerson",
                    "decoupledCameraAimTransitionTicks",
                    "decoupledCameraAimTransitionEasing")),
            menu(
                "smooth_gui_loading_screen",
                section(
                    config,
                    "smooth_gui_loading_screen.menu_animations",
                    "smooth_gui",
                    "smoothGuiEnabled",
                    "smoothGuiAnimationTime",
                    "smoothGuiAnimationScale",
                    "smoothGuiFadeBackground",
                    "smoothGuiBackgroundFadeTime",
                    "smoothGuiAnimationStyle",
                    "smoothGuiAnimationDirection",
                    "smoothGuiExcludedScreens"),
                property(config, "misc", "loadingProgressBarEnabled")),
            menu(
                "gameplay",
                property(config, "general", "boatControlsEnabled"),
                section(
                    config,
                    "gameplay.grass_and_plant_trampling",
                    "trampling",
                    "tramplingEnabled",
                    "tramplingMinPasses",
                    "tramplingMaxPasses",
                    "tramplingForgetTime",
                    "tramplingBlocks",
                    "tramplingEntityClassList",
                    "tramplingEntityClassListIsBlacklist"),
                property(config, "misc", "blizzSnowTrailEnabled")),
            menu("debug", property(config, "debug", "debugMode"), property(config, "debug", "printMobNames")));
    }

    private static IConfigElement menu(String name, IConfigElement... children) {
        return new DummyCategoryElement<Object>(name, "anextratouch.configgui.menu." + name, Arrays.asList(children));
    }

    private static IConfigElement section(Configuration config, String name, String category, String... keys) {
        List<IConfigElement> children = new ArrayList<>();
        for (String key : keys) {
            children.add(property(config, category, key));
        }
        return new DummyCategoryElement<Object>(name, "anextratouch.configgui.menu." + name, children);
    }

    private static IConfigElement property(Configuration config, String category, String key) {
        Property property = config.getCategory(category)
            .get(key);
        if (property == null) {
            throw new IllegalArgumentException("Unknown config property: " + category + "." + key);
        }
        // Keep Forge's typed value, default, bounds, comment and list editor on the real property.
        return ConfigElement.getTypedElement(property);
    }
}
