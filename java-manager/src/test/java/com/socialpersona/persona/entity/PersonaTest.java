package com.socialpersona.persona.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Persona 实体 POJO 测试 —— 验证核心字段的 getter/setter 和业务含义
 *
 * 重点验证：
 *   - aiQq / ownerQq 字段（QQ 路由核心）
 *   - 人格维度字段（关系引擎输入）
 */
public class PersonaTest {

    /**
     * aiQq 和 ownerQq 字段的 getter/setter 应正常工作
     */
    @Test
    public void testAiQqAndOwnerQq() {
        Persona p = new Persona();
        p.setAiQq("2387511709");
        p.setOwnerQq("1875552542");

        assertEquals("2387511709", p.getAiQq(), "aiQq 应返回 AI 登录的 QQ 号");
        assertEquals("1875552542", p.getOwnerQq(), "ownerQq 应返回主人的 QQ 号");
    }

    /**
     * 人格维度字段默认值应为 null（DB 可空）
     */
    @Test
    public void testDimensionDefaultsToNull() {
        Persona p = new Persona();
        assertNull(p.getAttachmentAnxiety(), "新 Persona 的依恋焦虑应默认为 null");
        assertNull(p.getAttachmentAvoidance(), "新 Persona 的依恋回避应默认为 null");
        assertNull(p.getInitiativeTendency(), "新 Persona 的主动联系倾向应默认为 null");
    }

    /**
     * 设置所有人格维度应为有效值
     */
    @Test
    public void testSetAllDimensions() {
        Persona p = new Persona();
        p.setAttachmentAnxiety(0.7);
        p.setAttachmentAvoidance(0.3);
        p.setSelfEsteemStability(0.6);
        p.setInitiativeTendency(0.8);
        p.setTypingSpeed(3.5);

        assertEquals(0.7, p.getAttachmentAnxiety(), 0.01);
        assertEquals(0.3, p.getAttachmentAvoidance(), 0.01);
        assertEquals(0.6, p.getSelfEsteemStability(), 0.01);
        assertEquals(0.8, p.getInitiativeTendency(), 0.01);
        assertEquals(3.5, p.getTypingSpeed(), 0.01);
    }

    /**
     * 状态字段默认为 null，设置后生效
     */
    @Test
    public void testStatusAndIds() {
        Persona p = new Persona();
        p.setId("d926f28e-ea21-4f6a-8d69-9ed3dba6052b");
        p.setName("小奈");
        p.setStatus("active");

        assertEquals("d926f28e-ea21-4f6a-8d69-9ed3dba6052b", p.getId());
        assertEquals("小奈", p.getName());
        assertEquals("active", p.getStatus());
    }

    /**
     * 文本配置字段的读写
     */
    @Test
    public void testTextConfigFields() {
        Persona p = new Persona();
        p.setBigFiveJson("{\"openness\":0.7,\"conscientiousness\":0.6}");
        p.setSocialRhythm("slow_warm");
        p.setConflictStyle("direct_confront");
        p.setInputMethod("phone_thumb");
        p.setCharacterCurrentContext("大学生，正在准备期末考试");

        assertTrue(p.getBigFiveJson().contains("openness"));
        assertEquals("slow_warm", p.getSocialRhythm());
        assertEquals("phone_thumb", p.getInputMethod());
        assertTrue(p.getCharacterCurrentContext().contains("期末考试"));
    }
}