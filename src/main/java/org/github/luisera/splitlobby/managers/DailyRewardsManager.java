// ============================================
// DailyRewardsManager.java - COMPATÍVEL COM 1.8.8
// ============================================
package org.github.luisera.splitlobby.managers;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.github.luisera.splitlobby.Main;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class DailyRewardsManager implements Listener {

    private final Main plugin;
    private File file;
    private FileConfiguration config;
    private final Map<String, RewardData> rewardsCache = new HashMap<>();

    public DailyRewardsManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        file = new File(plugin.getDataFolder(), "rewards_daily.yml");
        if (!file.exists()) {
            plugin.saveResource("rewards_daily.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadRewards();
    }

    private void loadRewards() {
        rewardsCache.clear();
        ConfigurationSection section = config.getConfigurationSection("recompensas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "recompensas." + key + ".";
            String titulo = config.getString(path + "Titulo", config.getString(path + "titulo", "Sem Titulo"));
            List<String> desc = config.getStringList(path + "Descrição");
            if (desc.isEmpty()) desc = config.getStringList(path + "lore");

            String configPath = config.contains(path + "Configurações") ? path + "Configurações." : path;
            String tempoStr = config.getString(configPath + "tempo");
            List<String> comandos = config.getStringList(configPath + "comandos_para_reivindicar");
            if (comandos.isEmpty()) comandos = config.getStringList(configPath + "comandos");

            int slot = config.getInt(configPath + "Slots no menu", config.getInt(configPath + "slot", -1));
            String iconeStr = config.getString(configPath + "icone", config.getString(configPath + "icone_disponivel", "WATCH"));

            long requiredTicks = parseTime(tempoStr);
            XMaterial material = XMaterial.matchXMaterial(iconeStr).orElse(XMaterial.BEDROCK);

            rewardsCache.put(key, new RewardData(key, titulo, desc, requiredTicks, comandos, slot, material));
        }
        plugin.enviarMensagem("§a[Rewards] Carregadas " + rewardsCache.size() + " recompensas.");
    }

    // ============================================
    // SISTEMA DE NPC COM VILLAGER (1.8.8 COMPATÍVEL)
    // ============================================

    /**
     * Spawna um NPC Villager compatível com 1.8.8
     */
    public void spawnNPC(Location loc) {
        removeNPCEntities();

        // 1. SPAWNA O VILLAGER
        Villager npc = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);

        // Configurações básicas (SEM NOME - só hologramas)
        npc.setCustomNameVisible(false); // ← Nome invisível
        npc.setProfession(Villager.Profession.LIBRARIAN);
        npc.setAdult();
        npc.setCanPickupItems(false);
        npc.setRemoveWhenFarAway(false);

        // Marca como NPC (metadata)
        npc.setMetadata("REWARDS_NPC", new FixedMetadataValue(plugin, true));

        // Remove IA usando NMS (1.8.8)
        try {
            removeAI(npc);
        } catch (Exception e) {
            plugin.enviarMensagem("§e[Rewards] Aviso: Não foi possível remover IA do Villager");
        }

        // Tenta tornar invulnerável (1.9+, ignora erro na 1.8)
        try {
            Method setInvulnerable = npc.getClass().getMethod("setInvulnerable", boolean.class);
            setInvulnerable.invoke(npc, true);
        } catch (Exception e) {
            // 1.8 não tem este método, ignora
        }

        // 2. HOLOGRAMAS (AJUSTADOS PARA FICAREM MAIS BAIXOS)
        Location holoLoc1 = loc.clone().add(0, 0.5, 0);  // ← Linha 1 (mais baixa)
        Location holoLoc2 = loc.clone().add(0, 0.25, 0); // ← Linha 2 (bem baixa)

        ArmorStand holo1 = spawnHologram(holoLoc1, "§6§lRECOMPENSAS DIÁRIAS");
        ArmorStand holo2 = spawnHologram(holoLoc2, "§7Clique com §eBOTÃO DIREITO");

        // 3. SALVA NO CONFIG
        plugin.getConfig().set("recompensas.npc-uuid", npc.getUniqueId().toString());
        plugin.getConfig().set("recompensas.npc-location", serializeLocation(loc));
        plugin.getConfig().set("recompensas.holo1-uuid", holo1.getUniqueId().toString());
        plugin.getConfig().set("recompensas.holo2-uuid", holo2.getUniqueId().toString());
        plugin.saveConfig();

        plugin.enviarMensagem("§a[Rewards] NPC Villager spawnado em: " + formatLocation(loc));
    }

    /**
     * Remove a IA do Villager usando NMS (1.8.8)
     */
    private void removeAI(Villager villager) throws Exception {
        // Pega a classe CraftVillager
        Object craftVillager = villager;
        Method getHandle = craftVillager.getClass().getMethod("getHandle");
        Object entityVillager = getHandle.invoke(craftVillager);

        // Desabilita os PathfinderGoals (IA)
        Class<?> entityInsentient = entityVillager.getClass().getSuperclass();

        // Limpa goalSelector (movimentação)
        try {
            Object goalSelector = entityInsentient.getDeclaredField("goalSelector").get(entityVillager);
            Method clearGoals = goalSelector.getClass().getMethod("a");
            clearGoals.invoke(goalSelector);
        } catch (Exception e) {
            // Ignora se falhar
        }

        // Limpa targetSelector (alvos)
        try {
            Object targetSelector = entityInsentient.getDeclaredField("targetSelector").get(entityVillager);
            Method clearTargets = targetSelector.getClass().getMethod("a");
            clearTargets.invoke(targetSelector);
        } catch (Exception e) {
            // Ignora se falhar
        }
    }

    /**
     * Cria holograma com ArmorStand
     */
    private ArmorStand spawnHologram(Location loc, String text) {
        ArmorStand holo = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

        holo.setVisible(false);
        holo.setGravity(false);
        holo.setSmall(true);
        holo.setCustomName(text.replace("&", "§"));
        holo.setCustomNameVisible(true);
        holo.setCanPickupItems(false);
        holo.setRemoveWhenFarAway(false);

        // Marca como holograma
        holo.setMetadata("REWARDS_HOLO", new FixedMetadataValue(plugin, true));

        try {
            Method setInvulnerable = holo.getClass().getMethod("setInvulnerable", boolean.class);
            setInvulnerable.invoke(holo, true);
        } catch (Exception e) {
            // 1.8 compatibility
        }

        return holo;
    }

    /**
     * Remove o NPC
     */
    public void removeNPC(Player p) {
        removeNPCEntities();

        plugin.getConfig().set("recompensas.npc-uuid", null);
        plugin.getConfig().set("recompensas.npc-location", null);
        plugin.getConfig().set("recompensas.holo1-uuid", null);
        plugin.getConfig().set("recompensas.holo2-uuid", null);
        plugin.saveConfig();

        p.sendMessage("§a✓ NPC Entregador e hologramas removidos!");
    }

    /**
     * Remove todas as entidades do NPC
     */
    private void removeNPCEntities() {
        removeEntityByUUID(plugin.getConfig().getString("recompensas.npc-uuid"));
        removeEntityByUUID(plugin.getConfig().getString("recompensas.holo1-uuid"));
        removeEntityByUUID(plugin.getConfig().getString("recompensas.holo2-uuid"));
    }

    /**
     * Remove entidade por UUID
     */
    private void removeEntityByUUID(String uuidStr) {
        if (uuidStr == null) return;

        try {
            UUID uuid = UUID.fromString(uuidStr);

            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getUniqueId().equals(uuid)) {
                        entity.remove();
                        return;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // UUID inválida
        }
    }

    /**
     * Listener para cliques no NPC
     * PRIORITY HIGHEST para cancelar ANTES do trade
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNPCClick(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Verifica se tem a metadata de NPC
        if (!entity.hasMetadata("REWARDS_NPC")) {
            // Verifica por UUID (fallback)
            String npcUUID = plugin.getConfig().getString("recompensas.npc-uuid");
            if (npcUUID == null || !entity.getUniqueId().toString().equals(npcUUID)) {
                return;
            }
        }

        // CANCELA O EVENTO (impede menu de trade)
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Efeito sonoro
        XSound.ENTITY_VILLAGER_YES.play(player);

        // Abre o menu de recompensas
        Bukkit.getScheduler().runTask(plugin, () -> {
            openMenu(player);
        });
    }

    /**
     * NOVO: Protege o NPC de dano
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNPCDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Verifica se é o NPC
        if (entity.hasMetadata("REWARDS_NPC")) {
            event.setCancelled(true);
            return;
        }

        // Fallback: Verifica por UUID
        String npcUUID = plugin.getConfig().getString("recompensas.npc-uuid");
        if (npcUUID != null && entity.getUniqueId().toString().equals(npcUUID)) {
            event.setCancelled(true);
        }
    }

    /**
     * NOVO: Protege o NPC de dano por jogador
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNPCDamageByPlayer(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // Verifica se é o NPC
        if (entity.hasMetadata("REWARDS_NPC")) {
            event.setCancelled(true);

            // Mensagem opcional ao jogador
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                player.sendMessage("§c§l✖ §cVocê não pode atacar o NPC!");
            }
            return;
        }

        // Fallback: Verifica por UUID
        String npcUUID = plugin.getConfig().getString("recompensas.npc-uuid");
        if (npcUUID != null && entity.getUniqueId().toString().equals(npcUUID)) {
            event.setCancelled(true);

            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                player.sendMessage("§c§l✖ §cVocê não pode atacar o NPC!");
            }
        }
    }

    /**
     * NOVO: Protege hologramas também
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHologramDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // Protege hologramas
        if (entity.hasMetadata("REWARDS_HOLO")) {
            event.setCancelled(true);
        }

        // Verifica por UUID dos hologramas
        String holo1UUID = plugin.getConfig().getString("recompensas.holo1-uuid");
        String holo2UUID = plugin.getConfig().getString("recompensas.holo2-uuid");

        String entityUUID = entity.getUniqueId().toString();

        if ((holo1UUID != null && entityUUID.equals(holo1UUID)) ||
                (holo2UUID != null && entityUUID.equals(holo2UUID))) {
            event.setCancelled(true);
        }
    }

    /**
     * Recarrega NPC após restart
     */
    public void reloadNPC() {
        String locStr = plugin.getConfig().getString("recompensas.npc-location");
        if (locStr == null) return;

        Location loc = deserializeLocation(locStr);
        if (loc == null) return;

        String npcUUID = plugin.getConfig().getString("recompensas.npc-uuid");
        if (npcUUID == null) return;

        // Verifica se NPC existe
        boolean npcExists = false;
        for (Entity entity : loc.getWorld().getEntities()) {
            if (entity.getUniqueId().toString().equals(npcUUID)) {
                npcExists = true;

                // Reaplica metadata se perdeu
                if (!entity.hasMetadata("REWARDS_NPC")) {
                    entity.setMetadata("REWARDS_NPC", new FixedMetadataValue(plugin, true));
                }
                break;
            }
        }

        if (!npcExists) {
            plugin.enviarMensagem("§e[Rewards] NPC não encontrado, recriando...");
            spawnNPC(loc);
        }
    }

    // ============================================
    // UTILITÁRIOS
    // ============================================

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ";" +
                loc.getX() + ";" +
                loc.getY() + ";" +
                loc.getZ() + ";" +
                loc.getYaw() + ";" +
                loc.getPitch();
    }

    private Location deserializeLocation(String str) {
        try {
            String[] parts = str.split(";");
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String formatLocation(Location loc) {
        return String.format("§e%s §7[§f%.1f, %.1f, %.1f§7]",
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ()
        );
    }

    // ============================================
    // SISTEMA DE MENU
    // ============================================

    public void openMenu(Player player) {
        int size = config.getInt("menu.tamanho", 27);
        String title = config.getString("menu.titulo", "Recompensas").replace("&", "§");
        Inventory inv = Bukkit.createInventory(null, size, title);

        long playerTicks = player.getStatistic(Statistic.PLAY_ONE_TICK);

        for (RewardData reward : rewardsCache.values()) {
            boolean claimed = plugin.getMySQL().hasClaimedReward(player.getUniqueId(), reward.key);
            boolean canClaim = playerTicks >= reward.requiredTicks;

            ItemStack item = reward.icon.parseItem();
            if (item == null) item = XMaterial.BEDROCK.parseItem();

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(reward.titulo.replace("&", "§"));

            String statusTxt;
            String pathMsg = config.contains("mensagens.recebido") ? "mensagens." : "status.";
            if (claimed) statusTxt = config.getString(pathMsg + "recebido", "&aSIM");
            else if (canClaim) statusTxt = config.getString(pathMsg + "disponivel", "&aRESGATAR");
            else statusTxt = config.getString(pathMsg + "pendente", "&cNÃO");

            String progressTxt = formatProgress(playerTicks, reward.requiredTicks);

            List<String> lore = new ArrayList<>();
            for (String line : reward.descricao) {
                lore.add(line.replace("%status%", statusTxt)
                        .replace("%progresso%", progressTxt)
                        .replace("&", "§"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(reward.slot, item);
        }

        player.openInventory(inv);
        XSound.BLOCK_CHEST_OPEN.play(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = config.getString("menu.titulo", "Recompensas").replace("&", "§");
        if (!event.getView().getTitle().equals(title)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        RewardData reward = rewardsCache.values().stream()
                .filter(r -> r.slot == event.getSlot())
                .findFirst().orElse(null);

        if (reward == null) return;

        long playerTicks = player.getStatistic(Statistic.PLAY_ONE_TICK);

        if (plugin.getMySQL().hasClaimedReward(player.getUniqueId(), reward.key)) {
            player.sendMessage("§cVocê já reivindicou esta recompensa.");
            XSound.ENTITY_VILLAGER_NO.play(player);
            return;
        }

        if (playerTicks < reward.requiredTicks) {
            player.sendMessage("§cVocê não tem tempo suficiente.");
            XSound.ENTITY_VILLAGER_NO.play(player);
            return;
        }

        if (reward.comandos != null) {
            for (String cmd : reward.comandos) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
        }

        plugin.getMySQL().setRewardClaimed(player.getUniqueId(), reward.key);
        player.sendMessage("§aRecompensa " + reward.titulo.replace("&", "§") + " §aresgatada!");
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        player.closeInventory();
    }

    private String formatProgress(long currentTicks, long targetTicks) {
        if (currentTicks >= targetTicks) return "§aConcluído";

        long remainingSeconds = (targetTicks - currentTicks) / 20;
        long days = remainingSeconds / 86400;
        long hours = (remainingSeconds % 86400) / 3600;
        long minutes = (remainingSeconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(minutes).append("m restantes");
        return sb.toString().trim();
    }

    private long parseTime(String time) {
        if (time == null || time.isEmpty()) return 0;

        time = time.toUpperCase();
        long multiplier;

        if (time.endsWith("MS")) multiplier = 2592000L * 20L;
        else if (time.endsWith("D")) multiplier = 86400L * 20L;
        else if (time.endsWith("H")) multiplier = 3600L * 20L;
        else if (time.endsWith("M")) multiplier = 60L * 20L;
        else {
            try {
                return Long.parseLong(time);
            } catch (Exception e) {
                return 0;
            }
        }

        try {
            String num = time.replaceAll("[A-Z]", "");
            return Long.parseLong(num) * multiplier;
        } catch (Exception e) {
            return 0;
        }
    }

    private static class RewardData {
        String key, titulo;
        List<String> descricao, comandos;
        long requiredTicks;
        int slot;
        XMaterial icon;

        public RewardData(String key, String titulo, List<String> desc, long ticks, List<String> cmds, int slot, XMaterial icon) {
            this.key = key;
            this.titulo = titulo;
            this.descricao = desc;
            this.requiredTicks = ticks;
            this.comandos = cmds;
            this.slot = slot;
            this.icon = icon;
        }
    }
}