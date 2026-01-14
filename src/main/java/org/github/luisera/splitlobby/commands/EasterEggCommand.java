package org.github.luisera.splitlobby.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.managers.EasterEggManager;

import java.util.Set;

public class EasterEggCommand implements CommandExecutor {

    private final EasterEggManager manager;

    public EasterEggCommand(EasterEggManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("sl.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUse: /slsecret <get | set | remove> [id]");
            return true;
        }

        // PEGAR CABEÇA
        if (args[0].equalsIgnoreCase("get")) {
            player.getInventory().addItem(manager.getSkullItem());
            player.sendMessage("§aVocê recebeu a cabeça secreta.");
            return true;
        }

        // SETAR
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /slsecret set <nome_do_id>");
                return true;
            }
            String id = args[1].toLowerCase();

            Block target = player.getTargetBlock((Set<Material>) null, 5);
            if (target == null || target.getType() == Material.AIR) {
                player.sendMessage("§cOlhe para um bloco.");
                return true;
            }

            manager.addSecret(id, target.getLocation());
            player.sendMessage("§aSegredo '" + id + "' definido no bloco " + target.getType().name());
            return true;
        }

        // REMOVER
        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                player.sendMessage("§cUse: /slsecret remove <nome_do_id>");
                return true;
            }
            String id = args[1].toLowerCase();
            manager.removeSecret(id);
            player.sendMessage("§aSegredo '" + id + "' removido.");
            return true;
        }

        return true;
    }
}