
package com.autotesthub.scenario.execution.controller;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.scenario.execution.service.ScenarioExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectScenarioTrendController {

    private final ScenarioExecutionService executionService;

    public ProjectScenarioTrendController(
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

    @GetMapping({"/scenario-execution-trends", "/{projectId}/scenario-execution-trends"})
    public ApiResponse<Map<String, Object>> getProjectExecutionTrends(
            @PathVariable(required = false) Long projectId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                executionService.getProjectExecutionTrends(
                        projectId,
                        getUserId(request),
                        startDate,
                        endDate
                )
        );
    }
}
