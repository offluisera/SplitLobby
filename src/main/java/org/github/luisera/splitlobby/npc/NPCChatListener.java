package org.github.luisera.splitlobby.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.github.luisera.splitlobby.Main;

public class NPCChatListener implements Listener {

    private final Main plugin;

    public NPCChatListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercepta mensagens do chat para o sistema de conversação
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Verifica se o jogador está em uma conversa
        if (plugin.getNPCConversationManager().isInConversation(player)) {
            event.setCancelled(true);

            String message = event.getMessage();

            // Processa a mensagem de forma síncrona (porque NPC Manager precisa da thread principal)
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getNPCConversationManager().handleMessage(player, message);
            });
        }
    }

    /**
     * Remove conversação quando o jogador sai do servidor
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getNPCConversationManager().cancelConversation(event.getPlayer());
    }
}