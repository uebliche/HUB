package net.uebliche.hub.common.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class SqlJumpRunScoreRepository implements JumpRunScoreRepository {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS hub_jump_run_scores (" +
            "course_id VARCHAR(64) NOT NULL," +
            "player_uuid CHAR(36) NOT NULL," +
            "run_count BIGINT NOT NULL," +
            "best_time_ms BIGINT NOT NULL," +
            "updated_at BIGINT NOT NULL," +
            "PRIMARY KEY (course_id, player_uuid)" +
            ")";

    private final SqlConnectionProvider provider;

    public SqlJumpRunScoreRepository(StorageSettings.SqlSettings settings) {
        this.provider = new SqlConnectionProvider(settings);
        ensureTables();
    }

    private void ensureTables() {
        try (Connection connection = provider.open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE);
        } catch (SQLException ex) {
            throw new StorageException("Failed to ensure SQL score tables", ex);
        }
    }

    @Override
    public JumpRunScore getScore(String courseId, UUID playerId) {
        String sql = "SELECT run_count, best_time_ms, updated_at FROM hub_jump_run_scores WHERE course_id = ? AND player_uuid = ?";
        try (Connection connection = provider.open(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, courseId);
            ps.setString(2, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long runCount = rs.getLong("run_count");
                long bestTime = rs.getLong("best_time_ms");
                long updatedAt = rs.getLong("updated_at");
                return new JumpRunScore(courseId, playerId, runCount, bestTime, updatedAt);
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load score", ex);
        }
    }

    @Override
    public JumpRunScore recordRun(String courseId, UUID playerId, long timeMillis) {
        String selectSql = "SELECT run_count, best_time_ms FROM hub_jump_run_scores WHERE course_id = ? AND player_uuid = ?";
        String insertSql = "INSERT INTO hub_jump_run_scores (course_id, player_uuid, run_count, best_time_ms, updated_at) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE hub_jump_run_scores SET run_count = ?, best_time_ms = ?, updated_at = ? WHERE course_id = ? AND player_uuid = ?";
        long now = System.currentTimeMillis();

        try (Connection connection = provider.open()) {
            connection.setAutoCommit(false);
            long runCount;
            long bestTime;
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setString(1, courseId);
                select.setString(2, playerId.toString());
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        runCount = rs.getLong("run_count") + 1;
                        bestTime = rs.getLong("best_time_ms");
                        if (timeMillis > 0 && (bestTime <= 0 || timeMillis < bestTime)) {
                            bestTime = timeMillis;
                        }
                        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                            update.setLong(1, runCount);
                            update.setLong(2, bestTime);
                            update.setLong(3, now);
                            update.setString(4, courseId);
                            update.setString(5, playerId.toString());
                            update.executeUpdate();
                        }
                    } else {
                        runCount = 1;
                        bestTime = timeMillis > 0 ? timeMillis : 0;
                        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                            insert.setString(1, courseId);
                            insert.setString(2, playerId.toString());
                            insert.setLong(3, runCount);
                            insert.setLong(4, bestTime);
                            insert.setLong(5, now);
                            insert.executeUpdate();
                        }
                    }
                }
            }
            connection.commit();
            return new JumpRunScore(courseId, playerId, runCount, bestTime, now);
        } catch (SQLException ex) {
            throw new StorageException("Failed to record score", ex);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
