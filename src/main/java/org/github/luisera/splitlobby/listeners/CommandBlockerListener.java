package org.github.luisera.splitlobby.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.github.luisera.splitlobby.Main;

import java.util.List;

public class CommandBlockerListener implements Listener {

    private final Main plugin;

    public CommandBlockerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Executa por último para garantir o cancelamento
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        // Se for Admin, pode tudo
        if (p.hasPermission("splitcore.admin") || p.hasPermission("splitcore.bypass")) {
            return;
        }

        // Pega a mensagem inteira (Ex: "/pl teste") e separa o comando ("/pl")
        String message = e.getMessage().toLowerCase();
        String command = message.split(" ")[0]; // Pega apenas a primeira palavra

        List<String> blockedCommands = plugin.getConfig().getStringList("commands.blocked-list");

        // Verifica se o comando digitado está na lista negra
        // Usamos "startsWith" para pegar variações ou o loop direto
        for (String blocked : blockedCommands) {
            if (command.equals(blocked) || command.startsWith(blocked + ":")) { // Bloqueia /bukkit:pl também
                e.setCancelled(true);

                String msg = plugin.getConfig().getString("commands.blocked-message", "&cComando desconhecido.");
                p.sendMessage(msg.replace("&", "§"));

                return;
            }
        }
    }
}