package org.github.luisera.splitlobby.listeners;

import net.minecraft.server.v1_12_R1.EntityPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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

    /**
     * Detecta cliques em NPCs
     * NOTA: Este evento não funciona perfeitamente em NPCs fake
     * Para detecção completa, você precisará usar PacketListener (ProtocolLib)
     */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Verifica se é um NPC (pela entity ID)
        for (NPCData npc : plugin.getNPCManager().getAllNPCs()) {
            EntityPlayer npcEntity = plugin.getNPCManager().getEntity(npc.getId());

            // Compara o entity ID do Bukkit com o ID do NMS
            if (npcEntity != null && npcEntity.getBukkitEntity().getEntityId() == entity.getEntityId()) {

                // Executa o comando se tiver
                if (npc.getCommand() != null && !npc.getCommand().isEmpty()) {
                    plugin.getNPCManager().executeCommand(npc.getId(), player);
                } else {
                    // Mensagem padrão se não tiver comando
                    player.sendMessage("§e§l[NPC] §f" + npc.getName() + "§e: §aOlá, " + player.getName() + "!");
                    player.sendMessage("§7Use §e/slnpc setcmd " + npc.getId() + " <comando> §7para definir uma ação");
                }

                event.setCancelled(true);
                break;
            }
        }
    }
}