package org.github.luisera.splitlobby.managers;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.luisera.splitlobby.Main;
import org.redesplit.github.offluisera.redesplitcore.RedeSplitCore;
import org.redesplit.github.offluisera.redesplitcore.managers.XPManager;
import org.redesplit.github.offluisera.redesplitcore.player.SplitPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LevelDisplayManager implements Listener {

    private final Main plugin;
    private File file;
    private FileConfiguration config;

    public LevelDisplayManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        file = new File(plugin.getDataFolder(), "nivel.yml");
        if (!file.exists()) {
            plugin.saveResource("nivel.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Entrega o item de XP na hotbar
     */
    public void giveLevelItem(Player player) {
        if (!config.getBoolean("ativado", true)) {
            return; // Sistema desativado
        }

        SplitPlayer sp = RedeSplitCore.getInstance().getPlayerManager().getPlayer(player.getUniqueId());
        if (sp == null) {
            return;
        }

        // Pega configurações
        int slot = config.getInt("slot", 8); // Slot 8 = último da hotbar
        String materialName = config.getString("icone", "NETHER_STAR");

        // Cria o item
        ItemStack item = XMaterial.matchXMaterial(materialName).orElse(XMaterial.NETHER_STAR).parseItem();
        ItemMeta meta = item.getItemMeta();

        // Nome do item
        String displayName = config.getString("nome", "§e§lSEU NÍVEL")
                .replace("%level%", String.valueOf(sp.getLevel()))
                .replace("%badge%", XPManager.getLevelBadge(sp.getLevel()))
                .replace("&", "§");
        meta.setDisplayName(displayName);

        // Lore do item
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("lore")) {
            lore.add(line
                    .replace("%level%", String.valueOf(sp.getLevel()))
                    .replace("%badge%", XPManager.getLevelBadge(sp.getLevel()))
                    .replace("%xp%", String.format("%,d", sp.getXp()))
                    .replace("%xp_next%", String.format("%,d", sp.getXpToNextLevel()))
                    .replace("%progress%", String.format("%.1f%%", sp.getProgressToNextLevel()))
                    .replace("&", "§")
            );
        }
        meta.setLore(lore);

        item.setItemMeta(meta);

        // Coloca no inventário
        player.getInventory().setItem(slot, item);
    }

    /**
     * Atualiza o item de XP
     */
    public void updateLevelItem(Player player) {
        if (!config.getBoolean("ativado", true)) {
            return;
        }

        // Remove e recoloca o item atualizado
        Bukkit.getScheduler().runTask(plugin, () -> {
            giveLevelItem(player);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Aguarda 1 segundo para dar tempo do Core carregar
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            giveLevelItem(event.getPlayer());
        }, 20L);
    }
}