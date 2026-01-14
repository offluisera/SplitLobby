package org.github.luisera.splitlobby.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.github.luisera.splitlobby.Main;

public class LobbyProtectionListener implements Listener {

    private final Main plugin;

    public LobbyProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    // 1. Bloquear Quebrar Blocos
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (canBuild(e.getPlayer())) return; // Se for admin, deixa quebrar
        if (plugin.getConfig().getBoolean("protection.block-break")) {
            e.setCancelled(true);
        }
    }

    // 2. Bloquear Colocar Blocos
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (canBuild(e.getPlayer())) return;
        if (plugin.getConfig().getBoolean("protection.block-place")) {
            e.setCancelled(true);
        }
    }

    // 3. Bloquear Dropar Itens (Não perder a bússola)
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (canBuild(e.getPlayer())) return;
        if (plugin.getConfig().getBoolean("protection.drop-item")) {
            e.setCancelled(true);
        }
    }

    // 4. Bloquear Pegar Itens (Lixo no chão)
    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        if (canBuild(e.getPlayer())) return;
        if (plugin.getConfig().getBoolean("protection.pickup-item")) {
            e.setCancelled(true);
        }
    }

    // 5. Bloquear Dano (Imortalidade)
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        // Bloqueia qualquer dano (PvP, Fogo, Queda, Sufocamento)
        if (plugin.getConfig().getBoolean("protection.damage")) {
            e.setCancelled(true);
            // Dica: Se o player cair no void, o dano cancela, mas ele fica caindo infinitamente.
            // O Void-TP resolve isso abaixo.
        }
    }

    // 6. Sem Fome
    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (plugin.getConfig().getBoolean("protection.hunger")) {
            e.setCancelled(true);
            e.setFoodLevel(20); // Mantém a barrinha cheia
        }
    }

    // 7. Sem Chuva
    @EventHandler
    public void onWeather(WeatherChangeEvent e) {
        if (plugin.getConfig().getBoolean("protection.weather")) {
            if (e.toWeatherState()) { // Se for começar a chover
                e.setCancelled(true); // Cancela a chuva
            }
        }
    }

    // 8. Void TP (Caiu no void, volta pro spawn)
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!plugin.getConfig().getBoolean("protection.void-tp")) return;

        // Otimização: Só checa se mudou de bloco Y
        if (e.getTo().getY() == e.getFrom().getY()) return;

        int voidHeight = plugin.getConfig().getInt("protection.void-height", -5);

        if (e.getTo().getY() <= voidHeight) {
            Player p = e.getPlayer();
            // Pega o spawn do mundo ou usa a localização atual se não tiver spawn setado
            Location spawn = p.getWorld().getSpawnLocation();

            // Adiciona 0.5 para centralizar no bloco e ajusta a rotação
            spawn.add(0.5, 0, 0.5);
            spawn.setYaw(p.getLocation().getYaw()); // Mantém a rotação ou define uma fixa

            p.teleport(spawn);
            p.setFallDistance(0); // Reseta o dano de queda acumulado
        }
    }

    // Método auxiliar para verificar permissão de ADMIN/BUILDER
    private boolean canBuild(Player p) {
        // Se estiver no criativo E tiver permissão, pode construir/quebrar
        return p.getGameMode() == GameMode.CREATIVE && p.hasPermission("splitcore.admin");
    }
}