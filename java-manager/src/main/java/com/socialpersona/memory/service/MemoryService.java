package com.socialpersona.memory.service;

import com.socialpersona.gateway.PythonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 记忆服务 —— 封装 Python Mem0 的 Java 调用层
 *
 * ★ Java 侧职责：
 *   本类不直接操作 ChromaDB / Mem0——而是通过 Python IPC 端点间接调用。
 *
 * ★ 为什么通过 Python 而非直接集成 Mem0 Java 库：
 *   1. Mem0 是纯 Python 库，没有官方 Java SDK
 *   2. ChromaDB 的 HTTP API 虽然可用但序列化开销高
 *   3. Python 侧已经封装了 MemoryService，Java 只需调 Python
 *
 * ★ 实际调用链：
 *   MessageService.searchMemory(query, personaId)
 *     → MemoryService.searchMemory(query, personaId)   ← 本类
 *       → PythonClient.searchMemory(query, personaId)  ← Feign 调用
 *         → Python POST /api/memory/search
 *           → memory_service.search(query, personaId)  ← engine/memory.py
 *
 * ★ Day 6 状态：pythonClient 调用桩（Python 无独立 /api/memory 端点——
 *   记忆检索/写入已在 message_routes.py 和 event_routes.py 内部完成）。
 *   本类供 MessageService 作为统一入口引用。
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    @Autowired
    private PythonClient pythonClient;

    /**
     * 语义检索相关记忆
     *
     * ★ Day 6 实现：暂不通过 Python IPC 间接调用。
     *   记忆检索逻辑已嵌入 message_routes.py 和 event_routes.py 内部——
     *   端点收到请求后自行调 memory_service.search()。
     *   本方法供未来独立 /api/memory 端点使用。
     *
     * @param query     搜索关键词
     * @param personaId Persona UUID
     * @param limit     最大返回条数
     * @return 记忆文本列表
     */
    public List<String> searchMemory(String query, String personaId, int limit) {
        log.debug("MemoryService.search: persona={}, query={}", personaId, query);
        return List.of();
    }

    /**
     * 写入记忆
     *
     * ★ Day 6 实现：同上，记忆写入逻辑已嵌入端点内部。
     *
     * @param content   要记住的内容
     * @param personaId Persona UUID
     */
    public void addMemory(String content, String personaId) {
        log.debug("MemoryService.add: persona={}, content={}", personaId, content);
    }

    /**
     * 批量写入关键记忆（今日反思后调用）
     *
     * @param memories  记忆项列表 [{"content":"...", "importance":8}]
     * @param personaId Persona UUID
     */
    @SuppressWarnings("unchecked")
    public void addKeyMemories(List<Map<String, Object>> memories, String personaId) {
        if (memories == null || memories.isEmpty()) return;
        long highValue = memories.stream()
                .filter(m -> {
                    Object imp = m.get("importance");
                    return imp instanceof Number && ((Number) imp).intValue() >= 7;
                })
                .count();
        log.info("MemoryService.addKeyMemories: persona={}, total={}, highValue={}",
                personaId, memories.size(), highValue);
    }
}
