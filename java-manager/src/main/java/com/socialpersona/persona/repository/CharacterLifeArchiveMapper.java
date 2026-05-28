package com.socialpersona.persona.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socialpersona.persona.entity.CharacterLifeArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人生档案 Mapper —— extends BaseMapper<CharacterLifeArchive>
 *
 * 仅需基础 CRUD，无自定义查询需求。
 */
@Mapper
public interface CharacterLifeArchiveMapper extends BaseMapper<CharacterLifeArchive> {
}
