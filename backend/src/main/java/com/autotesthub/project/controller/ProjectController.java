
package com.autotesthub.project.controller;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.project.dto.ProjectRequest;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // 从 JWT Filter 中获取当前登录用户 ID
    private Long getCurrentUserId(HttpServletRequest request) {

        Object userId = request.getAttribute("userId");

        if (!(userId instanceof Number)) {
            throw new RuntimeException("请先登录");
        }

        return ((Number) userId).longValue();
    }

    // 新增项目
    @PostMapping
    public ApiResponse<Project> create(
            @Valid @RequestBody ProjectRequest request,
            HttpServletRequest httpRequest
    ) {

        Long ownerId = getCurrentUserId(httpRequest);

        return ApiResponse.ok(
                projectService.create(request, ownerId)
        );
    }

    // 查询项目列表
    @GetMapping
    public ApiResponse<List<Project>> list(
            HttpServletRequest request
    ) {

        Long ownerId = getCurrentUserId(request);

        return ApiResponse.ok(
                projectService.list(ownerId)
        );
    }

    // 查询项目详情
    @GetMapping("/{id}")
    public ApiResponse<Project> detail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        Long ownerId = getCurrentUserId(request);

        return ApiResponse.ok(
                projectService.getById(id, ownerId)
        );
    }

    // 修改项目
    @PutMapping("/{id}")
    public ApiResponse<Project> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest projectRequest,
            HttpServletRequest request
    ) {

        Long ownerId = getCurrentUserId(request);

        return ApiResponse.ok(
                projectService.update(id, projectRequest, ownerId)
        );
    }

    // 删除项目
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        Long ownerId = getCurrentUserId(request);

        projectService.delete(id, ownerId);

        return ApiResponse.ok(null);
    }
}
