package org.github.luisera.splitlobby.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.data.PlayerData;

import java.util.Set;

public class TagManager {

    private final Main plugin;
    private final CustomConfig chatConfig;

    public TagManager(Main plugin) {
        this.plugin = plugin;
        this.chatConfig = new CustomConfig(plugin, "chat.yml");
    }

    // 1. Descobre o prefixo do jogador baseado na config
    public String getPrefix(Player p) {
        ConfigurationSection tags = chatConfig.getConfig().getConfigurationSection("tags");
        if (tags == null) return "";

        // Loopa todas as tags na ordem do arquivo
        Set<String> keys = tags.getKeys(false);
        for (String key : keys) {
            String permission = tags.getString(key + ".permission");

            // Se a permissão for "default" ou o jogador tiver a permissão
            if (permission.equalsIgnoreCase("default") || p.hasPermission(permission)) {
                return ChatColor.translateAlternateColorCodes('&', tags.getString(key + ".prefix"));
            }
        }

        return ""; // Se não achar nada
    }

    // 2. Método central para substituir placeholders (Engine Própria)
    public String aplicarPlaceholders(Player p, String texto) {
        // Dados básicos
        texto = texto.replace("%nick%", p.getName());
        texto = texto.replace("%displayname%", p.getDisplayName());

        // Nossa Tag Própria
        texto = texto.replace("%tag%", getPrefix(p));

        // Dados do nosso Banco de Dados (MySQL)
        PlayerData data = Main.getInstance().getDataManager().getPlayer(p);
        if (data != null) {
            texto = texto.replace("%coins%", String.valueOf((int) data.getCoins()));
        } else {
            texto = texto.replace("%coins%", "0");
        }

        // Você pode adicionar mais aqui futuramente:
        // texto = texto.replace("%level%", String.valueOf(p.getLevel()));

        return ChatColor.translateAlternateColorCodes('&', texto);
    }

    public void reload() {
        chatConfig.reload();
    }
}