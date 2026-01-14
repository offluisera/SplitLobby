package org.github.luisera.splitlobby.listeners;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.managers.ParkourManager;

public class ParkourListener implements Listener {

    private final Main plugin;
    private final ParkourManager parkourManager;

    public ParkourListener(Main plugin, ParkourManager parkourManager) {
        this.plugin = plugin;
        this.parkourManager = parkourManager;
    }

    // --- INTERAÇÃO FÍSICA (Pisar nas placas) ---
    @EventHandler
    public void onPlateStep(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;

        Player p = event.getPlayer();
        Block block = event.getClickedBlock();
        Location loc = block.getLocation();

        // Se o admin está configurando, não ativa o jogo
        if (parkourManager.isSetup(p)) return;

        // 1. Início (Placa de Ferro)
        if (locationsEqual(loc, parkourManager.getStartLocation())) {
            parkourManager.startParkour(p);
            return;
        }

        // 2. Fim (Placa de Madeira)
        if (locationsEqual(loc, parkourManager.getEndLocation())) {
            parkourManager.finishParkour(p);
            return;
        }

        // 3. Checkpoints (Qualquer placa de ouro configurada)
        if (block.getType() == XMaterial.LIGHT_WEIGHTED_PRESSURE_PLATE.parseMaterial() || block.getType().name().contains("GOLD_PLATE")) {
            parkourManager.reachCheckpoint(p, loc);
        }
    }

    // --- USO DE ITENS (Admin e Jogador) ---
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        String display = item.getItemMeta().getDisplayName();
        if (display == null) return;

        Action action = event.getAction();

        // MODO JOGADOR (Clica com os itens)
        if (parkourManager.isPlaying(p)) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                if (display.contains("Reiniciar")) {
                    parkourManager.restartParkour(p);
                } else if (display.contains("Voltar ao Checkpoint")) {
                    parkourManager.returnToCheckpoint(p);
                } else if (display.contains("Sair")) {
                    parkourManager.quitParkour(p, true);
                }
            }
            return;
        }

        // MODO ADMIN SETUP (Colocar placas ou Salvar)
        if (parkourManager.isSetup(p)) {
            // Salvar Configuração (Esmeralda)
            if (display.contains("Salvar Configuração")) {
                event.setCancelled(true);
                parkourManager.saveLocations();
                parkourManager.toggleSetup(p); // Sai do modo
                p.sendMessage("§aConfiguração do Parkour salva com sucesso!");
                return;
            }

            // Colocar Placas (BlockPlaceEvent trata melhor a colocação, mas vamos pegar aqui para configurar o local)
            // Na verdade, o ideal é usar o BlockPlaceEvent para registrar o local exato
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!parkourManager.isSetup(p)) return;

        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;
        String display = item.getItemMeta().getDisplayName();

        Location loc = event.getBlock().getLocation();

        if (display.contains("Marcar Início")) {
            parkourManager.setStart(loc);
            p.sendMessage("§aInício definido!");
        } else if (display.contains("Marcar Checkpoint")) {
            parkourManager.addCheckpoint(loc);
            p.sendMessage("§eCheckpoint adicionado!");
        } else if (display.contains("Marcar Fim")) {
            parkourManager.setEnd(loc);
            p.sendMessage("§cFim definido!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        parkourManager.quitParkour(event.getPlayer(), false);
    }

    // Helper para comparar locations ignorando Yaw/Pitch
    private boolean locationsEqual(Location l1, Location l2) {
        if (l1 == null || l2 == null) return false;
        return l1.getBlockX() == l2.getBlockX() &&
                l1.getBlockY() == l2.getBlockY() &&
                l1.getBlockZ() == l2.getBlockZ() &&
                l1.getWorld().getName().equals(l2.getWorld().getName());
    }
}