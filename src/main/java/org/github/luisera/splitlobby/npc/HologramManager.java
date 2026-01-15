package org.github.luisera.splitlobby.npc;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
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
        // CRÍTICO: Remove hologramas antigos ANTES de criar novos
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
            stand.setMarker(true);
            stand.setBasePlate(false);
            stand.setArms(false);

            stands.add(stand);
            y += 0.25; // Espaçamento entre linhas
        }

        holograms.put(npcId, stands);
    }

    /**
     * Mostra hologramas para um jogador COM METADATA ULTRA CUSTOMIZADO
     */
    public void showHologram(Player player, int npcId) {
        List<EntityArmorStand> stands = holograms.get(npcId);
        if (stands == null || stands.isEmpty()) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        for (EntityArmorStand stand : stands) {
            // Envia packet de spawn
            connection.sendPacket(new PacketPlayOutSpawnEntityLiving(stand));

            // ★ METADATA SUPER AGRESSIVO - Remove TUDO do ArmorStand
            DataWatcher watcher = new DataWatcher(null);

            // === ENTIDADE BASE ===
            // Byte 0: Entity flags
            // 0x20 = Invisível
            watcher.register(new DataWatcherObject<>(0, DataWatcherRegistry.a), (byte) 0x20);

            // === ARMOR STAND ESPECÍFICO ===
            // Byte 11: ArmorStand flags
            // 0x01 = isSmall
            // 0x08 = hasNoBasePlate
            // 0x10 = isMarker (SEM HITBOX - CRÍTICO!)
            watcher.register(new DataWatcherObject<>(11, DataWatcherRegistry.a), (byte) (0x01 | 0x08 | 0x10));

            // === NOME ===
            // String 2: Custom Name
            watcher.register(new DataWatcherObject<>(2, DataWatcherRegistry.d), stand.getCustomName());

            // Boolean 3: Custom Name Visible
            watcher.register(new DataWatcherObject<>(3, DataWatcherRegistry.h), true);

            // === ADDITIONAL: Remove partes do corpo ===
            // Rotation 12: Head rotation (0,0,0)
            watcher.register(new DataWatcherObject<>(12, DataWatcherRegistry.k), new Vector3f(0, 0, 0));

            // Rotation 13: Body rotation (0,0,0)
            watcher.register(new DataWatcherObject<>(13, DataWatcherRegistry.k), new Vector3f(0, 0, 0));

            // Rotation 14: Left arm rotation (0,0,0)
            watcher.register(new DataWatcherObject<>(14, DataWatcherRegistry.k), new Vector3f(0, 0, 0));

            // Rotation 15: Right arm rotation (0,0,0)
            watcher.register(new DataWatcherObject<>(15, DataWatcherRegistry.k), new Vector3f(0, 0, 0));

            // Rotation 16: Left leg rotation (0,0,0)
            watcher.register(new DataWatcherObject<>(16, DataWatcherRegistry.k), new Vector3f(0, 0, 0));

            // Rotation 17: Right leg rotation (0,0,0)
            watcher.register(new DataWatcherObject<>(17, DataWatcherRegistry.k), new Vector3f(0, 0, 0));

            // Envia o metadata customizado
            PacketPlayOutEntityMetadata metadataPacket = new PacketPlayOutEntityMetadata(
                    stand.getId(),
                    watcher,
                    true
            );
            connection.sendPacket(metadataPacket);
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
                .mapToInt(EntityArmorStand::getId)
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

        // Remove para todos os jogadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideHologram(player, npcId);
        }
    }

    /**
     * Remove hologramas completamente (do mundo e da memória)
     */
    public void removeHologram(int npcId) {
        List<EntityArmorStand> stands = holograms.get(npcId);
        if (stands == null || stands.isEmpty()) {
            holograms.remove(npcId);
            return;
        }

        // 1. Remove para todos os jogadores online primeiro
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

            int[] ids = stands.stream()
                    .mapToInt(EntityArmorStand::getId)
                    .toArray();

            if (ids.length > 0) {
                connection.sendPacket(new PacketPlayOutEntityDestroy(ids));
            }
        }

        // 2. Remove os armor stands do mundo (para evitar duplicação)
        for (EntityArmorStand stand : stands) {
            try {
                // Remove do mundo do servidor
                stand.getWorld().removeEntity(stand);
                stand.die();
            } catch (Exception e) {
                // Ignora erro silenciosamente
            }
        }

        // 3. Limpa a lista e remove do mapa
        stands.clear();
        holograms.remove(npcId);
    }

    /**
     * Remove TODOS os hologramas (útil para reload/shutdown)
     */
    public void removeAllHolograms() {
        // Cria cópia das keys para evitar ConcurrentModificationException
        Set<Integer> npcIds = new HashSet<>(holograms.keySet());

        for (Integer npcId : npcIds) {
            removeHologram(npcId);
        }

        holograms.clear();
    }

    /**
     * Obtém hologramas de um NPC
     */
    public List<EntityArmorStand> getHologram(int npcId) {
        return holograms.get(npcId);
    }
}