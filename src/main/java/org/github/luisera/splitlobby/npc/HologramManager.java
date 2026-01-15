package org.github.luisera.splitlobby.npc;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class HologramManager {

    private final Map<Integer, List<EntityArmorStand>> holograms = new HashMap<>();

    /**
     * Cria hologramas para o NPC
     */
    public void createHologram(int npcId, Location location, List<String> lines) {
        removeHologram(npcId);

        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        List<EntityArmorStand> stands = new ArrayList<>();

        // Altura inicial BEM BAIXA (na altura da cabeça/peito)
        double y = location.getY() + 1.7;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);

            EntityArmorStand stand = new EntityArmorStand(world);
            stand.setLocation(location.getX(), y, location.getZ(), 0, 0);
            stand.setCustomName(line);
            stand.setCustomNameVisible(true);
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setNoGravity(true);
            stand.setMarker(true); // IMPORTANTE: Permite clicar através
            stand.setBasePlate(false);

            // Remove colisão para não bloquear cliques
            stand.getWorld().addEntity(stand);

            stands.add(stand);
            y += 0.25; // Espaçamento entre linhas
        }

        holograms.put(npcId, stands);
    }

    /**
     * Mostra hologramas para um jogador
     */
    public void showHologram(Player player, int npcId) {
        List<EntityArmorStand> stands = holograms.get(npcId);
        if (stands == null || stands.isEmpty()) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        for (EntityArmorStand stand : stands) {
            connection.sendPacket(new PacketPlayOutSpawnEntityLiving(stand));
            connection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), true));
        }
    }

    /**
     * Esconde hologramas para um jogador
     */
    public void hideHologram(Player player, int npcId) {
        List<EntityArmorStand> stands = holograms.get(npcId);
        if (stands == null || stands.isEmpty()) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        // Coleta IDs de todos os armor stands
        int[] ids = stands.stream()
                .mapToInt(stand -> stand.getId())
                .toArray();

        if (ids.length > 0) {
            connection.sendPacket(new PacketPlayOutEntityDestroy(ids));
        }
    }

    /**
     * Remove hologramas para todos os jogadores online
     */
    public void removeHologramForAll(int npcId) {
        List<EntityArmorStand> stands = holograms.get(npcId);
        if (stands == null || stands.isEmpty()) return;

        // Remove para todos os jogadores
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            hideHologram(player, npcId);
        }
    }

    /**
     * Remove hologramas
     */
    public void removeHologram(int npcId) {
        List<EntityArmorStand> stands = holograms.remove(npcId);
        if (stands != null && !stands.isEmpty()) {
            // Remove os armor stands do mundo
            for (EntityArmorStand stand : stands) {
                try {
                    stand.die(); // Remove do mundo
                } catch (Exception e) {
                    // Ignora erro
                }
            }
            stands.clear();
        }
    }

    /**
     * Obtém hologramas de um NPC
     */
    public List<EntityArmorStand> getHologram(int npcId) {
        return holograms.get(npcId);
    }
}