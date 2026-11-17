
package com.autotesthub.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.autotesthub.api.entity.ApiExecutionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiExecutionRecordMapper
        extends BaseMapper<ApiExecutionRecord> {
}
