
package com.autotesthub.scenario.execution.controller;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.scenario.execution.service.ScenarioExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectScenarioReportController {

    private final ScenarioExecutionService executionService;

    public ProjectScenarioReportController(
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

    /**
     * 查询项目下所有场景的执行报告
     */
    @GetMapping({"/scenario-executions", "/{projectId}/scenario-executions"})
    public ApiResponse<List<Map<String, Object>>> getProjectExecutionReports(
            @PathVariable(required = false) Long projectId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                executionService.getProjectExecutionReports(
                        projectId,
                        getUserId(request)
                )
        );
    }
}
