package com.autotesthub.scenario.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.autotesthub.scenario.entity.TestScenario;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestScenarioMapper extends BaseMapper<TestScenario> {
}