package com.socialpersona.matchmaker.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socialpersona.matchmaker.entity.MatchmakerSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MatchmakerSessionMapper extends BaseMapper<MatchmakerSession> {
}
