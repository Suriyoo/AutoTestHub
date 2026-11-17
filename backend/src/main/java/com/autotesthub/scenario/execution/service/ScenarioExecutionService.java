
package com.autotesthub.scenario.execution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.autotesthub.api.service.ApiExecutionService;
import com.autotesthub.environment.entity.TestEnvironment;
import com.autotesthub.environment.mapper.TestEnvironmentMapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import com.autotesthub.scenario.entity.TestScenario;
import com.autotesthub.scenario.entity.TestScenarioStep;
import com.autotesthub.scenario.execution.entity.ScenarioExecution;
import com.autotesthub.scenario.execution.entity.ScenarioExecutionStep;
import com.autotesthub.scenario.execution.mapper.ScenarioExecutionMapper;
import com.autotesthub.scenario.execution.mapper.ScenarioExecutionStepMapper;
import com.autotesthub.scenario.mapper.TestScenarioMapper;
import com.autotesthub.scenario.mapper.TestScenarioStepMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;

@Service
public class ScenarioExecutionService {

    private final TestScenarioMapper scenarioMapper;
    private final TestScenarioStepMapper scenarioStepMapper;
    private final ScenarioExecutionMapper executionMapper;
    private final ScenarioExecutionStepMapper executionStepMapper;
    private final ProjectMapper projectMapper;
    private final TestEnvironmentMapper environmentMapper;
    private final ApiExecutionService apiExecutionService;

    public ScenarioExecutionService(
            TestScenarioMapper scenarioMapper,
            TestScenarioStepMapper scenarioStepMapper,
            ScenarioExecutionMapper executionMapper,
            ScenarioExecutionStepMapper executionStepMapper,
            ProjectMapper projectMapper,
            TestEnvironmentMapper environmentMapper,
            ApiExecutionService apiExecutionService
    ) {
        this.scenarioMapper = scenarioMapper;
        this.scenarioStepMapper = scenarioStepMapper;
        this.executionMapper = executionMapper;
        this.executionStepMapper = executionStepMapper;
        this.projectMapper = projectMapper;
        this.environmentMapper = environmentMapper;
        this.apiExecutionService = apiExecutionService;
    }

    private TestScenario checkScenarioOwner(
            Long scenarioId,
            Long userId
    ) {
        TestScenario scenario = scenarioMapper.selectById(scenarioId);

        if (scenario == null) {
            throw new RuntimeException("场景不存在");
        }

        Project project = projectMapper.selectById(
                scenario.getProjectId());

        if (project == null
                || project.getOwnerId() == null
                || !project.getOwnerId().equals(userId)) {
            throw new RuntimeException("场景不存在");
        }

        return scenario;
    }

    public Map<String, Object> execute(
            Long scenarioId,
            Long environmentId,
            Long userId
    ) {
        TestScenario scenario = checkScenarioOwner(
                scenarioId, userId);

        if (environmentId == null) {
            throw new RuntimeException("请选择测试环境");
        }

        TestEnvironment environment =
                environmentMapper.selectById(environmentId);

        if (environment == null
                || !scenario.getProjectId().equals(
                        environment.getProjectId())) {
            throw new RuntimeException("测试环境不存在");
        }

        List<TestScenarioStep> steps =
                scenarioStepMapper.selectList(
                        new LambdaQueryWrapper<TestScenarioStep>()
                                .eq(TestScenarioStep::getScenarioId, scenarioId)
                                .orderByAsc(TestScenarioStep::getSortOrder)
                                .orderByAsc(TestScenarioStep::getId)
                );

        if (steps.isEmpty()) {
            throw new RuntimeException("场景中没有可执行的步骤");
        }

        // 记录场景开始时间
        long executionStartNanos = System.nanoTime();

        ScenarioExecution execution = new ScenarioExecution();

        execution.setScenarioId(scenarioId);
        execution.setProjectId(scenario.getProjectId());
        execution.setEnvironmentId(environmentId);
        execution.setStatus("RUNNING");
        execution.setTotalSteps(steps.size());
        execution.setPassedSteps(0);
        execution.setFailedSteps(0);
        execution.setStartedAt(LocalDateTime.now());

        executionMapper.insert(execution);

        int passedCount = 0;
        int failedCount = 0;

        for (TestScenarioStep scenarioStep : steps) {

            // 记录当前步骤开始时间
            long stepStartNanos = System.nanoTime();

            ScenarioExecutionStep executionStep =
                    new ScenarioExecutionStep();

            executionStep.setExecutionId(execution.getId());
            executionStep.setScenarioStepId(scenarioStep.getId());
            executionStep.setCaseId(scenarioStep.getCaseId());
            executionStep.setSortOrder(scenarioStep.getSortOrder());
            executionStep.setStatus("RUNNING");
            executionStep.setStartedAt(LocalDateTime.now());

            executionStepMapper.insert(executionStep);

            try {
                Map<String, Object> result =
                        apiExecutionService.execute(
                                scenarioStep.getCaseId(),
                                userId,
                                environmentId
                        );

                Object recordId = result.get("executionRecordId");

                if (recordId instanceof Number number) {
                    executionStep.setExecutionRecordId(
                            number.longValue());
                }

                boolean passed =
                        Boolean.TRUE.equals(result.get("passed"));

                if (passed) {
                    executionStep.setStatus("PASSED");
                    passedCount++;
                } else {
                    executionStep.setStatus("FAILED");
                    failedCount++;

                    Object error = result.get("errorMessage");

                    if (error != null) {
                        executionStep.setErrorMessage(
                                String.valueOf(error));
                    } else {
                        executionStep.setErrorMessage(
                                "API执行未通过，实际状态码："
                                        + result.get("actualStatus")
                                        + "，预期状态码："
                                        + result.get("expectedStatus")
                        );
                    }
                }

            } catch (Exception e) {

                // 当前步骤失败，继续执行后续步骤
                executionStep.setStatus("FAILED");

                executionStep.setErrorMessage(
                        e.getMessage() == null
                                ? "步骤执行异常"
                                : e.getMessage()
                );

                failedCount++;
            }

            // 保存步骤结束时间和耗时
            executionStep.setFinishedAt(LocalDateTime.now());

            executionStep.setDuration(
                    (System.nanoTime() - stepStartNanos) / 1_000_000
            );

            executionStepMapper.updateById(executionStep);
        }

        // 更新场景执行汇总
        execution.setPassedSteps(passedCount);
        execution.setFailedSteps(failedCount);

        execution.setStatus(
                failedCount == 0 ? "PASSED" : "FAILED"
        );

        execution.setFinishedAt(LocalDateTime.now());

        // 保存场景总耗时
        execution.setDuration(
                (System.nanoTime() - executionStartNanos) / 1_000_000
        );

        executionMapper.updateById(execution);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("executionId", execution.getId());
        response.put("scenarioId", scenarioId);
        response.put("scenarioName", scenario.getName());
        response.put("environmentId", environmentId);
        response.put("environmentName", environment.getName());
        response.put("status", execution.getStatus());
        response.put("totalSteps", steps.size());
        response.put("passedSteps", passedCount);
        response.put("failedSteps", failedCount);
        response.put("duration", execution.getDuration());
        response.put("steps", getExecutionSteps(execution.getId()));

        return response;
    }

    public List<ScenarioExecution> getExecutionHistory(
            Long scenarioId,
            Long userId
    ) {
        checkScenarioOwner(scenarioId, userId);

        return executionMapper.selectList(
                new LambdaQueryWrapper<ScenarioExecution>()
                        .eq(ScenarioExecution::getScenarioId, scenarioId)
                        .orderByDesc(ScenarioExecution::getId)
        );
    }

    private List<ScenarioExecutionStep> getExecutionSteps(
            Long executionId
    ) {
        return executionStepMapper.selectList(
                new LambdaQueryWrapper<ScenarioExecutionStep>()
                        .eq(ScenarioExecutionStep::getExecutionId, executionId)
                        .orderByAsc(ScenarioExecutionStep::getSortOrder)
                        .orderByAsc(ScenarioExecutionStep::getId)
        );
    }

    public Map<String, Object> getExecutionDetail(
            Long scenarioId,
            Long executionId,
            Long userId
    ) {
        checkScenarioOwner(scenarioId, userId);

        ScenarioExecution execution =
                executionMapper.selectById(executionId);

        if (execution == null
                || !scenarioId.equals(execution.getScenarioId())) {
            throw new RuntimeException("场景执行记录不存在");
        }

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("execution", execution);
        response.put("steps", getExecutionSteps(executionId));

        return response;
    }

    /**
 * 查询项目下所有场景的执行报告
 */
public List<Map<String, Object>> getProjectExecutionReports(
        Long projectId,
        Long userId
) {
    // 校验项目是否存在，以及当前用户是否为项目所有者
    Project project = projectMapper.selectById(projectId);

    if (project == null
            || project.getOwnerId() == null
            || !project.getOwnerId().equals(userId)) {
        throw new RuntimeException("项目不存在");
    }

    // 查询项目下所有场景执行记录，按执行 ID 倒序排列
    List<ScenarioExecution> executions =
            executionMapper.selectList(
                    new LambdaQueryWrapper<ScenarioExecution>()
                            .eq(ScenarioExecution::getProjectId, projectId)
                            .orderByDesc(ScenarioExecution::getId)
            );

    List<Map<String, Object>> reports = new ArrayList<>();

    for (ScenarioExecution execution : executions) {
        TestScenario scenario =
                scenarioMapper.selectById(execution.getScenarioId());

        TestEnvironment environment =
                environmentMapper.selectById(execution.getEnvironmentId());

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("execution", execution);
        item.put(
                "scenarioName",
                scenario == null ? "未知场景" : scenario.getName()
        );
        item.put(
                "environmentName",
                environment == null ? "未知环境" : environment.getName()
        );

        reports.add(item);
    }

    return reports;
}
public Map<String, Object> getProjectExecutionTrends(
        Long projectId,
        Long userId,
        LocalDate startDate,
        LocalDate endDate
) {
    Project project = projectMapper.selectById(projectId);

    if (project == null
            || project.getOwnerId() == null
            || !project.getOwnerId().equals(userId)) {
        throw new RuntimeException("项目不存在");
    }

    if (startDate == null || endDate == null) {
        throw new RuntimeException("请选择统计日期范围");
    }

    if (startDate.isAfter(endDate)) {
        throw new RuntimeException("开始日期不能晚于结束日期");
    }

    if (startDate.plusDays(90).isBefore(endDate)) {
        throw new RuntimeException("统计范围不能超过90天");
    }

    List<ScenarioExecution> executions =
            executionMapper.selectList(
                    new LambdaQueryWrapper<ScenarioExecution>()
                            .eq(ScenarioExecution::getProjectId, projectId)
                            .ge(
                                    ScenarioExecution::getStartedAt,
                                    startDate.atStartOfDay()
                            )
                            .lt(
                                    ScenarioExecution::getStartedAt,
                                    endDate.plusDays(1).atStartOfDay()
                            )
                            .orderByAsc(ScenarioExecution::getStartedAt)
            );

    Map<LocalDate, Map<String, Object>> dailyMap = new LinkedHashMap<>();

    for (LocalDate date = startDate;
         !date.isAfter(endDate);
         date = date.plusDays(1)) {

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("date", date.toString());
        item.put("executionCount", 0);
        item.put("passedCount", 0);
        item.put("failedCount", 0);
        item.put("passRate", 0);
        item.put("avgDuration", 0);

        dailyMap.put(date, item);
    }

    int totalCount = 0;
    int passedCount = 0;
    int failedCount = 0;

    Map<LocalDate, Long> durationTotal = new HashMap<>();
    Map<LocalDate, Integer> durationCount = new HashMap<>();

    for (ScenarioExecution execution : executions) {

        if (execution.getStartedAt() == null) {
            continue;
        }

        LocalDate date = execution.getStartedAt().toLocalDate();

        Map<String, Object> item = dailyMap.get(date);

        if (item == null) {
            continue;
        }

        totalCount++;

        int count = (int) item.get("executionCount");
        item.put("executionCount", count + 1);

        if ("PASSED".equals(execution.getStatus())) {
            passedCount++;

            int passed = (int) item.get("passedCount");
            item.put("passedCount", passed + 1);

        } else if ("FAILED".equals(execution.getStatus())) {
            failedCount++;

            int failed = (int) item.get("failedCount");
            item.put("failedCount", failed + 1);
        }

        if (execution.getDuration() != null
                && execution.getDuration() >= 0) {

            durationTotal.merge(
                    date,
                    execution.getDuration(),
                    Long::sum
            );

            durationCount.merge(
                    date,
                    1,
                    Integer::sum
            );
        }
    }

    for (Map.Entry<LocalDate, Map<String, Object>> entry
            : dailyMap.entrySet()) {

        Map<String, Object> item = entry.getValue();

        int dailyTotal = (int) item.get("executionCount");
        int dailyPassed = (int) item.get("passedCount");

        item.put(
                "passRate",
                dailyTotal == 0
                        ? 0
                        : Math.round(dailyPassed * 100.0 / dailyTotal)
        );

        int count = durationCount.getOrDefault(entry.getKey(), 0);

        item.put(
                "avgDuration",
                count == 0
                        ? 0
                        : durationTotal.get(entry.getKey()) / count
        );
    }

    Map<String, Object> result = new LinkedHashMap<>();

    result.put("totalCount", totalCount);
    result.put("passedCount", passedCount);
    result.put("failedCount", failedCount);

    result.put(
            "overallPassRate",
            totalCount == 0
                    ? 0
                    : Math.round(passedCount * 100.0 / totalCount)
    );

    result.put("startDate", startDate.toString());
    result.put("endDate", endDate.toString());
    result.put("trends", new ArrayList<>(dailyMap.values()));

    return result;
}
}
