package com.suite.db;

import com.suite.config.ConfigReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thin JDBC helper. Every call opens and closes its own connection (fine at this scale). */
public final class DbUtil {

    private DbUtil() { }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                ConfigReader.get("db.url"),
                ConfigReader.get("db.user"),
                ConfigReader.get("db.password"));
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** Returns all rows for the query as a list of column-name -> value maps. */
    public static List<Map<String, Object>> queryForList(String sql, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + sql, e);
        }
    }

    /** Returns the first row, or null when the query returns nothing. */
    public static Map<String, Object> queryForRow(String sql, Object... params) {
        List<Map<String, Object>> rows = queryForList(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** For COUNT(*)-style queries. */
    public static long queryForLong(String sql, Object... params) {
        Map<String, Object> row = queryForRow(sql, params);
        return ((Number) row.values().iterator().next()).longValue();
    }

    /** INSERT / UPDATE / DELETE. Returns affected row count. */
    public static int update(String sql, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB update failed: " + sql, e);
        }
    }
}
