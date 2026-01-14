package org.github.luisera.splitlobby.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public PlayerData getPlayer(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    // Carrega do MySQL ao entrar (Async)
    public void loadPlayer(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            PlayerData data = new PlayerData(uuid, name);

            String query = "SELECT coins FROM splitlobby_players WHERE uuid = ?";

            try (Connection conn = Main.getInstance().getMySQL().getConnection();
                 PreparedStatement st = conn.prepareStatement(query)) {

                st.setString(1, uuid.toString());
                ResultSet rs = st.executeQuery();

                if (rs.next()) {
                    // Jogador existe, carrega dados
                    data.setCoins(rs.getDouble("coins"));
                    // Marca como limpo, pois acabou de vir do banco
                    data.setClean();
                } else {
                    // Jogador novo, cria registro inicial
                    createPlayer(uuid, name);
                }

            } catch (SQLException e) {
                Main.getInstance().getLogger().severe("Erro ao carregar dados de " + name + ": " + e.getMessage());
            }

            // Coloca no Cache (Volta para Main Thread para evitar erros de concorrência)
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> cache.put(uuid, data));
        });
    }

    private void createPlayer(UUID uuid, String name) {
        String insert = "INSERT INTO splitlobby_players (uuid, nome) VALUES (?, ?)";
        try (Connection conn = Main.getInstance().getMySQL().getConnection();
             PreparedStatement st = conn.prepareStatement(insert)) {
            st.setString(1, uuid.toString());
            st.setString(2, name);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Salva no MySQL ao sair (Async)
    public void savePlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid); // Remove do cache ao salvar/sair

        if (data != null && data.isDirty()) { // Só salva se houve alteração
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                String update = "UPDATE splitlobby_players SET coins = ? WHERE uuid = ?";
                try (Connection conn = Main.getInstance().getMySQL().getConnection();
                     PreparedStatement st = conn.prepareStatement(update)) {

                    st.setDouble(1, data.getCoins());
                    st.setString(2, uuid.toString());
                    st.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}