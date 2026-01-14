package org.github.luisera.splitlobby.managers;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.luisera.splitlobby.Main;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class EasterEggManager implements Listener {

    private final Main plugin;
    private File configFile;
    private FileConfiguration config;
    private File dataFile;
    private FileConfiguration dataConfig;

    private final Map<Location, String> secretsCache = new HashMap<>();

    public EasterEggManager(Main plugin) {
        this.plugin = plugin;
        loadFiles();
    }

    public void loadFiles() {
        configFile = new File(plugin.getDataFolder(), "easteregg.yml");
        if (!configFile.exists()) {
            plugin.saveResource("easteregg.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        dataFile = new File(plugin.getDataFolder(), "secrets.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        loadSecrets();
    }

    private void loadSecrets() {
        secretsCache.clear();
        if (!dataConfig.contains("locations")) return;

        for (String id : dataConfig.getConfigurationSection("locations").getKeys(false)) {
            Location loc = (Location) dataConfig.get("locations." + id);
            if (loc != null) {
                secretsCache.put(loc, id);
            }
        }
        plugin.enviarMensagem("§a[Secrets] " + secretsCache.size() + " segredos carregados.");
    }

    public ItemStack getSkullItem() {
        String texture = config.getString("skin_value");
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        if (head == null) head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);

        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName("§eCabeça Secreta §7(Coloque no chão)");

        if (texture != null && !texture.isEmpty()) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            try {
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (Exception e) {}
        }

        head.setItemMeta(meta);
        return head;
    }

    public void addSecret(String id, Location loc) {
        dataConfig.set("locations." + id, loc);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
        secretsCache.put(loc, id);
    }

    public void removeSecret(String id) {
        Location locToRemove = null;
        for (Map.Entry<Location, String> entry : secretsCache.entrySet()) {
            if (entry.getValue().equals(id)) {
                locToRemove = entry.getKey();
                break;
            }
        }
        if (locToRemove != null) {
            secretsCache.remove(locToRemove);
            locToRemove.getBlock().setType(Material.AIR);
        }

        dataConfig.set("locations." + id, null);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!secretsCache.containsKey(block.getLocation())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String secretId = secretsCache.get(block.getLocation());

        handleSecretClick(player, secretId);
    }

    private void handleSecretClick(Player player, String secretId) {
        // Verifica se já pegou (Bloqueia repetição)
        // OBS: hasFoundSecret pode ser sync, cuidado com spam.
        // Idealmente teríamos um cache local, mas para corrigir o contador isso basta.
        boolean found = plugin.getMySQL().hasFoundSecret(player.getUniqueId(), secretId);
        String prefix = config.getString("mensagens.prefixo", "&eSecrets &8» &f").replace("&", "§");

        if (found) {
            String msg = config.getString("mensagens.ja_encontrou", "&cJá encontrado!").replace("&", "§");
            player.sendMessage(prefix + msg);
            playEffect(player, "som_ja_encontrou", "VILLAGER_NO");
            return;
        }

        // Efeito imediato para feedback visual
        playEffect(player, "som_encontrou", "ORB_PICKUP");

        // --- LÓGICA DE CONTAGEM CORRIGIDA ---
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // 1. Pega a contagem ANTES de inserir o novo (Ex: retorna 0 se nunca achou nada)
            int currentCount = plugin.getMySQL().countFoundSecrets(player.getUniqueId());

            // 2. Manda salvar o novo segredo
            plugin.getMySQL().setSecretFound(player.getUniqueId(), secretId);

            // 3. Calculamos o que exibir: (Antigo + 1) = Novo Total
            // Isso corrige o bug de mostrar "0/4"
            int foundCount = currentCount + 1;
            int total = secretsCache.size();

            String msg = config.getString("mensagens.encontrou")
                    .replace("&", "§")
                    .replace("%encontrados%", String.valueOf(foundCount)) // Exibe o valor corrigido
                    .replace("%total%", String.valueOf(total));
            player.sendMessage(prefix + msg);

            // Recompensas Individuais
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> rewardsOne = config.getStringList("recompensas.por_cabeca");
                for (String cmd : rewardsOne) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            });

            // Verifica se completou tudo
            if (foundCount >= total) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String msgAll = config.getString("mensagens.completou")
                            .replace("&", "§")
                            .replace("%total%", String.valueOf(total));
                    player.sendMessage(prefix + msgAll);

                    playEffect(player, "som_completou", "LEVEL_UP");

                    List<String> rewardsAll = config.getStringList("recompensas.completou_tudo");
                    for (String cmd : rewardsAll) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                    }
                });
            }
        });
    }

    private void playEffect(Player p, String path, String defaultSound) {
        String soundName = config.getString("efeitos." + path, defaultSound);
        XSound.matchXSound(soundName).ifPresent(xSound -> xSound.play(p));
    }
}