package org.github.luisera.splitlobby;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.github.luisera.splitlobby.commands.*;
import org.github.luisera.splitlobby.data.DataManager;
import org.github.luisera.splitlobby.database.MySQLManager;
import org.github.luisera.splitlobby.listeners.*;
import org.github.luisera.splitlobby.managers.*;
import org.github.luisera.splitlobby.npc.NPCManager;
import org.github.luisera.splitlobby.redis.RedisManager;
import org.github.luisera.splitlobby.scoreboard.ScoreboardManager;
import org.github.luisera.splitlobby.selector.SelectorManager;
import org.github.luisera.splitlobby.tablist.TablistManager;
import org.github.luisera.splitlobby.utils.TagManager;

// IMPORTS CORRETOS DO REDESPLITCORE
import org.redesplit.github.offluisera.redesplitcore.RedeSplitCore;
import org.redesplit.github.offluisera.redesplitcore.api.PlaceholderAPI;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private static Main instance;

    // Gerenciadores
    private NPCManager npcManager;
    private MySQLManager mysqlManager;
    private DataManager dataManager;
    private RedisManager redisManager;
    private ScoreboardManager scoreboardManager;
    private TagManager tagManager;
    private TablistManager tablistManager;
    private File spawnFile;
    private FileConfiguration spawnConfig;
    private EasterEggManager easterEggManager;
    private ProfileManager profileManager;
    private SelectorManager selectorManager;

    // Managers de Funcionalidades
    private VisibilityManager visibilityManager;
    private LobbySecurityManager securityManager;
    private ParkourManager parkourManager;
    private DailyRewardsManager dailyRewardsManager;
    private Location lobbyLocation;
    private ChatGamesManager chatGamesManager;

    // INTEGRAÇÃO COM REDESPLITCORE
    private RedeSplitCore core;
    private PlaceholderAPI placeholderAPI;

    private LevelDisplayManager levelDisplayManager;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        createSpawnConfig();

        enviarMensagem("§e[SplitLobby] §fIniciando carregamento...");

        // === INTEGRAÇÃO COM REDESPLITCORE (PRIMEIRA COISA A FAZER) ===
        if (!setupCore()) {
            getLogger().severe("§c╔════════════════════════════════════════╗");
            getLogger().severe("§c║  RedeSplitCore não encontrado!        ║");
            getLogger().severe("§c║  O SplitLobby precisa do Core para    ║");
            getLogger().severe("§c║  funcionar. Desabilitando plugin...   ║");
            getLogger().severe("§c╚════════════════════════════════════════╝");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        enviarMensagem("§a╔════════════════════════════════════════╗");
        enviarMensagem("§a║  ✓ RedeSplitCore conectado!           ║");
        enviarMensagem("§a║  ✓ PlaceholderAPI carregada!          ║");
        enviarMensagem("§a╚════════════════════════════════════════╝");

        // 1. Conexões de Banco de Dados
        if (getConfig().getBoolean("mysql.enabled")) {
            try {
                this.mysqlManager = new MySQLManager(this);
                enviarMensagem("§a[MySQL] Conectado com sucesso!");
            } catch (Exception e) {
                enviarMensagem("§c[MySQL] Erro ao conectar.");
                e.printStackTrace();
            }
        }

        if (getConfig().getBoolean("redis.enabled")) {
            try {
                this.redisManager = new RedisManager(this);
                enviarMensagem("§a[Redis] Conectado com sucesso!");
            } catch (Exception e) {
                enviarMensagem("§c[Redis] Erro ao conectar.");
            }
        }

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // 2. Carregar Location do Spawn
        if (getSpawnConfig().contains("spawn-lobby")) {
            try {
                this.lobbyLocation = (Location) getSpawnConfig().get("spawn-lobby");
                enviarMensagem("§a[SplitLobby] Spawn carregado do spawn.yml!");
            } catch (Exception e) {
                enviarMensagem("§c[SplitLobby] Erro ao carregar spawn.yml");
            }
        }

        // Recarrega o NPC após restart (se existir)
        if (dailyRewardsManager != null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                dailyRewardsManager.reloadNPC();
            }, 40L); // 2 segundos após o servidor iniciar
        }

        // 3. Inicialização dos Managers
        this.dataManager = new DataManager();
        this.tagManager = new TagManager(this);
        this.parkourManager = new ParkourManager(this);
        this.visibilityManager = new VisibilityManager(this);
        this.securityManager = new LobbySecurityManager(this, this.parkourManager);
        this.dailyRewardsManager = new DailyRewardsManager(this);
        this.easterEggManager = new EasterEggManager(this);
        this.chatGamesManager = new ChatGamesManager(this);
        this.profileManager = new ProfileManager(this);
        this.selectorManager = new SelectorManager(this);
        this.levelDisplayManager = new LevelDisplayManager(this);
        this.npcManager = new NPCManager(this);


        enviarMensagem("§a[SplitLobby] Managers carregados!");

        // 4. REGISTRO DOS EVENTOS
        Bukkit.getPluginManager().registerEvents(this.easterEggManager, this);
        Bukkit.getPluginManager().registerEvents(this.profileManager, this);
        Bukkit.getPluginManager().registerEvents(this.selectorManager, this);
        Bukkit.getPluginManager().registerEvents(this.visibilityManager, this);
        Bukkit.getPluginManager().registerEvents(this.securityManager, this);
        Bukkit.getPluginManager().registerEvents(this.dailyRewardsManager, this);
        Bukkit.getPluginManager().registerEvents(this.chatGamesManager, this);
        Bukkit.getPluginManager().registerEvents(this.levelDisplayManager, this);
        enviarMensagem("§a[SplitLobby] Sistema de Nível Visual ativado!");


        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new DoubleJumpListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LaunchPadListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinLobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ParkourListener(this, this.parkourManager), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this), this);

        enviarMensagem("§a[SplitLobby] Eventos registrados!");

        // 5. Configurar Mundos
        setupWorlds();

        // 6. Tablist e Scoreboard (USA A PLACEHOLDERAPI DO CORE)
        this.tablistManager = new TablistManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        enviarMensagem("§a[SplitLobby] Scoreboard e Tablist iniciados!");

        // 7. Comandos
        getCommand("slscore").setExecutor(new ScoreboardCommand());
        getCommand("slsecret").setExecutor(new EasterEggCommand(this.easterEggManager));
        getCommand("sltab").setExecutor(new TablistCommand());
        getCommand("slset").setExecutor(new SetLobbyCommand(this));
        getCommand("perfil").setExecutor(new ProfileCommand(this.profileManager));
        getCommand("recompensas").setExecutor(new DailyRewardsCommand(this.dailyRewardsManager));
        getCommand("slparkour").setExecutor(new SetupParkourCommand(this.parkourManager));
        getCommand("slnpc").setExecutor(new NPCCommand(this));
        enviarMensagem("§a[SplitLobby] Comandos registrados!");

        verificarCompatibilidade();

        enviarMensagem("§a╔════════════════════════════════════════╗");
        enviarMensagem("§a║     SplitLobby v1.0 Carregado!        ║");
        enviarMensagem("§a║     Todas as funcionalidades OK       ║");
        enviarMensagem("§a╚════════════════════════════════════════╝");
    }

    private void setupWorlds() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setTime(6000);
            world.setGameRuleValue("doWeatherCycle", "false");
            world.setStorm(false);
            world.setThundering(false);
            world.setGameRuleValue("doMobSpawning", "false");
        }
    }

    public void createSpawnConfig() {
        spawnFile = new File(getDataFolder(), "spawn.yml");
        if (!spawnFile.exists()) {
            spawnFile.getParentFile().mkdirs();
            try {
                spawnFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
    }

    public FileConfiguration getSpawnConfig() {
        return this.spawnConfig;
    }

    public void saveSpawnConfig() {
        try {
            spawnConfig.save(spawnFile);
        } catch (IOException e) {
            enviarMensagem("§cErro ao salvar spawn.yml!");
            e.printStackTrace();
        }
    }

    public void giveLobbyItems(org.bukkit.entity.Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        if (getProfileManager() != null) {
            getProfileManager().giveProfileItem(player);
        }

        if (getSelectorManager() != null) {
            getSelectorManager().giveSelector(player);
        }

        if (getVisibilityManager() != null) {
            getVisibilityManager().giveItem(player);
        }

        if (levelDisplayManager != null) {
            levelDisplayManager.giveLevelItem(player);
        }

        player.updateInventory();
    }

    @Override
    public void onDisable() {
        enviarMensagem("§c[SplitLobby] Desligando...");
        if (mysqlManager != null) mysqlManager.desconectar();
        if (redisManager != null) redisManager.desconectar();
        enviarMensagem("§c[SplitLobby] Desligado!");
    }

    /**
     * Configura a integração com o RedeSplitCore
     * @return true se bem sucedido, false caso contrário
     */
    private boolean setupCore() {
        // Verifica se o plugin está instalado
        if (Bukkit.getPluginManager().getPlugin("RedeSplitCore") == null) {
            return false;
        }

        // Obtém a instância do Core
        core = RedeSplitCore.getInstance();

        if (core == null) {
            return false;
        }

        // Obtém a PlaceholderAPI do Core
        placeholderAPI = PlaceholderAPI.getInstance();

        // Retorna true apenas se tudo estiver OK
        return placeholderAPI != null;
    }

    // === GETTERS ===
    public static Main getInstance() {
        return instance;
    }

    public MySQLManager getMySQL() {
        return mysqlManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public RedisManager getRedis() {
        return redisManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TablistManager getTablistManager() {
        return tablistManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public ParkourManager getParkourManager() {
        return parkourManager;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public SelectorManager getSelectorManager() {
        return this.selectorManager;
    }

    public VisibilityManager getVisibilityManager() {
        return this.visibilityManager;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    // === GETTERS DA INTEGRAÇÃO COM REDESPLITCORE ===
    public RedeSplitCore getCore() {
        return core;
    }

    public LevelDisplayManager getLevelDisplayManager() {
        return levelDisplayManager;
    }

    /**
     * Retorna a PlaceholderAPI do RedeSplitCore
     * Use este método para acessar as placeholders do Core
     * @return PlaceholderAPI ou null se não estiver carregada
     */
    public PlaceholderAPI getPlaceholderAPI() {
        return placeholderAPI;
    }

    private void verificarCompatibilidade() {
        try {
            XMaterial.CLOCK.parseMaterial();
        } catch (Exception e) {
            enviarMensagem("§e[SplitLobby] Aviso: XSeries pode não estar funcionando corretamente");
        }
    }



    public void enviarMensagem(String msg) {
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}