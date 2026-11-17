
package com.autotesthub.scenario.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.autotesthub.scenario.execution.entity.ScenarioExecutionStep;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScenarioExecutionStepMapper
        extends BaseMapper<ScenarioExecutionStep> {
}
