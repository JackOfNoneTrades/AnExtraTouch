# An Extra Touch

![logo](images/logo_small.png)

Various small visual, audio, and gameplay tweaks, aimed to enhance the game experience. For Minecraft 1.7.10.

[![hub](images/badges/github.png)](https://github.com/JackOfNoneTrades/AnExtraTouch/releases)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/an-extra-touch)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/an-extra-touch)
[![maven](images/badges/maven.png)](https://maven.fentanylsolutions.org/#/releases/org/fentanylsolutions/anextratouch)
![forge](images/badges/forge.png)

![footsteps](images/gifs/footsteps.gif)

### Features:
* Entity footprints. Backported from modern Dynamic Surroundings.
* Armor walk and equip sounds. Backported from modern Dynamic Surroundings.
* Cold breath particles. Backported from modern Dynamic Surroundings.
* Water splash sound effects when walking in rain. Backported from modern Dynamic Surroundings.
* Ambient waterfall sounds. Backported from modern Dynamic Surroundings.
* Wet entities shed water particles. Backported from Legendary Survival Overhaul.
* Water splashes for entities, dropped items, and arrows entering water. Backported from Particular.
* Waterfall cascade foam particles. Backported from Particular.
* Bubbles released from underwater chests when opened. Backported from Particular.
* Water ripples from rain and water drips. Backported from Particular.
* Cake eating particles and sound. Backported from Particular.
* Boat and entity water trails. Backported from Wakes. Et Futurum boats support.
* Shore waves with breaking sounds. Backported from Coastal Waves.
* Biome water color tint applied to vanilla rain, splash, drip, and bubble particles when using [Cristaline Water](https://github.com/kanmikan/cristalinewatermod). Supports Forge fluids.
* Grass trampling. Entities can trample grass when repeatedly walking over it. Entirely server-side, off by default, players only by default.
* Thermal Foundation Blizz snow trail, similar to the Snow Golem.
* Smooth Gui backport.
* Minecraft-CameraOverhaul backport.
* Config system to add camera shakes to sounds.
* Some features backported from modern Shoulder Surfing (meant to be used along the `1.7.10` Shoulder Surfing, but should also work on their own):
  * Decoupled camera
  * Switch to first person when certain items are held, or some actions made (like bow shooting), configurable
  * Player model fading if the camera gets too close
  * Omnidirectional sprinting
* Camera follow lag in Shoulder Surfing mode
* Mixin-based Loading Progress Bar (green world load bar) port thanks to @kotmatross28729

The entire mod is heavily configurable.

This mod can be installed on client, server, or both. Armor walking sounds are more precise when the mod is installed on the server.

## Dependencies
* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)
* [GTNHLib](https://modrinth.com/mod/gtnhlib)   [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/gtnhlib)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/gtnhlib) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/GTNHLib/releases)
* [FentLib](https://github.com/JackOfNoneTrades/FentLib) [![git](images/icons/git.png)](https://github.com/JackOfNoneTrades/FentLib)

![breath](images/gifs/breath_pov.gif)
![buttons](images/gifs/drip.gif)
![footprints](images/screenshots/footprints.png)

## Building

`./gradlew build`.

## Credits
* [Dynamic Surroundings](https://github.com/OreCruncher/DynamicSurroundingsFabric). Armor sound assets and footprint texture come from this mod.
* [LegendarySurvivalOverhaul](https://github.com/Alex-Hashtag/LegendarySurvivalOverhaul).
* [Smooth Gui](https://github.com/Ezzenix/SmoothGui)
* [Minecraft-CameraOverhaul](https://github.com/Mirsario/Minecraft-CameraOverhaul)
* [Shoulder Surfing](https://github.com/Exopandora/shouldersurfing)
* [Particular](https://github.com/Chailotl/particular). Source of cascade, splash, ripple, chest bubble, and cake particle effects and textures.
* [Wakes](https://github.com/Goby56/wakes). Source of the water trail effect and textures.
* [Coastal Waves](https://www.curseforge.com/minecraft/mc-mods/coastal-waves) by Verph. Source of the shore wave effect, textures, and sounds.
* [Et Futurum Requiem](https://github.com/Roadhog360/Et-Futurum-Requiem).
* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10).

## License

`LgplV3 + SNEED`.

* [Dynamic Surroundings assets and code are licensed under MIT](https://github.com/OreCruncher/DynamicSurroundingsFabric/blob/main/LICENSE).
* [Legendary Survival Overhaul assets and code are licensed under LGPL 2.1](https://github.com/Alex-Hashtag/LegendarySurvivalOverhaul/blob/1.21.1/LICENSE.txt).
* [Smooth Gui code is licensed under CC0-1.0](https://github.com/Ezzenix/SmoothGui/blob/main/LICENSE).
* [Minecraft-CameraOverhaul code is licensed under GPL-3.0](https://github.com/Mirsario/Minecraft-CameraOverhaul/blob/dev/LICENSE.md)
* [Shoulder Surfing code is licensed under MIT](https://github.com/Exopandora/ShoulderSurfing/blob/master/LICENSE)
* [Particular assets and code are licensed under LGPL-3.0](https://github.com/Chailotl/particular/blob/master/LICENSE).
* [Wakes assets and code are licensed under GPL-3.0](https://github.com/Goby56/wakes/blob/main/LICENSE).
* [Coastal Waves](https://www.curseforge.com/minecraft/mc-mods/coastal-waves) assets and code are copyright Verph and licensed under BSD 2-Clause. `Waves-1.21.x-1.6.1.jar` metadata contains the BSD 2 license (`license = "BSD 2"`).
* [Et Futurum Requiem code is licensed under LGPL-3.0](https://github.com/Roadhog360/Et-Futurum-Requiem/blob/master/LICENSE).
* [Loading Progress Bar code is licensed under MIT](https://github.com/jbredwards/Loading-Progress-Bar/blob/1.7.10/LICENSE)

## Buy me some creatine

* [ko-fi.com](https://ko-fi.com/jackisasubtlejoke)
* Monero: `893tQ56jWt7czBsqAGPq8J5BDnYVCg2tvKpvwTcMY1LS79iDabopdxoUzNLEZtRTH4ewAcKLJ4DM4V41fvrJGHgeKArxwmJ`

<br>

![license](images/lgplsneed_small.png)
