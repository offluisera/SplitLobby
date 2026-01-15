package org.github.luisera.splitlobby.listeners;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.PacketPlayInUseEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;
import org.github.luisera.splitlobby.npc.NPCData;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PacketNPCListener {

    private final Main plugin;
    private Field actionField;
    private Field entityIdField;

    // Cooldown para evitar execução duplicada (player UUID -> último clique)
    private final Map<UUID, Long> clickCooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 500; // 500ms = 0.5 segundos

    public PacketNPCListener(Main plugin) {
        this.plugin = plugin;

        try {
            // Acessa os campos privados do packet
            actionField = PacketPlayInUseEntity.class.getDeclaredField("action");
            actionField.setAccessible(true);

            entityIdField = PacketPlayInUseEntity.class.getDeclaredField("a");
            entityIdField.setAccessible(true);
        } catch (Exception e) {
            plugin.getLogger().severe("§c[PACKET] Erro ao inicializar reflection:");
            e.printStackTrace();
        }
    }

    /**
     * Injeta o packet listener no jogador
     */
    public void injectPlayer(Player player) {
        ChannelDuplexHandler handler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
                // Detecta packet de "usar entidade" (clique)
                if (packet instanceof PacketPlayInUseEntity) {
                    handlePacket((PacketPlayInUseEntity) packet, player);
                }
                super.channelRead(ctx, packet);
            }
        };

        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();

        // Remove handler antigo se existir
        if (pipeline.get("npc_packet_listener") != null) {
            pipeline.remove("npc_packet_listener");
        }

        // Adiciona o novo handler
        pipeline.addBefore("packet_handler", "npc_packet_listener", handler);

        plugin.getLogger().info("§a[PACKET] Listener injetado em: " + player.getName());
    }

    /**
     * Remove o packet listener do jogador
     */
    public void uninjectPlayer(Player player) {
        try {
            Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
            channel.eventLoop().submit(() -> {
                if (channel.pipeline().get("npc_packet_listener") != null) {
                    channel.pipeline().remove("npc_packet_listener");
                }
                return null;
            });

            // Remove do cooldown
            clickCooldown.remove(player.getUniqueId());
        } catch (Exception e) {
            // Ignora erro (jogador já desconectou)
        }
    }

    /**
     * Processa o packet de clique
     */
    private void handlePacket(PacketPlayInUseEntity packet, Player player) {
        try {
            // ★ COOLDOWN: Evita execução duplicada
            UUID playerUUID = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastClick = clickCooldown.get(playerUUID);

            if (lastClick != null && (now - lastClick) < COOLDOWN_MS) {
                plugin.getLogger().info("§c[PACKET] Cooldown ativo - clique ignorado");
                return;
            }

            // Pega a ação (INTERACT, ATTACK, INTERACT_AT)
            PacketPlayInUseEntity.EnumEntityUseAction action =
                    (PacketPlayInUseEntity.EnumEntityUseAction) actionField.get(packet);

            // Pega o ID da entidade clicada
            int entityId = entityIdField.getInt(packet);

            plugin.getLogger().info("§e[PACKET] ========== PACKET DETECTADO ==========");
            plugin.getLogger().info("§e[PACKET] Player: " + player.getName());
            plugin.getLogger().info("§e[PACKET] Action: " + action);
            plugin.getLogger().info("§e[PACKET] Entity ID: " + entityId);
            plugin.getLogger().info("§e[PACKET] Item na mão: " +
                    (player.getItemInHand() != null && player.getItemInHand().getType() != org.bukkit.Material.AIR
                            ? player.getItemInHand().getType() : "VAZIO/SEM ITEM"));

            // Só processa INTERACT (clique direito)
            if (action != PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
                plugin.getLogger().info("§c[PACKET] Ação ignorada (não é INTERACT)");
                return;
            }

            // Busca o NPC pelo Entity ID
            for (NPCData npc : plugin.getNPCManager().getAllNPCs()) {
                EntityPlayer npcEntity = plugin.getNPCManager().getEntity(npc.getId());

                if (npcEntity != null) {
                    plugin.getLogger().info("§7[PACKET] Comparando com NPC #" + npc.getId() +
                            " (Entity ID: " + npcEntity.getId() + ")");

                    if (npcEntity.getId() == entityId) {
                        plugin.getLogger().info("§a[PACKET] ★★★ NPC ENCONTRADO! ★★★");
                        plugin.getLogger().info("§a[PACKET] NPC #" + npc.getId() + " - " + npc.getName());

                        // ★ ATUALIZA O COOLDOWN
                        clickCooldown.put(playerUUID, now);

                        // Executa na thread principal do servidor
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getNPCManager().lookAt(npc.getId(), player);
                            plugin.getNPCManager().executeCommand(npc.getId(), player);
                        });

                        plugin.getLogger().info("§e[PACKET] ==========================================");
                        return;
                    }
                }
            }

            plugin.getLogger().info("§c[PACKET] NPC não encontrado");
            plugin.getLogger().info("§e[PACKET] ==========================================");

        } catch (Exception e) {
            plugin.getLogger().severe("§c[PACKET] Erro ao processar packet:");
            e.printStackTrace();
        }
    }
}