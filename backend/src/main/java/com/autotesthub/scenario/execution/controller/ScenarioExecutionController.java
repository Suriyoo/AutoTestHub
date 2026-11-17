
package com.autotesthub.scenario.execution.controller;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.scenario.execution.entity.ScenarioExecution;
import com.autotesthub.scenario.execution.service.ScenarioExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioExecutionController {

    private final ScenarioExecutionService executionService;

    public ScenarioExecutionController(
            ScenarioExecutionService executionService
    ) {
        this.executionService = executionService;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");

        if (!(userId instanceof Number number)) {
            throw new RuntimeException("请先登录");
        }

        return number.longValue();
    }

    // 执行整个场景
    @PostMapping("/{id}/execute")
    public ApiResponse<Map<String, Object>> execute(
            @PathVariable Long id,
            @RequestParam Long environmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                executionService.execute(
                        id,
                        environmentId,
                        getUserId(request)
                )
        );
    }

    // 查询场景执行历史
    @GetMapping("/{id}/executions")
    public ApiResponse<List<ScenarioExecution>> getExecutionHistory(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                executionService.getExecutionHistory(
                        id,
                        getUserId(request)
                )
        );
    }

    // 查询单次场景执行详情
    @GetMapping("/{id}/executions/{executionId}")
    public ApiResponse<Map<String, Object>> getExecutionDetail(
            @PathVariable Long id,
            @PathVariable Long executionId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                executionService.getExecutionDetail(
                        id,
                        executionId,
                        getUserId(request)
                )
        );
    }
}
