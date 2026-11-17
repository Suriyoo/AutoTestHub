
package com.autotesthub.api.controller;

import com.autotesthub.api.service.ApiExecutionService;
import com.autotesthub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class ApiExecutionController {

    private final ApiExecutionService apiExecutionService;

    public ApiExecutionController(
            ApiExecutionService apiExecutionService) {
        this.apiExecutionService = apiExecutionService;
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<Map<String, Object>> execute(
            @PathVariable Long id,
            @RequestParam(required = false) Long environmentId,
            HttpServletRequest request) {

        Object userIdObject = request.getAttribute("userId");

        if (!(userIdObject instanceof Number number)) {
            throw new RuntimeException("请先登录");
        }

        Long userId = number.longValue();

        Map<String, Object> result =
                apiExecutionService.execute(
                        id, userId, environmentId);

        return ApiResponse.ok(result);
    }
}
