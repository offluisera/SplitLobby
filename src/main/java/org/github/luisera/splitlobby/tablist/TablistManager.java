package org.github.luisera.splitlobby.tablist;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.utils.CustomConfig;

// IMPORT CORRETO: PlaceholderAPI vem do RedeSplitCore
import org.redesplit.github.offluisera.redesplitcore.api.PlaceholderAPI;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class TablistManager {

    private final Main plugin;
    private CustomConfig configFile;
    private BukkitRunnable task;
    private PlaceholderAPI placeholderAPI;

    public TablistManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new CustomConfig(plugin, "tablist.yml");

        // Obtém a PlaceholderAPI do Core através do Main
        this.placeholderAPI = plugin.getPlaceholderAPI();

        if (this.placeholderAPI == null) {
            plugin.enviarMensagem("§c[TablistManager] AVISO: PlaceholderAPI não disponível!");
        } else {
            plugin.enviarMensagem("§a[TablistManager] PlaceholderAPI conectada!");
        }

        iniciarTask();
    }

    public void reload() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
        configFile.reload();
        iniciarTask();
        plugin.enviarMensagem("§a[TablistManager] Recarregado!");
    }

    private void iniciarTask() {
        if (!configFile.getConfig().getBoolean("settings.enabled")) {
            plugin.enviarMensagem("§e[TablistManager] Tablist desabilitada no config");
            return;
        }

        int interval = configFile.getConfig().getInt("settings.update-interval", 20);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) return;

                List<String> headerList = configFile.getConfig().getStringList("header");
                List<String> footerList = configFile.getConfig().getStringList("footer");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String header = montarTexto(p, headerList);
                    String footer = montarTexto(p, footerList);

                    enviarTablist(p, header, footer);
                }
            }
        };
        task.runTaskTimer(plugin, 20L, interval);
    }

    private String montarTexto(Player p, List<String> linhas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);

            // === APLICAR PLACEHOLDERS DO REDESPLITCORE ===
            if (placeholderAPI != null) {
                linha = placeholderAPI.replace(p, linha);
            }

            // === PLACEHOLDERS LOCAIS DO LOBBY ===
            linha = linha.replace("%ping%", String.valueOf(getPing(p)));
            linha = linha.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
            linha = linha.replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size()));
            linha = linha.replace("%player%", p.getName());

            sb.append(ChatColor.translateAlternateColorCodes('&', linha));

            if (i < linhas.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void enviarTablist(Player p, String header, String footer) {
        // 1. Tenta usar o método oficial da API (1.13+)
        try {
            Method method = p.getClass().getMethod("setPlayerListHeaderFooter", String.class, String.class);
            method.invoke(p, header, footer);
            return;
        } catch (Exception ignored) {
            // Continua para método legado
        }

        // 2. Método Legado (NMS) para 1.8-1.12
        try {
            sendPacketNMS(p, header, footer);
        } catch (Exception e) {
            // Ignora erros
        }
    }

    private void sendPacketNMS(Player p, String header, String footer) throws Exception {
        Object headerJson = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0]
                .getMethod("a", String.class)
                .invoke(null, "{\"text\":\"" + header.replace("\n", "\\n") + "\"}");

        Object footerJson = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0]
                .getMethod("a", String.class)
                .invoke(null, "{\"text\":\"" + footer.replace("\n", "\\n") + "\"}");

        Object packet = getNMSClass("PacketPlayOutPlayerListHeaderFooter")
                .getConstructor(getNMSClass("IChatBaseComponent"))
                .newInstance(headerJson);

        Field footerField = packet.getClass().getDeclaredField("b");
        footerField.setAccessible(true);
        footerField.set(packet, footerJson);

        Object entityPlayer = p.getClass().getMethod("getHandle").invoke(p);
        Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
        playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet"))
                .invoke(playerConnection, packet);
    }

    private Class<?> getNMSClass(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private int getPing(Player p) {
        try {
            Object entityPlayer = p.getClass().getMethod("getHandle").invoke(p);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            return 0;
        }
    }
}