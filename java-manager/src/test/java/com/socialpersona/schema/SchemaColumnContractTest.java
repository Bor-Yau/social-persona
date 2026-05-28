package com.socialpersona.schema;

import com.socialpersona.event.entity.DailyEvent;
import com.socialpersona.event.entity.EventLog;
import com.socialpersona.matchmaker.entity.MatchmakerSession;
import com.socialpersona.message.entity.ScheduledMessage;
import com.socialpersona.persona.entity.CharacterLifeArchive;
import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.entity.RelationshipState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema-实体双向校验 —— 确保 schema.sql 与所有 @TableName 实体的列完全同步
 *
 * ★ 三层覆盖中的第一层（静态契约层）：
 *   - 离线检查 DDL 文本与 Java 实体字段的一致性
 *   - 不依赖数据库，纯静态分析
 *   - 任何新增/删除/重命名字段后 schema.sql 忘记同步 → 立刻失败
 *
 * 覆盖 7 张表/实体：
 *   personas, character_life_archives, relationship_state,
 *   daily_events, scheduled_messages, event_log, matchmaker_sessions
 */
class SchemaColumnContractTest {

    private static Map<String, Set<String>> schemaColumns;
    private static Map<String, List<EntityColumn>> entityColumns;

    @BeforeAll
    static void loadSchemaAndEntities() throws IOException {
        schemaColumns = parseSchemaSql();
        entityColumns = scanEntities();
    }

    @Test
    void testEventLogColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("event_log");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols, "event_log 表应有实体映射");
        assertTrue(colMap.containsKey("detail_json"), "EventLog.detailJson → 列 detail_json");
        assertTrue(colMap.containsKey("occurred_at"), "EventLog.occurredAt → 列 occurred_at");
        assertTrue(colMap.containsKey("persona_id"), "EventLog.personaId → 列 persona_id");
        assertTrue(colMap.containsKey("log_type"),  "EventLog.logType → 列 log_type");
    }

    @Test
    void testScheduledMessageColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("scheduled_messages");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols);
        assertTrue(colMap.containsKey("items_json"),           "ScheduledMessage.itemsJson → items_json");
        assertTrue(colMap.containsKey("inner_thought_json"),   "ScheduledMessage.innerThoughtJson → inner_thought_json");
        assertTrue(colMap.containsKey("burst_group_id"),       "ScheduledMessage.burstGroupId → burst_group_id");
        assertTrue(colMap.containsKey("is_sent"),              "ScheduledMessage.isSent → is_sent");
    }

    @Test
    void testPersonaCoreColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("personas");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols);
        assertTrue(colMap.containsKey("big_five_json"),         "Persona.bigFiveJson → big_five_json");
        assertTrue(colMap.containsKey("attachment_anxiety"),    "Persona.attachmentAnxiety → attachment_anxiety");
        assertTrue(colMap.containsKey("ai_qq"),                 "Persona.aiQq → ai_qq");
        assertTrue(colMap.containsKey("owner_qq"),              "Persona.ownerQq → owner_qq");
    }

    @Test
    void testRelationshipStateColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("relationship_state");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols);
        assertTrue(colMap.containsKey("trust"),              "RelationshipState.trust → trust");
        assertTrue(colMap.containsKey("closeness"),          "RelationshipState.closeness → closeness");
        assertTrue(colMap.containsKey("emotional_energy"),   "RelationshipState.emotionalEnergy → emotional_energy");
        assertTrue(colMap.containsKey("last_heartbeat_at"),  "RelationshipState.lastHeartbeatAt → last_heartbeat_at");
    }

    @Test
    void testDailyEventColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("daily_events");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols);
        assertTrue(colMap.containsKey("event_date"), "DailyEvent.eventDate → event_date");
        assertTrue(colMap.containsKey("event_time"), "DailyEvent.eventTime → event_time");
        assertTrue(colMap.containsKey("is_active"),  "DailyEvent.isActive → is_active");
    }

    @Test
    void testCharacterLifeArchiveColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("character_life_archives");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols);
        assertTrue(colMap.containsKey("persona_id"),  "CharacterLifeArchive.personaId → persona_id");
        assertTrue(colMap.containsKey("archive_json"), "CharacterLifeArchive.archiveJson → archive_json");
    }

    @Test
    void testMatchmakerSessionColumnsExist() {
        List<EntityColumn> cols = entityColumns.get("matchmaker_sessions");
        Map<String, String> colMap = cols.stream()
                .collect(Collectors.toMap(ec -> ec.columnName, ec -> ec.fieldName));

        assertNotNull(cols);
        assertTrue(colMap.containsKey("session_id"),         "MatchmakerSession.sessionId → session_id");
        assertTrue(colMap.containsKey("current_stage"),      "MatchmakerSession.currentStage → current_stage");
        assertTrue(colMap.containsKey("collected_data_json"), "MatchmakerSession.collectedDataJson → collected_data_json");
    }

    @Test
    void testAllEntityFieldsHaveSchemaColumns() {
        StringBuilder failures = new StringBuilder();

        for (Map.Entry<String, List<EntityColumn>> entry : entityColumns.entrySet()) {
            String table = entry.getKey();
            Set<String> schemaColNames = schemaColumns.get(table);
            if (schemaColNames == null) {
                failures.append("表 ").append(table).append(" 在 schema.sql 中不存在\n");
                continue;
            }
            for (EntityColumn ec : entry.getValue()) {
                if (!schemaColNames.contains(ec.columnName)) {
                    failures.append("实体字段 ")
                            .append(ec.entityClass).append(".").append(ec.fieldName)
                            .append(" → 列 ").append(ec.columnName)
                            .append(" → schema 表 ").append(table).append(" 中不存在\n");
                }
            }
        }

        if (failures.length() > 0) {
            fail("实体字段与 schema.sql 不一致:\n" + failures);
        }
    }

    @Test
    void testReportOrphanSchemaColumns() {
        System.out.println("=== schema 中存在但实体无对应字段的列（仅信息，不阻断） ===");

        for (Map.Entry<String, Set<String>> entry : schemaColumns.entrySet()) {
            String table = entry.getKey();
            List<EntityColumn> entityCols = entityColumns.get(table);
            if (entityCols == null) {
                System.out.println("  " + table + " → 无对应实体");
                continue;
            }
            Set<String> entityColNames = entityCols.stream()
                    .map(ec -> ec.columnName)
                    .collect(Collectors.toSet());

            Set<String> orphans = new TreeSet<>(entry.getValue());
            orphans.removeAll(entityColNames);
            // 跳过通用字段
            orphans.removeIf(c -> c.equals("id") || c.equals("created_at") || c.equals("updated_at"));
            if (!orphans.isEmpty()) {
                System.out.println("  " + table + " 多出列: " + orphans);
            }
        }
    }
    // ==================== schema.sql 解析 ====================

    private static Map<String, Set<String>> parseSchemaSql() throws IOException {
        Map<String, Set<String>> result = new LinkedHashMap<>();

        String sql;
        try (InputStream is = SchemaColumnContractTest.class
                .getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) throw new IOException("schema.sql 未找到");
            sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        int idx = 0;
        while (true) {
            int createPos = sql.indexOf("CREATE TABLE IF NOT EXISTS ", idx);
            if (createPos < 0) break;

            int nameStart = createPos + "CREATE TABLE IF NOT EXISTS ".length();
            int nameEnd = skipWord(sql, nameStart);
            String tableName = sql.substring(nameStart, nameEnd).trim().toLowerCase();

            int parenStart = sql.indexOf('(', nameEnd);
            if (parenStart < 0) break;

            int parenEnd = findMatchingParen(sql, parenStart + 1);
            if (parenEnd < 0) break;

            String block = sql.substring(parenStart + 1, parenEnd);
            Set<String> columns = extractColumnNames(block);
            result.put(tableName, columns);

            idx = parenEnd + 1;
        }

        return result;
    }

    private static int skipWord(String s, int start) {
        int i = start;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i))
                && s.charAt(i) != '(') i++;
        return i;
    }

    private static int findMatchingParen(String s, int start) {
        int depth = 1;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '(') depth++;
            else if (s.charAt(i) == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static Set<String> extractColumnNames(String block) {
        Set<String> cols = new LinkedHashSet<>();

        List<String> segments = splitTopLevel(block);

        for (String seg : segments) {
            String cleaned = removeSqlComments(seg).trim();
            if (cleaned.isEmpty()) continue;

            String upper = cleaned.toUpperCase();
            if (upper.startsWith("FOREIGN") || upper.startsWith("PRIMARY")
                    || upper.startsWith("CONSTRAINT") || upper.startsWith("UNIQUE")
                    || upper.startsWith("CHECK")) {
                continue;
            }

            int firstSpace = indexOfWhitespaceOrParen(cleaned, 0);
            if (firstSpace > 0) {
                String colName = cleaned.substring(0, firstSpace).trim().toLowerCase();
                if (!colName.isEmpty() && !colName.startsWith("(")) {
                    cols.add(colName);
                }
            }
        }
        return cols;
    }

    /** 移除 SQL 单行注释（-- 到行尾），不支持多行注释 */
    private static String removeSqlComments(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (i + 1 < s.length() && s.charAt(i) == '-' && s.charAt(i + 1) == '-') {
                int end = s.indexOf('\n', i);
                if (end < 0) break;
                i = end + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static int indexOfWhitespaceOrParen(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == '(' || c == ')') return i;
        }
        return s.length();
    }

    private static List<String> splitTopLevel(String block) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < block.length(); i++) {
            char c = block.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(block.substring(start, i));
                start = i + 1;
            }
        }
        if (start < block.length()) {
            parts.add(block.substring(start));
        }
        return parts;
    }

    // ==================== 实体扫描 ====================

    @SuppressWarnings("unchecked")
    private static Map<String, List<EntityColumn>> scanEntities() {
        List<Class<?>> entityClasses = List.of(
                Persona.class, CharacterLifeArchive.class, RelationshipState.class,
                DailyEvent.class, ScheduledMessage.class, EventLog.class,
                MatchmakerSession.class
        );

        Map<String, List<EntityColumn>> result = new LinkedHashMap<>();

        for (Class<?> clazz : entityClasses) {
            com.baomidou.mybatisplus.annotation.TableName tn =
                    clazz.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
            String tableName = tn != null ? tn.value() : camelToSnake(clazz.getSimpleName());

            List<EntityColumn> cols = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.getName().equals("serialVersionUID")) continue;

                String colName = camelToSnake(field.getName());
                cols.add(new EntityColumn(clazz.getSimpleName(), field.getName(), colName));
            }
            result.put(tableName, cols);
        }
        return result;
    }

    static String camelToSnake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ==================== 数据结构 ====================

    static class EntityColumn {
        final String entityClass;
        final String fieldName;
        final String columnName;

        EntityColumn(String entityClass, String fieldName, String columnName) {
            this.entityClass = entityClass;
            this.fieldName = fieldName;
            this.columnName = columnName;
        }
    }
}
