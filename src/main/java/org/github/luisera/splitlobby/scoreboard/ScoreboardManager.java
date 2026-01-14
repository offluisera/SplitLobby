package org.github.luisera.splitlobby.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.utils.CustomConfig;

// IMPORT CORRETO: PlaceholderAPI vem do RedeSplitCore
import org.redesplit.github.offluisera.redesplitcore.api.PlaceholderAPI;

import java.util.List;

public class ScoreboardManager {

    private final Main plugin;
    private CustomConfig configFile;
    private BukkitRunnable task;
    private PlaceholderAPI placeholderAPI;

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new CustomConfig(plugin, "scoreboard.yml");

        // Obtém a PlaceholderAPI do Core através do Main
        this.placeholderAPI = plugin.getPlaceholderAPI();

        if (this.placeholderAPI == null) {
            plugin.enviarMensagem("§c[ScoreboardManager] AVISO: PlaceholderAPI não disponível!");
        } else {
            plugin.enviarMensagem("§a[ScoreboardManager] PlaceholderAPI conectada!");
        }

        iniciarTask();
    }

    public void reload() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception e) {
                // Ignora se já estiver cancelada
            }
        }

        configFile.reload();
        iniciarTask();
        plugin.enviarMensagem("§a[ScoreboardManager] Recarregado!");
    }

    private void iniciarTask() {
        if (!configFile.getConfig().getBoolean("settings.enabled")) {
            plugin.enviarMensagem("§e[ScoreboardManager] Scoreboard desabilitada no config");
            return;
        }

        int interval = configFile.getConfig().getInt("settings.update-interval", 20);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    atualizarScoreboard(player);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, interval);
    }

    private void atualizarScoreboard(Player p) {
        if (!configFile.getConfig().getBoolean("settings.enabled")) return;

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("SplitLobby", "dummy");

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(cor(configFile.getConfig().getString("settings.title")));

        List<String> linhas = configFile.getConfig().getStringList("lines");
        int score = linhas.size();

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);

            // === APLICAR PLACEHOLDERS DO REDESPLITCORE ===
            // Verifica se a PlaceholderAPI está disponível
            if (placeholderAPI != null) {
                linha = placeholderAPI.replace(p, linha);
            }

            // === PLACEHOLDERS LOCAIS DO LOBBY ===
            linha = linha.replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("%player%", p.getName());

            // TRUQUE DO ESPAÇAMENTO - Linhas vazias únicas
            if (linha.trim().isEmpty() || linha.equals("&7") || linha.equals("&r")) {
                linha = ChatColor.values()[i % ChatColor.values().length].toString() + "§r";
            } else {
                linha = cor(linha);
            }

            // Corta textos muito grandes (limite de 40 chars na 1.8)
            if (linha.length() > 40) {
                linha = linha.substring(0, 40);
            }

            obj.getScore(linha).setScore(score);
            score--;
        }

        p.setScoreboard(sb);
    }

    private String cor(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}