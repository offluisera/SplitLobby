package org.github.luisera.splitlobby.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.github.luisera.splitlobby.Main;
// Certifique-se de que o import do ParkourManager está correto
import org.github.luisera.splitlobby.managers.ParkourManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbySecurityManager implements Listener {

    private final Main plugin;
    private final ParkourManager parkourManager;
    private final Map<UUID, String> sessionHashes = new HashMap<>();
    private final SimpleDateFormat dateFormat;

    public LobbySecurityManager(Main plugin, ParkourManager parkourManager) {
        this.plugin = plugin;
        this.parkourManager = parkourManager;
        this.dateFormat = new SimpleDateFormat("dd/MM HH:mm");

        startSecurityTask();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String rawUUID = event.getPlayer().getUniqueId().toString().replace("-", "");
        String secureHash = "SHA256-" + rawUUID.substring(0, 8).toUpperCase();
        sessionHashes.put(event.getPlayer().getUniqueId(), secureHash);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessionHashes.remove(event.getPlayer().getUniqueId());
    }

    private void startSecurityTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String currentDate = dateFormat.format(new Date());

                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (parkourManager != null && parkourManager.isPlaying(player)) {
                        String timer = parkourManager.getTimer(player);
                        // O timer do parkour pode continuar verde e chamativo
                        sendActionBar(player, "§a§lTEMPO: §f" + timer);
                    }
                    else {
                        String hash = sessionHashes.get(player.getUniqueId());
                        if (hash == null) continue;

                        String message = "§e" + hash + " §e| §e" + currentDate;

                        sendActionBar(player, message);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    // --- SISTEMA DE ACTION BAR (Mantido igual) ---
    private void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;

        try {
            String nmsVersion = Bukkit.getServer().getClass().getPackage().getName();
            nmsVersion = nmsVersion.substring(nmsVersion.lastIndexOf(".") + 1);

            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);

            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");
            Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");

            Class<?> chatSerializerClass;
            if (nmsVersion.equals("v1_8_R1")) {
                chatSerializerClass = Class.forName("net.minecraft.server." + nmsVersion + ".ChatSerializer");
            } else {
                chatSerializerClass = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent$ChatSerializer");
            }

            Method aMethod = chatSerializerClass.getMethod("a", String.class);
            Object chatComponent = aMethod.invoke(null, "{\"text\":\"" + message + "\"}");

            Object packet;
            if (nmsVersion.startsWith("v1_8") || nmsVersion.startsWith("v1_9") || nmsVersion.startsWith("v1_10") || nmsVersion.startsWith("v1_11")) {
                packet = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class).newInstance(chatComponent, (byte) 2);
            } else {
                Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + nmsVersion + ".ChatMessageType");
                Object chatMessageType = Enum.valueOf((Class<Enum>) chatMessageTypeClass, "GAME_INFO");
                packet = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, chatMessageTypeClass, UUID.class)
                        .newInstance(chatComponent, chatMessageType, player.getUniqueId());
            }

            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(craftPlayer);
            Field playerConnectionField = entityPlayer.getClass().getField("playerConnection");
            Object playerConnection = playerConnectionField.get(entityPlayer);
            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + nmsVersion + ".Packet"));
            sendPacket.invoke(playerConnection, packet);

        } catch (Exception e) {
            // Ignorar erros silenciosamente
        }
    }
}