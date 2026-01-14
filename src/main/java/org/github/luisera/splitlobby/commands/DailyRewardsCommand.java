package org.github.luisera.splitlobby.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.managers.DailyRewardsManager;

public class DailyRewardsCommand implements CommandExecutor {

    private final DailyRewardsManager manager;

    public DailyRewardsCommand(DailyRewardsManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Apenas jogadores podem usar
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return true;
        }

        Player p = (Player) sender;

        // ===================================
        // SEM ARGUMENTOS: Abre o menu
        // ===================================
        if (args.length == 0) {
            manager.openMenu(p);
            return true;
        }

        // ===================================
        // COM ARGUMENTOS: Verifica qual
        // ===================================
        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "reload":
                // /recompensas reload
                if (!p.hasPermission("splitlobby.admin")) {
                    p.sendMessage("§cVocê não tem permissão!");
                    return true;
                }

                manager.loadConfig();
                p.sendMessage("§a✓ Configuração de recompensas recarregada!");
                return true;

            case "setnpc":
                // /recompensas setnpc
                if (!p.hasPermission("splitlobby.admin")) {
                    p.sendMessage("§cVocê não tem permissão!");
                    return true;
                }

                manager.spawnNPC(p.getLocation());
                p.sendMessage("§a✓ NPC Entregador spawnado com sucesso!");
                return true;

            case "removenpc":
            case "unsetnpc":
            case "delnpc":
                // /recompensas removenpc
                if (!p.hasPermission("splitlobby.admin")) {
                    p.sendMessage("§cVocê não tem permissão!");
                    return true;
                }

                manager.removeNPC(p);
                p.sendMessage("§a✓ NPC Entregador removido com sucesso!");
                return true;

            case "help":
            case "ajuda":
                // /recompensas help
                p.sendMessage("");
                p.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                p.sendMessage("§6§l  RECOMPENSAS - COMANDOS");
                p.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                p.sendMessage("");
                p.sendMessage("  §f/recompensas §7- Abre o menu");

                if (p.hasPermission("splitlobby.admin")) {
                    p.sendMessage("  §f/recompensas setnpc §7- Spawna NPC");
                    p.sendMessage("  §f/recompensas removenpc §7- Remove NPC");
                    p.sendMessage("  §f/recompensas reload §7- Recarrega config");
                }

                p.sendMessage("");
                p.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                return true;

            default:
                // Comando desconhecido, abre o menu
                manager.openMenu(p);
                return true;
        }
    }
}