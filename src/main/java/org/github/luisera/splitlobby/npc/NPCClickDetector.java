package org.github.luisera.splitlobby.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.github.luisera.splitlobby.Main;

public class NPCClickDetector implements Listener {

    private final Main plugin;
    private final double DETECTION_RANGE = 5.0; // Alcance máximo
    private final double NPC_HITBOX = 0.6; // Tamanho da hitbox do NPC

    public NPCClickDetector(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Detecta clique direito no ar ou em bloco
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        plugin.getLogger().info("§b[DETECTOR] Clique direito detectado!");
        plugin.getLogger().info("§b[DETECTOR] Jogador: " + player.getName());
        plugin.getLogger().info("§b[DETECTOR] Ação: " + event.getAction());

        // Verifica se está olhando para algum NPC
        NPCData clickedNPC = getNPCInSight(player);

        if (clickedNPC != null) {
            plugin.getLogger().info("§a[DETECTOR] ╔═══════════════════════════════════╗");
            plugin.getLogger().info("§a[DETECTOR] ║  ✓ NPC DETECTADO!                ║");
            plugin.getLogger().info("§a[DETECTOR] ╚═══════════════════════════════════╝");
            plugin.getLogger().info("§a[DETECTOR] NPC: #" + clickedNPC.getId() + " - " + clickedNPC.getName());
            plugin.getLogger().info("§a[DETECTOR] Comando: " + (clickedNPC.getCommand() != null ? clickedNPC.getCommand() : "NENHUM"));

            // Executa comando
            if (clickedNPC.getCommand() != null && !clickedNPC.getCommand().isEmpty()) {
                plugin.getLogger().info("§a[DETECTOR] >>> Executando comando...");
                plugin.getNPCManager().executeCommand(clickedNPC.getId(), player);
            } else {
                player.sendMessage("§e§l[NPC] §f" + clickedNPC.getName() + "§e: §aOlá, " + player.getName() + "!");
                player.sendMessage("§7Use §e/slnpc setcmd " + clickedNPC.getId() + " §7para definir uma ação");
            }

            event.setCancelled(true);
        } else {
            plugin.getLogger().info("§7[DETECTOR] Nenhum NPC na mira");
        }
    }

    /**
     * Verifica se o jogador está olhando para algum NPC
     */
    private NPCData getNPCInSight(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        plugin.getLogger().info("§e[DETECTOR] ----- Verificando NPCs -----");
        plugin.getLogger().info("§e[DETECTOR] Posição olho: " + formatLocation(eyeLocation));
        plugin.getLogger().info("§e[DETECTOR] Direção: " + formatVector(direction));

        NPCData closestNPC = null;
        double closestDistance = DETECTION_RANGE;

        for (NPCData npc : plugin.getNPCManager().getAllNPCs()) {
            Location npcLoc = npc.getLocation().clone().add(0, 1, 0); // Centro do NPC (altura do peito)

            plugin.getLogger().info("§e[DETECTOR] Verificando NPC #" + npc.getId() + " (" + npc.getName() + ")");
            plugin.getLogger().info("§e[DETECTOR]   Posição NPC: " + formatLocation(npcLoc));

            // Calcula distância do olho do jogador até o NPC
            double distance = eyeLocation.distance(npcLoc);

            plugin.getLogger().info("§e[DETECTOR]   Distância: " + String.format("%.2f", distance));

            if (distance > DETECTION_RANGE) {
                plugin.getLogger().info("§7[DETECTOR]   ✗ Muito longe (> " + DETECTION_RANGE + ")");
                continue;
            }

            // Verifica se o jogador está olhando na direção do NPC
            Vector toNPC = npcLoc.toVector().subtract(eyeLocation.toVector()).normalize();
            double dot = direction.dot(toNPC);

            plugin.getLogger().info("§e[DETECTOR]   Dot product: " + String.format("%.3f", dot));

            // dot > 0.98 significa que está olhando quase direto (ângulo < ~11 graus)
            if (dot < 0.95) {
                plugin.getLogger().info("§7[DETECTOR]   ✗ Não está olhando diretamente (dot < 0.95)");
                continue;
            }

            // Calcula o ponto mais próximo na linha de visão
            Vector closestPoint = getClosestPointOnLine(eyeLocation.toVector(), direction, npcLoc.toVector());
            double distanceToLine = closestPoint.distance(npcLoc.toVector());

            plugin.getLogger().info("§e[DETECTOR]   Distância à linha: " + String.format("%.3f", distanceToLine));

            // Verifica se a linha de visão passa perto o suficiente do NPC
            if (distanceToLine < NPC_HITBOX && distance < closestDistance) {
                plugin.getLogger().info("§a[DETECTOR]   ✓ DENTRO DA HITBOX!");
                closestNPC = npc;
                closestDistance = distance;
            } else {
                plugin.getLogger().info("§7[DETECTOR]   ✗ Fora da hitbox (> " + NPC_HITBOX + ")");
            }
        }

        plugin.getLogger().info("§e[DETECTOR] ---------------------------");

        return closestNPC;
    }

    /**
     * Calcula o ponto mais próximo em uma linha
     */
    private Vector getClosestPointOnLine(Vector linePoint, Vector lineDirection, Vector point) {
        Vector pointToLine = point.clone().subtract(linePoint);
        double projection = pointToLine.dot(lineDirection);
        return linePoint.clone().add(lineDirection.clone().multiply(projection));
    }

    private String formatLocation(Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }

    private String formatVector(Vector vec) {
        return String.format("(%.2f, %.2f, %.2f)", vec.getX(), vec.getY(), vec.getZ());
    }
}