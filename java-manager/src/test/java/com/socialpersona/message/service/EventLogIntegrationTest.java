package com.socialpersona.message.service;

import com.socialpersona.event.entity.EventLog;
import com.socialpersona.event.repository.EventLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventLog 集成测试 —— 真实 SQLite 中 INSERT → SELECT 往返
 *
 * ★ 三层覆盖中的第三层（动态运行层）：
 *   - 验证真实数据库的 schema 列完整性
 *   - 验证 MyBatis-Plus 实体映射正确性
 *   - 任何列缺失 / 类型不匹配 → SQLite 异常 → 测试失败
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EventLogIntegrationTest {

    @Autowired
    private EventLogMapper eventLogMapper;

    private String testLogId;

    @AfterEach
    public void tearDown() {
        if (testLogId != null) {
            try { eventLogMapper.deleteById(testLogId); } catch (Exception ignored) {}
            testLogId = null;
        }
    }

    /**
     * ★ 核心验收：写入 & 回读 innter_thought 类型 event_log
     */
    @Test
    public void testInsertAndSelectInnerThought() {
        String id = UUID.randomUUID().toString();
        EventLog entry = new EventLog();
        entry.setId(id);
        entry.setPersonaId("integration-test-persona");
        entry.setLogType("inner_thought");
        entry.setDetailJson("{\"raw_thought\":\"还不错\",\"attitude\":\"positive\"}");
        entry.setOccurredAt("2026-05-22T07:53:44Z");
        eventLogMapper.insert(entry);

        EventLog fetched = eventLogMapper.selectById(id);

        assertNotNull(fetched, "写入后应能立即读出");
        assertEquals("inner_thought", fetched.getLogType());
        assertNotNull(fetched.getDetailJson(), "detail_json 列应存在");
        assertTrue(fetched.getDetailJson().contains("raw_thought"), "detail_json 内容应完整");
        assertNotNull(fetched.getOccurredAt(), "occurred_at 列应存在");
        assertEquals("2026-05-22T07:53:44Z", fetched.getOccurredAt());

        testLogId = id;
    }

    /**
     * detailJson 为 null → 不抛异常
     */
    @Test
    public void testInsertNullDetailJson() {
        String id = UUID.randomUUID().toString();
        EventLog entry = new EventLog();
        entry.setId(id);
        entry.setPersonaId("integration-test-persona");
        entry.setLogType("inner_thought");
        entry.setDetailJson(null);
        entry.setOccurredAt("2026-05-22T07:53:44Z");
        eventLogMapper.insert(entry);

        EventLog fetched = eventLogMapper.selectById(id);
        assertNotNull(fetched);
        assertNull(fetched.getDetailJson(), "detailJson 为 null 时应存 null");

        testLogId = id;
    }

    /**
     * occurredAt 为 null → 不抛异常
     */
    @Test
    public void testInsertNullOccurredAt() {
        String id = UUID.randomUUID().toString();
        EventLog entry = new EventLog();
        entry.setId(id);
        entry.setPersonaId("integration-test-persona");
        entry.setLogType("inner_thought");
        entry.setDetailJson("{}");
        entry.setOccurredAt(null);
        eventLogMapper.insert(entry);

        EventLog fetched = eventLogMapper.selectById(id);
        assertNotNull(fetched);
        assertNull(fetched.getOccurredAt(), "occurredAt 为 null 时应存 null");

        testLogId = id;
    }

    /**
     * 全部字段填充 → 完整写入 & 回读
     */
    @Test
    public void testInsertAllFields() {
        String id = UUID.randomUUID().toString();
        EventLog entry = new EventLog();
        entry.setId(id);
        entry.setPersonaId("integration-test-persona");
        entry.setLogType("state_snapshot");
        entry.setDetailJson("{\"old_value\":30,\"new_value\":40}");
        entry.setOccurredAt("2026-05-22T08:00:00Z");
        eventLogMapper.insert(entry);

        EventLog fetched = eventLogMapper.selectById(id);
        assertNotNull(fetched);
        assertEquals("state_snapshot", fetched.getLogType());
        assertEquals("{\"old_value\":30,\"new_value\":40}", fetched.getDetailJson());
        assertEquals("2026-05-22T08:00:00Z", fetched.getOccurredAt());

        testLogId = id;
    }

    /**
     * 同一 persona 两条 event_log → 互不影响
     */
    @Test
    public void testInsertTwoLogsForSamePersona() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        EventLog e1 = new EventLog();
        e1.setId(id1);
        e1.setPersonaId("integration-test-persona");
        e1.setLogType("inner_thought");
        e1.setDetailJson("{\"msg\":\"first\"}");
        e1.setOccurredAt("2026-05-22T08:00:00Z");
        eventLogMapper.insert(e1);

        EventLog e2 = new EventLog();
        e2.setId(id2);
        e2.setPersonaId("integration-test-persona");
        e2.setLogType("event");
        e2.setDetailJson("{\"msg\":\"second\"}");
        e2.setOccurredAt("2026-05-22T09:00:00Z");
        eventLogMapper.insert(e2);

        EventLog f1 = eventLogMapper.selectById(id1);
        EventLog f2 = eventLogMapper.selectById(id2);
        assertNotNull(f1);
        assertNotNull(f2);
        assertTrue(f1.getDetailJson().contains("first"));
        assertTrue(f2.getDetailJson().contains("second"));

        // 清理两条
        eventLogMapper.deleteById(id1);
        eventLogMapper.deleteById(id2);
    }
}
