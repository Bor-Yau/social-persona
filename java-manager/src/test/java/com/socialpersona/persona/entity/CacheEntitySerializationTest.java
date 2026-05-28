package com.socialpersona.persona.entity;

import com.socialpersona.matchmaker.entity.MatchmakerSession;
import com.socialpersona.relationship.entity.RelationshipState;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存实体序列化测试 —— 验证所有 @Cacheable 返回值可被 JDK 序列化
 *
 * ★ 为什么需要这个测试：
 *   当 Redis 不可用且 @ConditionalOnBean 不命中时，
 *   Spring 回退到默认的 JdkSerializationRedisSerializer。
 *   如果实体不实现 Serializable，抛出序列化异常 → 请求失败。
 *   这个测试确保回退路径也能正常工作。
 */
public class CacheEntitySerializationTest {

    @Test
    public void testPersonaSerializable() throws Exception {
        Persona p = new Persona();
        p.setId("test-id");
        p.setName("小奈");
        p.setBigFiveJson("{}");
        p.setAiQq("2387511709");
        p.setOwnerQq("1875552542");

        byte[] bytes = serialize(p);
        Persona deserialized = deserialize(bytes);

        assertEquals("test-id", deserialized.getId());
        assertEquals("小奈", deserialized.getName());
        assertEquals("2387511709", deserialized.getAiQq());
        assertEquals("1875552542", deserialized.getOwnerQq());
    }

    @Test
    public void testPersonaWithAllFields() throws Exception {
        Persona p = new Persona();
        p.setId("full-id");
        p.setName("完整测试");
        p.setBigFiveJson("{\"o\":0.8,\"c\":0.6}");
        p.setAttachmentAnxiety(0.5);
        p.setAttachmentAvoidance(0.3);
        p.setSelfEsteemStability(0.7);
        p.setSocialRhythm("night_owl");
        p.setConflictStyle("direct");
        p.setInitiativeTendency(0.6);
        p.setInputMethod("phone_thumb");
        p.setTypingSpeed(2.5);
        p.setAiQq("1111111111");
        p.setOwnerQq("2222222222");
        p.setStatus("active");

        byte[] bytes = serialize(p);
        Persona d = deserialize(bytes);

        assertEquals("完整测试", d.getName());
        assertEquals(0.5, d.getAttachmentAnxiety(), 0.001);
        assertEquals("night_owl", d.getSocialRhythm());
        assertEquals("active", d.getStatus());
    }

    @Test
    public void testRelationshipStateSerializable() throws Exception {
        RelationshipState rs = new RelationshipState();
        rs.setPersonaId("persona-1");
        rs.setTrust(50.0);
        rs.setCloseness(20.0);
        rs.setTension(10.0);
        rs.setEmotionalEnergy(30.0);
        rs.setTensionPressure(5.0);
        rs.setContactUrge(0.0);

        byte[] bytes = serialize(rs);
        RelationshipState d = deserialize(bytes);

        assertEquals("persona-1", d.getPersonaId());
        assertEquals(50.0, d.getTrust(), 0.001);
        assertEquals(20.0, d.getCloseness(), 0.001);
    }

    @Test
    public void testMatchmakerSessionSerializable() throws Exception {
        MatchmakerSession s = new MatchmakerSession();
        s.setSessionId("session-1");
        s.setCurrentStage("basic_profile");
        s.setCollectedDataJson("{\"hobby\":\"读书\"}");
        s.setHistoryJson("[]");
        s.setStatus("in_progress");

        byte[] bytes = serialize(s);
        MatchmakerSession d = deserialize(bytes);

        assertEquals("session-1", d.getSessionId());
        assertEquals("basic_profile", d.getCurrentStage());
        assertEquals("in_progress", d.getStatus());
    }

    // ==================== 辅助 ====================

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    private byte[] serialize(Object obj) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }
}