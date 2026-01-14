package org.github.luisera.splitlobby.listeners;

import com.cryptomorin.xseries.XSound;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.utils.CustomConfig;
import org.redesplit.github.offluisera.redesplitcore.api.PlaceholderAPI;

public class ChatListener implements Listener {

    private final Main plugin;
    private final CustomConfig chatConfig;
    private final PlaceholderAPI placeholderAPI;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
        this.chatConfig = new CustomConfig(plugin, "chat.yml");

        // Obtém a PlaceholderAPI do RedeSplitCore
        this.placeholderAPI = plugin.getPlaceholderAPI();

        if (this.placeholderAPI == null) {
            plugin.enviarMensagem("§c[ChatListener] AVISO: PlaceholderAPI não disponível!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.isCancelled()) return;
        if (!chatConfig.getConfig().getBoolean("chat.enabled")) return;

        Player p = e.getPlayer();

        // 1. Pega o formato do chat do arquivo
        String format = chatConfig.getConfig().getString("chat.format", "%splitcore_rank%%player%: &f%mensagem%");

        // 2. Aplica TODAS as placeholders do RedeSplitCore
        if (placeholderAPI != null) {
            format = placeholderAPI.replace(p, format);
        }

        // 3. Aplica cores na mensagem se tiver permissão
        String mensagem = e.getMessage();
        if (p.hasPermission("splitlobby.chat.color")) {
            mensagem = ChatColor.translateAlternateColorCodes('&', mensagem);
        }

        // 4. Substitui a placeholder da mensagem
        format = format.replace("%mensagem%", mensagem);
        format = format.replace("%message%", mensagem); // Alias

        // 5. Aplica cores finais
        format = ChatColor.translateAlternateColorCodes('&', format);

        // 6. IMPORTANTE: Escapar caracteres especiais do String.format()
        // O setFormat() usa String.format() internamente, então precisamos escapar % que sobrou
        format = format.replace("%", "%%");

        // 7. Define o formato final
        // Usamos %1$s para o nome (não usado) e %2$s para a mensagem (não usado)
        // porque já montamos tudo manualmente
        e.setFormat(format);

        // 8. Sistema de som no chat
        if (chatConfig.getConfig().getBoolean("chat.sound-enabled", false)) {
            String soundName = chatConfig.getConfig().getString("chat.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");

            if (soundName != null && !soundName.isEmpty()) {
                try {
                    XSound.matchXSound(soundName).ifPresent(sound -> {
                        // Toca o som para todos os receptores
                        for (Player recipient : e.getRecipients()) {
                            sound.play(recipient, 0.5f, 1.0f);
                        }
                    });
                } catch (Exception ex) {
                    // Ignora erros de som inválido
                }
            }
        }
    }
}