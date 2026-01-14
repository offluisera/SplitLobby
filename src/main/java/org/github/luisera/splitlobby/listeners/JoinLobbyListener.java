package org.github.luisera.splitlobby.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.github.luisera.splitlobby.Main;

public class JoinLobbyListener implements Listener {

    private final Main plugin;

    public JoinLobbyListener(Main plugin) {
        this.plugin = plugin;
    }

    private Location getLobbyLocation() {
        if (plugin.getSpawnConfig().contains("spawn-lobby")) {
            return (Location) plugin.getSpawnConfig().get("spawn-lobby");
        }
        return null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location lobby = getLobbyLocation();

        if (lobby != null) player.teleport(lobby);

        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);

        // CHAMA O MÉTODO DA MAIN (Que entrega tudo configurado)
        plugin.giveLobbyItems(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location lobby = getLobbyLocation();
        if (lobby != null) event.setRespawnLocation(lobby);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.giveLobbyItems(event.getPlayer());
        }, 5L);
    }

    @EventHandler
    public void onVoidFall(PlayerMoveEvent event) {
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;
        Location lobby = getLobbyLocation();
        if (lobby == null || !event.getPlayer().getWorld().equals(lobby.getWorld())) return;

        if (event.getTo().getY() <= -10) {
            event.getPlayer().teleport(lobby);
            event.getPlayer().setFallDistance(0);
            // Devolve itens se cair no void
            plugin.giveLobbyItems(event.getPlayer());
        }
    }
}