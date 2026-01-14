package org.github.luisera.splitlobby.commands;

import com.cryptomorin.xseries.XSound;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
// Importe sua classe Main corretamente
import org.github.luisera.splitlobby.Main;

public class SetLobbyCommand implements CommandExecutor {

    private final Main plugin;

    public SetLobbyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sl.admin")) {
            player.sendMessage("§cVocê não tem permissão.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("lobby")) {
            Location location = player.getLocation();

            // --- MUDANÇA AQUI ---
            // Usa o getSpawnConfig() e saveSpawnConfig() que criamos na Main
            plugin.getSpawnConfig().set("spawn-lobby", location);
            plugin.saveSpawnConfig();

            // Atualiza a variável na memória imediatamente para não precisar recarregar
            // (Você precisará criar um 'setLobbyLocation' na Main ou acessar a variável se for pública,
            // mas como o Listener lê do arquivo ou da variável carregada no onEnable,
            // o ideal é avisar para recarregar ou reiniciar, mas para simplificar:)
            player.sendMessage("§7[§e§fSPLITCORE§7] §aLocal de spawn salvo com suceso!");

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
            return true;
        }

        player.sendMessage("§cUse: /slset lobby");
        return true;
    }
}