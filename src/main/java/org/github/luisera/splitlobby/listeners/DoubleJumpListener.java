package org.github.luisera.splitlobby.listeners;

import com.cryptomorin.xseries.XSound;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;
import org.github.luisera.splitlobby.Main;

public class DoubleJumpListener implements Listener {

    private final Main plugin;

    public DoubleJumpListener(Main plugin) {
        this.plugin = plugin;
    }

    // 1. Configura o voo ao entrar
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        atualizarVoo(e.getPlayer());
    }

    // 2. Configura o voo ao mudar de gamemode (Evita bugs se trocar de GM)
    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        // O evento acontece ANTES da troca, então rodamos um tick depois
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> atualizarVoo(e.getPlayer()), 1L);
    }

    private void atualizarVoo(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        if (p.hasPermission("splitlobby.jump") && plugin.getConfig().getBoolean("double-jump.enabled")) {
            p.setAllowFlight(true);
            p.setFlying(false);
        } else {
            // CORREÇÃO: Garante que quem não tem permissão NÃO possa voar
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    // 3. A Mágica do Pulo
    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();

        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        // CORREÇÃO CRÍTICA: Se não tiver permissão, bloqueia o voo e cancela
        if (!p.hasPermission("splitlobby.jump") || !plugin.getConfig().getBoolean("double-jump.enabled")) {
            e.setCancelled(true);
            p.setAllowFlight(false);
            p.setFlying(false);
            return;
        }

        // Executa o Double Jump
        e.setCancelled(true);
        p.setAllowFlight(false);
        p.setFlying(false);

        // Impulso
        double velocity = plugin.getConfig().getDouble("double-jump.velocity", 1.2);
        double height = plugin.getConfig().getDouble("double-jump.height", 0.8);

        Vector jump = p.getLocation().getDirection().multiply(velocity).setY(height);
        p.setVelocity(jump);

        playEffects(p);
    }

    // 4. Recarrega o pulo ao tocar no chão
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (e.getFrom().getY() == e.getTo().getY()) return;

        // Só recarrega se tiver permissão
        if (p.getGameMode() != GameMode.CREATIVE
                && p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR
                && p.hasPermission("splitlobby.jump")) {

            p.setAllowFlight(true);
        }
    }

    // 5. Remove dano de queda
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (plugin.getConfig().getBoolean("double-jump.enabled")) {
                e.setCancelled(true);
            }
        }
    }

    private void playEffects(Player p) {
        String soundName = plugin.getConfig().getString("double-jump.sound", "BAT_TAKEOFF");
        String particleName = plugin.getConfig().getString("double-jump.particle", "CLOUD");

        try {
            XSound.matchXSound(soundName).ifPresent(xSound -> xSound.play(p));
            p.getWorld().playEffect(p.getLocation(), Effect.valueOf(particleName), 1);
        } catch (Exception ignored) {}
    }
}