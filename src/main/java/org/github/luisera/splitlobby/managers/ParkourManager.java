package org.github.luisera.splitlobby.managers;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.github.luisera.splitlobby.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ParkourManager {

    private final Main plugin;
    private File file;
    private FileConfiguration config;

    private final Map<UUID, Long> startTimes = new HashMap<>();
    private final Map<UUID, Integer> playerCheckpoints = new HashMap<>();
    private final Set<UUID> setupMode = new HashSet<>();

    // Locais
    private Location startLocation;
    private Location endLocation;
    private List<Location> checkpoints = new ArrayList<>();

    // Local do Holograma de Top 10
    private Location topHologramLocation;

    // CORREÇÃO: Lista para rastrear e remover hologramas antigos
    private final List<ArmorStand> activeLeaderboardHolograms = new ArrayList<>();

    public ParkourManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        file = new File(plugin.getDataFolder(), "parkour.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("start")) startLocation = (Location) config.get("start");
        if (config.contains("end")) endLocation = (Location) config.get("end");
        if (config.contains("checkpoints")) {
            checkpoints = (List<Location>) config.getList("checkpoints");
        }
        if (config.contains("top-location")) {
            topHologramLocation = (Location) config.get("top-location");
            // Atualiza ao ligar (com delay pequeno para garantir carregamento de mundos)
            new BukkitRunnable() {
                @Override
                public void run() { updateLeaderboard(); }
            }.runTaskLater(plugin, 40L);
        }
    }

    public void saveLocations() {
        config.set("start", startLocation);
        config.set("end", endLocation);
        config.set("checkpoints", checkpoints);
        config.set("top-location", topHologramLocation);
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // --- HOLOGRAMAS VISUAIS (Start / Checkpoint) ---
    public void spawnStaticHologram(Location loc, String[] lines) {
        Location hologramLoc = loc.clone().add(0.5, +0.5, 0.5);
        double spacing = 0.25;
        for (int i = lines.length - 1; i >= 0; i--) {
            hologramLoc.add(0, spacing, 0);
            spawnArmorStand(hologramLoc, lines[i]);
        }
    }

    // --- LEADERBOARD (TOP 10) ---
    public void setTopHologramLocation(Location loc) {
        this.topHologramLocation = loc;
        saveLocations();
        updateLeaderboard();
    }

    public void updateLeaderboard() {
        if (topHologramLocation == null) return;

        // 1. LIMPEZA VIA LISTA (Remove hologramas que sabemos que criamos)
        Iterator<ArmorStand> it = activeLeaderboardHolograms.iterator();
        while (it.hasNext()) {
            ArmorStand as = it.next();
            if (as != null && !as.isDead()) {
                as.remove();
            }
            it.remove();
        }

        // 2. LIMPEZA DE RESÍDUOS (Caso o servidor tenha reiniciado e a lista esteja vazia)
        // Procura ArmorStands num raio de 4 blocos que pareçam ser do nosso plugin
        if (topHologramLocation.getWorld() != null) {
            for (Entity e : topHologramLocation.getWorld().getNearbyEntities(topHologramLocation, 4, 6, 4)) {
                if (e.getType() == EntityType.ARMOR_STAND && e.getCustomName() != null) {
                    String name = e.getCustomName();
                    // Remove se tiver nossos títulos ou se parecer uma linha de top (Ex: "1. Nick")
                    if (name.contains("VELOZES NO PARKOUR") ||
                            name.contains("TOP 10") ||
                            name.matches(".*§e\\d+\\. §f.*")) { // Regex simples para capturar linhas de rank
                        e.remove();
                    }
                }
            }
        }

        // Busca dados no MySQL
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getMySQL() == null) return;
                Map<String, Long> tops = plugin.getMySQL().getTop10();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        spawnLeaderboardHolograms(tops);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void spawnLeaderboardHolograms(Map<String, Long> tops) {
        if (topHologramLocation == null || topHologramLocation.getWorld() == null) return;

        Location loc = topHologramLocation.clone();

        // Cabeçalho
        activeLeaderboardHolograms.add(spawnArmorStand(loc.add(0, 0.3, 0), "§e§lVELOZES NO PARKOUR"));
        activeLeaderboardHolograms.add(spawnArmorStand(loc.add(0, 0.3, 0), "§a§lTOP 10"));

        loc.subtract(0, 0.6, 0); // Reset altura

        int i = 1;

        if (tops.isEmpty()) {
            loc.subtract(0, 0.3, 0);
            activeLeaderboardHolograms.add(spawnArmorStand(loc, "§7Nenhum recorde ainda."));
        } else {
            for (Map.Entry<String, Long> entry : tops.entrySet()) {
                loc.subtract(0, 0.3, 0);
                String time = formatTime(entry.getValue());
                String line = "§e" + i + ". §f" + entry.getKey() + " §7- §a" + time;

                // Adiciona na lista para podermos remover depois
                activeLeaderboardHolograms.add(spawnArmorStand(loc, line));
                i++;
            }
        }
    }

    private ArmorStand spawnArmorStand(Location loc, String name) {
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setGravity(false);
        as.setVisible(false);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setBasePlate(false);
        as.setSmall(true); // Opcional: deixa menorzinho e mais agrupado
        return as;
    }

    // --- MÉTODOS DO JOGO (Mantidos iguais) ---

    public void finishParkour(Player p) {
        if (!isPlaying(p)) return;

        long timeMillis = System.currentTimeMillis() - startTimes.get(p.getUniqueId());
        String timeString = formatTime(timeMillis);

        quitParkour(p, false);
        org.github.luisera.splitlobby.Main.getInstance().giveLobbyItems(p);

        p.sendMessage("§a§lPARABÉNS! §fVocê completou em §e" + timeString);
        p.sendTitle("§6§lVITÓRIA!", "§fTempo: " + timeString);
        XSound.UI_TOAST_CHALLENGE_COMPLETE.play(p);

        if (plugin.getLobbyLocation() != null) p.teleport(plugin.getLobbyLocation());

        if (plugin.getMySQL() != null) {
            plugin.getMySQL().saveParkourTime(p, timeMillis);
            // Atualiza o Top 10 após salvar
            new BukkitRunnable() {
                @Override
                public void run() { updateLeaderboard(); }
            }.runTaskLater(plugin, 40L);
        }
    }

    // ... (O resto do código: startParkour, toggleSetup, etc. permanece idêntico ao anterior) ...
    // Vou incluir os métodos essenciais para não quebrar a classe

    public void toggleSetup(Player p) {
        if (setupMode.contains(p.getUniqueId())) {
            setupMode.remove(p.getUniqueId());
            p.getInventory().clear();
            p.sendMessage("§cModo configuração finalizado.");
        } else {
            setupMode.add(p.getUniqueId());
            p.getInventory().clear();
            giveSetupItems(p);
            p.sendMessage("§aModo configuração iniciado!");
        }
    }
    public boolean isSetup(Player p) { return setupMode.contains(p.getUniqueId()); }
    private void giveSetupItems(Player p) {
        p.getInventory().setItem(0, createItem(XMaterial.HEAVY_WEIGHTED_PRESSURE_PLATE.parseMaterial(), "§aMarcar Início §7(Ferro)"));
        p.getInventory().setItem(1, createItem(XMaterial.LIGHT_WEIGHTED_PRESSURE_PLATE.parseMaterial(), "§eMarcar Checkpoint §7(Ouro)"));
        p.getInventory().setItem(2, createItem(XMaterial.OAK_PRESSURE_PLATE.parseMaterial(), "§cMarcar Fim §7(Madeira)"));
        p.getInventory().setItem(8, createItem(XMaterial.EMERALD.parseMaterial(), "§aSalvar Configuração"));
    }
    public boolean isPlaying(Player p) { return startTimes.containsKey(p.getUniqueId()); }
    public void startParkour(Player p) {
        if (isPlaying(p)) return;
        startTimes.put(p.getUniqueId(), System.currentTimeMillis());
        playerCheckpoints.put(p.getUniqueId(), -1);
        p.getInventory().clear();
        p.getInventory().setItem(3, createItem(XMaterial.STRING.parseMaterial(), "§eReiniciar"));
        p.getInventory().setItem(4, createItem(XMaterial.OAK_DOOR.parseMaterial(), "§6Voltar ao Checkpoint"));
        p.getInventory().setItem(5, createItem(XMaterial.REDSTONE.parseMaterial(), "§cSair"));
        p.sendTitle("§a§lPARKOUR", "§fIniciado!");
        XSound.ENTITY_PLAYER_LEVELUP.play(p);
    }
    public void reachCheckpoint(Player p, Location loc) {
        if (!isPlaying(p)) return;
        int index = -1;
        for (int i = 0; i < checkpoints.size(); i++) {
            Location cp = checkpoints.get(i);
            if (cp.getBlockX() == loc.getBlockX() && cp.getBlockY() == loc.getBlockY() && cp.getBlockZ() == loc.getBlockZ()) {
                index = i; break;
            }
        }
        if (index == -1) return;
        int current = playerCheckpoints.get(p.getUniqueId());
        if (index > current) {
            playerCheckpoints.put(p.getUniqueId(), index);
            p.sendMessage("§a§lCHECKPOINT! §fVocê alcançou o checkpoint #" + (index + 1));
            p.sendTitle("§3§lPARKOUR", "§eCheckpoint #" + (index + 1));
            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(p);
        }
    }
    public void restartParkour(Player p) {
        if (!isPlaying(p)) return;
        if (startLocation != null) p.teleport(startLocation);
        startTimes.put(p.getUniqueId(), System.currentTimeMillis());
        playerCheckpoints.put(p.getUniqueId(), -1);
        p.sendMessage("§eVocê reiniciou o parkour.");
    }
    public void returnToCheckpoint(Player p) {
        if (!isPlaying(p)) return;
        int cpIndex = playerCheckpoints.get(p.getUniqueId());
        if (cpIndex == -1) {
            if (startLocation != null) p.teleport(startLocation);
        } else {
            Location cpLoc = checkpoints.get(cpIndex);
            Location teleportLoc = cpLoc.clone().add(0.5, 0, 0.5);
            teleportLoc.setYaw(p.getLocation().getYaw());
            teleportLoc.setPitch(p.getLocation().getPitch());
            p.teleport(teleportLoc);
        }
        XSound.ENTITY_ENDERMAN_TELEPORT.play(p);
    }
    public void quitParkour(Player p, boolean teleportSpawn) {
        if (setupMode.contains(p.getUniqueId())) return;
        startTimes.remove(p.getUniqueId());
        playerCheckpoints.remove(p.getUniqueId());
        p.getInventory().clear();
        if (teleportSpawn && plugin.getLobbyLocation() != null) {
            p.teleport(plugin.getLobbyLocation());
            org.github.luisera.splitlobby.Main.getInstance().giveLobbyItems(p);
            p.sendMessage("§cVocê saiu do parkour.");

        }
    }
    public String getTimer(Player p) {
        if (!startTimes.containsKey(p.getUniqueId())) return "00:00";
        long diff = System.currentTimeMillis() - startTimes.get(p.getUniqueId());
        return formatTime(diff);
    }
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }
    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public Location getTopHologramLocation() {
        return topHologramLocation;
    }

    public void removeLeaderboard() {
        // 1. Remove os ArmorStands físicos do mundo
        if (!activeLeaderboardHolograms.isEmpty()) {
            for (ArmorStand as : activeLeaderboardHolograms) {
                if (as != null && !as.isDead()) {
                    as.remove();
                }
            }
            activeLeaderboardHolograms.clear();
        }

        // 2. Tenta limpar qualquer resíduo na localização antiga (limpeza profunda)
        if (topHologramLocation != null && topHologramLocation.getWorld() != null) {
            for (Entity e : topHologramLocation.getWorld().getNearbyEntities(topHologramLocation, 2, 5, 2)) {
                if (e.getType() == EntityType.ARMOR_STAND && e.getCustomName() != null) {
                    String name = e.getCustomName();
                    if (name.contains("VELOZES NO PARKOUR") || name.contains("TOP 10")) {
                        e.remove();
                    }
                }
            }
        }

        // 3. Remove da memória e do arquivo
        this.topHologramLocation = null;
        config.set("top-location", null); // Remove do YAML
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void setStart(Location loc) { this.startLocation = loc; spawnStaticHologram(loc, new String[]{"§3§lPARKOUR", "§aInicio"}); }
    public void addCheckpoint(Location loc) { this.checkpoints.add(loc); spawnStaticHologram(loc, new String[]{"§3§lPARKOUR", "§eCheckpoint #" + checkpoints.size()}); }
    public void setEnd(Location loc) { this.endLocation = loc; spawnStaticHologram(loc, new String[]{"§3§lPARKOUR", "§cFim"}); }
    public Location getStartLocation() { return startLocation; }
    public Location getEndLocation() { return endLocation; }
}