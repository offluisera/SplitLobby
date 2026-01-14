package org.github.luisera.splitlobby.redis;

import org.bukkit.Bukkit;
import org.github.luisera.splitlobby.Main;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {

    private final Main plugin;
    private JedisPool pool;
    private RedisUpdateListener activeListener;
    private String channel;

    public RedisManager(Main plugin) {
        this.plugin = plugin;
        conectar();
    }

    private void conectar() {
        String host = plugin.getConfig().getString("redis.host");
        int port = plugin.getConfig().getInt("redis.port");
        String password = plugin.getConfig().getString("redis.password");
        this.channel = plugin.getConfig().getString("redis.channel");

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(8); // Número de conexões simultâneas

        // Se tiver senha, usa o construtor com senha, se não, usa o simples
        if (password != null && !password.isEmpty()) {
            this.pool = new JedisPool(config, host, port, 2000, password);
        } else {
            this.pool = new JedisPool(config, host, port, 2000);
        }

        plugin.getLogger().info("§a[Redis] Conectado e pronto.");

        // Inicia o Subscriber (Ouvinte) em uma Thread separada para não travar o servidor
        iniciarOuvinte();
    }

    private void iniciarOuvinte() {
        new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                activeListener = new RedisUpdateListener();
                // Esse método "subscribe" bloqueia a thread, por isso criamos uma new Thread acima
                jedis.subscribe(activeListener, channel);
            } catch (Exception e) {
                plugin.getLogger().severe("§c[Redis] Erro no servico de mensagens: " + e.getMessage());
            }
        }, "SplitLobby-RedisSubscriber").start();
    }

    // Método para ENVIAR mensagens para outros servidores
    public void publicMessage(String type, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = pool.getResource()) {
                String payload = type + ";" + message;
                jedis.publish(channel, payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void desconectar() {
        if (activeListener != null && activeListener.isSubscribed()) {
            activeListener.unsubscribe();
        }
        if (pool != null) {
            pool.close();
        }
        plugin.getLogger().info("§e[Redis] Conexao encerrada.");
    }
}