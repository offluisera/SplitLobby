package org.github.luisera.splitlobby.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;

import java.util.*;

public class NPCManager {

    private final Main plugin;
    private final Map<Integer, NPCData> npcs = new HashMap<>();
    private final Map<Integer, EntityPlayer> entities = new HashMap<>();
    private final HologramManager hologramManager = new HologramManager();
    private int nextId = 1;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        loadNPCs();
    }

    /**
     * Cria um novo NPC
     */
    public NPCData createNPC(String name, Location location) {
        UUID uuid = UUID.randomUUID();
        NPCData npc = new NPCData(nextId++, uuid, name, location);

        npcs.put(npc.getId(), npc);

        plugin.getLogger().info("§e[NPC] Criando NPC #" + npc.getId() + " - Nome: " + name);

        spawnNPC(npc);
        saveNPC(npc);

        return npc;
    }

    /**
     * Spawna o NPC no mundo
     */
    public void spawnNPC(NPCData npc) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) npc.getLocation().getWorld()).getHandle();

        // Cria GameProfile com UUID único e nome vazio (para não aparecer na tablist)
        GameProfile profile = new GameProfile(npc.getUuid(), "");

        // Aplica skin se tiver
        if (npc.getSkinTexture() != null && npc.getSkinSignature() != null) {
            profile.getProperties().put("textures",
                    new Property("textures", npc.getSkinTexture(), npc.getSkinSignature()));
        }

        // Cria EntityPlayer (NPC)
        EntityPlayer entityPlayer = new EntityPlayer(server, world, profile, new PlayerInteractManager(world));

        // Define posição
        Location loc = npc.getLocation();
        entityPlayer.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        // Salva entidade
        entities.put(npc.getId(), entityPlayer);

        // Cria hologramas - APENAS DESCRIÇÃO (sem nome duplicado)
        List<String> holoLines = new ArrayList<>();

        // Adiciona a descrição (se tiver)
        if (npc.getDescription() != null && !npc.getDescription().isEmpty()) {
            holoLines.addAll(npc.getDescription());
        }

        // Se não tiver descrição, adiciona só o nome
        if (holoLines.isEmpty()) {
            holoLines.add(npc.getName());
        }

        hologramManager.createHologram(npc.getId(), loc.clone(), holoLines);

        // Mostra para todos os jogadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            showNPC(player, npc.getId());
        }
    }

    /**
     * Mostra o NPC para um jogador específico
     */
    public void showNPC(Player player, int npcId) {
        EntityPlayer npcEntity = entities.get(npcId);
        if (npcEntity == null) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        // Packet de adicionar na tablist (NECESSÁRIO para spawnar)
        connection.sendPacket(new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npcEntity));

        // Packet de spawn do player
        connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npcEntity));

        // Packet de rotação da cabeça
        connection.sendPacket(new PacketPlayOutEntityHeadRotation(npcEntity,
                (byte) (npcEntity.yaw * 256 / 360)));

        // Remove da tablist após alguns ticks (para não aparecer na lista)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerConnection conn = ((CraftPlayer) player).getHandle().playerConnection;
            if (conn != null) {
                conn.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npcEntity));
            }
        }, 5L); // 5 ticks = 0.25 segundos

        // Mostra holograma
        hologramManager.showHologram(player, npcId);
    }

    /**
     * Remove o NPC para um jogador
     */
    public void hideNPC(Player player, int npcId) {
        EntityPlayer npcEntity = entities.get(npcId);
        if (npcEntity == null) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        // Remove o NPC
        connection.sendPacket(new PacketPlayOutEntityDestroy(npcEntity.getId()));

        // Remove da tablist
        connection.sendPacket(new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npcEntity));

        // Esconde holograma
        hologramManager.hideHologram(player, npcId);
    }

    /**
     * Deleta um NPC permanentemente
     */
    public void deleteNPC(int npcId) {
        NPCData npc = npcs.get(npcId);
        if (npc == null) return;

        // Remove hologramas para todos primeiro
        hologramManager.removeHologramForAll(npcId);

        // Remove para todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideNPC(player, npcId);
        }

        // Remove hologramas completamente
        hologramManager.removeHologram(npcId);

        // Remove do banco
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (java.sql.Connection conn = plugin.getMySQL().getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM rs_lobby_npcs WHERE id = ?"
                );
                ps.setInt(1, npcId);
                ps.executeUpdate();

                plugin.getLogger().info("§a[NPC] NPC #" + npcId + " deletado do banco de dados!");
            } catch (Exception e) {
                plugin.getLogger().severe("§c[NPC] Erro ao deletar NPC #" + npcId + ":");
                e.printStackTrace();
            }
        });

        npcs.remove(npcId);
        entities.remove(npcId);

        plugin.getLogger().info("§a[NPC] NPC #" + npcId + " removido completamente!");
    }

    /**
     * Define a skin do NPC buscando da API do Mojang
     */
    public void setSkin(int npcId, String skinName) {
        NPCData npc = npcs.get(npcId);
        if (npc == null) return;

        // Busca skin da API
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String[] skinData = SkinFetcher.fetchSkin(skinName);

                if (skinData != null) {
                    npc.setSkinName(skinName);
                    npc.setSkinTexture(skinData[0]);
                    npc.setSkinSignature(skinData[1]);

                    // Atualiza no banco
                    saveNPC(npc);

                    // Respawna o NPC com a nova skin
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Remove o antigo
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            hideNPC(player, npcId);
                        }
                        entities.remove(npcId);

                        // Spawna com nova skin
                        spawnNPC(npc);
                    });

                    plugin.getLogger().info("§a[NPC] Skin aplicada: " + skinName + " no NPC #" + npcId);
                } else {
                    plugin.getLogger().warning("§c[NPC] Falha ao buscar skin: " + skinName);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("§c[NPC] Erro ao aplicar skin: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Define o comando do NPC
     */
    public void setCommand(int npcId, String command) {
        NPCData npc = npcs.get(npcId);
        if (npc == null) return;

        npc.setCommand(command);
        saveNPC(npc);
    }

    /**
     * Define a descrição do NPC (holograma)
     */
    public void setDescription(int npcId, List<String> description) {
        NPCData npc = npcs.get(npcId);
        if (npc == null) return;

        npc.setDescription(description);
        saveNPC(npc);

        // Respawna o NPC para atualizar o holograma (NOME + DESCRIÇÃO)
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideNPC(player, npcId);
        }
        entities.remove(npcId);
        hologramManager.removeHologram(npcId);
        spawnNPC(npc);
    }

    /**
     * Executa o comando do NPC
     */
    public void executeCommand(int npcId, Player player) {
        plugin.getLogger().info("§e[CMD DEBUG] ========== EXECUTANDO COMANDO ==========");
        plugin.getLogger().info("§e[CMD DEBUG] NPC ID: " + npcId);
        plugin.getLogger().info("§e[CMD DEBUG] Player: " + player.getName());

        NPCData npc = npcs.get(npcId);

        if (npc == null) {
            plugin.getLogger().warning("§c[CMD DEBUG] NPC #" + npcId + " é NULL!");
            return;
        }

        plugin.getLogger().info("§e[CMD DEBUG] NPC encontrado: " + npc.getName());

        if (npc.getCommand() == null) {
            plugin.getLogger().warning("§c[CMD DEBUG] Comando é NULL!");
            return;
        }

        if (npc.getCommand().isEmpty()) {
            plugin.getLogger().warning("§c[CMD DEBUG] Comando está VAZIO!");
            return;
        }

        plugin.getLogger().info("§a[CMD DEBUG] Comando encontrado: '" + npc.getCommand() + "'");

        String command = npc.getCommand()
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString());

        plugin.getLogger().info("§a[CMD DEBUG] Após replacements: '" + command + "'");

        // Remove '/' do início se tiver
        if (command.startsWith("/")) {
            command = command.substring(1);
            plugin.getLogger().info("§a[CMD DEBUG] Removeu /: '" + command + "'");
        }

        // Se o comando começa com [CONSOLE], executa como console
        if (command.startsWith("[CONSOLE]")) {
            String cmd = command.replace("[CONSOLE]", "").trim();
            plugin.getLogger().info("§a[CMD DEBUG] ★ Executando como CONSOLE: '" + cmd + "'");

            boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            plugin.getLogger().info("§a[CMD DEBUG] Resultado: " + (result ? "SUCESSO" : "FALHOU"));
        } else {
            // Executa como jogador
            plugin.getLogger().info("§a[CMD DEBUG] ★ Executando como JOGADOR: '" + command + "'");

            boolean result = Bukkit.dispatchCommand(player, command);
            plugin.getLogger().info("§a[CMD DEBUG] Resultado: " + (result ? "SUCESSO" : "FALHOU"));
        }

        plugin.getLogger().info("§e[CMD DEBUG] ==========================================");
    }

    /**
     * Faz o NPC olhar para um jogador
     */
    public void lookAt(int npcId, Player player) {
        EntityPlayer npcEntity = entities.get(npcId);
        NPCData npc = npcs.get(npcId);
        if (npcEntity == null || npc == null) return;

        Location npcLoc = npc.getLocation();
        Location playerLoc = player.getLocation();

        // Calcula yaw e pitch
        double dx = playerLoc.getX() - npcLoc.getX();
        double dz = playerLoc.getZ() - npcLoc.getZ();
        double dy = playerLoc.getY() + player.getEyeHeight() - (npcLoc.getY() + 1.62);

        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan(-dy / distance));

        // Atualiza rotação do NPC
        npcEntity.yaw = yaw;
        npcEntity.pitch = pitch;

        // Envia packets de rotação
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        connection.sendPacket(new PacketPlayOutEntityHeadRotation(npcEntity,
                (byte) (yaw * 256 / 360)));

        connection.sendPacket(new PacketPlayOutEntity.PacketPlayOutEntityLook(
                npcEntity.getId(),
                (byte) (yaw * 256 / 360),
                (byte) (pitch * 256 / 360),
                true
        ));
    }

    /**
     * Salva NPC no banco de dados
     */
    private void saveNPC(NPCData npc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (java.sql.Connection conn = plugin.getMySQL().getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rs_lobby_npcs (id, uuid, name, skin_name, skin_texture, skin_signature, " +
                                "command, description, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE name=VALUES(name), skin_name=VALUES(skin_name), " +
                                "skin_texture=VALUES(skin_texture), skin_signature=VALUES(skin_signature), " +
                                "command=VALUES(command), description=VALUES(description), world=VALUES(world), " +
                                "x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)"
                );

                Location loc = npc.getLocation();
                ps.setInt(1, npc.getId());
                ps.setString(2, npc.getUuid().toString());
                ps.setString(3, npc.getName());
                ps.setString(4, npc.getSkinName());
                ps.setString(5, npc.getSkinTexture());
                ps.setString(6, npc.getSkinSignature());
                ps.setString(7, npc.getCommand());

                // Converte lista de descrição para string
                String descriptionStr = null;
                if (npc.getDescription() != null && !npc.getDescription().isEmpty()) {
                    descriptionStr = String.join("||", npc.getDescription());
                }
                ps.setString(8, descriptionStr);

                ps.setString(9, loc.getWorld().getName());
                ps.setDouble(10, loc.getX());
                ps.setDouble(11, loc.getY());
                ps.setDouble(12, loc.getZ());
                ps.setFloat(13, loc.getYaw());
                ps.setFloat(14, loc.getPitch());

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    plugin.getLogger().info("§a[NPC] NPC #" + npc.getId() + " salvo no banco de dados!");
                } else {
                    plugin.getLogger().warning("§c[NPC] Falha ao salvar NPC #" + npc.getId());
                }

            } catch (Exception e) {
                plugin.getLogger().severe("§c[NPC] Erro ao salvar NPC #" + npc.getId() + ":");
                e.printStackTrace();
            }
        });
    }

    /**
     * Carrega NPCs do banco
     */
    private void loadNPCs() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (java.sql.Connection conn = plugin.getMySQL().getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM rs_lobby_npcs"
                );
                java.sql.ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    String skinName = rs.getString("skin_name");
                    String skinTexture = rs.getString("skin_texture");
                    String skinSignature = rs.getString("skin_signature");
                    String command = rs.getString("command");
                    String descriptionStr = rs.getString("description");

                    Location loc = new Location(
                            Bukkit.getWorld(rs.getString("world")),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );

                    NPCData npc = new NPCData(id, uuid, name, loc);
                    npc.setSkinName(skinName);
                    npc.setSkinTexture(skinTexture);
                    npc.setSkinSignature(skinSignature);
                    npc.setCommand(command);

                    if (descriptionStr != null && !descriptionStr.isEmpty()) {
                        npc.setDescription(java.util.Arrays.asList(descriptionStr.split("\\|\\|")));
                    }

                    npcs.put(id, npc);

                    if (id >= nextId) {
                        nextId = id + 1;
                    }
                }

                // Spawna todos na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (NPCData npc : npcs.values()) {
                        spawnNPC(npc);
                    }
                    plugin.getLogger().info("§a[NPC] " + npcs.size() + " NPCs carregados!");
                });

            } catch (Exception e) {
                plugin.getLogger().severe("§c[NPC] Erro ao carregar NPCs:");
                e.printStackTrace();
            }
        });
    }

    // Getters
    public NPCData getNPC(int id) { return npcs.get(id); }
    public Collection<NPCData> getAllNPCs() { return npcs.values(); }
    public EntityPlayer getEntity(int id) { return entities.get(id); }
}