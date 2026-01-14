package org.github.luisera.splitlobby.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.github.luisera.splitlobby.Main;

public class PlayerConnectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST) // Executa primeiro que outros plugins
    public void onJoin(PlayerJoinEvent e) {
        // Carrega os dados do jogador de forma assíncrona (gerenciado pelo DataManager)
        Main.getInstance().getDataManager().loadPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName());

        // Mensagem de join customizada (Opcional, pode remover se quiser)
        e.setJoinMessage(null); // Remove mensagem padrão do minecraft
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Salva os dados e remove do cache
        Main.getInstance().getDataManager().savePlayer(e.getPlayer().getUniqueId());

        e.setQuitMessage(null); // Remove mensagem padrão
    }
}