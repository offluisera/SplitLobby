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
        plugin.enviarMensagem("§a[TablistManager] Tablist iniciada com sucesso!");

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
            plugin.getLogger().warning("§c[TablistManager] Erro ao enviar tablist para " + p.getName());
        }
    }

    private void sendPacketNMS(Player p, String header, String footer) throws Exception {
        // Cria os componentes de chat (IChatBaseComponent)
        Class<?> chatComponentClass = getNMSClass("IChatBaseComponent");
        Class<?> chatSerializerClass = chatComponentClass.getDeclaredClasses()[0];

        Method serializeMethod = chatSerializerClass.getMethod("a", String.class);

        // Converte as strings para JSON e depois para IChatBaseComponent
        Object headerComponent = serializeMethod.invoke(null,
                "{\"text\":\"" + header.replace("\"", "\\\"").replace("\n", "\\n") + "\"}");

        Object footerComponent = serializeMethod.invoke(null,
                "{\"text\":\"" + footer.replace("\"", "\\\"").replace("\n", "\\n") + "\"}");

        // No 1.12, o construtor é VAZIO - precisa setar os campos depois
        Class<?> packetClass = getNMSClass("PacketPlayOutPlayerListHeaderFooter");
        Object packet = packetClass.getConstructor().newInstance(); // Construtor vazio!

        // Seta o header (campo "a")
        Field headerField = packetClass.getDeclaredField("a");
        headerField.setAccessible(true);
        headerField.set(packet, headerComponent);

        // Seta o footer (campo "b")
        Field footerField = packetClass.getDeclaredField("b");
        footerField.setAccessible(true);
        footerField.set(packet, footerComponent);

        // Envia o packet
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