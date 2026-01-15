package org.github.luisera.splitlobby.listeners;

import net.minecraft.server.v1_12_R1.EntityPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.npc.NPCData;

public class NPCListener implements Listener {

    private final Main plugin;
    private final PacketNPCListener packetListener;

    public NPCListener(Main plugin) {
        this.plugin = plugin;
        this.packetListener = new PacketNPCListener(plugin);
    }

    /**
     * Mostra NPCs e injeta packet listener quando o jogador entra
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Aguarda 1 segundo para garantir que o jogador carregou completamente
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Mostra NPCs
            for (NPCData npc : plugin.getNPCManager().getAllNPCs()) {
                plugin.getNPCManager().showNPC(player, npc.getId());
            }

            // Injeta packet listener para detectar cliques
            packetListener.injectPlayer(player);
        }, 20L);
    }

    /**
     * Remove packet listener quando o jogador sai
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        packetListener.uninjectPlayer(event.getPlayer());
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