package org.github.luisera.splitlobby.managers;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.github.luisera.splitlobby.Main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileManager implements Listener {

    private final Main plugin;

    private FileConfiguration perfilConfig;
    private FileConfiguration statsConfig;
    private FileConfiguration settingsConfig;
    private FileConfiguration friendsConfig;
    private FileConfiguration hotbarConfig;

    private final Set<UUID> waitingForFriendInput = new HashSet<>();

    public ProfileManager(Main plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        File folder = new File(plugin.getDataFolder(), "perfil");
        if (!folder.exists()) folder.mkdirs();

        perfilConfig = loadYml(folder, "perfil.yml");
        setupPerfilDefaults();

        statsConfig = loadYml(folder, "estatisticas.yml");
        setupStatsDefaults();

        settingsConfig = loadYml(folder, "configuracoes.yml");
        setupSettingsDefaults();

        friendsConfig = loadYml(folder, "amigos.yml");
        setupFriendsDefaults();

        File hotbarFile = new File(plugin.getDataFolder(), "perfil_config.yml");
        if (!hotbarFile.exists()) {
            try { hotbarFile.createNewFile(); } catch(Exception ignored){}
        }
        hotbarConfig = YamlConfiguration.loadConfiguration(hotbarFile);
        hotbarConfig.addDefault("hotbar.slot", 1);
        hotbarConfig.addDefault("hotbar.nome", "&aMeu Perfil &7(Clique)");
        hotbarConfig.options().copyDefaults(true);
        saveYml(hotbarConfig, hotbarFile);
    }

    // --- SETUP DEFAULTS ---
    private void setupFriendsDefaults() {
        friendsConfig.addDefault("menu.titulo", "&8Amigos");
        friendsConfig.addDefault("menu.titulo_lista", "&8Lista de Amigos");

        friendsConfig.addDefault("itens.adicionar.nome", "&aAdicionar Amigo");
        friendsConfig.addDefault("itens.adicionar.icone", "LIME_DYE");
        friendsConfig.addDefault("itens.adicionar.slot", 11);

        friendsConfig.addDefault("itens.lista.nome", "&eLista de Amigos");
        friendsConfig.addDefault("itens.lista.icone", "BOOK");
        friendsConfig.addDefault("itens.lista.slot", 13);
        friendsConfig.addDefault("itens.lista.lore_vazio", "&cNinguém");

        friendsConfig.addDefault("itens.solicitacoes.nome", "&bSolicitações");
        friendsConfig.addDefault("itens.solicitacoes.icone", "PAPER");
        friendsConfig.addDefault("itens.solicitacoes.slot", 15);

        friendsConfig.options().copyDefaults(true);
        saveYml(friendsConfig, new File(plugin.getDataFolder(), "perfil/amigos.yml"));
    }

    // ... (Manter setupStatsDefaults, setupSettingsDefaults, setupPerfilDefaults iguais ao anterior para economizar espaço) ...
    private void setupStatsDefaults() {
        statsConfig.addDefault("games.parkour.nome", "&aParkour");
        statsConfig.addDefault("games.parkour.icone", "FEATHER");
        statsConfig.addDefault("games.parkour.slot", 11);
        statsConfig.addDefault("games.parkour.lore", Arrays.asList("&7Melhor tempo: &f%tempo_parkour%"));
        statsConfig.addDefault("games.chat.nome", "&eJogos do Chat");
        statsConfig.addDefault("games.chat.icone", "PAPER");
        statsConfig.addDefault("games.chat.slot", 13);
        statsConfig.addDefault("games.chat.lore", Arrays.asList("&7Vitórias: &f%vitorias_chat%"));
        statsConfig.options().copyDefaults(true);
        saveYml(statsConfig, new File(plugin.getDataFolder(), "perfil/estatisticas.yml"));
    }
    private void setupSettingsDefaults() {
        settingsConfig.addDefault("menu.titulo", "&8Configurações");
        settingsConfig.addDefault("itens.tell.nome", "&eMensagens Privadas");
        settingsConfig.addDefault("itens.tell.slot", 11);
        settingsConfig.addDefault("itens.chat.nome", "&eChat Global");
        settingsConfig.addDefault("itens.chat.slot", 13);
        settingsConfig.addDefault("itens.pedidos.nome", "&ePedidos de Amizade");
        settingsConfig.addDefault("itens.pedidos.slot", 15);
        settingsConfig.options().copyDefaults(true);
        saveYml(settingsConfig, new File(plugin.getDataFolder(), "perfil/configuracoes.yml"));
    }
    private void setupPerfilDefaults() {
        perfilConfig.addDefault("menu.titulo", "&8Meu Perfil");
        perfilConfig.addDefault("itens.informacoes.slot", 13);
        perfilConfig.addDefault("itens.estatisticas.nome", "&eEstatísticas");
        perfilConfig.addDefault("itens.estatisticas.icone", "BOOK");
        perfilConfig.addDefault("itens.estatisticas.slot", 11);
        perfilConfig.addDefault("itens.configuracoes.nome", "&bConfigurações");
        perfilConfig.addDefault("itens.configuracoes.icone", "COMPARATOR");
        perfilConfig.addDefault("itens.configuracoes.slot", 15);
        perfilConfig.addDefault("itens.amigos.nome", "&aAmigos");
        perfilConfig.addDefault("itens.amigos.icone", "PAPER");
        perfilConfig.addDefault("itens.amigos.slot", 16);
        perfilConfig.options().copyDefaults(true);
        saveYml(perfilConfig, new File(plugin.getDataFolder(), "perfil/perfil.yml"));
    }

    // --- MENUS ---
    public void openMainMenu(Player p) {
        String title = ChatColor.translateAlternateColorCodes('&', perfilConfig.getString("menu.titulo", "&8Meu Perfil"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        int headSlot = perfilConfig.getInt("itens.informacoes.slot", 13);
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(p.getName());
        meta.setDisplayName("§a" + p.getName());
        String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        meta.setLore(Collections.singletonList("§7Data: §f" + today));
        skull.setItemMeta(meta);
        inv.setItem(headSlot, skull);

        setItemFromConfig(inv, perfilConfig, "itens.estatisticas");
        setItemFromConfig(inv, perfilConfig, "itens.configuracoes");
        setItemFromConfig(inv, perfilConfig, "itens.amigos");

        p.openInventory(inv);
        XSound.UI_BUTTON_CLICK.play(p);
    }

    public void openStatsMenu(Player p) {
        String title = "§8Estatísticas";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        if (statsConfig.contains("games")) {
            ConfigurationSection section = statsConfig.getConfigurationSection("games");
            for (String key : section.getKeys(false)) {
                String path = "games." + key;
                ItemStack item = buildItem(statsConfig, path);
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                List<String> finalLore = new ArrayList<>();

                long parkourTime = plugin.getMySQL().getBestParkourTime(p.getUniqueId());
                int chatWins = plugin.getMySQL().getChatGameWins(p.getUniqueId());

                for (String l : lore) {
                    String replaced = l.replace("%tempo_parkour%", parkourTime > 0 ? formatTime(parkourTime) : "---")
                            .replace("%vitorias_chat%", String.valueOf(chatWins));
                    finalLore.add(replaced);
                }
                meta.setLore(finalLore);
                item.setItemMeta(meta);
                inv.setItem(statsConfig.getInt(path + ".slot"), item);
            }
        }
        inv.setItem(49, createSimpleItem(XMaterial.ARROW, "§cVoltar"));
        p.openInventory(inv);
    }

    public void openFriendsMenu(Player p) {
        String title = ChatColor.translateAlternateColorCodes('&', friendsConfig.getString("menu.titulo", "&8Amigos"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        setItemFromConfig(inv, friendsConfig, "itens.adicionar");

        List<String> friends = plugin.getMySQL().getFriendsList(p.getUniqueId());
        String pathLista = "itens.lista";
        ItemStack book = buildItem(friendsConfig, pathLista);
        ItemMeta bm = book.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (friends.isEmpty()) lore.add(friendsConfig.getString("itens.lista.lore_vazio", "&cNinguém").replace("&", "§"));
        else {
            lore.add("§7Total: §f" + friends.size());
            lore.add("");
            lore.add("§eClique para ver todos!");
        }
        bm.setLore(lore);
        book.setItemMeta(bm);
        inv.setItem(friendsConfig.getInt(pathLista + ".slot"), book);

        // Requests
        String pathReq = "itens.solicitacoes";
        ItemStack paper = buildItem(friendsConfig, pathReq);
        ItemMeta pm = paper.getItemMeta();
        List<String> pmLore = new ArrayList<>();
        int reqCount = plugin.getMySQL().getPendingRequests(p.getUniqueId()).size();
        if (pm.hasLore()) for (String l : pm.getLore()) pmLore.add(l.replace("%qtd%", String.valueOf(reqCount)));
        pm.setLore(pmLore);
        paper.setItemMeta(pm);
        inv.setItem(friendsConfig.getInt(pathReq + ".slot"), paper);

        inv.setItem(22, createSimpleItem(XMaterial.ARROW, "§cVoltar"));
        p.openInventory(inv);
    }

    public void openFriendListGUI(Player p) {
        String title = ChatColor.translateAlternateColorCodes('&', friendsConfig.getString("menu.titulo_lista", "&8Lista de Amigos"));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<String> friends = plugin.getMySQL().getFriendsList(p.getUniqueId());

        if (friends.isEmpty()) {
            inv.setItem(22, createSimpleItem(XMaterial.BARRIER, "§cNenhum amigo adicionado"));
        } else {
            for (String friendName : friends) {
                Player target = Bukkit.getPlayerExact(friendName);
                boolean isOnline = (target != null);

                ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwner(friendName);
                meta.setDisplayName("§e" + friendName);

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Status: " + (isOnline ? "§aOnline" : "§cOffline"));
                lore.add("");
                if (isOnline) lore.add("§aClique esquerdo: §7Enviar mensagem");
                lore.add("§cShift+Direito: §7Remover amigo");

                meta.setLore(lore);
                skull.setItemMeta(meta);
                inv.addItem(skull);
            }
        }
        inv.setItem(49, createSimpleItem(XMaterial.ARROW, "§cVoltar"));
        p.openInventory(inv);
    }

    public void openRequestsMenu(Player p) {
        String title = ChatColor.translateAlternateColorCodes('&', friendsConfig.getString("menu.titulo_solicitacoes", "&8Solicitações"));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        Map<String, Integer> requests = plugin.getMySQL().getPendingRequests(p.getUniqueId());
        if (requests.isEmpty()) {
            inv.setItem(22, createSimpleItem(XMaterial.BARRIER, "§cNenhuma solicitação"));
        } else {
            int slot = 0;
            for (Map.Entry<String, Integer> entry : requests.entrySet()) {
                String nick = entry.getKey();
                int id = entry.getValue();
                ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwner(nick);
                meta.setDisplayName("§e" + nick);
                List<String> lore = Arrays.asList("§aAceitar (Clique esq)", "§cRecusar (Clique dir)", "§0ID:" + id);
                meta.setLore(lore);
                skull.setItemMeta(meta);
                inv.setItem(slot++, skull);
            }
        }
        inv.setItem(49, createSimpleItem(XMaterial.ARROW, "§cVoltar"));
        p.openInventory(inv);
    }

    public void openSettingsMenu(Player p) {
        String title = ChatColor.translateAlternateColorCodes('&', settingsConfig.getString("menu.titulo", "&8Configurações"));
        Inventory inv = Bukkit.createInventory(null, 27, title);
        createSettingIcon(inv, p, "tell", "allow_tell");
        createSettingIcon(inv, p, "chat", "allow_chat");
        createSettingIcon(inv, p, "pedidos", "allow_friend_requests");
        inv.setItem(22, createSimpleItem(XMaterial.ARROW, "§cVoltar"));
        p.openInventory(inv);
    }

    // --- ONCLICK ---
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        String itemName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
        Player p = (Player) e.getWhoClicked();

        String titlePerfil = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', perfilConfig.getString("menu.titulo", "Meu Perfil")));
        String titleAmigos = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', friendsConfig.getString("menu.titulo", "Amigos")));
        String titleLista = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', friendsConfig.getString("menu.titulo_lista", "Lista de Amigos")));
        String titleConfig = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', settingsConfig.getString("menu.titulo", "Configurações")));

        if (title.equals(titlePerfil)) {
            e.setCancelled(true);
            if (isItem(itemName, perfilConfig, "itens.configuracoes")) openSettingsMenu(p);
            if (isItem(itemName, perfilConfig, "itens.estatisticas")) openStatsMenu(p);
            if (isItem(itemName, perfilConfig, "itens.amigos")) openFriendsMenu(p);
        }

        if (title.equals(titleAmigos)) {
            e.setCancelled(true);
            if (itemName.contains("Voltar")) openMainMenu(p);
            if (isItem(itemName, friendsConfig, "itens.adicionar")) {
                p.closeInventory();
                waitingForFriendInput.add(p.getUniqueId());
                XSound.ENTITY_PLAYER_LEVELUP.play(p);
                p.sendMessage("§r");
                p.sendMessage("§a§lAMIGOS");
                p.sendMessage("§fDigite o nick do seu amigo(a) no chat para enviar");
                p.sendMessage("§fuma solicitação de amizade!");
                p.sendMessage("§7(Caso queira cancelar digite §cCANCELAR§7)");
                p.sendMessage("§r");
            }
            if (isItem(itemName, friendsConfig, "itens.lista")) {
                openFriendListGUI(p);
            }
            if (isItem(itemName, friendsConfig, "itens.solicitacoes")) openRequestsMenu(p);
        }

        if (title.equals(titleLista)) {
            e.setCancelled(true);
            if (itemName.contains("Voltar")) openFriendsMenu(p);
            if (e.getCurrentItem().getType() == XMaterial.PLAYER_HEAD.parseItem().getType()) {
                String targetName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    UUID targetUUID = plugin.getMySQL().getUUID(targetName);
                    if (targetUUID != null) {
                        plugin.getMySQL().removeFriend(p.getUniqueId(), targetUUID);
                        p.sendMessage("§cVocê removeu " + targetName + " dos amigos.");
                        XSound.BLOCK_ANVIL_BREAK.play(p);
                        openFriendListGUI(p);
                    }
                } else if (e.isLeftClick()) {
                    Player target = Bukkit.getPlayerExact(targetName);
                    if (target != null) {
                        p.closeInventory();
                        p.sendMessage("§eDigite sua mensagem para " + targetName + ":");
                    } else {
                        p.sendMessage("§c" + targetName + " está offline.");
                        XSound.BLOCK_NOTE_BLOCK_BASS.play(p);
                    }
                }
            }
        }

        // Solicitações
        if (title.contains("Solicitacoes") || title.contains("Solicitações")) {
            e.setCancelled(true);
            if (itemName.contains("Voltar")) openFriendsMenu(p);
            if (e.getCurrentItem().getType() == XMaterial.PLAYER_HEAD.parseItem().getType()) {
                handleRequestClick(p, e.getCurrentItem(), e.isLeftClick());
            }
        }

        if (title.equals(titleConfig)) {
            e.setCancelled(true);
            if (itemName.contains("Voltar")) openMainMenu(p);
            if (isItem(itemName, settingsConfig, "itens.tell")) toggleAndRefresh(p, "allow_tell");
            if (isItem(itemName, settingsConfig, "itens.chat")) toggleAndRefresh(p, "allow_chat");
            if (isItem(itemName, settingsConfig, "itens.pedidos")) toggleAndRefresh(p, "allow_friend_requests");
        }

        if (title.contains("Estatisticas") || title.contains("Estatísticas")) {
            e.setCancelled(true);
            if (itemName.contains("Voltar")) openMainMenu(p);
        }
    }

    // --- EVENTO DE CHAT (COM VERIFICAÇÃO E MENSAGENS) ---
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (waitingForFriendInput.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Player p = e.getPlayer();
            waitingForFriendInput.remove(p.getUniqueId());

            if (msg.equalsIgnoreCase("cancelar")) {
                p.sendMessage("§r");
                p.sendMessage("§a§lAMIGOS");
                p.sendMessage("&fSua solicitação foi §cCANCELADA§f!");
                p.sendMessage("§r");
                return;
            }

            p.sendMessage("§eProcessando...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                UUID targetUUID = plugin.getMySQL().getUUID(msg);

                // Verifica se jogador existe
                if (targetUUID == null) {
                    p.sendMessage("§r");
                    p.sendMessage("§b§lAMIGOS");
                    p.sendMessage("§fJogador §CNÃO ENCONTRADO§f!");
                    p.sendMessage("§r");
                    return;
                }

                // Verifica se é ele mesmo
                if (targetUUID.equals(p.getUniqueId())) {
                    p.sendMessage("§cVocê não pode adicionar a si mesmo.");
                    return;
                }

                // Verifica se já são amigos ou pendente (USANDO O NOVO METODO DO MYSQL)
                if (plugin.getMySQL().areFriendsOrPending(p.getUniqueId(), targetUUID)) {
                    p.sendMessage("§r");
                    p.sendMessage("§b§lAMIGOS");
                    p.sendMessage("§cNão foi possível enviar solicitação pois vocês");
                    p.sendMessage("§cjá são amigos ou já existe um pedido!");
                    p.sendMessage("§r");
                    XSound.BLOCK_NOTE_BLOCK_BASS.play(p);
                    return;
                }

                // Envia solicitação
                plugin.getMySQL().sendFriendRequest(p.getUniqueId(), targetUUID);

                // Mensagem para quem enviou
                p.sendMessage("§r");
                p.sendMessage("§b§lAMIGOS");
                p.sendMessage("§aVocê enviou uma solicitação de amizade para §e" + msg);
                p.sendMessage("§r");
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(p);

                // Mensagem para quem recebeu (se online)
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    target.sendMessage("§r");
                    target.sendMessage("§b§lAMIGOS");
                    target.sendMessage("§e" + p.getName() + " §ate enviou uma solicitação de amizade!");
                    target.sendMessage("§r");
                    XSound.BLOCK_NOTE_BLOCK_PLING.play(target);
                }
            });
        }
    }

    // --- MÉTODOS AUXILIARES ---
    private void handleRequestClick(Player p, ItemStack item, boolean isLeft) {
        String idLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
        String senderName = ChatColor.stripColor(item.getItemMeta().getDisplayName()); // Pega o nome de quem enviou

        if (idLine.startsWith("§0ID:")) {
            int id = Integer.parseInt(idLine.replace("§0ID:", ""));

            if (isLeft) {
                plugin.getMySQL().acceptRequest(id);
                p.sendMessage("§r");
                p.sendMessage("§b§lAMIGOS");
                p.sendMessage("§aVocê aceitou a solicitação de amizade!");
                p.sendMessage("§r");
                XSound.ENTITY_PLAYER_LEVELUP.play(p);

                // Avisa quem enviou (se online)
                Player sender = Bukkit.getPlayerExact(senderName);
                if (sender != null) {
                    sender.sendMessage("§r");
                    sender.sendMessage("§b§lAMIGOS");
                    sender.sendMessage("§e" + p.getName() + " §aaceitou sua solicitação de amizade!");
                    sender.sendMessage("§r");
                    XSound.ENTITY_PLAYER_LEVELUP.play(sender);
                }

            } else {
                plugin.getMySQL().rejectRequest(id);
                p.sendMessage("§r");
                p.sendMessage("§b§lAMIGOS");
                p.sendMessage("§cSolicitação recusada.");
                p.sendMessage("§r");
                XSound.UI_BUTTON_CLICK.play(p);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> openRequestsMenu(p), 2L);
        }
    }

    private boolean isItem(String clickedNameStripped, FileConfiguration config, String path) {
        String configName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', config.getString(path + ".nome", "")));
        return clickedNameStripped.equals(configName);
    }
    private void createSettingIcon(Inventory inv, Player p, String key, String dbCol) {
        boolean status = plugin.getMySQL().getSetting(p.getUniqueId(), dbCol);
        String path = "itens." + key;
        String name = settingsConfig.getString(path + ".nome", "&eOpção").replace("&", "§");
        int slot = settingsConfig.getInt(path + ".slot");
        XMaterial mat = status ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE;
        String statusTxt = status ? "§aATIVADO" : "§cDESATIVADO";
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String l : settingsConfig.getStringList(path + ".lore")) lore.add(l.replace("&", "§").replace("%status%", statusTxt));
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
    private void toggleAndRefresh(Player p, String col) {
        XSound.UI_BUTTON_CLICK.play(p);
        plugin.getMySQL().toggleSetting(p.getUniqueId(), col, () -> openSettingsMenu(p));
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null || !e.getItem().hasItemMeta()) return;
        String itemName = e.getItem().getItemMeta().getDisplayName();
        String configName = ChatColor.translateAlternateColorCodes('&', hotbarConfig.getString("hotbar.nome", "&aMeu Perfil"));
        if (itemName.equals(configName)) {
            e.setCancelled(true);
            if (e.getAction().toString().contains("RIGHT") || e.getAction().toString().contains("LEFT")) openMainMenu(e.getPlayer());
        }
    }
    public void giveProfileItem(Player p) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(p.getName());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', hotbarConfig.getString("hotbar.nome", "&aMeu Perfil")));
        List<String> lore = new ArrayList<>();
        for (String l : hotbarConfig.getStringList("hotbar.lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));
        meta.setLore(lore);
        skull.setItemMeta(meta);
        p.getInventory().setItem(hotbarConfig.getInt("hotbar.slot", 1), skull);
    }
    private FileConfiguration loadYml(File folder, String name) {
        File file = new File(folder, name);
        if (!file.exists()) { try { file.createNewFile(); } catch (Exception ignored) {} }
        return YamlConfiguration.loadConfiguration(file);
    }
    private void saveYml(FileConfiguration config, File file) {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
    private void setItemFromConfig(Inventory inv, FileConfiguration config, String path) {
        if (!config.contains(path)) return;
        inv.setItem(config.getInt(path + ".slot"), buildItem(config, path));
    }
    private ItemStack buildItem(FileConfiguration config, String path) {
        String matName = config.getString(path + ".icone", "STONE");
        String name = config.getString(path + ".nome", "Item").replace("&", "§");
        XMaterial mat = XMaterial.matchXMaterial(matName).orElse(XMaterial.STONE);
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack createSimpleItem(XMaterial mat, String name) {
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis % 1000);
    }
}