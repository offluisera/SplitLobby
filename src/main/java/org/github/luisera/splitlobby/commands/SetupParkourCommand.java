package org.github.luisera.splitlobby.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.managers.ParkourManager;

public class SetupParkourCommand implements CommandExecutor {

    private final ParkourManager parkourManager;

    public SetupParkourCommand(ParkourManager parkourManager) {
        this.parkourManager = parkourManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sl.admin")) {
            player.sendMessage("§cVocê não tem permissão.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUse: /slparkour <setup | set | unset>");
            return true;
        }

        // 1. SETUP DO PERCURSO
        if (args[0].equalsIgnoreCase("setup")) {
            parkourManager.toggleSetup(player);
            return true;
        }

        // 2. SETAR OS TOPS
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("tops")) {

                // --- VERIFICAÇÃO NOVA ---
                if (parkourManager.getTopHologramLocation() != null) {
                    player.sendMessage("§cERRO! Holograma de tops já setado.");
                    player.sendMessage("§cUtilize /slparkour unset tops e tente novamente.");
                    return true;
                }

                parkourManager.setTopHologramLocation(player.getLocation());
                player.sendMessage("§aHolograma de TOP 10 definido na sua posição!");
                player.sendMessage("§7Ele será atualizado automaticamente.");
                return true;
            }
        }

        // 3. REMOVER (UNSET) OS TOPS
        if (args[0].equalsIgnoreCase("unset")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("tops")) {

                if (parkourManager.getTopHologramLocation() == null) {
                    player.sendMessage("§cNão há nenhum holograma de Top 10 definido.");
                    return true;
                }

                parkourManager.removeLeaderboard();
                player.sendMessage("§aSucesso! O holograma de Top 10 foi removido.");
                return true;
            }
        }

        player.sendMessage("§cComando inválido.");
        return true;
    }
}