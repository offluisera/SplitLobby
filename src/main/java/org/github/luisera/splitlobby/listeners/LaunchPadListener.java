package org.github.luisera.splitlobby.listeners;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.github.luisera.splitlobby.Main;

import java.util.Optional;

public class LaunchPadListener implements Listener {

    private final Main plugin;

    public LaunchPadListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        // Otimização: Só verifica se mudou de posição X, Y ou Z
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                e.getFrom().getBlockY() == e.getTo().getBlockY() &&
                e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        if (!plugin.getConfig().getBoolean("launch-pad.enabled")) return;

        Player p = e.getPlayer();
        Location loc = p.getLocation();
        Block block = loc.getBlock();

        // Verifica se o bloco atual é o configurado (Ex: Placa de pressão)
        String topMaterial = plugin.getConfig().getString("launch-pad.top-block", "STONE_PRESSURE_PLATE");

        if (!compararMaterial(block.getType(), topMaterial)) {
            return;
        }

        // Verifica o bloco de baixo (Ex: Redstone Block)
        Block blockUnder = block.getRelative(BlockFace.DOWN);
        String bottomMaterial = plugin.getConfig().getString("launch-pad.bottom-block", "REDSTONE_BLOCK");

        if (!compararMaterial(blockUnder.getType(), bottomMaterial)) {
            return;
        }

        // --- ATIVA O LAUNCH PAD ---

        // Som
        String soundName = plugin.getConfig().getString("launch-pad.sound", "ENTITY_BAT_TAKEOFF");
        try {
            XSound.matchXSound(soundName).ifPresent(s -> s.play(p));
        } catch (Exception ignored) {}

        // Partícula (Simples)
        String effectName = plugin.getConfig().getString("launch-pad.effect", "EXPLOSION_LARGE");
        try {
            // Nota: Effect.valueOf pode variar versão, mas EXPLOSION_LARGE costuma ser universal
            p.getWorld().playEffect(loc, Effect.valueOf(effectName), 1);
        } catch (Exception ignored) {}

        // Física: Lança o jogador na direção que ele está olhando
        double velocity = plugin.getConfig().getDouble("launch-pad.velocity", 2.0);
        double height = plugin.getConfig().getDouble("launch-pad.height", 1.5);

        Vector v = p.getLocation().getDirection().multiply(velocity).setY(height);
        p.setVelocity(v);

        // Remove dano de queda (A proteção geral já faz isso, mas é bom garantir)
        p.setFallDistance(-100);
    }

    // Método auxiliar para comparar materiais usando XMaterial (Compatibilidade 1.8 - 1.20)
    private boolean compararMaterial(Material mat, String configName) {
        Optional<XMaterial> xMat = XMaterial.matchXMaterial(configName);
        return xMat.map(material -> material.parseMaterial() == mat).orElse(false);
    }
}