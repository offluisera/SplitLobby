package org.github.luisera.splitlobby.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.github.luisera.splitlobby.Main;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

public class ChatGamesManager implements Listener {

    private final Main plugin;
    private File file;
    private FileConfiguration config;
    private final Random random = new Random();

    private String currentAnswer = null;
    private long startTime;
    private boolean gameRunning = false;

    public ChatGamesManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
        startGameLoop();
    }

    public void loadConfig() {
        file = new File(plugin.getDataFolder(), "chatgames.yml");
        if (!file.exists()) {
            plugin.saveResource("chatgames.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void startGameLoop() {
        int interval = config.getInt("config.intervalo_segundos", 300) * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() < 1) return;
                if (gameRunning) return;

                startRandomGame();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void startRandomGame() {
        gameRunning = true;
        startTime = System.currentTimeMillis();

        String header = config.getString("mensagens.header", "&b&lJOGOS").replace("&", "§");
        String rewardTxt = config.getString("config.premio_visual", "100 Coins");
        String rewardLine = config.getString("mensagens.formato_recompensa", "&eRecompensa: &f%premio%")
                .replace("&", "§")
                .replace("%premio%", rewardTxt);

        String questionLine;

        if (random.nextBoolean()) {
            // MATEMÁTICA
            int min = config.getInt("matematica.min", 10);
            int max = config.getInt("matematica.max", 100);
            int n1 = random.nextInt(max - min) + min;
            int n2 = random.nextInt(max - min) + min;

            boolean subtract = config.getBoolean("matematica.diminuir") && random.nextBoolean();
            String operationStr;

            if (subtract) {
                if (n2 > n1) { int temp = n1; n1 = n2; n2 = temp; }
                currentAnswer = String.valueOf(n1 - n2);
                operationStr = n1 + " - " + n2;
            } else {
                currentAnswer = String.valueOf(n1 + n2);
                operationStr = n1 + " + " + n2;
            }

            questionLine = config.getString("mensagens.formato_matematica", "&eMatemática: Quanto é %conta%?")
                    .replace("&", "§")
                    .replace("%conta%", operationStr);

        } else {
            // PALAVRA
            List<String> words = config.getStringList("palavras");
            if (words.isEmpty()) { gameRunning = false; return; }

            String original = words.get(random.nextInt(words.size()));
            currentAnswer = original;
            String scrambled = scramble(original);

            questionLine = config.getString("mensagens.formato_palavra", "&eDesembaralhe: Qual é a palavra %palavra%?")
                    .replace("&", "§")
                    .replace("%palavra%", scrambled);
        }

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(header);
        Bukkit.broadcastMessage(questionLine);
        Bukkit.broadcastMessage(rewardLine);
        Bukkit.broadcastMessage(" ");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameRunning && currentAnswer != null) {
                    String msg = config.getString("mensagens.ninguem_acertou", "&cTempo esgotado!")
                            .replace("&", "§")
                            .replace("%resposta%", currentAnswer);
                    Bukkit.broadcastMessage(header + " " + msg);
                    stopGame();
                }
            }
        }.runTaskLater(plugin, 600L);
    }

    private void stopGame() {
        gameRunning = false;
        currentAnswer = null;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!gameRunning || currentAnswer == null) return;

        if (event.getMessage().equalsIgnoreCase(currentAnswer)) {
            event.setCancelled(true);

            Player winner = event.getPlayer();
            double timeTaken = (System.currentTimeMillis() - startTime) / 1000.0;
            String timeStr = new DecimalFormat("#.##").format(timeTaken);

            // Mensagem de Vitória
            String header = config.getString("mensagens.header", "&b&lJOGOS").replace("&", "§");
            String msgWin = config.getString("mensagens.venceu")
                    .replace("&", "§")
                    .replace("%player%", winner.getName())
                    .replace("%resposta%", currentAnswer)
                    .replace("%tempo%", timeStr);

            Bukkit.broadcastMessage(header + " " + msgWin);

            String prizeMsg = config.getString("mensagens.venceu_premio").replace("&", "§");
            winner.sendMessage(prizeMsg);

            // --- NOVO: Salvar no Banco de Dados ---
            plugin.getMySQL().addChatGameWin(winner.getUniqueId(), winner.getName());
            // --------------------------------------

            // Dar prêmios
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String cmd : config.getStringList("premios")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", winner.getName()));
                }
            });

            stopGame();
        }
    }

    private String scramble(String word) {
        List<Character> chars = new java.util.ArrayList<>();
        for (char c : word.toCharArray()) chars.add(c);
        java.util.Collections.shuffle(chars);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }
}