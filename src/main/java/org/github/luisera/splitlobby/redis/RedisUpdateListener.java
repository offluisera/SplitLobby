package org.github.luisera.splitlobby.redis;

import org.bukkit.Bukkit;
import org.github.luisera.splitlobby.Main;
import redis.clients.jedis.JedisPubSub;

public class RedisUpdateListener extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        // O Redis roda em uma thread separada.
        // Se formos mexer com Bukkit (Players, Chat), precisamos voltar para a Main Thread.

        // Exemplo de protocolo simples: "TIPO;DADO"
        // Ex: "ALERT;O servidor vai reiniciar"

        String[] parts = message.split(";", 2);
        String type = parts[0];
        String content = parts.length > 1 ? parts[1] : "";

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            switch (type) {
                case "ALERT":
                    // Envia mensagem para todos no Lobby
                    Bukkit.broadcastMessage("§c§l[REDE] §f" + content);
                    break;

                case "KICK_ALL":
                    // Exemplo: Comando de manutenção vindo de outro lugar
                    Bukkit.getOnlinePlayers().forEach(p -> p.kickPlayer("§cManutencao Global!"));
                    break;

                // Aqui futuramente entrará: "UPDATE_RANK", "SEND_PLAYER", etc.
            }
        });
    }
}