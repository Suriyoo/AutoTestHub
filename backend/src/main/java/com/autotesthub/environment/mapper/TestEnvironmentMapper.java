package com.autotesthub.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.autotesthub.environment.entity.TestEnvironment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestEnvironmentMapper
        extends BaseMapper<TestEnvironment> {
}