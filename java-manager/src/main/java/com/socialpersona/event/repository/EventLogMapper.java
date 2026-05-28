package com.socialpersona.event.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socialpersona.event.entity.EventLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventLogMapper extends BaseMapper<EventLog> {
}
