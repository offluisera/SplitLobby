package org.github.luisera.splitlobby.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.github.luisera.splitlobby.Main;

public class TablistCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("splitcore.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            Main.getInstance().getTablistManager().reload();
            sender.sendMessage("§a[SplitLobby] §fTablist recarregada com sucesso!");
            return true;
        }

        sender.sendMessage("§eUse: /sltab reload");
        return true;
    }
}