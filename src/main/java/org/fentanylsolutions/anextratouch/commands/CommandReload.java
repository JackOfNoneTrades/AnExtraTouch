package org.fentanylsolutions.anextratouch.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

public class CommandReload extends CommandBase {

    @Override
    public String getCommandName() {
        return "aet_reload";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/aet_reload Reloads An Extra Touch config from file";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Config.loadConfig(AnExtraTouch.confFile, false);
        sender.addChatMessage(new ChatComponentText("[AnExtraTouch] Config reloaded."));
    }
}
