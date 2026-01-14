package org.github.luisera.splitlobby.selector;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.utils.CustomConfig;

import java.util.ArrayList;
import java.util.List;

public class SelectorManager implements Listener {

    private final Main plugin;
    private final CustomConfig config;

    public SelectorManager(Main plugin) {
        this.plugin = plugin;
        this.config = new CustomConfig(plugin, "selector.yml");
    }

    // 1. Dar o item ao entrar
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        giveSelector(e.getPlayer());
    }

    public void giveSelector(Player p) {
        String matName = config.getConfig().getString("hotbar-item.material", "COMPASS");
        int slot = config.getConfig().getInt("hotbar-item.slot", 4);
        String name = config.getConfig().getString("hotbar-item.name", "&aServidores");

        ItemStack item = criarItem(matName, name, null);
        p.getInventory().setItem(slot, item);
    }

    // 2. Abrir o menu ao clicar
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = e.getItem();
            // Verificação de segurança antes de ler o nome
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

            String configName = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("hotbar-item.name"));

            if (item.getItemMeta().getDisplayName().equals(configName)) {
                openMenu(e.getPlayer());
                e.setCancelled(true);
            }
        }
    }

    // 3. Gerenciar cliques no Inventário
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("menu.title"));

        // Verifica se é o menu correto
        if (e.getView().getTitle().equals(title)) {
            e.setCancelled(true);

            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta() || !e.getCurrentItem().getItemMeta().hasDisplayName()) return;

            Player p = (Player) e.getWhoClicked();

            ConfigurationSection servers = config.getConfig().getConfigurationSection("servers");
            for (String key : servers.getKeys(false)) {
                String srvName = ChatColor.translateAlternateColorCodes('&', servers.getString(key + ".name"));

                if (e.getCurrentItem().getItemMeta().getDisplayName().equals(srvName)) {
                    String bungeeServer = servers.getString(key + ".bungee-server");
                    enviarParaServer(p, bungeeServer);
                    p.closeInventory();
                    break;
                }
            }
        }

        // Proteção: Não deixa mover o item da hotbar no inventário
        String selectorName = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("hotbar-item.name"));
        if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName()) {
            if (e.getCurrentItem().getItemMeta().getDisplayName().equals(selectorName)) {
                e.setCancelled(true);
            }
        }
    }

    // 4. Impedir drop do item (AQUI ESTAVA O ERRO)
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();

        // FIX: Verifica se o item existe, se tem meta e se tem nome ANTES de comparar
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String selectorName = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("hotbar-item.name"));

        if (item.getItemMeta().getDisplayName().equals(selectorName)) {
            e.setCancelled(true);
        }
    }

    private void openMenu(Player p) {
        String title = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("menu.title"));
        int rows = config.getConfig().getInt("menu.rows", 3);

        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        ConfigurationSection servers = config.getConfig().getConfigurationSection("servers");
        if (servers != null) {
            for (String key : servers.getKeys(false)) {
                int slot = servers.getInt(key + ".slot");
                String mat = servers.getString(key + ".material");
                String name = servers.getString(key + ".name");
                List<String> lore = servers.getStringList(key + ".lore");

                inv.setItem(slot, criarItem(mat, name, lore));
            }
        }

        p.openInventory(inv);
    }

    private ItemStack criarItem(String materialName, String name, List<String> lore) {
        ItemStack item = XMaterial.matchXMaterial(materialName).orElse(XMaterial.STONE).parseItem();
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        if (lore != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(coloredLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void enviarParaServer(Player p, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        p.sendMessage("§aConectando ao servidor: " + serverName + "...");
    }
}