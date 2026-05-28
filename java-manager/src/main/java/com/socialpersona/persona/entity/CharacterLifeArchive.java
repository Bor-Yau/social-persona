package com.socialpersona.persona.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色人生档案实体 —— 映射 character_life_archives 表
 *
 * ★ 为什么独立于 personas 表：
 *   人生档案是 3~5KB 的大字段，创建后几乎不变。
 *   每次 SELECT Persona 不需要带着它，独立存储避免拖慢主查询。
 *
 * ★ 与 Persona 的关系：一对一
 */
@TableName("character_life_archives")
public class CharacterLifeArchive {

    /** 关联的 Persona ID（同时是主键，一对一） */
    @TableId
    private String personaId;

    /**
     * 完整人生档案 JSON
     * 结构：{name, birth_date, birth_place, family, childhood:[], adolescence:[],
     *        young_adult:[], future_milestones:[], current_context:{}, personality_shapers:[]}
     */
    private String archiveJson;

    private String createdAt;

    private String updatedAt;

    // ==================== Getter / Setter ====================

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getArchiveJson() { return archiveJson; }
    public void setArchiveJson(String archiveJson) { this.archiveJson = archiveJson; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
