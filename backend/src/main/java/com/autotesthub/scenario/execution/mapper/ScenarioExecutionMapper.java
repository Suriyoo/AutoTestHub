
package com.autotesthub.scenario.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.autotesthub.scenario.execution.entity.ScenarioExecution;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScenarioExecutionMapper
        extends BaseMapper<ScenarioExecution> {
}
