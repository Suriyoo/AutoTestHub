
package com.autotesthub.api.controller;

import com.autotesthub.api.dto.ApiCaseRequest;
import com.autotesthub.api.entity.ApiCase;
import com.autotesthub.api.service.ApiCaseService;
import com.autotesthub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
public class ApiCaseController {

    private final ApiCaseService apiCaseService;

    public ApiCaseController(ApiCaseService apiCaseService) {
        this.apiCaseService = apiCaseService;
    }

    // 从 JWT 过滤器写入的请求属性中获取当前用户 ID
    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("请先登录");
        }

        if (userId instanceof Number number) {
            return number.longValue();
        }

        throw new RuntimeException("用户身份信息无效");
    }

    // 新增用例
    @PostMapping
    public ApiResponse<ApiCase> create(
            @Valid @RequestBody ApiCaseRequest apiCaseRequest,
            HttpServletRequest request) {

        return ApiResponse.ok(
                apiCaseService.create(apiCaseRequest, getUserId(request))
        );
    }

    // 查询项目下的用例列表
    @GetMapping
    public ApiResponse<List<ApiCase>> list(
            @RequestParam Long projectId,
            HttpServletRequest request) {

        return ApiResponse.ok(
                apiCaseService.list(projectId, getUserId(request))
        );
    }

    // 查询用例详情
    @GetMapping("/{id}")
    public ApiResponse<ApiCase> detail(
            @PathVariable Long id,
            HttpServletRequest request) {

        return ApiResponse.ok(
                apiCaseService.getById(id, getUserId(request))
        );
    }

    // 修改用例
    @PutMapping("/{id}")
    public ApiResponse<ApiCase> update(
            @PathVariable Long id,
            @Valid @RequestBody ApiCaseRequest apiCaseRequest,
            HttpServletRequest request) {

        return ApiResponse.ok(
                apiCaseService.update(id, apiCaseRequest, getUserId(request))
        );
    }

    // 删除用例
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request) {

        apiCaseService.delete(id, getUserId(request));

        return ApiResponse.ok(null);
    }
}
