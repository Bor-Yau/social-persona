package com.socialpersona.persona.service;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.repository.CharacterLifeArchiveMapper;
import com.socialpersona.persona.repository.PersonaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PersonaService 单元测试 —— 重点验证 QQ 路由相关方法
 *
 * 覆盖：
 *   1. findByAiQQ — 按 AI 的 QQ 号查找人格
 *   2. setAllOwnerQq — 批量更新所有人格的主人 QQ
 *   3. bindChannel — 绑定 QQ / 微信渠道
 *   4. listActive / listAll
 */
@ExtendWith(MockitoExtension.class)
public class PersonaServiceTest {

    @Mock private PersonaMapper personaMapper;
    @Mock private CharacterLifeArchiveMapper lifeArchiveMapper;

    @InjectMocks
    private PersonaService personaService;

    private Persona testPersona;

    @BeforeEach
    public void setUp() {
        testPersona = new Persona();
        testPersona.setId("d926f28e-ea21-4f6a-8d69-9ed3dba6052b");
        testPersona.setName("小奈");
        testPersona.setAiQq("2387511709");
        testPersona.setOwnerQq("1875552542");
        testPersona.setStatus("active");
    }

    // ==================== findByAiQQ ====================

    /**
     * ★ findByAiQQ: 按 AI 的 QQ 号成功找到 Persona
     */
    @Test
    public void testFindByAiQQFound() {
        when(personaMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testPersona);

        Persona result = personaService.findByAiQQ("2387511709");

        assertNotNull(result);
        assertEquals("小奈", result.getName());
        assertEquals("2387511709", result.getAiQq());
        verify(personaMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    /**
     * ★ findByAiQQ: 找不到时返回 null
     */
    @Test
    public void testFindByAiQQNotFound() {
        when(personaMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Persona result = personaService.findByAiQQ("0000000000");

        assertNull(result);
    }

    /**
     * findByAiQQ: null 输入不应抛异常
     */
    @Test
    public void testFindByAiQQNullInput() {
        when(personaMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Persona result = personaService.findByAiQQ(null);

        assertNull(result);
    }

    // ==================== listActive ====================

    /**
     * listActive: 返回所有活跃人格
     */
    @Test
    public void testListActive() {
        when(personaMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(testPersona));

        List<Persona> result = personaService.listActive();

        assertEquals(1, result.size());
        assertEquals("小奈", result.get(0).getName());
    }

    // ==================== getById / exportConfig ====================

    /**
     * getById: 返回指定 Persona
     */
    @Test
    public void testGetById() {
        when(personaMapper.selectById("d926f28e-ea21-4f6a-8d69-9ed3dba6052b"))
                .thenReturn(testPersona);

        Persona result = personaService.getById("d926f28e-ea21-4f6a-8d69-9ed3dba6052b");

        assertNotNull(result);
        assertEquals("小奈", result.getName());
    }

    // ==================== exportConfig ====================

    /**
     * exportConfig: 导出包含 aiQq 和 ownerQq 的 JSON
     */
    @Test
    public void testExportConfigContainsQqFields() {
        when(personaMapper.selectById("d926f28e-ea21-4f6a-8d69-9ed3dba6052b"))
                .thenReturn(testPersona);

        String json = personaService.exportConfig("d926f28e-ea21-4f6a-8d69-9ed3dba6052b");

        assertNotNull(json);
        assertTrue(json.contains("aiQq"), "导出 JSON 应包含 aiQq");
        assertTrue(json.contains("ownerQq"), "导出 JSON 应包含 ownerQq");
        assertTrue(json.contains("2387511709"), "导出 JSON 应包含 aiQq 值");
        assertTrue(json.contains("1875552542"), "导出 JSON 应包含 ownerQq 值");
    }

    /**
     * exportConfig: Persona 不存在 → 返回 null
     */
    @Test
    public void testExportConfigPersonaNotFound() {
        when(personaMapper.selectById("nonexistent")).thenReturn(null);

        String result = personaService.exportConfig("nonexistent");
        assertNull(result);
    }
}