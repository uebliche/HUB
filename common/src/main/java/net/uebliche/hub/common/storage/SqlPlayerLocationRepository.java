package net.uebliche.hub.common.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class SqlPlayerLocationRepository implements PlayerLocationRepository {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS hub_player_locations (" +
            "server_id VARCHAR(64) NOT NULL," +
            "player_uuid CHAR(36) NOT NULL," +
            "world VARCHAR(128) NOT NULL," +
            "x DOUBLE NOT NULL," +
            "y DOUBLE NOT NULL," +
            "z DOUBLE NOT NULL," +
            "yaw FLOAT NOT NULL," +
            "pitch FLOAT NOT NULL," +
            "updated_at BIGINT NOT NULL," +
            "PRIMARY KEY (server_id, player_uuid)" +
            ")";

    private final SqlConnectionProvider provider;

    public SqlPlayerLocationRepository(StorageSettings.SqlSettings settings) {
        this.provider = new SqlConnectionProvider(settings);
        ensureTables();
    }

    private void ensureTables() {
        try (Connection connection = provider.open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE);
        } catch (SQLException ex) {
            throw new StorageException("Failed to ensure SQL location tables", ex);
        }
    }

    @Override
    public PlayerLocation getLocation(String serverId, UUID playerId) {
        String sql = "SELECT world, x, y, z, yaw, pitch, updated_at FROM hub_player_locations WHERE server_id = ? AND player_uuid = ?";
        try (Connection connection = provider.open(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, serverId);
            ps.setString(2, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PlayerLocation(
                        serverId,
                        playerId,
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getLong("updated_at")
                );
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load location", ex);
        }
    }

    @Override
    public void saveLocation(PlayerLocation location) {
        String selectSql = "SELECT player_uuid FROM hub_player_locations WHERE server_id = ? AND player_uuid = ?";
        String insertSql = "INSERT INTO hub_player_locations (server_id, player_uuid, world, x, y, z, yaw, pitch, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE hub_player_locations SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, updated_at = ? WHERE server_id = ? AND player_uuid = ?";

        try (Connection connection = provider.open()) {
            connection.setAutoCommit(false);
            boolean exists = false;
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setString(1, location.serverId());
                select.setString(2, location.playerId().toString());
                try (ResultSet rs = select.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setString(1, location.world());
                    update.setDouble(2, location.x());
                    update.setDouble(3, location.y());
                    update.setDouble(4, location.z());
                    update.setFloat(5, location.yaw());
                    update.setFloat(6, location.pitch());
                    update.setLong(7, location.updatedAt());
                    update.setString(8, location.serverId());
                    update.setString(9, location.playerId().toString());
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setString(1, location.serverId());
                    insert.setString(2, location.playerId().toString());
                    insert.setString(3, location.world());
                    insert.setDouble(4, location.x());
                    insert.setDouble(5, location.y());
                    insert.setDouble(6, location.z());
                    insert.setFloat(7, location.yaw());
                    insert.setFloat(8, location.pitch());
                    insert.setLong(9, location.updatedAt());
                    insert.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException ex) {
            throw new StorageException("Failed to save location", ex);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
