package com.meridian.service;

import com.meridian.dto.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;

/** Read-only database explorer for the live demo: tables, rows, relationships and a restricted SQL console. */
@Service
public class DbExplorerService {
    private static final Pattern BLOCKED = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|replace|call|load|handler|lock|unlock|set|into|outfile|dumpfile|sleep|benchmark|kill|shutdown)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern START = Pattern.compile("^(select|show|describe|desc|explain|with)\\b", Pattern.CASE_INSENSITIVE);
    private static final int MAX_ROWS = 200;

    private final JdbcTemplate jdbc;

    public DbExplorerService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static boolean sensitive(String column) {
        return column != null && column.toLowerCase(Locale.ROOT).contains("hash");
    }

    private static Object cell(Object v, boolean mask) {
        if (mask) return "••••••••";
        if (v == null || v instanceof Number || v instanceof Boolean || v instanceof String) return v;
        return String.valueOf(v);
    }

    @Transactional(readOnly = true)
    public List<TableInfo> tables() {
        Map<String, Integer> colCount = new HashMap<>();
        jdbc.query("SELECT TABLE_NAME, COUNT(*) AS c FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() GROUP BY TABLE_NAME",
                rs -> { colCount.put(rs.getString("TABLE_NAME"), rs.getInt("c")); });
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT TABLE_NAME AS n, TABLE_TYPE AS t FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_TYPE, TABLE_NAME");
        List<TableInfo> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String name = String.valueOf(r.get("n"));
            Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + name + "`", Long.class);
            out.add(new TableInfo(name, "VIEW".equals(String.valueOf(r.get("t"))) || String.valueOf(r.get("t")).contains("VIEW") ? "VIEW" : "TABLE",
                    count == null ? 0 : count, colCount.getOrDefault(name, 0)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public TableData browse(String table, int page, int size) {
        if (tables().stream().noneMatch(t -> t.name().equals(table))) throw new BankException("Unknown table", HttpStatus.NOT_FOUND);
        int sz = Math.min(Math.max(size, 1), 100), pg = Math.max(page, 0);
        List<ColumnInfo> cols = jdbc.query(
                "SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_KEY, IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                (rs, i) -> new ColumnInfo(rs.getString(1), rs.getString(2), rs.getString(3), "YES".equals(rs.getString(4))), table);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
        List<List<Object>> rows = jdbc.query("SELECT * FROM `" + table + "` ORDER BY 1 DESC LIMIT ? OFFSET ?", rs -> {
            List<List<Object>> list = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= md.getColumnCount(); i++) row.add(cell(rs.getObject(i), sensitive(md.getColumnLabel(i))));
                list.add(row);
            }
            return list;
        }, sz, pg * sz);
        return new TableData(table, cols, rows, total == null ? 0 : total, pg, sz);
    }

    @Transactional(readOnly = true)
    public List<Relation> relations() {
        return jdbc.query("SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE "
                + "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME, COLUMN_NAME",
                (rs, i) -> new Relation(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    @Transactional(readOnly = true)
    public QueryResult query(String raw) {
        String sql = raw.trim();
        while (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
        if (sql.isEmpty()) throw new BankException("Enter a SQL query");
        if (!START.matcher(sql).find()) throw new BankException("Only SELECT, SHOW, DESCRIBE and EXPLAIN statements are allowed");
        if (sql.contains(";") || sql.contains("--") || sql.contains("#") || sql.contains("/*")) throw new BankException("Only a single statement without comments is allowed");
        if (BLOCKED.matcher(sql).find()) throw new BankException("This console is read-only. Data-changing keywords are blocked.");
        if (sql.toLowerCase(Locale.ROOT).contains("hash")) throw new BankException("Credential (hash) columns are protected and cannot be queried.");
        final String stmt = sql;
        long start = System.currentTimeMillis();
        try {
            return jdbc.execute((java.sql.Connection con) -> {
                try (Statement st = con.createStatement()) {
                    st.setMaxRows(MAX_ROWS + 1);
                    st.setQueryTimeout(8);
                    st.execute(stmt);
                    try (ResultSet rs = st.getResultSet()) {
                        if (rs == null) return new QueryResult(List.of(), List.of(), 0, System.currentTimeMillis() - start, false);
                        ResultSetMetaData md = rs.getMetaData();
                        List<String> cols = new ArrayList<>();
                        for (int i = 1; i <= md.getColumnCount(); i++) cols.add(md.getColumnLabel(i));
                        List<List<Object>> rows = new ArrayList<>();
                        while (rs.next() && rows.size() <= MAX_ROWS) {
                            List<Object> row = new ArrayList<>();
                            for (int i = 1; i <= cols.size(); i++) row.add(cell(rs.getObject(i), sensitive(cols.get(i - 1))));
                            rows.add(row);
                        }
                        boolean truncated = rows.size() > MAX_ROWS;
                        if (truncated) rows.remove(rows.size() - 1);
                        return new QueryResult(cols, rows, rows.size(), System.currentTimeMillis() - start, truncated);
                    }
                }
            });
        } catch (BankException e) {
            throw e;
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            throw new BankException("SQL error: " + root.getMessage());
        }
    }
}
