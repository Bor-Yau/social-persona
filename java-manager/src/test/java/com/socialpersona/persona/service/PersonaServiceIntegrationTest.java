package com.socialpersona.persona.service;

import com.socialpersona.persona.dto.PersonaConfigDTO;
import com.socialpersona.persona.entity.CharacterLifeArchive;
import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.repository.CharacterLifeArchiveMapper;
import com.socialpersona.persona.repository.PersonaMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersonaService 集成测试 —— 真实 SQLite 数据库 + MyBatis-Plus
 *
 * 验证涉及 LambdaUpdateWrapper 的方法：
 *   1. setAllOwnerQq — 批量更新所有人格的 owner_qq
 *   2. findByAiQQ — 按 AI QQ 号查找
 *   3. exportConfig — 导出含 aiQq/ownerQq 的 JSON
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PersonaServiceIntegrationTest {

    @Autowired
    private PersonaService personaService;

    @Autowired
    private PersonaMapper personaMapper;

    @Autowired
    private CharacterLifeArchiveMapper lifeArchiveMapper;

    private Persona testPersona;
    private String createdPersonaId;

    @BeforeEach
    public void setUp() {
        testPersona = new Persona();
        testPersona.setId("test-persona-1");
        testPersona.setName("测试小奈");
        testPersona.setAiQq("2387511709");
        testPersona.setOwnerQq("1875552542");
        testPersona.setBigFiveJson("{}");
        testPersona.setAttachmentAnxiety(0.5);
        testPersona.setAttachmentAvoidance(0.3);
        testPersona.setInitiativeTendency(0.6);
        testPersona.setStatus("active");
        personaMapper.insert(testPersona);

        Persona persona2 = new Persona();
        persona2.setId("test-persona-2");
        persona2.setName("测试小律");
        persona2.setAiQq("1234567890");
        persona2.setOwnerQq(null);
        persona2.setBigFiveJson("{}");
        persona2.setStatus("active");
        personaMapper.insert(persona2);
    }

    @AfterEach
    public void tearDown() {
        try {
            if (createdPersonaId != null) {
                lifeArchiveMapper.deleteById(createdPersonaId);
                personaMapper.deleteById(createdPersonaId);
                // 清理测试创建的图片目录
                try {
                    Path testImgDir = Path.of("target", "test-generated_images", createdPersonaId.substring(0, 8));
                    if (Files.isDirectory(testImgDir)) {
                        try (var files = Files.list(testImgDir)) {
                            for (Path f : files.toList()) Files.delete(f);
                        }
                        Files.delete(testImgDir);
                    }
                } catch (Exception ignored) {}
                createdPersonaId = null;
            }
            personaMapper.deleteById("test-persona-1");
            personaMapper.deleteById("test-persona-2");
        } catch (Exception ignored) {}
    }

    /**
     * ★ 核心验收：setAllOwnerQq 批量更新 owner_qq
     */
    @Test
    public void testSetAllOwnerQqUpdatesAll() {
        int count = personaService.setAllOwnerQq("1888888888");

        assertEquals(2, count, "应影响 2 条记录");

        Persona updated1 = personaMapper.selectById("test-persona-1");
        Persona updated2 = personaMapper.selectById("test-persona-2");

        assertEquals("1888888888", updated1.getOwnerQq());
        assertEquals("1888888888", updated2.getOwnerQq());
    }

    /**
     * findByAiQQ: 应该能找到
     */
    @Test
    public void testFindByAiQQFound() {
        Persona result = personaService.findByAiQQ("2387511709");

        assertNotNull(result);
        assertEquals("测试小奈", result.getName());
        assertEquals("1875552542", result.getOwnerQq());
    }

    /**
     * findByAiQQ: 不存在返回 null
     */
    @Test
    public void testFindByAiQQNotFound() {
        Persona result = personaService.findByAiQQ("0000000000");
        assertNull(result);
    }

    /**
     * exportConfig: 应包含 aiQq 和 ownerQq 字段
     */
    @Test
    public void testExportConfigContainsQqFields() {
        String json = personaService.exportConfig("test-persona-1");

        assertNotNull(json);
        assertTrue(json.contains("aiQq"), "导出 JSON 应包含 aiQq");
        assertTrue(json.contains("ownerQq"), "导出 JSON 应包含 ownerQq");
        assertTrue(json.contains("2387511709"), "导出 JSON 应包含 aiQq 值");
        assertTrue(json.contains("1875552542"), "导出 JSON 应包含 ownerQq 值");
    }

    /**
     * exportConfig: Persona 不存在返回 null
     */
    @Test
    public void testExportConfigNotFound() {
        String result = personaService.exportConfig("nonexistent");
        assertNull(result);
    }

    /**
     * getById: 正常获取
     */
    @Test
    public void testGetById() {
        Persona result = personaService.getById("test-persona-1");
        assertNotNull(result);
        assertEquals("测试小奈", result.getName());
    }

    // ==================== createPersona 事务测试 ====================

    /**
     * createPersona: 成功创建 Persona + LifeArchive
     */
    @Test
    public void testCreatePersonaCreatesBothTables() {
        PersonaConfigDTO config = new PersonaConfigDTO();
        config.setName("create测试");
        config.setBigFiveJson("{\"o\":0.8,\"c\":0.6}");
        config.setAttachmentAnxiety(0.4);
        config.setAttachmentAvoidance(0.2);
        config.setSelfEsteemStability(0.7);
        config.setSocialRhythm("night_owl");
        config.setConflictStyle("direct");
        config.setInitiativeTendency(0.5);
        config.setInputMethod("phone_thumb");
        config.setTypingSpeed(2.5);
        config.setAiQq("8888888888");
        config.setOwnerQq("9999999999");

        String lifeArchiveJson = "{\"birthplace\":\"测试城市\",\"education\":\"大学\"}";

        Persona created = personaService.createPersona(config, "sk-test-key", "test-master-key", lifeArchiveJson);
        createdPersonaId = created.getId();

        assertNotNull(createdPersonaId);
        assertEquals("create测试", created.getName());

        // 验证 persona 表
        Persona dbPersona = personaMapper.selectById(createdPersonaId);
        assertNotNull(dbPersona, "Persona 应写入数据库");
        assertEquals("create测试", dbPersona.getName());
        assertEquals("8888888888", dbPersona.getAiQq());
        assertEquals("9999999999", dbPersona.getOwnerQq());

        // 验证 life_archive 表
        CharacterLifeArchive archive = lifeArchiveMapper.selectById(createdPersonaId);
        assertNotNull(archive, "LifeArchive 应写入数据库");
        assertTrue(archive.getArchiveJson().contains("测试城市"));
    }

    /**
     * createPersona: API Key 加密存储
     */
    @Test
    public void testCreatePersonaEncryptsApiKey() {
        PersonaConfigDTO config = new PersonaConfigDTO();
        config.setName("加密测试");
        config.setBigFiveJson("{}");

        Persona created = personaService.createPersona(config, "sk-my-secret-key", "test-master-key", null);
        createdPersonaId = created.getId();

        Persona dbPersona = personaMapper.selectById(createdPersonaId);
        assertNotNull(dbPersona);
        assertNotNull(dbPersona.getApiKeyEncrypted());
        assertFalse(dbPersona.getApiKeyEncrypted().isEmpty(), "API Key 不应为空加密串");
        assertNotEquals("sk-my-secret-key", dbPersona.getApiKeyEncrypted(), "API Key 应被加密，不能存明文");
    }

    /**
     * createPersona: 无 lifeArchive → 只有 persona 表写入
     */
    @Test
    public void testCreatePersonaWithoutLifeArchive() {
        PersonaConfigDTO config = new PersonaConfigDTO();
        config.setName("无档案测试");
        config.setBigFiveJson("{}");

        Persona created = personaService.createPersona(config, "", "test-master-key", null);
        createdPersonaId = created.getId();

        Persona dbPersona = personaMapper.selectById(createdPersonaId);
        assertNotNull(dbPersona, "即使无 lifeArchive，persona 也应创建");
        assertEquals("无档案测试", dbPersona.getName());

        CharacterLifeArchive archive = lifeArchiveMapper.selectById(createdPersonaId);
        assertNull(archive, "无 lifeArchive 时不应有 archive 记录");
    }

    /**
     * createPersona: 空 API Key → 存空字符串
     */
    @Test
    public void testCreatePersonaEmptyApiKey() {
        PersonaConfigDTO config = new PersonaConfigDTO();
        config.setName("空Key测试");
        config.setBigFiveJson("{}");

        Persona created = personaService.createPersona(config, "", "test-master-key", null);
        createdPersonaId = created.getId();

        Persona dbPersona = personaMapper.selectById(createdPersonaId);
        assertEquals("", dbPersona.getApiKeyEncrypted(), "空 API Key 应存空字符串");
    }
}
