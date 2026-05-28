package com.socialpersona;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQLite Schema 迁移测试 —— 为现有数据库添加缺失列
 *
 * event_log 表缺少 detail_json + occurred_at 列，此测试执行 ALTER TABLE 迁移。
 */

@Disabled("本地迁移工具测试，依赖本地 social_persona.db 文件，不适用于 CI/CD")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DbMigrationTest {

    private static final String DB_URL = "jdbc:sqlite:./data/social_persona.db";

    @Test
    @Order(1)
    public void migrateEventLogColumns() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            System.out.println("=== SQLite Schema 迁移: event_log ===");

            String[] migrations = {
                    "ALTER TABLE event_log ADD COLUMN detail_json TEXT",
                    "ALTER TABLE event_log ADD COLUMN occurred_at TEXT",
                    "ALTER TABLE personas ADD COLUMN relationship_phase TEXT DEFAULT 'stranger'"
            };

            for (String sql : migrations) {
                try {
                    stmt.execute(sql);
                    System.out.println("  ✅ " + sql);
                } catch (Exception e) {
                    if (e.getMessage().contains("duplicate column") || e.getMessage().contains("already exists")) {
                        System.out.println("  ⏭️ 跳过(已存在): " + sql);
                    } else {
                        System.err.println("  ❌ " + sql + ": " + e.getMessage());
                        throw e;
                    }
                }
            }

            var rs = stmt.executeQuery("PRAGMA table_info(event_log)");
            System.out.println("\n  event_log 列清单:");
            while (rs.next()) {
                System.out.printf("    %-3d %-25s %-10s%n",
                        rs.getInt("cid"), rs.getString("name"), rs.getString("type"));
            }
            rs.close();

            System.out.println("  迁移完成。");
        }
    }

    @Test
    @Order(2)
    public void verifyEventLogColumnsExist() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("PRAGMA table_info(event_log)");
            boolean hasDetailJson = false;
            boolean hasOccurredAt = false;

            while (rs.next()) {
                String col = rs.getString("name");
                if ("detail_json".equals(col)) hasDetailJson = true;
                if ("occurred_at".equals(col)) hasOccurredAt = true;
            }
            rs.close();

            assertTrue(hasDetailJson, "event_log 表应有 detail_json 列");
            assertTrue(hasOccurredAt, "event_log 表应有 occurred_at 列");
        }
    }

    @Test
    @Order(3)
    public void verifyPersonasHasRelationshipPhase() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("PRAGMA table_info(personas)");
            boolean hasPhase = false;
            while (rs.next()) {
                if ("relationship_phase".equals(rs.getString("name"))) {
                    hasPhase = true;
                    break;
                }
            }
            rs.close();
            assertTrue(hasPhase, "personas 表应有 relationship_phase 列");
        }
    }
}
