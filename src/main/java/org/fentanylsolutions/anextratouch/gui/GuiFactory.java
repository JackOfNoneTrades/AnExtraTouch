package org.fentanylsolutions.anextratouch.gui;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;

@SuppressWarnings("unused")
public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigGui extends GuiConfig {

        public ConfigGui(GuiScreen parentScreen) {
            super(
                parentScreen,
                ImmutableList.of(
                    // Construct directly here to prevent stale references
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.general)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.footprints)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.breath)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.armor)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.rainSplash)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.misc)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.wetness)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.trampling)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.smoothGui)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.camera)),
                    new ConfigElement(
                        Config.getRawConfig()
                            .getCategory(Config.Categories.debug))),
                AnExtraTouch.MODID,
                AnExtraTouch.MODID,
                false,
                false,
                I18n.format("anextratouch.configgui.title"));
        }

        @Override
        public void initGui() {
            // You can add buttons and initialize fields here
            super.initGui();
            AnExtraTouch.debug("Initializing config gui");
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            // You can do things like create animations, draw additional elements, etc. here
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void actionPerformed(GuiButton b) {
            AnExtraTouch.debug("Config button id " + b.id + " pressed");
            super.actionPerformed(b);
            /* "Done" button */
            if (b.id == 2000) {
                /* Syncing config */
                AnExtraTouch.debug("Saving config");
                Config.getRawConfig()
                    .save();
                Config.loadConfig(AnExtraTouch.confFile);
                AnExtraTouch.proxy.onConfigReload();
            }
        }
    }
}
