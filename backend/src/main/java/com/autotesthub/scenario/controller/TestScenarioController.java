package com.autotesthub.scenario.controller;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.scenario.dto.ScenarioStepOrderRequest;
import com.autotesthub.scenario.dto.ScenarioStepRequest;
import com.autotesthub.scenario.dto.TestScenarioRequest;
import com.autotesthub.scenario.dto.TestScenarioUpdateRequest;
import com.autotesthub.scenario.entity.TestScenario;
import com.autotesthub.scenario.entity.TestScenarioStep;
import com.autotesthub.scenario.service.TestScenarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
public class TestScenarioController {

    private final TestScenarioService scenarioService;

    public TestScenarioController(TestScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("请先登录");
        }

        return ((Number) userId).longValue();
    }

    // 创建场景
    @PostMapping
    public ApiResponse<TestScenario> create(
            @Valid @RequestBody TestScenarioRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                scenarioService.create(request, getUserId(httpRequest))
        );
    }

    // 查询场景列表
    @GetMapping
    public ApiResponse<List<TestScenario>> list(
            @RequestParam Long projectId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                scenarioService.list(projectId, getUserId(request))
        );
    }

    // 查询场景详情及步骤
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);

        TestScenario scenario = scenarioService.getById(id, userId);
        List<TestScenarioStep> steps = scenarioService.getSteps(id, userId);

        return ApiResponse.ok(Map.of(
                "scenario", scenario,
                "steps", steps
        ));
    }

    // 修改场景
    @PutMapping("/{id}")
    public ApiResponse<TestScenario> update(
            @PathVariable Long id,
            @Valid @RequestBody TestScenarioUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                scenarioService.update(id, request, getUserId(httpRequest))
        );
    }

    // 删除场景
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        scenarioService.delete(id, getUserId(request));
        return ApiResponse.ok(null);
    }

    // 添加场景步骤
    @PostMapping("/{id}/steps")
    public ApiResponse<TestScenarioStep> addStep(
            @PathVariable Long id,
            @Valid @RequestBody ScenarioStepRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                scenarioService.addStep(
                        id,
                        request.caseId(),
                        getUserId(httpRequest)
                )
        );
    }

    // 调整场景步骤顺序
    @PutMapping("/{id}/steps")
    public ApiResponse<List<TestScenarioStep>> updateStepOrder(
            @PathVariable Long id,
            @Valid @RequestBody ScenarioStepOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                scenarioService.updateStepOrder(
                        id,
                        request.steps(),
                        getUserId(httpRequest)
                )
        );
    }

    // 删除场景步骤
    @DeleteMapping("/{id}/steps/{stepId}")
    public ApiResponse<Void> deleteStep(
            @PathVariable Long id,
            @PathVariable Long stepId,
            HttpServletRequest request
    ) {
        scenarioService.deleteStep(id, stepId, getUserId(request));
        return ApiResponse.ok(null);
    }
}