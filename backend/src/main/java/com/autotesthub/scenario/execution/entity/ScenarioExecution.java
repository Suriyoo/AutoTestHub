
package com.autotesthub.scenario.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("scenario_execution")
public class ScenarioExecution {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scenario_id")
    private Long scenarioId;

    @TableField("project_id")
    private Long projectId;

    @TableField("environment_id")
    private Long environmentId;

    private String status;

    @TableField("total_steps")
    private Integer totalSteps;

    @TableField("passed_steps")
    private Integer passedSteps;

    @TableField("failed_steps")
    private Integer failedSteps;

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

    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(Long environmentId) { this.environmentId = environmentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalSteps() { return totalSteps; }
    public void setTotalSteps(Integer totalSteps) { this.totalSteps = totalSteps; }

    public Integer getPassedSteps() { return passedSteps; }
    public void setPassedSteps(Integer passedSteps) { this.passedSteps = passedSteps; }

    public Integer getFailedSteps() { return failedSteps; }
    public void setFailedSteps(Integer failedSteps) { this.failedSteps = failedSteps; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
