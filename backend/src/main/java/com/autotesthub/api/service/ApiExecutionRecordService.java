
package com.autotesthub.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.autotesthub.api.entity.ApiExecutionRecord;
import com.autotesthub.api.mapper.ApiExecutionRecordMapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiExecutionRecordService {

    private final ApiExecutionRecordMapper recordMapper;
    private final ProjectMapper projectMapper;

    public ApiExecutionRecordService(
            ApiExecutionRecordMapper recordMapper,
            ProjectMapper projectMapper) {
        this.recordMapper = recordMapper;
        this.projectMapper = projectMapper;
    }

    /**
     * 查询项目执行历史，最多返回最近100条。
     */
    public List<ApiExecutionRecord> list(
            Long projectId, Long caseId, Long userId) {

        checkProjectOwner(projectId, userId);

        LambdaQueryWrapper<ApiExecutionRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(ApiExecutionRecord::getProjectId, projectId);

        if (caseId != null) {
            wrapper.eq(ApiExecutionRecord::getCaseId, caseId);
        }

        wrapper.orderByDesc(ApiExecutionRecord::getExecutedAt)
                .orderByDesc(ApiExecutionRecord::getId)
                .last("LIMIT 100");

        return recordMapper.selectList(wrapper);
    }

    /**
     * 查询单条执行记录详情。
     */
    public ApiExecutionRecord getById(Long id, Long userId) {

        ApiExecutionRecord record = recordMapper.selectById(id);

        if (record == null) {
            throw new RuntimeException("执行记录不存在");
        }

        checkProjectOwner(record.getProjectId(), userId);

        return record;
    }

    /**
     * 检查项目是否属于当前用户。
     */
    private void checkProjectOwner(Long projectId, Long userId) {

        Project project = projectMapper.selectById(projectId);

        if (project == null
                || project.getOwnerId() == null
                || !project.getOwnerId().equals(userId)) {
            throw new RuntimeException("项目不存在");
        }
    }
}
