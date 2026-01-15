package org.github.luisera.splitlobby.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.npc.NPCData;

public class NPCListener implements Listener {

    private final Main plugin;

    public NPCListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Mostra NPCs quando o jogador entra
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Aguarda 1 segundo para garantir que o jogador carregou completamente
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (NPCData npc : plugin.getNPCManager().getAllNPCs()) {
                plugin.getNPCManager().showNPC(player, npc.getId());
            }
        }, 20L);
    }

    /**
     * Faz NPCs olharem para jogadores próximos
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Otimização: Verifica apenas se mudou de bloco
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        for (NPCData npc : plugin.getNPCManager().getAllNPCs()) {
            double distance = npc.getLocation().distance(player.getLocation());

            // NPCs olham para jogadores a até 5 blocos de distância
            if (distance <= 5.0) {
                plugin.getNPCManager().lookAt(npc.getId(), player);
            }
        }
    }
}