package com.mentalhealth.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mentalhealth.assistant.entity.SessionMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMessageMapper extends BaseMapper<SessionMessage> {
}
