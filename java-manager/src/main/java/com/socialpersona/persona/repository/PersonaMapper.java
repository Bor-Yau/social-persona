package com.socialpersona.persona.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socialpersona.persona.entity.Persona;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人格 Mapper —— MyBatis-Plus BaseMapper 提供全部免费 CRUD
 *
 * ★ 为什么继承 BaseMapper<Persona> 就够用：
 *   BaseMapper 自带 insert / deleteById / updateById / selectById / selectList。
 *   9 成查询不需要手写 SQL。
 *
 * ★ 什么时候需要自定义方法：
 *   - 复杂 WHERE 条件（如 listActive: WHERE status='active'）
 *   - 联表查询（Day 以后加 @Select 注解或 XML）
 */
@Mapper
public interface PersonaMapper extends BaseMapper<Persona> {
}
