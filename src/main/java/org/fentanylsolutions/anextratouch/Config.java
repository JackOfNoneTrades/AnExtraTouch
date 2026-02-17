package org.fentanylsolutions.anextratouch;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    private static Configuration config;

    public static class Categories {

        public static final String general = "general";
        public static final String footprints = "footprints";
        public static final String breath = "breath";
        public static final String armor = "armor";
        public static final String wetness = "wetness";
        public static final String debug = "debug";
        public static final String trampling = "trampling";
        public static final String rainSplash = "rain_splash";
        public static final String misc = "misc";
        public static final String smoothGui = "smooth_gui";
        public static final String camera = "camera";
    }

    // general

    // footprints
    public static String[] entityClassList = { "Blaze" };
    public static boolean entityClassListIsBlacklist = true;
    public static String[] entityOverrides = {};
    public static boolean footprintsEnabled = true;
    public static float defaultStride = 1.0f;
    public static float defaultFootSize = 1.0f;
    public static float defaultStanceWidth = 0.1f;
    public static float babyStrideMultiplier = 0.5f;
    public static float babyFootSizeMultiplier = 0.5f;
    public static float babyStanceWidthMultiplier = 0.5f;
    public static String[] babyEntityOverrides = {};
    public static String[] footprintSoundTypes = { "sand", "snow", "grass", "gravel" };
    public static String[] blockBlacklist = {};
    public static String[] blockWhitelist = {};
    public static int defaultFootprintLifespan = 200;
    public static float rainLifespanMultiplier = 0.4f;
    public static float snowLifespanMultiplier = 0.6f;
    public static String[] soundTypeLifespans = { "snow;100", "sand;300", "grass;200", "gravel;250" };
    public static float defaultFootprintOpacity = 0.4f;
    public static String[] soundTypeOpacities = { "snow;0.6", "sand;0.5", "grass;0.4", "gravel;0.35" };
    public static String[] blockOpacityOverrides = {};
    public static int footprintParticleCap = 2000;

    // breath
    public static String[] breathEntityClassList = { "Chicken", "SnowMan", "Zombie", "VillagerGolem", "Skeleton",
        "Creeper", "Enderman" };
    public static boolean breathEntityClassListIsBlacklist = true;
    public static float breathDefaultUpOffset = -0.4f;
    public static float breathDefaultForwardDist = 0.3f;
    public static float breathDefaultBabyUpOffset = -0.1f;
    public static float breathDefaultBabyForwardDist = 0.0f;
    public static String[] breathEntityOverrides = { "Pig;-0.3;1.1", "Cow;-0.15;0.85", "Sheep;-0.15;0.85",
        "MushroomCow;-0.15;0.85", "EntityHorse;-0.3;1.2", "Wolf;-0.2;0.3", "Ozelot;-0.15;0.2", "Villager;-0.4;0.25" };
    public static String[] breathBabyEntityOverrides = {};
    public static float breathTemperatureThreshold = 0.5f;
    public static int breathAltitudeThreshold = 150;
    public static String[] breathDimensionRules = { "-1;never", "1;never" };
    public static String[] breathColdBiomes = {};
    public static int breathRenderDistance = 64;

    // armor
    public static boolean armorSoundsEnabled = true;
    public static String armorSoundMode = "priority";
    public static float armorSoundVolume = 0.3f;
    public static String armorDefaultCategory = "heavy";
    public static String[] armorCategoryOverrides = {};
    public static String[] armorSoundEntityWhitelist = { "Player", "Zombie", "Skeleton", "PigZombie" };

    // wetness
    public static boolean wetParticlesEnabled = true;
    public static String[] wetnessEntityClassList = {};
    public static boolean wetnessEntityClassListIsBlacklist = true;
    public static boolean wetnessRainEnabled = true;
    public static float wetnessDuration = 1.0f;
    public static float wetnessParticleDensity = 1.0f;

    // trampling
    public static boolean tramplingEnabled = false;
    public static int tramplingMinPasses = 3;
    public static int tramplingMaxPasses = 8;
    public static int tramplingForgetTime = 5;
    public static String[] tramplingBlocks = { "minecraft:tallgrass", "minecraft:double_plant", "minecraft:deadbush" };
    public static String[] tramplingEntityClassList = { "Player" };
    public static boolean tramplingEntityClassListIsBlacklist = false;

    // rain splash
    public static boolean rainSplashEnabled = true;
    public static float rainSplashVolume = 0.15f;
    public static String[] rainSplashEntityClassList = {};
    public static boolean rainSplashEntityClassListIsBlacklist = true;

    // smooth gui
    public static boolean smoothGuiEnabled = true;
    public static int smoothGuiAnimationTime = 220;
    public static float smoothGuiAnimationScale = 1.0f;
    public static boolean smoothGuiFadeBackground = true;
    public static String smoothGuiAnimationStyle = "BACK";
    public static String smoothGuiAnimationDirection = "DOWN";
    public static String[] smoothGuiExcludedScreens = { "GuiChat", "GuiDownloadTerrain", "GuiMemoryErrorScreen",
        "GuiGameOver", "GuiMainMenu" };

    // camera
    public static boolean cameraOverhaulEnabled = true;
    public static boolean cameraOverhaulThirdPerson = true;
    public static boolean cameraDisableWhilePaused = true;
    public static boolean cameraKeepFirstPersonHandStable = true;
    // turning roll
    public static float cameraTurningRollAccumulation = 0.7f;
    public static float cameraTurningRollIntensity = 0.9f;
    public static float cameraTurningRollSmoothing = 1.0f;
    // camera sway
    public static float cameraSwayIntensity = 0.6f;
    public static float cameraSwayFrequency = 0.16f;
    public static float cameraSwayFadeInDelay = 0.15f;
    public static float cameraSwayFadeInLength = 5.0f;
    public static float cameraSwayFadeOutLength = 0.75f;
    public static boolean cameraFallingShakeEnabled = true;
    public static float cameraFallingShakeMinDistance = 3.0f;
    public static float cameraFallingShakeMaxDistance = 16.0f;
    public static float cameraFallingShakeIntensity = 1.2f;
    public static float cameraFallingShakeFrequency = 1.4f;
    // screen shakes
    public static float cameraShakeMaxIntensity = 2.5f;
    public static float cameraShakeMaxFrequency = 6.0f;
    public static float cameraExplosionTrauma = 1.0f;
    public static float cameraExplosionLength = 2.0f;
    public static float cameraThunderTrauma = 0.05f;
    public static float cameraHandSwingTrauma = 0.03f;
    public static boolean cameraFallShakeEnabled = true;
    public static float cameraFallShakeMinDistance = 4.0f;
    public static float cameraFallShakeMaxDistance = 20.0f;
    public static float cameraFallShakeMaxTrauma = 0.45f;
    public static float cameraFallShakeFrequency = 0.8f;
    public static float cameraFallShakeLength = 0.55f;
    // walking context
    public static float cameraWalkStrafingRoll = 10.0f;
    public static float cameraWalkForwardPitch = 7.0f;
    public static float cameraWalkVerticalPitch = 4.5f;
    public static float cameraWalkHorizSmoothing = 1.0f;
    public static float cameraWalkVertSmoothing = 0.75f;
    // riding context
    public static float cameraRideStrafingRoll = 5.0f;
    public static float cameraRideForwardPitch = 3.5f;
    public static float cameraRideVerticalPitch = 7.0f;
    public static float cameraRideHorizSmoothing = 1.0f;
    public static float cameraRideVertSmoothing = 1.0f;
    // decoupled camera (shoulder surfing integration)
    public static boolean decoupledCameraEnabled = true;
    public static float decoupledCameraOffsetDecay = 0.5f;
    public static float decoupledCameraPlayerTurnSpeed = 0.25f;

    // sound shakes
    public static boolean cameraSoundShakesEnabled = true;
    public static String[] cameraSoundShakes = {
        "sound=mob.enderdragon.growl;trauma=0.7;radius=80;frequency=2.0;duration=2.5",
        "sound=mob.enderdragon.end;trauma=1.0;radius=120;frequency=0.3;duration=11.0",
        "sound=mob.wither.spawn;trauma=0.5;radius=60;frequency=0.3;duration=2.0",
        "sound=mob.wither.idle;trauma=0.15;radius=40;frequency=0.5;duration=0.8",
        "sound=mob.wither.death;trauma=0.7;radius=80;frequency=0.3;duration=2.5",
        "sound=mob.wither.shoot;trauma=0.1;radius=30;frequency=1.0;duration=0.3", };

    // misc
    public static boolean blizzSnowTrailEnabled = true;

    // debug
    public static boolean debugMode = false;
    public static boolean printMobNames = false;

    public static void loadConfig(File configFile) {
        loadConfig(configFile, true);
    }

    public static void loadConfig(File configFile, boolean save) {
        config = new Configuration(configFile);

        try {
            // general

            // footprints
            entityClassList = config.getStringList(
                "entityClassList",
                Categories.footprints,
                entityClassList,
                "List of mobs which are either leaving footprints, or not, depending on entityClassListIsBlacklist.");
            entityClassListIsBlacklist = config.getBoolean(
                "entityClassListIsBlacklist",
                Categories.footprints,
                entityClassListIsBlacklist,
                "Whether entityClassList is a blacklist (or a whitelist).");
            entityOverrides = config.getStringList(
                "entityOverrides",
                Categories.footprints,
                entityOverrides,
                "Per-entity overrides. Format: \"mob_class_name;stride;foot_size;stance_width\". Leave a value empty to use defaults, e.g. \"Creeper;;0.5;\"");
            footprintsEnabled = config.getBoolean(
                "footprintsEnabled",
                Categories.footprints,
                footprintsEnabled,
                "Whether footprint particles are enabled.");
            defaultStride = config.getFloat(
                "defaultStride",
                Categories.footprints,
                defaultStride,
                0.1f,
                100.0f,
                "Default distance between footprints in blocks.");
            defaultFootSize = config.getFloat(
                "defaultFootSize",
                Categories.footprints,
                defaultFootSize,
                0.1f,
                100.0f,
                "Default foot size scale multiplier for the footprint particle.");
            defaultStanceWidth = config.getFloat(
                "defaultStanceWidth",
                Categories.footprints,
                defaultStanceWidth,
                0.0f,
                100.0f,
                "Default perpendicular offset between left and right foot in blocks.");
            babyStrideMultiplier = config.getFloat(
                "babyStrideMultiplier",
                Categories.footprints,
                babyStrideMultiplier,
                0.1f,
                100.0f,
                "Multiplier applied to stride for baby mobs.");
            babyFootSizeMultiplier = config.getFloat(
                "babyFootSizeMultiplier",
                Categories.footprints,
                babyFootSizeMultiplier,
                0.1f,
                100.0f,
                "Multiplier applied to foot size for baby mobs.");
            babyStanceWidthMultiplier = config.getFloat(
                "babyStanceWidthMultiplier",
                Categories.footprints,
                babyStanceWidthMultiplier,
                0.1f,
                100.0f,
                "Multiplier applied to stance width for baby mobs.");
            babyEntityOverrides = config.getStringList(
                "babyEntityOverrides",
                Categories.footprints,
                babyEntityOverrides,
                "Per-entity baby overrides (absolute values, not multiplied). Format: \"mob_class_name;stride;foot_size;stance_width\". Leave a value empty to use adult value * baby multiplier.");
            footprintSoundTypes = config.getStringList(
                "footprintSoundTypes",
                Categories.footprints,
                footprintSoundTypes,
                "Sound types that allow footprints. Valid values: stone, wood, gravel, grass, cloth, sand, snow, ladder, anvil.");
            blockBlacklist = config.getStringList(
                "blockBlacklist",
                Categories.footprints,
                blockBlacklist,
                "Blocks that should never have footprints, regardless of sound type. Use registry names, e.g. \"minecraft:tallgrass\".");
            blockWhitelist = config.getStringList(
                "blockWhitelist",
                Categories.footprints,
                blockWhitelist,
                "Blocks that should always have footprints, even if their sound type is not in the list. Use registry names, e.g. \"minecraft:stone\".");
            defaultFootprintLifespan = config.getInt(
                "defaultFootprintLifespan",
                Categories.footprints,
                defaultFootprintLifespan,
                1,
                6000,
                "Default footprint lifespan in ticks (20 ticks = 1 second).");
            rainLifespanMultiplier = config.getFloat(
                "rainLifespanMultiplier",
                Categories.footprints,
                rainLifespanMultiplier,
                0.0f,
                100.0f,
                "Multiplier applied to footprint lifespan when raining at the footprint's position.");
            snowLifespanMultiplier = config.getFloat(
                "snowLifespanMultiplier",
                Categories.footprints,
                snowLifespanMultiplier,
                0.0f,
                100.0f,
                "Multiplier applied to footprint lifespan when snowing at the footprint's position.");
            soundTypeLifespans = config.getStringList(
                "soundTypeLifespans",
                Categories.footprints,
                soundTypeLifespans,
                "Per-sound-type lifespan overrides. Format: \"sound_type;lifespan_in_ticks\". e.g. \"snow;100\", \"sand;300\".");
            defaultFootprintOpacity = config.getFloat(
                "defaultFootprintOpacity",
                Categories.footprints,
                defaultFootprintOpacity,
                0.0f,
                100.0f,
                "Default footprint opacity (0.0 = invisible, 1.0 = fully opaque).");
            soundTypeOpacities = config.getStringList(
                "soundTypeOpacities",
                Categories.footprints,
                soundTypeOpacities,
                "Per-sound-type opacity overrides. Format: \"sound_type;opacity\". e.g. \"snow;0.6\", \"sand;0.3\".");
            blockOpacityOverrides = config.getStringList(
                "blockOpacityOverrides",
                Categories.footprints,
                blockOpacityOverrides,
                "Per-block opacity overrides (takes priority over sound type). Format: \"registry_name;opacity\". e.g. \"minecraft:snow_layer;0.7\".");
            footprintParticleCap = config.getInt(
                "footprintParticleCap",
                Categories.footprints,
                footprintParticleCap,
                0,
                100000,
                "Maximum number of footprint particles tracked at once. 0 = unlimited.");

            // breath
            breathEntityClassList = config.getStringList(
                "breathEntityClassList",
                Categories.breath,
                breathEntityClassList,
                "List of mobs which either show breath, or don't, depending on breathEntityClassListIsBlacklist.");
            breathEntityClassListIsBlacklist = config.getBoolean(
                "breathEntityClassListIsBlacklist",
                Categories.breath,
                breathEntityClassListIsBlacklist,
                "Whether breathEntityClassList is a blacklist (or a whitelist).");
            breathDefaultUpOffset = config.getFloat(
                "breathDefaultUpOffset",
                Categories.breath,
                breathDefaultUpOffset,
                -100.0f,
                100.0f,
                "Default vertical offset from top of bounding box for breath spawn point.");
            breathDefaultForwardDist = config.getFloat(
                "breathDefaultForwardDist",
                Categories.breath,
                breathDefaultForwardDist,
                -100.0f,
                100.0f,
                "Default forward distance along look direction for breath spawn point.");
            breathDefaultBabyUpOffset = config.getFloat(
                "breathDefaultBabyUpOffset",
                Categories.breath,
                breathDefaultBabyUpOffset,
                -100.0f,
                100.0f,
                "Default vertical offset for baby entities.");
            breathDefaultBabyForwardDist = config.getFloat(
                "breathDefaultBabyForwardDist",
                Categories.breath,
                breathDefaultBabyForwardDist,
                -100.0f,
                100.0f,
                "Default forward distance for baby entities.");
            breathEntityOverrides = config.getStringList(
                "breathEntityOverrides",
                Categories.breath,
                breathEntityOverrides,
                "Per-entity breath position overrides. Format: \"mob_name;upOffset;forwardDist\". Leave a value empty to use defaults, e.g. \"Pig;;0.4\".");
            breathBabyEntityOverrides = config.getStringList(
                "breathBabyEntityOverrides",
                Categories.breath,
                breathBabyEntityOverrides,
                "Per-entity baby breath position overrides. Format: \"mob_name;upOffset;forwardDist\". Leave empty to use adult value.");
            breathTemperatureThreshold = config.getFloat(
                "breathTemperatureThreshold",
                Categories.breath,
                breathTemperatureThreshold,
                -100.0f,
                100.0f,
                "Biome temperature below which breath is visible. Vanilla temps: Ice Plains 0.0, Taiga 0.25, Extreme Hills 0.2, Plains 0.8, Desert 2.0. Temperature also drops with altitude above Y=64.");
            breathAltitudeThreshold = config.getInt(
                "breathAltitudeThreshold",
                Categories.breath,
                breathAltitudeThreshold,
                0,
                1000,
                "Above this Y level, breath is always visible regardless of biome temperature.");
            breathDimensionRules = config.getStringList(
                "breathDimensionRules",
                Categories.breath,
                breathDimensionRules,
                "Per-dimension breath rules. Format: \"dimensionId;mode\". Modes: \"normal\" (temperature + altitude checks), \"always\" (breath always visible), \"never\" (breath disabled). Unlisted dimensions default to \"normal\".");
            breathColdBiomes = config.getStringList(
                "breathColdBiomes",
                Categories.breath,
                breathColdBiomes,
                "Biomes that always show breath regardless of temperature. Use biome names, e.g. \"Extreme Hills\", \"FrozenRiver\".");
            breathRenderDistance = config.getInt(
                "breathRenderDistance",
                Categories.breath,
                breathRenderDistance,
                1,
                1000,
                "Maximum distance in blocks at which breath particles are rendered.");

            // armor
            armorSoundsEnabled = config.getBoolean(
                "armorSoundsEnabled",
                Categories.armor,
                armorSoundsEnabled,
                "Enable armor accent sounds on footsteps.");
            armorSoundMode = config.getString(
                "armorSoundMode",
                Categories.armor,
                armorSoundMode,
                "How to handle mixed armor. \"priority\" picks the highest-priority category between chest and legs. \"mixed\" plays both chest and legs sounds if different.");
            armorSoundVolume = config.getFloat(
                "armorSoundVolume",
                Categories.armor,
                armorSoundVolume,
                0.0f,
                1.0f,
                "Base volume for armor accent sounds.");
            armorDefaultCategory = config.getString(
                "armorDefaultCategory",
                Categories.armor,
                armorDefaultCategory,
                "Default armor sound category for unknown/modded armor items. Valid: light, medium, heavy, crystal.");
            armorCategoryOverrides = config.getStringList(
                "armorCategoryOverrides",
                Categories.armor,
                armorCategoryOverrides,
                "Per-item armor category overrides. Format: \"registry_name;category\". e.g. \"minecraft:iron_chestplate;crystal\". Valid categories: light, medium, heavy, crystal.");
            armorSoundEntityWhitelist = config.getStringList(
                "armorSoundEntityWhitelist",
                Categories.armor,
                armorSoundEntityWhitelist,
                "Entity classes that play armor sounds. Only entities in this list will have armor accent sounds.");

            // wetness
            wetParticlesEnabled = config.getBoolean(
                "wetParticlesEnabled",
                Categories.wetness,
                wetParticlesEnabled,
                "Enable water drip particles when entities exit water or stand in rain.");
            wetnessEntityClassList = config.getStringList(
                "wetnessEntityClassList",
                Categories.wetness,
                wetnessEntityClassList,
                "List of mobs which either show wetness particles, or don't, depending on wetnessEntityClassListIsBlacklist.");
            wetnessEntityClassListIsBlacklist = config.getBoolean(
                "wetnessEntityClassListIsBlacklist",
                Categories.wetness,
                wetnessEntityClassListIsBlacklist,
                "Whether wetnessEntityClassList is a blacklist (or a whitelist).");
            wetnessRainEnabled = config.getBoolean(
                "wetnessRainEnabled",
                Categories.wetness,
                wetnessRainEnabled,
                "Whether standing in rain causes wetness buildup.");
            wetnessDuration = config.getFloat(
                "wetnessDuration",
                Categories.wetness,
                wetnessDuration,
                0.1f,
                100.0f,
                "Multiplier for how long entities stay wet after leaving water. Higher values = longer dripping. 1.0 = default (~65 seconds from fully soaked).");
            wetnessParticleDensity = config.getFloat(
                "wetnessParticleDensity",
                Categories.wetness,
                wetnessParticleDensity,
                0.1f,
                100.0f,
                "Multiplier for particle spawn frequency. Higher values = more water drops.");

            // trampling
            tramplingEnabled = config.getBoolean(
                "tramplingEnabled",
                Categories.trampling,
                tramplingEnabled,
                "Enable grass/plant trampling. Blocks walked through repeatedly will break after a random number of passes.");
            tramplingMinPasses = config.getInt(
                "tramplingMinPasses",
                Categories.trampling,
                tramplingMinPasses,
                1,
                100,
                "Minimum number of passes before a block breaks.");
            tramplingMaxPasses = config.getInt(
                "tramplingMaxPasses",
                Categories.trampling,
                tramplingMaxPasses,
                1,
                100,
                "Maximum number of passes before a block breaks. A random threshold between min and max is chosen per block.");
            tramplingForgetTime = config.getInt(
                "tramplingForgetTime",
                Categories.trampling,
                tramplingForgetTime,
                1,
                60,
                "Minutes before the tracker forgets about a block that hasn't been walked through.");
            tramplingBlocks = config.getStringList(
                "tramplingBlocks",
                Categories.trampling,
                tramplingBlocks,
                "Blocks that can be trampled. Use registry names, e.g. \"minecraft:tallgrass\".");
            tramplingEntityClassList = config.getStringList(
                "tramplingEntityClassList",
                Categories.trampling,
                tramplingEntityClassList,
                "Entity classes that trample blocks (or are excluded, depending on tramplingEntityClassListIsBlacklist). Use entity class names, e.g. \"Player\", \"Zombie\", \"Cow\".");
            tramplingEntityClassListIsBlacklist = config.getBoolean(
                "tramplingEntityClassListIsBlacklist",
                Categories.trampling,
                tramplingEntityClassListIsBlacklist,
                "Whether tramplingEntityClassList is a blacklist (true) or whitelist (false).");

            // rain splash
            rainSplashEnabled = config.getBoolean(
                "rainSplashEnabled",
                Categories.rainSplash,
                rainSplashEnabled,
                "Enable rain splash accent sounds on footsteps when walking in rain.");
            rainSplashVolume = config.getFloat(
                "rainSplashVolume",
                Categories.rainSplash,
                rainSplashVolume,
                0.0f,
                1.0f,
                "Base volume for rain splash sounds. Actual volume is scaled by rain intensity.");
            rainSplashEntityClassList = config.getStringList(
                "rainSplashEntityClassList",
                Categories.rainSplash,
                rainSplashEntityClassList,
                "List of mobs which either play rain splash sounds, or don't, depending on rainSplashEntityClassListIsBlacklist.");
            rainSplashEntityClassListIsBlacklist = config.getBoolean(
                "rainSplashEntityClassListIsBlacklist",
                Categories.rainSplash,
                rainSplashEntityClassListIsBlacklist,
                "Whether rainSplashEntityClassList is a blacklist (or a whitelist).");

            // smooth gui
            smoothGuiEnabled = config.getBoolean(
                "smoothGuiEnabled",
                Categories.smoothGui,
                smoothGuiEnabled,
                "Enable smooth GUI opening animations (slide-in and background fade).");
            smoothGuiAnimationTime = config.getInt(
                "smoothGuiAnimationTime",
                Categories.smoothGui,
                smoothGuiAnimationTime,
                10,
                10000,
                "Animation duration in milliseconds.");
            smoothGuiAnimationScale = config.getFloat(
                "smoothGuiAnimationScale",
                Categories.smoothGui,
                smoothGuiAnimationScale,
                0.0f,
                100.0f,
                "Animation intensity multiplier. Higher values = larger displacement.");
            smoothGuiFadeBackground = config.getBoolean(
                "smoothGuiFadeBackground",
                Categories.smoothGui,
                smoothGuiFadeBackground,
                "Fade in the dark background overlay when opening GUIs in-world.");
            smoothGuiAnimationStyle = config.getString(
                "smoothGuiAnimationStyle",
                Categories.smoothGui,
                smoothGuiAnimationStyle,
                "Easing curve for the animation. \"BACK\" has a slight overshoot bounce, \"CUBIC\" is a simple smooth curve.",
                new String[] { "BACK", "CUBIC" });
            smoothGuiAnimationDirection = config.getString(
                "smoothGuiAnimationDirection",
                Categories.smoothGui,
                smoothGuiAnimationDirection,
                "Direction the GUI slides in from. \"DOWN\" slides up from below, \"UP\" slides down from above.",
                new String[] { "DOWN", "UP" });
            smoothGuiExcludedScreens = config.getStringList(
                "smoothGuiExcludedScreens",
                Categories.smoothGui,
                smoothGuiExcludedScreens,
                "GUI screen classes that should not be animated. Can be simple names (e.g. \"GuiChat\") or fully qualified (e.g. \"net.minecraft.client.gui.GuiChat\").");

            // camera
            cameraOverhaulEnabled = config.getBoolean(
                "cameraOverhaulEnabled",
                Categories.camera,
                cameraOverhaulEnabled,
                "Enable dynamic camera effects (velocity pitch, strafing roll, turning roll, idle sway).");
            cameraOverhaulThirdPerson = config.getBoolean(
                "cameraOverhaulThirdPerson",
                Categories.camera,
                cameraOverhaulThirdPerson,
                "Apply camera effects in third person view.");
            cameraDisableWhilePaused = config.getBoolean(
                "cameraDisableWhilePaused",
                Categories.camera,
                cameraDisableWhilePaused,
                "Disable camera offsets and shakes while the pause menu is open.");
            cameraKeepFirstPersonHandStable = config.getBoolean(
                "cameraKeepFirstPersonHandStable",
                Categories.camera,
                cameraKeepFirstPersonHandStable,
                "Keep first-person hand/item stable instead of inheriting camera tilt effects.");
            cameraTurningRollAccumulation = config.getFloat(
                "cameraTurningRollAccumulation",
                Categories.camera,
                cameraTurningRollAccumulation,
                0.0f,
                100.0f,
                "How quickly turning roll accumulates when rotating the camera.");
            cameraTurningRollIntensity = config.getFloat(
                "cameraTurningRollIntensity",
                Categories.camera,
                cameraTurningRollIntensity,
                0.0f,
                100.0f,
                "Maximum intensity of the turning roll effect.");
            cameraTurningRollSmoothing = config.getFloat(
                "cameraTurningRollSmoothing",
                Categories.camera,
                cameraTurningRollSmoothing,
                0.0f,
                100.0f,
                "Smoothing factor for turning roll decay. Higher = slower decay.");
            cameraSwayIntensity = config.getFloat(
                "cameraSwayIntensity",
                Categories.camera,
                cameraSwayIntensity,
                0.0f,
                100.0f,
                "Intensity of idle camera sway.");
            cameraSwayFrequency = config.getFloat(
                "cameraSwayFrequency",
                Categories.camera,
                cameraSwayFrequency,
                0.01f,
                100.0f,
                "Frequency of idle camera sway oscillation.");
            cameraSwayFadeInDelay = config.getFloat(
                "cameraSwayFadeInDelay",
                Categories.camera,
                cameraSwayFadeInDelay,
                0.0f,
                100.0f,
                "Seconds of inactivity before camera sway begins fading in.");
            cameraSwayFadeInLength = config.getFloat(
                "cameraSwayFadeInLength",
                Categories.camera,
                cameraSwayFadeInLength,
                0.0f,
                100.0f,
                "Duration in seconds for camera sway to fully fade in.");
            cameraSwayFadeOutLength = config.getFloat(
                "cameraSwayFadeOutLength",
                Categories.camera,
                cameraSwayFadeOutLength,
                0.0f,
                100.0f,
                "Duration in seconds for camera sway to fade out when the player moves.");
            cameraFallingShakeEnabled = config.getBoolean(
                "cameraFallingShakeEnabled",
                Categories.camera,
                cameraFallingShakeEnabled,
                "Add continuous shake while descending after falling past a threshold.");
            cameraFallingShakeMinDistance = config.getFloat(
                "cameraFallingShakeMinDistance",
                Categories.camera,
                cameraFallingShakeMinDistance,
                0.0f,
                1000.0f,
                "Minimum fall distance before in-air fall shake starts.");
            cameraFallingShakeMaxDistance = config.getFloat(
                "cameraFallingShakeMaxDistance",
                Categories.camera,
                cameraFallingShakeMaxDistance,
                0.1f,
                1000.0f,
                "Fall distance at which in-air fall shake reaches maximum intensity.");
            cameraFallingShakeIntensity = config.getFloat(
                "cameraFallingShakeIntensity",
                Categories.camera,
                cameraFallingShakeIntensity,
                0.0f,
                100.0f,
                "Maximum intensity of in-air fall shake.");
            cameraFallingShakeFrequency = config.getFloat(
                "cameraFallingShakeFrequency",
                Categories.camera,
                cameraFallingShakeFrequency,
                0.1f,
                100.0f,
                "Noise frequency of in-air fall shake.");
            cameraShakeMaxIntensity = config.getFloat(
                "cameraShakeMaxIntensity",
                Categories.camera,
                cameraShakeMaxIntensity,
                0.0f,
                100.0f,
                "Maximum combined intensity of all active screen shakes.");
            cameraShakeMaxFrequency = config.getFloat(
                "cameraShakeMaxFrequency",
                Categories.camera,
                cameraShakeMaxFrequency,
                0.1f,
                100.0f,
                "Maximum frequency of screen shake noise sampling.");
            cameraExplosionTrauma = config.getFloat(
                "cameraExplosionTrauma",
                Categories.camera,
                cameraExplosionTrauma,
                0.0f,
                1.0f,
                "Trauma intensity for explosion screen shakes.");
            cameraExplosionLength = config.getFloat(
                "cameraExplosionLength",
                Categories.camera,
                cameraExplosionLength,
                0.0f,
                100.0f,
                "Duration in seconds for explosion screen shakes.");
            cameraThunderTrauma = config.getFloat(
                "cameraThunderTrauma",
                Categories.camera,
                cameraThunderTrauma,
                0.0f,
                1.0f,
                "Trauma intensity for lightning/thunder screen shakes.");
            cameraHandSwingTrauma = config.getFloat(
                "cameraHandSwingTrauma",
                Categories.camera,
                cameraHandSwingTrauma,
                0.0f,
                1.0f,
                "Trauma intensity for hand swing screen shakes.");
            cameraFallShakeEnabled = config.getBoolean(
                "cameraFallShakeEnabled",
                Categories.camera,
                cameraFallShakeEnabled,
                "Add a landing screen shake when falling from sufficient height.");
            cameraFallShakeMinDistance = config.getFloat(
                "cameraFallShakeMinDistance",
                Categories.camera,
                cameraFallShakeMinDistance,
                0.0f,
                1000.0f,
                "Minimum fall distance before landing shake starts.");
            cameraFallShakeMaxDistance = config.getFloat(
                "cameraFallShakeMaxDistance",
                Categories.camera,
                cameraFallShakeMaxDistance,
                0.1f,
                1000.0f,
                "Fall distance at which landing shake reaches maximum trauma.");
            cameraFallShakeMaxTrauma = config.getFloat(
                "cameraFallShakeMaxTrauma",
                Categories.camera,
                cameraFallShakeMaxTrauma,
                0.0f,
                1.0f,
                "Maximum trauma applied by landing shake.");
            cameraFallShakeFrequency = config.getFloat(
                "cameraFallShakeFrequency",
                Categories.camera,
                cameraFallShakeFrequency,
                0.1f,
                100.0f,
                "Noise frequency multiplier for landing shake.");
            cameraFallShakeLength = config.getFloat(
                "cameraFallShakeLength",
                Categories.camera,
                cameraFallShakeLength,
                0.05f,
                100.0f,
                "Duration in seconds of landing shake.");
            cameraWalkStrafingRoll = config.getFloat(
                "cameraWalkStrafingRoll",
                Categories.camera,
                cameraWalkStrafingRoll,
                0.0f,
                100.0f,
                "Strafing roll factor when walking.");
            cameraWalkForwardPitch = config.getFloat(
                "cameraWalkForwardPitch",
                Categories.camera,
                cameraWalkForwardPitch,
                0.0f,
                100.0f,
                "Forward velocity pitch factor when walking.");
            cameraWalkVerticalPitch = config.getFloat(
                "cameraWalkVerticalPitch",
                Categories.camera,
                cameraWalkVerticalPitch,
                0.0f,
                100.0f,
                "Vertical velocity pitch factor when walking.");
            cameraWalkHorizSmoothing = config.getFloat(
                "cameraWalkHorizSmoothing",
                Categories.camera,
                cameraWalkHorizSmoothing,
                0.0f,
                100.0f,
                "Horizontal velocity smoothing factor when walking.");
            cameraWalkVertSmoothing = config.getFloat(
                "cameraWalkVertSmoothing",
                Categories.camera,
                cameraWalkVertSmoothing,
                0.0f,
                100.0f,
                "Vertical velocity smoothing factor when walking.");
            cameraRideStrafingRoll = config.getFloat(
                "cameraRideStrafingRoll",
                Categories.camera,
                cameraRideStrafingRoll,
                0.0f,
                100.0f,
                "Strafing roll factor when riding.");
            cameraRideForwardPitch = config.getFloat(
                "cameraRideForwardPitch",
                Categories.camera,
                cameraRideForwardPitch,
                0.0f,
                100.0f,
                "Forward velocity pitch factor when riding.");
            cameraRideVerticalPitch = config.getFloat(
                "cameraRideVerticalPitch",
                Categories.camera,
                cameraRideVerticalPitch,
                0.0f,
                100.0f,
                "Vertical velocity pitch factor when riding.");
            cameraRideHorizSmoothing = config.getFloat(
                "cameraRideHorizSmoothing",
                Categories.camera,
                cameraRideHorizSmoothing,
                0.0f,
                100.0f,
                "Horizontal velocity smoothing factor when riding.");
            cameraRideVertSmoothing = config.getFloat(
                "cameraRideVertSmoothing",
                Categories.camera,
                cameraRideVertSmoothing,
                0.0f,
                100.0f,
                "Vertical velocity smoothing factor when riding.");

            // decoupled camera
            decoupledCameraEnabled = config.getBoolean(
                "decoupledCameraEnabled",
                Categories.camera,
                decoupledCameraEnabled,
                "Enable decoupled camera when Shoulder Surfing is active. Mouse rotates camera independently of player body.");
            decoupledCameraOffsetDecay = config.getFloat(
                "decoupledCameraOffsetDecay",
                Categories.camera,
                decoupledCameraOffsetDecay,
                0.0f,
                1.0f,
                "Free look offset decay rate per tick. 0 = instant snap back, 1 = no decay (offsets persist).");
            decoupledCameraPlayerTurnSpeed = config.getFloat(
                "decoupledCameraPlayerTurnSpeed",
                Categories.camera,
                decoupledCameraPlayerTurnSpeed,
                0.0f,
                1.0f,
                "How fast the player body turns to face movement direction when decoupled. 0 = no turning, 1 = instant.");

            // sound shakes
            cameraSoundShakesEnabled = config.getBoolean(
                "cameraSoundShakesEnabled",
                Categories.camera,
                cameraSoundShakesEnabled,
                "Enable camera shakes triggered by specific sounds.");
            cameraSoundShakes = config.getStringList(
                "cameraSoundShakes",
                Categories.camera,
                cameraSoundShakes,
                "Sounds that trigger camera shake. Format: \"sound=<name>;trauma=<0..1>;radius=<blocks>;frequency=<speed>;duration=<seconds>\". Only sound and trauma are required. trauma is scaled by the sound's effective volume (including category/master sliders). radius (default 16) is the distance falloff range. frequency (default 1.0) controls shake speed. duration (default 0.3) is shake length in seconds. Use the sound resource path (e.g. mob.zombie.step) or full name (e.g. minecraft:mob.zombie.step) to only match a specific domain. Examples: \"sound=mob.zombie.step;trauma=0.05\", \"sound=random.explode;trauma=0.8;radius=32;frequency=1.5;duration=2.0\".");

            // misc
            blizzSnowTrailEnabled = config.getBoolean(
                "blizzSnowTrailEnabled",
                Categories.misc,
                blizzSnowTrailEnabled,
                "Allow Thermal Foundation Blizz mobs to leave a snow trail like vanilla snow golems.");

            // Debug
            debugMode = config.getBoolean("debugMode", Categories.debug, debugMode, "Enable debug logging");
            printMobNames = config.getBoolean(
                "printMobNames",
                Categories.debug,
                printMobNames,
                "Print all registered mob class names to the log during postInit. Useful for configuring entityClassList and entityOverrides.");

        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
        } finally {
            if (save) {
                config.save();
            }
        }
    }

    public static Configuration getRawConfig() {
        return config;
    }
}
