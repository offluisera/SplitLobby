package org.github.luisera.splitlobby.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.github.luisera.splitlobby.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public MySQLManager(Main plugin) {
        this.plugin = plugin;
        conectar();
    }

    private void conectar() {
        // Carrega dados da config.yml
        String host = plugin.getConfig().getString("mysql.host");
        String port = plugin.getConfig().getString("mysql.port");
        String database = plugin.getConfig().getString("mysql.database", "SplitLobby");
        String username = plugin.getConfig().getString("mysql.username");
        String password = plugin.getConfig().getString("mysql.password");

        HikariConfig config = new HikariConfig();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Driver moderno (CJ) nao encontrado. Usando driver legado.");
            config.setDriverClassName("com.mysql.jdbc.Driver");
        }

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true");

        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool-size", 10));
        config.setConnectionTimeout(plugin.getConfig().getLong("mysql.connection-timeout", 30000));
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("§a[MySQL] Conexao estabelecida com sucesso ao banco: " + database);
            criarTabelas();
        } catch (Exception e) {
            plugin.getLogger().severe("§c[MySQL] Erro fatal ao conectar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void criarTabelas() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // 1. Tabela de Jogadores (Geral)
            String sqlPlayers = "CREATE TABLE IF NOT EXISTS splitlobby_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "nome VARCHAR(16) NOT NULL, " +
                    "coins DOUBLE DEFAULT 0, " +
                    "first_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ");";

            // 2. Tabela de Parkour (NOVA)
            String sqlParkour = "CREATE TABLE IF NOT EXISTS parkour_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "time_millis BIGINT" +
                    ");";

            // 2. Tabela de Recompensa
            String sqlRewards = "CREATE TABLE IF NOT EXISTS splitlobby_rewards (" +
                    "uuid VARCHAR(36), " +
                    "reward_key VARCHAR(50), " +
                    "date_claimed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY(uuid, reward_key)" +
                    ");";

            // Tabela de surpresas
            String sqlSecrets = "CREATE TABLE IF NOT EXISTS splitlobby_secrets (" +
                    "uuid VARCHAR(36), " +
                    "secret_id VARCHAR(50), " +
                    "date_found TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY(uuid, secret_id)" +
                    ");";

            // 4. Tabela de Chat Games
            String sqlChatGames = "CREATE TABLE IF NOT EXISTS splitlobby_chatgames (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "nickname VARCHAR(16), " +
                    "wins INT DEFAULT 0" +
                    ");";

            // 5. Tabela de Amigos
            String sqlFriends = "CREATE TABLE IF NOT EXISTS splitlobby_friends (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid1 VARCHAR(36) NOT NULL, " +
                    "uuid2 VARCHAR(36) NOT NULL, " +
                    "status VARCHAR(10) DEFAULT 'PENDING', " + // PENDING, ACCEPTED
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            // 6. Tabela de Configurações do Jogador
            String sqlSettings = "CREATE TABLE IF NOT EXISTS splitlobby_settings (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "allow_tell BOOLEAN DEFAULT TRUE, " +
                    "allow_chat BOOLEAN DEFAULT TRUE, " +
                    "allow_friend_requests BOOLEAN DEFAULT TRUE, " +
                    "allow_clan_invites BOOLEAN DEFAULT TRUE" +
                    ");";

            try (Connection conn = getConnection()) {
                try (PreparedStatement st1 = conn.prepareStatement(sqlPlayers)) {
                    st1.execute();
                }
                try (PreparedStatement st2 = conn.prepareStatement(sqlParkour)) {
                    st2.execute();
                }
                try (PreparedStatement st3 = conn.prepareStatement(sqlRewards)) {
                    st3.execute();
                }
                try (PreparedStatement st4 = conn.prepareStatement(sqlSecrets)){
                     st4.execute();
                }
                try (PreparedStatement st5 = conn.prepareStatement(sqlChatGames)) {
                    st5.execute();
                }
                try (PreparedStatement st6 = conn.prepareStatement(sqlFriends)) {
                    st6.execute();
                }
                try (PreparedStatement st7 = conn.prepareStatement(sqlSettings)) {
                    st7.execute();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("§c[MySQL] Erro ao criar tabelas: " + e.getMessage());
            }
        });
    }

    // --- MÉTODOS DE AMIGOS ---

    // Busca o UUID pelo Nick (usando nossa tabela de jogadores registrados)
    public java.util.UUID getUUID(String name) {
        String sql = "SELECT uuid FROM splitlobby_players WHERE nome = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return java.util.UUID.fromString(rs.getString("uuid"));
        } catch (java.sql.SQLException e) {}
        return null;
    }

    // Verifica se já são amigos ou se tem pedido pendente
    public boolean areFriendsOrPending(java.util.UUID u1, java.util.UUID u2) {
        // Verifica se já são amigos
        String sqlFriends = "SELECT id FROM splitlobby_friends WHERE (uuid1 = ? AND uuid2 = ?) OR (uuid1 = ? AND uuid2 = ?)";
        // Verifica se já tem pedido pendente (de qualquer um dos lados)
        String sqlRequests = "SELECT id FROM splitlobby_friend_requests WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)";

        try (java.sql.Connection conn = getConnection()) {
            // Checa Amizade
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlFriends)) {
                ps.setString(1, u1.toString());
                ps.setString(2, u2.toString());
                ps.setString(3, u2.toString());
                ps.setString(4, u1.toString());
                if (ps.executeQuery().next()) return true;
            }
            // Checa Pedidos
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlRequests)) {
                ps.setString(1, u1.toString());
                ps.setString(2, u2.toString());
                ps.setString(3, u2.toString());
                ps.setString(4, u1.toString());
                if (ps.executeQuery().next()) return true;
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendFriendRequest(java.util.UUID from, java.util.UUID to) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO splitlobby_friends (uuid1, uuid2, status) VALUES (?, ?, 'PENDING')";
            try (java.sql.Connection conn = getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                ps.executeUpdate();
            } catch (java.sql.SQLException e) { e.printStackTrace(); }
        });
    }

    // Retorna Lista de Nomes dos Amigos (Status ACCEPTED)
    public java.util.List<String> getFriendsList(java.util.UUID uuid) {
        java.util.List<String> friends = new java.util.ArrayList<>();
        // Query complexa para pegar o nome do "outro" jogador
        String sql = "SELECT p.nome FROM splitlobby_friends f " +
                "JOIN splitlobby_players p ON (CASE WHEN f.uuid1 = ? THEN f.uuid2 ELSE f.uuid1 END) = p.uuid " +
                "WHERE (f.uuid1 = ? OR f.uuid2 = ?) AND f.status = 'ACCEPTED'";

        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            String u = uuid.toString();
            ps.setString(1, u); ps.setString(2, u); ps.setString(3, u);
            java.sql.ResultSet rs = ps.executeQuery();
            while(rs.next()) friends.add(rs.getString("nome"));
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return friends;
    }

    // Retorna Lista de Nomes de quem enviou pedido (Status PENDING e uuid2 = Eu)
    public java.util.Map<String, Integer> getPendingRequests(java.util.UUID myUuid) {
        java.util.Map<String, Integer> requests = new java.util.HashMap<>();
        String sql = "SELECT f.id, p.nome FROM splitlobby_friends f " +
                "JOIN splitlobby_players p ON f.uuid1 = p.uuid " +
                "WHERE f.uuid2 = ? AND f.status = 'PENDING'";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, myUuid.toString());
            java.sql.ResultSet rs = ps.executeQuery();
            while(rs.next()) requests.put(rs.getString("nome"), rs.getInt("id"));
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return requests;
    }

    public void acceptRequest(int id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE splitlobby_friends SET status = 'ACCEPTED' WHERE id = ?";
            try (java.sql.Connection conn = getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (java.sql.SQLException e) {}
        });
    }

    public void rejectRequest(int id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM splitlobby_friends WHERE id = ?";
            try (java.sql.Connection conn = getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (java.sql.SQLException e) {}
        });
    }

    // --- MÉTODOS GENÉRICOS DE ESTATÍSTICA (IMPORTANTE) ---

    // Busca um valor inteiro de qualquer tabela (Ex: 'bw_stats', 'kills')
    public int getGenericInt(java.util.UUID uuid, String tableName, String columnName, String uuidColumn) {
        // CUIDADO: Em produção real, valide tableName/columnName para evitar SQL Injection se vier de config editável
        String sql = "SELECT " + columnName + " FROM " + tableName + " WHERE " + uuidColumn + " = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(columnName);
        } catch (java.sql.SQLException e) {
            // Tabela ainda não existe ou plugin não instalado, retorna 0
            return 0;
        }
        return 0;
    }

    // --- MÉTODOS DE CONFIGURAÇÕES ---

    public boolean getSetting(java.util.UUID uuid, String column) {
        String sql = "SELECT " + column + " FROM splitlobby_settings WHERE uuid = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean(column);
        } catch (java.sql.SQLException e) {}
        return true; // Padrão TRUE
    }

    public void toggleSetting(java.util.UUID uuid, String column, Runnable callback) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO splitlobby_settings (uuid, " + column + ") VALUES (?, FALSE) " +
                    "ON DUPLICATE KEY UPDATE " + column + " = NOT " + column;
            try (java.sql.Connection conn = getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();

                // Executa a ação de volta na thread principal (reabrir menu)
                if (callback != null) {
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, callback);
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeFriend(java.util.UUID u1, java.util.UUID u2) {
        // Deleta a amizade independentemente da ordem dos UUIDs
        String sql = "DELETE FROM splitlobby_friends WHERE (uuid1 = ? AND uuid2 = ?) OR (uuid1 = ? AND uuid2 = ?)";

        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u1.toString());
            ps.setString(2, u2.toString());
            ps.setString(3, u2.toString());
            ps.setString(4, u1.toString());

            ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }



    // --- MÉTODOS DE ESTATÍSTICAS

    // Estatística: Parkour
    public long getBestParkourTime(java.util.UUID uuid) {
        String sql = "SELECT time_millis FROM parkour_data WHERE uuid = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("time_millis");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Estatística: Chat Games
    public int getChatGameWins(java.util.UUID uuid) {
        String sql = "SELECT wins FROM splitlobby_chatgames WHERE uuid = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("wins");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // METÓDOS CHAT GAMES

    public void addChatGameWin(java.util.UUID uuid, String nickname) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO splitlobby_chatgames (uuid, nickname, wins) VALUES (?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE wins = wins + 1, nickname = ?";

            try (java.sql.Connection conn = getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, nickname);
                ps.setString(3, nickname); // Atualiza o nick caso tenha mudado
                ps.executeUpdate();
            } catch (java.sql.SQLException e) {
                plugin.getLogger().severe("Erro ao salvar vitoria no ChatGame: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // --- MÉTODOS PARA SECRETS ---

    public boolean hasFoundSecret(java.util.UUID uuid, String secretId) {
        String sql = "SELECT secret_id FROM splitlobby_secrets WHERE uuid = ? AND secret_id = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, secretId);
            java.sql.ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setSecretFound(java.util.UUID uuid, String secretId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO splitlobby_secrets (uuid, secret_id) VALUES (?, ?)";
            try (java.sql.Connection conn = getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, secretId);
                ps.executeUpdate();
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public int countFoundSecrets(java.util.UUID uuid) {
        String sql = "SELECT COUNT(*) FROM splitlobby_secrets WHERE uuid = ?";
        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // METÓDOS DIARIAS

    public boolean hasClaimedReward(UUID uuid, String rewardKey) {
        String sql = "SELECT reward_key FROM splitlobby_rewards WHERE uuid = ? AND reward_key = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, rewardKey);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setRewardClaimed(UUID uuid, String rewardKey) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO splitlobby_rewards (uuid, reward_key) VALUES (?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rewardKey);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    // --- MÉTODOS DO PARKOUR ---



    public void saveParkourTime(Player player, long time) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                // 1. Verificar se já existe um tempo melhor
                String queryCheck = "SELECT time_millis FROM parkour_data WHERE uuid = ?";
                try (PreparedStatement check = conn.prepareStatement(queryCheck)) {
                    check.setString(1, player.getUniqueId().toString());
                    ResultSet rs = check.executeQuery();

                    if (rs.next()) {
                        long oldTime = rs.getLong("time_millis");
                        // Se o novo tempo for maior (pior) que o antigo, não salva.
                        if (time >= oldTime) return;
                    }
                }

                // 2. Salvar ou Atualizar (Upsert)
                String queryInsert = "INSERT INTO parkour_data (uuid, name, time_millis) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE time_millis = ?, name = ?";

                try (PreparedStatement ps = conn.prepareStatement(queryInsert)) {
                    ps.setString(1, player.getUniqueId().toString());
                    ps.setString(2, player.getName());
                    ps.setLong(3, time);
                    // Update part
                    ps.setLong(4, time);
                    ps.setString(5, player.getName()); // Atualiza o nome caso tenha mudado

                    ps.executeUpdate();
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("§c[MySQL] Erro ao salvar parkour: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Map<String, Long> getTop10() {
        Map<String, Long> tops = new LinkedHashMap<>();
        String sql = "SELECT name, time_millis FROM parkour_data ORDER BY time_millis ASC LIMIT 10";

        // Importante: Executamos síncrono ou assíncrono dependendo de quem chama.
        // Como o updateLeaderboard do ParkourManager já roda async, aqui apenas executamos.
        // Se usar connection pool, o try-with-resources é essencial.

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tops.put(rs.getString("name"), rs.getLong("time_millis"));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("§c[MySQL] Erro ao buscar TOP 10: " + e.getMessage());
            e.printStackTrace();
        }
        return tops;
    }

    // --- CONEXÃO PADRÃO ---

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("A conexao MySQL ainda nao foi inicializada.");
        }
        return dataSource.getConnection();
    }

    public void desconectar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("§e[MySQL] Conexoes encerradas.");
        }
    }
}