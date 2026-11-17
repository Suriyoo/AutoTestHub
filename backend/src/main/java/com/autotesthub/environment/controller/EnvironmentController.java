package com.autotesthub.environment.controller;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.environment.dto.EnvironmentRequest;
import com.autotesthub.environment.entity.TestEnvironment;
import com.autotesthub.environment.service.EnvironmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userIdObject = request.getAttribute("userId");

        if (!(userIdObject instanceof Number number)) {
            throw new RuntimeException("请先登录");
        }

        return number.longValue();
    }

    @PostMapping
    public ApiResponse<TestEnvironment> create(
            @Valid @RequestBody EnvironmentRequest body,
            HttpServletRequest request) {

        return ApiResponse.ok(
                environmentService.create(body, getUserId(request))
        );
    }

    @GetMapping
    public ApiResponse<List<TestEnvironment>> list(
            @RequestParam Long projectId,
            HttpServletRequest request) {

        return ApiResponse.ok(
                environmentService.list(projectId, getUserId(request))
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<TestEnvironment> detail(
            @PathVariable Long id,
            HttpServletRequest request) {

        return ApiResponse.ok(
                environmentService.getById(id, getUserId(request))
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<TestEnvironment> update(
            @PathVariable Long id,
            @Valid @RequestBody EnvironmentRequest body,
            HttpServletRequest request) {

        return ApiResponse.ok(
                environmentService.update(id, body, getUserId(request))
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request) {

        environmentService.delete(id, getUserId(request));
        return ApiResponse.ok(null);
    }
}