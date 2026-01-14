package org.github.luisera.splitlobby.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.github.luisera.splitlobby.Main;

public class ScoreboardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("splitcore.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            Main.getInstance().getScoreboardManager().reload();
            sender.sendMessage("§a[SplitLobby] §fScoreboard recarregada com sucesso!");
            return true;
        }

        sender.sendMessage("§eUse: /slscore reload");
        return true;
    }
}