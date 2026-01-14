package org.github.luisera.splitlobby.managers;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.luisera.splitlobby.Main;

import java.util.*;

public class VisibilityManager implements Listener {

    private final Main plugin;
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    // --- NOVO: Map para o Cooldown ---
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 15 * 1000; // 15 segundos em milissegundos

    private ItemStack itemVisible;
    private ItemStack itemHidden;

    public VisibilityManager(Main plugin) {
        this.plugin = plugin;
        setupItems();
    }

    private void setupItems() {
        // Item VERDE (Todos visíveis)
        itemVisible = XMaterial.LIME_DYE.parseItem();
        ItemMeta metaV = itemVisible.getItemMeta();
        metaV.setDisplayName("§aJogadores: §fVisíveis");
        List<String> loreV = new ArrayList<>();
        loreV.add("§7Clique para esconder todos.");
        metaV.setLore(loreV);
        itemVisible.setItemMeta(metaV);

        // Item CINZA (Todos escondidos)
        itemHidden = XMaterial.GRAY_DYE.parseItem();
        ItemMeta metaH = itemHidden.getItemMeta();
        metaH.setDisplayName("§cJogadores: §fInvisíveis");
        List<String> loreH = new ArrayList<>();
        loreH.add("§7Clique para mostrar todos.");
        metaH.setLore(loreH);
        itemHidden.setItemMeta(metaH);
    }

    public void giveItem(Player player) {
        if (hiddenPlayers.contains(player.getUniqueId())) {
            player.getInventory().setItem(7, itemHidden);
        } else {
            player.getInventory().setItem(7, itemVisible);
        }
    }

    public void toggleVisibility(Player player) {
        if (hiddenPlayers.contains(player.getUniqueId())) {
            // MOSTRAR TODOS
            hiddenPlayers.remove(player.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) {
                player.showPlayer(target);
            }
            player.sendMessage("§aVocê agora vê todos os jogadores.");
            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        } else {
            // ESCONDER TODOS
            hiddenPlayers.add(player.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player) && !target.hasPermission("sl.bypass.hide")) {
                    player.hidePlayer(target);
                }
            }
            player.sendMessage("§cVocê escondeu os jogadores.");
            XSound.UI_BUTTON_CLICK.play(player);
        }
        giveItem(player);
    }

    // --- EVENTOS ---

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player p = event.getPlayer();
            ItemStack hand = p.getItemInHand();

            if (hand == null || !hand.hasItemMeta()) return;

            if (hand.getItemMeta().getDisplayName().equals("§aJogadores: §fVisíveis") ||
                    hand.getItemMeta().getDisplayName().equals("§cJogadores: §fInvisíveis")) {

                event.setCancelled(true);

                // --- LÓGICA DE COOLDOWN ---
                if (cooldowns.containsKey(p.getUniqueId())) {
                    long tempoFim = cooldowns.get(p.getUniqueId());
                    long agora = System.currentTimeMillis();

                    if (agora < tempoFim) {
                        long segundosRestantes = (tempoFim - agora) / 1000;
                        p.sendMessage("§cAguarde " + segundosRestantes + "s para usar novamente.");
                        return; // Para o código aqui, não deixa trocar
                    }
                }

                // Se passou do tempo ou não tem cooldown, executa e atualiza o tempo
                toggleVisibility(p);
                cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + COOLDOWN_TIME);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        giveItem(p);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (hiddenPlayers.contains(online.getUniqueId()) && !p.hasPermission("sl.bypass.hide")) {
                online.hidePlayer(p);
            }
        }
    }

    // Limpar a memória quando sair (Boa prática)
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hiddenPlayers.remove(event.getPlayer().getUniqueId());
        cooldowns.remove(event.getPlayer().getUniqueId());
    }
}