
package com.autotesthub.scenario.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("scenario_execution_step")
public class ScenarioExecutionStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("execution_id")
    private Long executionId;

    @TableField("scenario_step_id")
    private Long scenarioStepId;

    @TableField("case_id")
    private Long caseId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("execution_record_id")
    private Long executionRecordId;

    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("duration")
    private Long duration;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public Long getScenarioStepId() { return scenarioStepId; }
    public void setScenarioStepId(Long scenarioStepId) { this.scenarioStepId = scenarioStepId; }

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Long getExecutionRecordId() { return executionRecordId; }
    public void setExecutionRecordId(Long executionRecordId) { this.executionRecordId = executionRecordId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
