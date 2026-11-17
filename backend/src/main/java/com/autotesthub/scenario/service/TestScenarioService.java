package com.autotesthub.scenario.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import com.autotesthub.api.entity.ApiCase;
import com.autotesthub.api.mapper.ApiCaseMapper;
import com.autotesthub.scenario.dto.ScenarioStepOrderItem;
import com.autotesthub.scenario.dto.TestScenarioRequest;
import com.autotesthub.scenario.dto.TestScenarioUpdateRequest;
import com.autotesthub.scenario.entity.TestScenario;
import com.autotesthub.scenario.entity.TestScenarioStep;
import com.autotesthub.scenario.mapper.TestScenarioMapper;
import com.autotesthub.scenario.mapper.TestScenarioStepMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TestScenarioService {

    private final TestScenarioMapper scenarioMapper;
    private final TestScenarioStepMapper stepMapper;
    private final ProjectMapper projectMapper;
    private final ApiCaseMapper apiCaseMapper;

    public TestScenarioService(
            TestScenarioMapper scenarioMapper,
            TestScenarioStepMapper stepMapper,
            ProjectMapper projectMapper,
            ApiCaseMapper apiCaseMapper
    ) {
        this.scenarioMapper = scenarioMapper;
        this.stepMapper = stepMapper;
        this.projectMapper = projectMapper;
        this.apiCaseMapper = apiCaseMapper;
    }

    // 校验项目是否属于当前用户
    private Project checkProjectOwner(Long projectId, Long userId) {
        Project project = projectMapper.selectById(projectId);

        if (project == null || !userId.equals(project.getOwnerId())) {
            throw new RuntimeException("项目不存在");
        }

        return project;
    }

    // 查询场景并校验权限
    private TestScenario checkScenarioOwner(Long scenarioId, Long userId) {
        TestScenario scenario = scenarioMapper.selectById(scenarioId);

        if (scenario == null) {
            throw new RuntimeException("场景不存在");
        }

        checkProjectOwner(scenario.getProjectId(), userId);

        return scenario;
    }

    // 创建场景
    public TestScenario create(TestScenarioRequest request, Long userId) {

        checkProjectOwner(request.projectId(), userId);

        TestScenario scenario = new TestScenario();
        scenario.setProjectId(request.projectId());
        scenario.setName(request.name());
        scenario.setDescription(request.description());

        scenarioMapper.insert(scenario);

        return scenario;
    }

    // 查询项目下的场景列表
    public List<TestScenario> list(Long projectId, Long userId) {
        LambdaQueryWrapper<TestScenario> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            checkProjectOwner(projectId, userId);
            wrapper.eq(TestScenario::getProjectId, projectId);
        } else {
            List<Long> ownedProjectIds = projectMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Project>()
                            .eq("owner_id", userId)
            ).stream().map(Project::getId).toList();
            if (ownedProjectIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(TestScenario::getProjectId, ownedProjectIds);
        }
        return scenarioMapper.selectList(wrapper.orderByDesc(TestScenario::getId));
    }

    // 查询场景详情
    public List<TestScenarioStep> getSteps(Long scenarioId, Long userId) {

        checkScenarioOwner(scenarioId, userId);

        return stepMapper.selectList(
                new LambdaQueryWrapper<TestScenarioStep>()
                        .eq(TestScenarioStep::getScenarioId, scenarioId)
                        .orderByAsc(TestScenarioStep::getSortOrder)
                        .orderByAsc(TestScenarioStep::getId)
        );
    }

    // 查询单个场景
    public TestScenario getById(Long scenarioId, Long userId) {
        return checkScenarioOwner(scenarioId, userId);
    }

    // 修改场景
    public TestScenario update(
            Long scenarioId,
            TestScenarioUpdateRequest request,
            Long userId
    ) {
        TestScenario scenario = checkScenarioOwner(scenarioId, userId);

        scenario.setName(request.name());
        scenario.setDescription(request.description());

        scenarioMapper.updateById(scenario);

        return scenarioMapper.selectById(scenarioId);
    }

    // 删除场景及其步骤
    @Transactional
    public void delete(Long scenarioId, Long userId) {

        checkScenarioOwner(scenarioId, userId);

        stepMapper.delete(
                new LambdaQueryWrapper<TestScenarioStep>()
                        .eq(TestScenarioStep::getScenarioId, scenarioId)
        );

        scenarioMapper.deleteById(scenarioId);
    }

    // 添加场景步骤
    @Transactional
    public TestScenarioStep addStep(
            Long scenarioId,
            Long caseId,
            Long userId
    ) {
        TestScenario scenario = checkScenarioOwner(scenarioId, userId);

        ApiCase apiCase = apiCaseMapper.selectById(caseId);

        if (apiCase == null) {
            throw new RuntimeException("用例不存在");
        }

        if (!scenario.getProjectId().equals(apiCase.getProjectId())) {
            throw new RuntimeException("不允许修改用例所属项目");
        }

        TestScenarioStep lastStep = stepMapper.selectOne(
                new LambdaQueryWrapper<TestScenarioStep>()
                        .eq(TestScenarioStep::getScenarioId, scenarioId)
                        .orderByDesc(TestScenarioStep::getSortOrder)
                        .last("LIMIT 1")
        );

        int nextOrder = lastStep == null ? 1 : lastStep.getSortOrder() + 1;

        TestScenarioStep step = new TestScenarioStep();
        step.setScenarioId(scenarioId);
        step.setCaseId(caseId);
        step.setSortOrder(nextOrder);

        stepMapper.insert(step);

        return step;
    }

    // 修改步骤顺序
    @Transactional
    public List<TestScenarioStep> updateStepOrder(
            Long scenarioId,
            List<ScenarioStepOrderItem> items,
            Long userId
    ) {
        checkScenarioOwner(scenarioId, userId);

        Set<Long> stepIds = new HashSet<>();

        for (ScenarioStepOrderItem item : items) {

            if (!stepIds.add(item.stepId())) {
                throw new RuntimeException("步骤排序参数错误");
            }

            TestScenarioStep step = stepMapper.selectById(item.stepId());

            if (step == null || !scenarioId.equals(step.getScenarioId())) {
                throw new RuntimeException("场景步骤不存在");
            }

            step.setSortOrder(item.sortOrder());
            stepMapper.updateById(step);
        }

        return getSteps(scenarioId, userId);
    }

    // 删除场景步骤
    public void deleteStep(
            Long scenarioId,
            Long stepId,
            Long userId
    ) {
        checkScenarioOwner(scenarioId, userId);

        TestScenarioStep step = stepMapper.selectById(stepId);

        if (step == null || !scenarioId.equals(step.getScenarioId())) {
            throw new RuntimeException("场景步骤不存在");
        }

        stepMapper.deleteById(stepId);
    }
}