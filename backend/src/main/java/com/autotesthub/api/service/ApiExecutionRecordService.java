package com.autotesthub.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.autotesthub.api.entity.ApiExecutionRecord;
import com.autotesthub.api.mapper.ApiExecutionRecordMapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
     * 查询当前用户的执行历史，最多返回最近100条。
     * 不传 projectId 时，仅查询当前用户拥有的项目。
     */
    public List<ApiExecutionRecord> list(
            Long projectId,
            Long caseId,
            String result,
            LocalDate startDate,
            LocalDate endDate,
            Long userId) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }

        LambdaQueryWrapper<ApiExecutionRecord> wrapper =
                new LambdaQueryWrapper<>();

        if (projectId != null) {
            checkProjectOwner(projectId, userId);
            wrapper.eq(ApiExecutionRecord::getProjectId, projectId);
        } else {
            List<Long> projectIds = projectMapper.selectList(
                    new LambdaQueryWrapper<Project>()
                            .select(Project::getId)
                            .eq(Project::getOwnerId, userId)
            ).stream().map(Project::getId).toList();

            if (projectIds.isEmpty()) {
                return List.of();
            }

            wrapper.in(ApiExecutionRecord::getProjectId, projectIds);
        }

        if (caseId != null) {
            wrapper.eq(ApiExecutionRecord::getCaseId, caseId);
        }

        if (result != null && !result.isBlank()) {
            switch (result) {
                case "PASSED" -> wrapper
                        .eq(ApiExecutionRecord::getSuccess, 1)
                        .eq(ApiExecutionRecord::getPassed, 1);
                case "ASSERTION_FAILED" -> wrapper
                        .eq(ApiExecutionRecord::getSuccess, 1)
                        .eq(ApiExecutionRecord::getPassed, 0);
                case "REQUEST_FAILED" -> wrapper
                        .eq(ApiExecutionRecord::getSuccess, 0);
                default -> throw new RuntimeException("不支持的执行结果筛选条件");
            }
        }

        if (startDate != null) {
            wrapper.ge(
                    ApiExecutionRecord::getExecutedAt,
                    startDate.atStartOfDay()
            );
        }

        if (endDate != null) {
            wrapper.lt(
                    ApiExecutionRecord::getExecutedAt,
                    endDate.plusDays(1).atStartOfDay()
            );
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
